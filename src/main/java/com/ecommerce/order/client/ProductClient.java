package com.ecommerce.order.client;

import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.util.AppConstants;
import com.ecommerce.common.util.JwtConstant;
import com.ecommerce.order.dto.ProductResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.ecommerce.common.exception.ServiceUnavailableException;
import com.ecommerce.common.exception.UnauthorizedException;
import com.ecommerce.common.exception.UpstreamServerException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductClient {

    private final RestTemplate restTemplate;

    @Value("${product.service.url}")
    private String productServiceUrl;

    /**
     * Fetch product details from product-service by product id.
     */
    public ProductResponseDTO getProductById(Long productId) {
        String url = productServiceUrl + "/" + productId;

        try {
            ResponseEntity<ProductResponseDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    createRequestEntity(),
                    ProductResponseDTO.class
            );

            return response.getBody();

        } catch (HttpClientErrorException.NotFound ex) {
            log.error("Product not found in product-service. ProductId: {}", productId);
            throw new ResourceNotFoundException(AppConstants.PRODUCT_NOT_FOUND);

        } catch (HttpClientErrorException.BadRequest ex) {
            log.error("Invalid product request. ProductId: {}, Response: {}",
                    productId, ex.getResponseBodyAsString());
            throw new BadRequestException(AppConstants.INVALID_PRODUCT_REQUEST);

        } catch (HttpClientErrorException.Unauthorized ex) {
            log.error("Unauthorized request while fetching product. ProductId: {}", productId);
            throw new UnauthorizedException(AppConstants.UNAUTHORIZED_PRODUCT_SERVICE);

        } catch (HttpClientErrorException.Forbidden ex) {
            log.error("Forbidden request while fetching product. ProductId: {}", productId);
            throw new BadRequestException(AppConstants.PRODUCT_SERVICE_ACCESS_DENIED);

        } catch (HttpServerErrorException ex) {
            log.error("Product-service server error while fetching product. ProductId: {}",
                    productId, ex);
            throw new UpstreamServerException(AppConstants.PRODUCT_SERVICE_NOT_AVAILABLE);

        } catch (ResourceAccessException ex) {
            log.error("Unable to connect to product-service. ProductId: {}", productId, ex);
            throw new ServiceUnavailableException(AppConstants.PRODUCT_SERVICE_CONNECTION_FAILED);

        }
    }

    /**
     * Calls product-service to reduce product stock.
     */
    public void reduceStock(Long productId, Integer quantity) {
        String url = productServiceUrl + "/" + productId + "/reduce-stock?quantity=" + quantity;

        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    createRequestEntity(),
                    Void.class
            );

        } catch (HttpClientErrorException.NotFound ex) {
            log.error("Product not found while reducing stock. ProductId: {}", productId);
            throw new ResourceNotFoundException(AppConstants.PRODUCT_NOT_FOUND);

        } catch (HttpClientErrorException.BadRequest ex) {
            log.error("Invalid stock reduce request. ProductId: {}, Quantity: {}, Response: {}",
                    productId, quantity, ex.getResponseBodyAsString());
            throw new BadRequestException(AppConstants.PRODUCT_QUANTITY_INVALID);

        } catch (HttpClientErrorException.Unauthorized ex) {
            log.error("Unauthorized request while reducing stock. ProductId: {}", productId);
            throw new BadRequestException(AppConstants.UNAUTHORIZED_PRODUCT_SERVICE);

        } catch (HttpClientErrorException.Forbidden ex) {
            log.error("Forbidden request while reducing stock. ProductId: {}", productId);
            throw new BadRequestException(AppConstants.PRODUCT_SERVICE_ACCESS_DENIED);

        } catch (HttpServerErrorException ex) {
            log.error("Product-service server error while reducing stock. ProductId: {}", productId, ex);
            throw new RuntimeException(AppConstants.PRODUCT_SERVICE_NOT_AVAILABLE);

        } catch (ResourceAccessException ex) {
            log.error("Unable to connect to product-service while reducing stock. ProductId: {}", productId, ex);
            throw new RuntimeException(AppConstants.PRODUCT_SERVICE_CONNECTION_FAILED);

        } catch (Exception ex) {
            log.error("Unexpected error while reducing stock. ProductId: {}", productId, ex);
            throw new RuntimeException(AppConstants.PRODUCT_STOCK_REDUCE_FAILED);
        }
    }

    /**
     * Calls product-service to restore product stock after order cancellation.
     */
    public void restoreStock(Long productId, Integer quantity) {
        String url = productServiceUrl + "/" + productId + "/restore-stock?quantity=" + quantity;

        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    createRequestEntity(),
                    Void.class
            );

        } catch (HttpClientErrorException.NotFound ex) {
            log.error("Product not found while restoring stock. ProductId: {}", productId);
            throw new ResourceNotFoundException(AppConstants.PRODUCT_NOT_FOUND);

        } catch (HttpClientErrorException.BadRequest ex) {
            log.error("Invalid stock restore request. ProductId: {}, Quantity: {}, Response: {}",
                    productId, quantity, ex.getResponseBodyAsString());
            throw new BadRequestException(AppConstants.PRODUCT_QUANTITY_INVALID);

        } catch (HttpClientErrorException.Unauthorized ex) {
            log.error("Unauthorized request while restoring stock. ProductId: {}", productId);
            throw new BadRequestException(AppConstants.UNAUTHORIZED_PRODUCT_SERVICE);

        } catch (HttpClientErrorException.Forbidden ex) {
            log.error("Forbidden request while restoring stock. ProductId: {}", productId);
            throw new BadRequestException(AppConstants.PRODUCT_SERVICE_ACCESS_DENIED);

        } catch (HttpServerErrorException ex) {
            log.error("Product-service server error while restoring stock. ProductId: {}", productId, ex);
            throw new RuntimeException(AppConstants.PRODUCT_SERVICE_NOT_AVAILABLE);

        } catch (ResourceAccessException ex) {
            log.error("Unable to connect to product-service while restoring stock. ProductId: {}", productId, ex);
            throw new RuntimeException(AppConstants.PRODUCT_SERVICE_CONNECTION_FAILED);

        } catch (Exception ex) {
            log.error("Unexpected error while restoring stock. ProductId: {}", productId, ex);
            throw new RuntimeException(AppConstants.PRODUCT_FETCH_FAILED);
        }
    }
    /**
     * Create HTTP entity with Authorization header.
     */
    private HttpEntity<Void> createRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getTokenFromCurrentRequest());

        return new HttpEntity<>(headers);
    }

    /**
     * Extract JWT token from current incoming request.
     * This token is forwarded to product-service.
     */
    private String getTokenFromCurrentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            log.error("Request context not found while calling product-service");
            throw new UnauthorizedException(AppConstants.AUTHORIZATION_TOKEN_MISSING);
        }

        HttpServletRequest request = attributes.getRequest();
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(JwtConstant.TOKEN_PREFIX)) {
            log.error("Authorization token missing while calling product-service");
            throw new UnauthorizedException(AppConstants.AUTHORIZATION_TOKEN_MISSING);
        }

        return authHeader.substring(JwtConstant.TOKEN_PREFIX.length());
    }
}