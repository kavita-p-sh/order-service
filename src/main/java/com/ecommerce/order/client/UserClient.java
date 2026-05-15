package com.ecommerce.order.client;

import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.util.AppConstants;
import com.ecommerce.common.util.JwtConstant;
import com.ecommerce.order.dto.UserResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Client component used by the order-service to communicate with the user-service.
 * This class is responsible for fetching user details from the user-service
 * using REST API calls.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserClient {

    /**
     * RestTemplate is used to make HTTP calls from order-service to user-service.
     */
    private final RestTemplate restTemplate;

    @Value("${user.service.url}")
    private String userServiceUrl;

    /**
     * Fetches user details from user-service using the given user ID.
     * @param userId unique ID of the user
     * @return user details as {@link UserResponseDTO}
     * @throws ResourceNotFoundException if user-service call fails or user is not found
     */
    public UserResponseDTO getUserById(UUID userId) {
        try {
            String url = userServiceUrl + "/" + userId;

            log.info("Calling user-service API to fetch user by ID: {}", userId);

            HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders());

            ResponseEntity<UserResponseDTO> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            UserResponseDTO.class
                    );

            return response.getBody();

        } catch (Exception ex) {
            log.error("Failed to fetch user from user-service for userId: {}", userId, ex);
            throw new ResourceNotFoundException(AppConstants.USER_NOT_FOUND);
        }
    }

    /**
     * Fetches currently logged-in user by forwarding JWT token to user-service.
     *
     * This method calls:
     * GET {user.service.url}/profile
     *
     * @return current user details
     * @throws ResourceNotFoundException if user-service call fails or token is missing
     */
    public UserResponseDTO getCurrentUser() {
        try {
            log.info("Calling user-service current profile API");

            HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders());

            ResponseEntity<UserResponseDTO> response = restTemplate.exchange(
                    userServiceUrl + "/profile",
                    HttpMethod.GET,
                    entity,
                    UserResponseDTO.class
            );

            return response.getBody();

        } catch (Exception ex) {
            log.error("Failed to fetch current user from user-service", ex);
            throw new ResourceNotFoundException(AppConstants.USER_NOT_FOUND);
        }
    }

    /**
     * Creates authentication headers by forwarding the Authorization header
     * from the current incoming request.
     *
     * @return HttpHeaders containing Bearer token
     * @throws ResourceNotFoundException if request attributes or token are missing
     */
    private HttpHeaders createAuthHeaders() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            log.error("No request attributes found while creating auth headers");
            throw new ResourceNotFoundException(AppConstants.USER_NOT_FOUND);
        }

        HttpServletRequest request = attributes.getRequest();
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(JwtConstant.TOKEN_PREFIX)) {
            log.error("Authorization header is missing or invalid");
            throw new ResourceNotFoundException(AppConstants.USER_NOT_FOUND);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authHeader);

        return headers;
    }
}