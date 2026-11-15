package com.example.crud.repository;

import com.example.crud.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /*Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);*/
    
    /*@Query("SELECT u FROM User u WHERE u.name LIKE %:name%")
    List<User> findByNameContaining(@Param("name") String name);*/

    //ESCAPE '!' 告诉数据库：! 后面的 % 或 _ 是普通字符，而不是通配符
    /*@Query("SELECT u FROM User u WHERE u.name LIKE :pattern ESCAPE '!'")
    List<User> searchByNamePattern(@Param("pattern") String pattern);
    
    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<User> findAllOrderByCreatedAtDesc();

    List<User> findByNameContaining(String name);*/
    //分页查询
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // 分页搜索用户，保留 LIKE 特殊字符转义
    @Query("SELECT u FROM User u WHERE u.name LIKE :pattern ESCAPE '!'")
    Page<User> searchByNamePattern(@Param("pattern") String pattern ,Pageable pageable);

    // 按创建时间倒序分页查询全部用户
    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);

    //Page<User> findByNameContaining(String name,Pageable pageable);

}