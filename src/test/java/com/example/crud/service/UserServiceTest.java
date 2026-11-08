package com.example.crud.service;

import com.example.crud.dto.UserCreateRequest;
import com.example.crud.dto.UserDTO;
import com.example.crud.entity.User;
import com.example.crud.exception.UserNotFoundException;
import com.example.crud.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

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
    }

    @Test
    void getAllUsers_ShouldReturnListOfUsers() {
        List<User> users = Arrays.asList(testUser);
        when(userRepository.findAllOrderByCreatedAtDesc()).thenReturn(users);

        List<UserDTO> result = userService.getAllUsers();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getName());
        verify(userRepository).findAllOrderByCreatedAtDesc();
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

    @Test
    void createUser_WhenEmailNotExists_ShouldCreateUser() {
        when(userRepository.existsByEmail(testUserDTO.getEmail())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDTO result = userService.createUser(Request);

        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        verify(userRepository).existsByEmail(testUserDTO.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_WhenEmailAlreadyExists_ShouldThrowException() {
        when(userRepository.existsByEmail(testUserDTO.getEmail())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.createUser(Request));
        verify(userRepository).existsByEmail(testUserDTO.getEmail());
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
}