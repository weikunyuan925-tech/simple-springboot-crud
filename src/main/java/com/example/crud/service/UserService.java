package com.example.crud.service;

import com.example.crud.dto.UserCreateRequest;
import com.example.crud.dto.UserDTO;
import com.example.crud.entity.SysUser;
import com.example.crud.entity.User;
import com.example.crud.exception.EmailAlreadyExistsException;
import com.example.crud.exception.UserNotFoundException;
import com.example.crud.repository.SysUserRepository;
import com.example.crud.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final SysUserRepository sysUserRepository;

    @Autowired
    public UserService(UserRepository userRepository, SysUserRepository sysUserRepository) {
        this.userRepository = userRepository;
        this.sysUserRepository = sysUserRepository;
    }

    // 获取当前登录账号对应的 SysUser
    private SysUser getCurrentOwner() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            throw new AccessDeniedException("未获取到有效的登录身份");
        }

        return sysUserRepository.findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new AccessDeniedException("当前登录账号不存在"));
    }

    // 分页查询：只能查询当前账号拥有的资料
    @Transactional(readOnly = true)
    public Page<UserDTO> getAllUsers(int page, int size) {

        SysUser owner = getCurrentOwner();
        Pageable pageable = PageRequest.of(page, size);

        return userRepository
                .findAllByOwner_IdOrderByCreatedAtDesc(owner.getId(), pageable)
                .map(this::convertToDTO);
    }

    /*@Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAllOrderByCreatedAtDesc()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }*/
    // 查询单条资料：同时验证 ID 和所有者
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        SysUser owner = getCurrentOwner();
        User user = userRepository.findByIdAndOwner_Id(id, owner.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        return convertToDTO(user);
    }

    // 按邮箱查询：只能查询当前账号拥有的资料
    @Transactional(readOnly = true)
    public UserDTO getUserByEmail(String email) {
        SysUser owner = getCurrentOwner();
        User user = userRepository.findByEmailAndOwner_Id(email, owner.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        return convertToDTO(user);
    }

    /*@Transactional(readOnly = true)
    public List<UserDTO> searchUsersByName(String name) {
        return userRepository.findByNameContaining(name)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }*/

    /* @Transactional(readOnly = true)
     public List<UserDTO> searchUsersByName(String name) {

         // 防止空搜索条件导致返回全部用户
         if (name == null || name.isBlank()) {
             return List.of();
         }

         // 转义 LIKE 特殊字符
         String escapedName = escapeLikeWildcards(name);

         // 保留普通的前后模糊匹配
         String pattern = "%" + escapedName + "%";

         return userRepository.searchByNamePattern(pattern)
                 .stream()
                 .map(this::convertToDTO)
                 .collect(Collectors.toList());
     }*/
    // 搜索：搜索范围限制在当前账号
    @Transactional(readOnly = true)
    public Page<UserDTO> searchUsersByName(String name, int page, int size) {
        SysUser owner = getCurrentOwner();
        Pageable pageable = PageRequest.of(page, size);

        // 防止空搜索条件导致返回全部用户
        if (name == null || name.isBlank()) {
            return Page.empty(pageable);
        }

        // 转义 LIKE 特殊字符
        String escapedName = escapeLikeWildcards(name);

        // 保留普通的前后模糊匹配
        String pattern = "%" + escapedName + "%";

        return userRepository.searchByOwnerIdAndNamePattern(owner.getId(), pattern, pageable)
                .map(this::convertToDTO);
    }

    //! 是定义的转义字符，所以必须先转义它本身，避免后续产生的转义字符再次被处理。
    private String escapeLikeWildcards(String value) {

        return value
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }

    /*public UserDTO createUser(UserDTO userDTO) {
        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new IllegalArgumentException("User with email " + userDTO.getEmail() + " already exists");
        }

        User user = convertToEntity(userDTO);
        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }*/
// 创建资料：所有者由服务端从登录身份确定
    public UserDTO createUser(UserCreateRequest request) {
        SysUser owner = getCurrentOwner();
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("邮箱已注册");
        }
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setOwner(owner);
        User savedUser = userRepository.save(user);

        return convertToDTO(savedUser);
    }

    // 更新资料：只能更新当前账号自己的记录
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        SysUser owner = getCurrentOwner();
        User existingUser = userRepository.findByIdAndOwner_Id(id, owner.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

       /* if (userRepository.existsByEmail(userDTO.getEmail()) && !Objects.equals(userDTO.getEmail(), existingUser.getEmail())) {
            throw new EmailAlreadyExistsException("邮箱已注册");
        }*/
        //与上面的写法的区别，不需要先查询邮箱是否存在，再进行 Java 层比较，另外，数据库唯一约束仍然必须保留。应用层检查不能完全防止两个并发请求同时通过检查的问题。
        if (userRepository.existsByEmailAndIdNot(
                userDTO.getEmail(), existingUser.getId())) {
            throw new EmailAlreadyExistsException("邮箱已注册");
        }

        existingUser.setName(userDTO.getName());
        existingUser.setEmail(userDTO.getEmail());
        existingUser.setPhone(userDTO.getPhone());

        User updatedUser = userRepository.save(existingUser);
        return convertToDTO(updatedUser);
    }

    // 删除资料：只能删除当前账号自己的记录
    public void deleteUser(Long id) {
        SysUser owner = getCurrentOwner();
        User existingUser = userRepository.findByIdAndOwner_Id(id, owner.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        userRepository.delete(existingUser);
    }

    private UserDTO convertToDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

}