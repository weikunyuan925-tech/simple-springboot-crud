package com.example.crud.service;

import com.example.crud.dto.UserCreateRequest;
import com.example.crud.dto.UserDTO;
import com.example.crud.entity.User;
import com.example.crud.exception.EmailAlreadyExistsException;
import com.example.crud.exception.UserNotFoundException;
import com.example.crud.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    private UserCreateRequest request;


    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserDTO testUserDTO;
    private UserCreateRequest Request;

    @BeforeEach
    void setUp() {
        testUser = new User("John Doe", "john@example.com", "010-1234-5678");
        testUser.setId(1L);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        testUserDTO = new UserDTO();
        testUserDTO.setName("John Doe");
        testUserDTO.setEmail("john@example.com");
        testUserDTO.setPhone("010-1234-5678");
        // 初始化创建用户请求对象
        request = new UserCreateRequest();

        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPhone("010-1234-5678");
    }

    /*@Test
    void getAllUsers_ShouldReturnListOfUsers() {
        List<User> users = Arrays.asList(testUser);
        when(userRepository.findAllOrderByCreatedAtDesc()).thenReturn(users);

        List<UserDTO> result = userService.getAllUsers();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getName());
        verify(userRepository).findAllOrderByCreatedAtDesc();
    }*/

    @Test
    void getAllUsers_ShouldReturnPageOfUsers() {

        List<User> users = Arrays.asList(testUser);

        Page<User> userPage = new PageImpl<>(users, PageRequest.of(0, 10), 1);

        when(userRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(userPage);

        Page<UserDTO> result = userService.getAllUsers(0, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("John Doe", result.getContent().get(0).getName());
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertEquals(0, result.getNumber());
        assertEquals(10, result.getSize());

        verify(userRepository).findAllByOrderByCreatedAtDesc(any(Pageable.class));
    }

    @Test
    void getUserById_WhenUserExists_ShouldReturnUserDTO() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserDTO result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        assertEquals("john@example.com", result.getEmail());
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserById_WhenUserNotExists_ShouldThrowUserNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserById(1L));
        verify(userRepository).findById(1L);
    }

    /*@Test
    void createUser_WhenEmailNotExists_ShouldCreateUser() {
        when(userRepository.existsByEmail(testUserDTO.getEmail())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDTO result = userService.createUser(Request);

        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        verify(userRepository).existsByEmail(testUserDTO.getEmail());
        verify(userRepository).save(any(User.class));
    }*/

    @Test
    void createUser_WhenEmailNotExists_ShouldCreateUser() {

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);

        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDTO result = userService.createUser(request);

        assertNotNull(result);
        assertEquals("John Doe", result.getName());

        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository).save(any(User.class));
    }

   /* @Test
    void createUser_WhenEmailAlreadyExists_ShouldThrowException() {
        when(userRepository.existsByEmail(testUserDTO.getEmail())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.createUser(Request));
        verify(userRepository).existsByEmail(testUserDTO.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }*/

  /*  @Test
    void createUser_WhenEmailAlreadyExists_ShouldThrowException() {

        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(request)
        );

        verify(userRepository).existsByEmail(request.getEmail());

        verify(userRepository, never())
                .save(any(User.class));
    }*/

    @Test
    void createUser_WhenEmailAlreadyExists_ShouldThrowException() {

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        EmailAlreadyExistsException exception = assertThrows(EmailAlreadyExistsException.class, () -> userService.createUser(request));

        assertEquals("邮箱已注册", exception.getMessage());

        verify(userRepository).existsByEmail(request.getEmail());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_WhenUserExists_ShouldUpdateUser() {
        testUserDTO.setName("Updated Name");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDTO result = userService.updateUser(1L, testUserDTO);

        assertNotNull(result);
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void deleteUser_WhenUserExists_ShouldDeleteUser() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_WhenUserNotExists_ShouldThrowUserNotFoundException() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> userService.deleteUser(1L));
        verify(userRepository).existsById(1L);
        verify(userRepository, never()).deleteById(1L);
    }

    @Test
    void searchUsersByName_ShouldReturnPageOfUsers() {
        List<User> users = Arrays.asList(testUser);
        Pageable pageable = PageRequest.of(0, 5);
        Page<User> userPage = new PageImpl<>(users, pageable, 1);

        when(userRepository.searchByNamePattern(
                eq("%John%"), any(Pageable.class)))
                .thenReturn(userPage);

        Page<UserDTO> result = userService.searchUsersByName("John", 0, 5);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("John Doe", result.getContent().get(0).getName());
        assertEquals(1, result.getTotalElements());
        assertEquals(5, result.getSize());

        verify(userRepository).searchByNamePattern(
                eq("%John%"), any(Pageable.class));
    }

}