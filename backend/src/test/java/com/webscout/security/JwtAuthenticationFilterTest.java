package com.webscout.security;

import com.webscout.entity.User;
import com.webscout.repository.UserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {


    private final JwtService jwtService = mock(JwtService.class);
    private UserRepository userRepository;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);

        filter = new JwtAuthenticationFilter(
                jwtService,
                userRepository
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueWithoutAuthenticationWhenAuthorizationHeaderIsMissing()
            throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldContinueWithoutAuthenticationWhenAuthorizationHeaderIsNotBearer()
            throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc123");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldAuthenticateWhenBearerTokenIsValid()
            throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(jwtService.extractUserId("valid-token"))
                .thenReturn(42L);
        when(userRepository.findByIdAndStatus(42L, "ACTIVE"))
                .thenReturn(Optional.of(mock(User.class)));

        filter.doFilter(request, response, filterChain);

        var authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(authentication);

        assertEquals(
                42L,
                authentication.getPrincipal()
        );

        assertInstanceOf(
                UsernamePasswordAuthenticationToken.class,
                authentication
        );

        verify(jwtService).extractUserId("valid-token");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateWhenJwtIsInvalid()
            throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(jwtService.extractUserId("invalid-token"))
                .thenThrow(new RuntimeException("Invalid JWT"));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(jwtService).extractUserId("invalid-token");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotOverwriteExistingAuthentication()
            throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer another-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        var existingAuthentication =
                new UsernamePasswordAuthenticationToken(
                        99L,
                        null,
                        java.util.Collections.emptyList()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(existingAuthentication);

        filter.doFilter(request, response, filterChain);

        assertSame(
                existingAuthentication,
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );

        verifyNoInteractions(jwtService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateWhenUserIsNotActive()
            throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer disabled-user-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(jwtService.extractUserId("disabled-user-token"))
                .thenReturn(42L);

        when(userRepository.findByIdAndStatus(42L, "ACTIVE"))
                .thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        verify(jwtService)
                .extractUserId("disabled-user-token");

        verify(userRepository)
                .findByIdAndStatus(42L, "ACTIVE");

        verify(filterChain)
                .doFilter(request, response);
    }
}