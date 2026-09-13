package com.webscout.security;

import com.webscout.controller.AuthController;
import com.webscout.controller.UserController;
import com.webscout.dto.UserResponse;
import com.webscout.entity.User;
import com.webscout.repository.UserRepository;
import com.webscout.service.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({
        UserController.class,
        AuthController.class
})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class SecurityBoundaryTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void shouldReturn401WhenProtectedEndpointHasNoJwt() throws Exception {

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void shouldReturn401WhenJwtIsInvalid() throws Exception {

        when(jwtService.extractUserId("invalid-token"))
                .thenThrow(new RuntimeException("Invalid JWT"));

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header("Authorization", "Bearer invalid-token")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void shouldAllowProtectedEndpointWhenJwtIsValid() throws Exception {

        when(jwtService.extractUserId("valid-token"))
                .thenReturn(42L);
        when(userRepository.findByIdAndStatus(42L, "ACTIVE"))
                .thenReturn(Optional.of(mock(User.class)));

        UserResponse response = new UserResponse();
        response.setId(42L);
        response.setEmail("test@example.com");
        response.setStatus("ACTIVE");

        when(userService.getCurrentUser(42L))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header("Authorization", "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldAllowRegistrationWithoutJwt() throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "email": "new@example.com",
                                            "password": "password123"
                                        }
                                        """)
                )
                .andExpect(status().isCreated());
    }

    @Test
    void shouldAllowLoginWithoutJwt() throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "email": "test@example.com",
                                            "password": "password123"
                                        }
                                        """)
                )
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowRefreshWithoutJwt() throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "refreshToken": "refresh-token"
                                        }
                                        """)
                )
                .andExpect(status().isOk());
    }
}