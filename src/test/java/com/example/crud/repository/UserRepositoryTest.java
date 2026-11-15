package com.example.crud.repository;

import com.example.crud.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // 清理数据库中已有的用户数据
        userRepository.deleteAll();
        userRepository.flush();
    }

    @Test
    void findByEmail_WhenUserExists_ShouldReturnUser() {
        User user = new User("John Doe", "john@example.com", "010-1234-5678");
        entityManager.persistAndFlush(user);

        Optional<User> found = userRepository.findByEmail("john@example.com");

        assertTrue(found.isPresent());
        assertEquals("John Doe", found.get().getName());
        assertEquals("john@example.com", found.get().getEmail());
    }

    @Test
    void findByEmail_WhenUserNotExists_ShouldReturnEmpty() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        assertFalse(found.isPresent());
    }

    @Test
    void existsByEmail_WhenUserExists_ShouldReturnTrue() {
        User user = new User("John Doe", "john@example.com", "010-1234-5678");
        entityManager.persistAndFlush(user);

        boolean exists = userRepository.existsByEmail("john@example.com");

        assertTrue(exists);
    }

    @Test
    void existsByEmail_WhenUserNotExists_ShouldReturnFalse() {
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        assertFalse(exists);
    }

    /*@Test
    void findByNameContaining_ShouldReturnMatchingUsers() {
        User user1 = new User("John Doe", "john@example.com", "010-1234-5678");
        User user2 = new User("Jane Doe", "jane@example.com", "010-2345-6789");
        User user3 = new User("Bob Smith", "bob@example.com", "010-3456-7890");

        entityManager.persistAndFlush(user1);
        entityManager.persistAndFlush(user2);
        entityManager.persistAndFlush(user3);

        List<User> found = userRepository.findByNameContaining("Doe");

        assertEquals(2, found.size());
        assertTrue(found.stream().anyMatch(u -> u.getName().equals("John Doe")));
        assertTrue(found.stream().anyMatch(u -> u.getName().equals("Jane Doe")));
    }*/

    /*@Test
    void findAllOrderByCreatedAtDesc_ShouldReturnUsersInDescendingOrder() {
        User user1 = new User("First User", "first@example.com", "010-1111-1111");
        User user2 = new User("Second User", "second@example.com", "010-2222-2222");
        User user3 = new User("Third User", "third@example.com", "010-3333-3333");

        entityManager.persistAndFlush(user1);
        entityManager.persistAndFlush(user2);
        entityManager.persistAndFlush(user3);

        List<User> users = userRepository.findAllOrderByCreatedAtDesc();

        assertFalse(users.isEmpty());
        assertEquals(3, users.size());
    }*/

    @Test
    void save_ShouldPersistUser() {
        User user = new User("Test User", "test@example.com", "010-1234-5678");

        User saved = userRepository.save(user);

        assertNotNull(saved.getId());
        assertEquals("Test User", saved.getName());
        assertEquals("test@example.com", saved.getEmail());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void deleteById_ShouldRemoveUser() {
        User user = new User("To Delete", "delete@example.com", "010-1234-5678");
        User saved = entityManager.persistAndFlush(user);

        userRepository.deleteById(saved.getId());

        Optional<User> found = userRepository.findById(saved.getId());
        assertFalse(found.isPresent());
    }
}