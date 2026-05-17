package com.ecommerce.order.client;

import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.exception.ServiceUnavailableException;
import com.ecommerce.common.exception.UnauthorizedException;
import com.ecommerce.common.exception.UpstreamServerException;
import com.ecommerce.order.dto.UserResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private UserClient userClient;

    private static final String USER_SERVICE_URL = "http://localhost:8095/api/users";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(userClient, "userServiceUrl", USER_SERVICE_URL);

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer test-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void getUserById_shouldReturnUser_whenUserExists() {
        UUID userId = UUID.randomUUID();
        UserResponseDTO responseDTO = new UserResponseDTO();

        when(restTemplate.exchange(
                eq(USER_SERVICE_URL + "/" + userId),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(UserResponseDTO.class)
        )).thenReturn(ResponseEntity.ok(responseDTO));

        UserResponseDTO result = userClient.getUserById(userId);

        assertNotNull(result);
        verify(restTemplate).exchange(
                eq(USER_SERVICE_URL + "/" + userId),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(UserResponseDTO.class)
        );
    }

    @Test
    void getUserById_shouldThrowResourceNotFoundException_whenUserNotFound() {
        UUID userId = UUID.randomUUID();

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(UserResponseDTO.class)))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        HttpStatus.NOT_FOUND,
                        "Not Found",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        assertThrows(ResourceNotFoundException.class, () -> userClient.getUserById(userId));
    }

    @Test
    void getUserById_shouldThrowUnauthorizedException_whenUnauthorized() {
        UUID userId = UUID.randomUUID();

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(UserResponseDTO.class)))
                .thenThrow(HttpClientErrorException.Unauthorized.create(
                        HttpStatus.UNAUTHORIZED,
                        "Unauthorized",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        assertThrows(UnauthorizedException.class, () -> userClient.getUserById(userId));
    }

    @Test
    void getUserById_shouldThrowUpstreamServerException_whenUserServiceReturns5xx() {
        UUID userId = UUID.randomUUID();

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(UserResponseDTO.class)))
                .thenThrow(HttpServerErrorException.create(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Server Error",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        assertThrows(UpstreamServerException.class, () -> userClient.getUserById(userId));
    }

    @Test
    void getUserById_shouldThrowServiceUnavailableException_whenConnectionFails() {
        UUID userId = UUID.randomUUID();

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(UserResponseDTO.class)))
                .thenThrow(new ResourceAccessException("Connection failed"));

        assertThrows(ServiceUnavailableException.class, () -> userClient.getUserById(userId));
    }

    @Test
    void getCurrentUser_shouldReturnCurrentUser_whenProfileExists() {
        UserResponseDTO responseDTO = new UserResponseDTO();

        when(restTemplate.exchange(
                eq(USER_SERVICE_URL + "/profile"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(UserResponseDTO.class)
        )).thenReturn(ResponseEntity.ok(responseDTO));

        UserResponseDTO result = userClient.getCurrentUser();

        assertNotNull(result);
        verify(restTemplate).exchange(
                eq(USER_SERVICE_URL + "/profile"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(UserResponseDTO.class)
        );
    }

    @Test
    void getCurrentUser_shouldThrowResourceNotFoundException_whenProfileNotFound() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(UserResponseDTO.class)))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        HttpStatus.NOT_FOUND,
                        "Not Found",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        assertThrows(ResourceNotFoundException.class, () -> userClient.getCurrentUser());
    }

    @Test
    void getCurrentUser_shouldThrowUnauthorizedException_whenUnauthorized() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(UserResponseDTO.class)))
                .thenThrow(HttpClientErrorException.Unauthorized.create(
                        HttpStatus.UNAUTHORIZED,
                        "Unauthorized",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        assertThrows(UnauthorizedException.class, () -> userClient.getCurrentUser());
    }

    @Test
    void getCurrentUser_shouldThrowUpstreamServerException_whenUserServiceReturns5xx() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(UserResponseDTO.class)))
                .thenThrow(HttpServerErrorException.create(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Server Error",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        assertThrows(UpstreamServerException.class, () -> userClient.getCurrentUser());
    }

    @Test
    void getCurrentUser_shouldThrowServiceUnavailableException_whenConnectionFails() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(UserResponseDTO.class)))
                .thenThrow(new ResourceAccessException("Connection failed"));

        assertThrows(ServiceUnavailableException.class, () -> userClient.getCurrentUser());
    }

    @Test
    void getCurrentUser_shouldThrowUnauthorizedException_whenRequestContextMissing() {
        RequestContextHolder.resetRequestAttributes();

        assertThrows(UnauthorizedException.class, () -> userClient.getCurrentUser());
    }

    @Test
    void getCurrentUser_shouldThrowUnauthorizedException() {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> userClient.getCurrentUser());
    }
}