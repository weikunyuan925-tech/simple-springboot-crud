package com.example.crud.controller;

import com.example.crud.dto.UserDTO;
import com.example.crud.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllUsers_ShouldReturnListOfUsers() throws Exception {
        UserDTO userDTO = new UserDTO(1L, "John Doe", "john@example.com", "010-1234-5678", 
                                     LocalDateTime.now(), LocalDateTime.now());
        List<UserDTO> users = Arrays.asList(userDTO);

        when(userService.getAllUsers()).thenReturn(users);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.users").isArray())
                .andExpect(jsonPath("$.users[0].name").value("John Doe"))
                .andExpect(jsonPath("$.total").value(1));

        verify(userService).getAllUsers();
    }

    @Test
    void getUserById_ShouldReturnUser() throws Exception {
        UserDTO userDTO = new UserDTO(1L, "John Doe", "john@example.com", "010-1234-5678", 
                                     LocalDateTime.now(), LocalDateTime.now());

        when(userService.getUserById(1L)).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));

        verify(userService).getUserById(1L);
    }

    @Test
    void createUser_WithValidData_ShouldCreateUser() throws Exception {
        UserDTO inputDTO = new UserDTO();
        inputDTO.setName("John Doe");
        inputDTO.setEmail("john@example.com");
        inputDTO.setPhone("010-1234-5678");

        UserDTO outputDTO = new UserDTO(1L, "John Doe", "john@example.com", "010-1234-5678", 
                                       LocalDateTime.now(), LocalDateTime.now());

        when(userService.createUser(any(com.example.crud.dto.UserCreateRequest.class))).thenReturn(outputDTO);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDTO)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User created successfully"))
                .andExpect(jsonPath("$.user.name").value("John Doe"));

        verify(userService).createUser(any(com.example.crud.dto.UserCreateRequest.class));
    }

    @Test
    void createUser_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        UserDTO inputDTO = new UserDTO();
        inputDTO.setName(""); // Invalid: empty name
        inputDTO.setEmail("invalid-email"); // Invalid: not a valid email
        inputDTO.setPhone("010-1234-5678");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDTO)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUser(any(com.example.crud.dto.UserCreateRequest.class));
    }

    @Test
    void updateUser_WithValidData_ShouldUpdateUser() throws Exception {
        UserDTO inputDTO = new UserDTO();
        inputDTO.setName("Updated Name");
        inputDTO.setEmail("updated@example.com");
        inputDTO.setPhone("010-9876-5432");

        UserDTO outputDTO = new UserDTO(1L, "Updated Name", "updated@example.com", "010-9876-5432", 
                                       LocalDateTime.now(), LocalDateTime.now());

        when(userService.updateUser(eq(1L), any(UserDTO.class))).thenReturn(outputDTO);

        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDTO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User updated successfully"))
                .andExpect(jsonPath("$.user.name").value("Updated Name"));

        verify(userService).updateUser(eq(1L), any(UserDTO.class));
    }

    @Test
    void deleteUser_ShouldDeleteUser() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User deleted successfully"))
                .andExpect(jsonPath("$.userId").value(1));

        verify(userService).deleteUser(1L);
    }

    @Test
    void healthCheck_ShouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/users/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("User Management API"));
    }
}