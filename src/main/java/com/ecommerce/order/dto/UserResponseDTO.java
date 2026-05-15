package com.ecommerce.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class UserResponseDTO implements Serializable {

    private UUID userId;
    private String username;
    private String email;
    private String phone;
}