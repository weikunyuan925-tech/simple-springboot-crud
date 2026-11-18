package com.example.crud.controller;

import com.example.crud.config.SecurityConfig;
import com.example.crud.dto.UserCreateRequest;
import com.example.crud.dto.UserDTO;
import com.example.crud.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.example.crud.service.JwtService;
import org.springframework.security.core.userdetails.UserDetailsService;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.springframework.security.test.context.support.WithMockUser;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    /*@Test
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
    }*/

    @Test
    @WithMockUser
    void getAllUsers_ShouldReturnPageOfUsers() throws Exception {

        UserDTO userDTO = new UserDTO(1L, "John Doe", "john@example.com", "010-1234-5678", LocalDateTime.now(), LocalDateTime.now());

        Page<UserDTO> userPage = new PageImpl<>(Arrays.asList(userDTO), PageRequest.of(0, 10), 1);

        when(userService.getAllUsers(0, 10)).thenReturn(userPage);

        mockMvc.perform(get("/api/users")).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$.users").isArray()).andExpect(jsonPath("$.users[0].name").value("John Doe")).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(10)).andExpect(jsonPath("$.totalPages").value(1));

        verify(userService).getAllUsers(0, 10);
    }

    @Test
    @WithMockUser
    void getAllUsers_WithCustomPagination_ShouldReturnRequestedPage() throws Exception {

        Page<UserDTO> userPage = new PageImpl<>(Arrays.asList(), PageRequest.of(1, 5), 12);

        when(userService.getAllUsers(1, 5)).thenReturn(userPage);

        mockMvc.perform(get("/api/users").param("page", "1").param("size", "5")).andExpect(status().isOk()).andExpect(jsonPath("$.page").value(1)).andExpect(jsonPath("$.size").value(5)).andExpect(jsonPath("$.total").value(12)).andExpect(jsonPath("$.totalPages").value(3));

        verify(userService).getAllUsers(1, 5);
    }

    @Test
    @WithMockUser
    void getAllUsers_WithInvalidPagination_ShouldReturnBadRequest() throws Exception {

        mockMvc.perform(get("/api/users").param("page", "-1").param("size", "10")).andExpect(status().isBadRequest());

        verify(userService, never()).getAllUsers(anyInt(), anyInt());
    }

    @Test
    @WithMockUser
    void getAllUsers_WithOversizedPageSize_ShouldReturnBadRequest() throws Exception {

        mockMvc.perform(get("/api/users").param("page", "0").param("size", "101")).andExpect(status().isBadRequest());

        verify(userService, never()).getAllUsers(anyInt(), anyInt());
    }

    @Test
    @WithMockUser
    void getUserById_ShouldReturnUser() throws Exception {
        UserDTO userDTO = new UserDTO(1L, "John Doe", "john@example.com", "010-1234-5678", LocalDateTime.now(), LocalDateTime.now());

        when(userService.getUserById(1L)).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$.name").value("John Doe")).andExpect(jsonPath("$.email").value("john@example.com"));

        verify(userService).getUserById(1L);
    }

    @Test
    @WithMockUser
    void createUser_WithValidData_ShouldCreateUser() throws Exception {
        UserCreateRequest inputDTO = new UserCreateRequest();
        inputDTO.setName("John Doe");
        inputDTO.setEmail("john@example.com");
        inputDTO.setPhone("13812345678");

        UserDTO outputDTO = new UserDTO(1L, "John Doe", "john@example.com", "010-1234-5678", LocalDateTime.now(), LocalDateTime.now());

        when(userService.createUser(any(com.example.crud.dto.UserCreateRequest.class))).thenReturn(outputDTO);

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(inputDTO))).andExpect(status().isCreated()).andDo(print()).andExpect(content().contentType(MediaType.APPLICATION_JSON));
                /*.andExpect(jsonPath("$.message").value("User created successfully"))
                .andExpect(jsonPath("$.user.name").value("John Doe"));*/

        verify(userService).createUser(any(com.example.crud.dto.UserCreateRequest.class));
    }

    @Test
    @WithMockUser
    void createUser_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        UserCreateRequest inputDTO = new UserCreateRequest();
        inputDTO.setName(""); // Invalid: empty name
        inputDTO.setEmail("invalid-email"); // Invalid: not a valid email
        inputDTO.setPhone("13812345678");

/*        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDTO)))
                .andExpect(status().isBadRequest());*/

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(inputDTO))).andDo(print()).andExpect(status().isBadRequest()).andExpect(content().contentType(MediaType.APPLICATION_JSON));
               /* .andExpect(jsonPath("$.message").value("User created successfully"))
                .andExpect(jsonPath("$.user.name").value("John Doe"));*/

        verify(userService, never()).createUser(any(com.example.crud.dto.UserCreateRequest.class));
    }

    @Test
    @WithMockUser
    void updateUser_WithValidData_ShouldUpdateUser() throws Exception {
        UserCreateRequest inputDTO = new UserCreateRequest();

        inputDTO.setName("John Doe");
        inputDTO.setEmail("john@example.com");
        inputDTO.setPhone("13812345878");
        UserDTO outputDTO = new UserDTO(1L, "Updated Name", "updated@example.com", "010-9876-5432", LocalDateTime.now(), LocalDateTime.now());

        when(userService.updateUser(eq(1L), any(UserDTO.class))).thenReturn(outputDTO);

        mockMvc.perform(put("/api/users/1").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(inputDTO))).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$.message").value("User updated successfully")).andExpect(jsonPath("$.user.name").value("Updated Name")).andDo(print());

        verify(userService).updateUser(eq(1L), any(UserDTO.class));
    }

    @Test
    @WithMockUser
    void deleteUser_ShouldDeleteUser() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1")).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$.message").value("User deleted successfully")).andExpect(jsonPath("$.userId").value(1));

        verify(userService).deleteUser(1L);
    }

    @Test
    void healthCheck_ShouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/users/health")).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$.status").value("UP")).andExpect(jsonPath("$.service").value("User Management API"));
    }

    @Test
    @WithMockUser
    void searchUsers_ShouldReturnPageOfUsers() throws Exception {
        UserDTO userDTO = new UserDTO(1L, "John Doe", "john@example.com", "010-1234-5678", LocalDateTime.now(), LocalDateTime.now());

        Page<UserDTO> userPage = new PageImpl<>(Arrays.asList(userDTO), PageRequest.of(0, 5), 1);

        when(userService.searchUsersByName("John", 0, 5)).thenReturn(userPage);

        mockMvc.perform(get("/api/users/search").param("name", "John").param("page", "0").param("size", "5")).andExpect(status().isOk()).andExpect(jsonPath("$.users").isArray()).andExpect(jsonPath("$.users[0].name").value("John Doe")).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(5)).andExpect(jsonPath("$.totalPages").value(1)).andExpect(jsonPath("$.searchTerm").value("John"));

        verify(userService).searchUsersByName("John", 0, 5);
    }
}