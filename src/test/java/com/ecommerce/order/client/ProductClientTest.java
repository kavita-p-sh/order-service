package com.ecommerce.order.client;

import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.exception.ServiceUnavailableException;
import com.ecommerce.common.exception.UnauthorizedException;
import com.ecommerce.common.exception.UpstreamServerException;
import com.ecommerce.order.dto.ProductResponseDTO;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProductClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ProductClient productClient;

    private static final String PRODUCT_SERVICE_URL = "http://localhost:8097/api/products";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(productClient, "productServiceUrl", PRODUCT_SERVICE_URL);

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer test-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void getProductById_shouldReturnProduct_whenProductExists() {
        ProductResponseDTO responseDTO = new ProductResponseDTO();

        when(restTemplate.exchange(
                eq(PRODUCT_SERVICE_URL + "/1"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(ProductResponseDTO.class)
        )).thenReturn(ResponseEntity.ok(responseDTO));

        ProductResponseDTO result = productClient.getProductById(1L);

        assertNotNull(result);
        verify(restTemplate).exchange(
                eq(PRODUCT_SERVICE_URL + "/1"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(ProductResponseDTO.class)
        );
    }

    @Test
    void getProductById_shouldThrowResourceNotFoundException_whenProductNotFound() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(ProductResponseDTO.class)))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        HttpStatus.NOT_FOUND,
                        "Not Found",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        assertThrows(ResourceNotFoundException.class, () -> productClient.getProductById(1L));
    }

    @Test
    void getProductById_shouldThrowBadRequestException_whenBadRequest() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(ProductResponseDTO.class)))
                .thenThrow(HttpClientErrorException.BadRequest.create(
                        HttpStatus.BAD_REQUEST,
                        "Bad Request",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        assertThrows(BadRequestException.class, () -> productClient.getProductById(1L));
    }

    @Test
    void getProductById_shouldThrowUnauthorizedException_whenUnauthorized() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(ProductResponseDTO.class)))
                .thenThrow(HttpClientErrorException.Unauthorized.create(
                        HttpStatus.UNAUTHORIZED,
                        "Unauthorized",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        assertThrows(UnauthorizedException.class, () -> productClient.getProductById(1L));
    }

    @Test
    void getProductById_shouldThrowBadRequestException_whenForbidden() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(ProductResponseDTO.class)))
                .thenThrow(HttpClientErrorException.Forbidden.create(
                        HttpStatus.FORBIDDEN,
                        "Forbidden",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        assertThrows(BadRequestException.class, () -> productClient.getProductById(1L));
    }

    @Test
    void getProductById_shouldThrowUpstreamServerException_whenProductServiceReturns5xx() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(ProductResponseDTO.class)))
                .thenThrow(HttpServerErrorException.create(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Server Error",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        assertThrows(UpstreamServerException.class, () -> productClient.getProductById(1L));
    }

    @Test
    void getProductById_shouldThrowServiceUnavailableException_whenProductServiceConnectionFails() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(ProductResponseDTO.class)))
                .thenThrow(new ResourceAccessException("Connection failed"));

        assertThrows(ServiceUnavailableException.class, () -> productClient.getProductById(1L));
    }

    @Test
    void reduceStock_shouldCallProductServiceSuccessfully() {
        when(restTemplate.exchange(
                eq(PRODUCT_SERVICE_URL + "/1/reduce-stock?quantity=2"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenReturn(ResponseEntity.ok().build());

        assertDoesNotThrow(() -> productClient.reduceStock(1L, 2));

        verify(restTemplate).exchange(
                eq(PRODUCT_SERVICE_URL + "/1/reduce-stock?quantity=2"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(Void.class)
        );
    }

    @Test
    void reduceStock_shouldThrowResourceNotFoundException_whenProductNotFound() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        HttpStatus.NOT_FOUND,
                        "Not Found",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        assertThrows(ResourceNotFoundException.class, () -> productClient.reduceStock(1L, 2));
    }

    @Test
    void reduceStock_shouldThrowBadRequestException_whenQuantityInvalid() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(HttpClientErrorException.BadRequest.create(
                        HttpStatus.BAD_REQUEST,
                        "Bad Request",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        assertThrows(BadRequestException.class, () -> productClient.reduceStock(1L, 2));
    }

    @Test
    void reduceStock_shouldThrowRuntimeException_whenProductServiceReturns5xx() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(HttpServerErrorException.create(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Server Error",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        assertThrows(RuntimeException.class, () -> productClient.reduceStock(1L, 2));
    }

    @Test
    void reduceStock_shouldThrowRuntimeException_whenConnectionFails() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new ResourceAccessException("Connection failed"));

        assertThrows(RuntimeException.class, () -> productClient.reduceStock(1L, 2));
    }

    @Test
    void getProductById_shouldThrowUnauthorizedException_whenRequestContextMissing() {
        RequestContextHolder.resetRequestAttributes();

        assertThrows(UnauthorizedException.class, () -> productClient.getProductById(1L));
    }

    @Test
    void getProductById_shouldThrowUnauthorizedException_whenAuthorizationHeaderMissing() {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> productClient.getProductById(1L));
    }
}