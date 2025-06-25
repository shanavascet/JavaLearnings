// Project Structure: springboot-user-client
// Copy the proto file: user-registration.proto (same as Quarkus service)

// ========================================
// src/main/java/com/springbootclient/config/GrpcClientConfig.java

package com.springbootclient.config;

import com.quarkuspoc.grpc.UserRegistrationServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfig {
    
    @Value("${grpc.client.quarkus-poc.host:localhost}")
    private String grpcHost;
    
    @Value("${grpc.client.quarkus-poc.port:9000}")
    private int grpcPort;
    
    @Bean
    public ManagedChannel userRegistrationChannel() {
        return ManagedChannelBuilder.forAddress(grpcHost, grpcPort)
                .usePlaintext()
                .keepAliveTime(30, java.util.concurrent.TimeUnit.SECONDS)
                .keepAliveTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .maxInboundMessageSize(4 * 1024 * 1024) // 4MB
                .build();
    }
    
    @Bean
    public UserRegistrationServiceGrpc.UserRegistrationServiceBlockingStub userRegistrationBlockingStub(ManagedChannel channel) {
        return UserRegistrationServiceGrpc.newBlockingStub(channel);
    }
    
    @Bean
    public UserRegistrationServiceGrpc.UserRegistrationServiceStub userRegistrationAsyncStub(ManagedChannel channel) {
        return UserRegistrationServiceGrpc.newStub(channel);
    }
}

// ========================================
// src/main/java/com/springbootclient/dto/UserRegistrationRequest.java

package com.springbootclient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    
    @Size(max = 100, message = "First name should not exceed 100 characters")
    @JsonProperty("first_name")
    private String firstName;
    
    @Size(max = 100, message = "Last name should not exceed 100 characters")
    @JsonProperty("last_name")
    private String lastName;
    
    @Size(max = 20, message = "Phone should not exceed 20 characters")
    private String phone;
    
    @Size(max = 500, message = "Address should not exceed 500 characters")
    private String address;
}

// ========================================
// src/main/java/com/springbootclient/dto/UserUpdateRequest.java

package com.springbootclient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    
    @Email(message = "Email should be valid")
    private String email;
    
    @Size(max = 100, message = "First name should not exceed 100 characters")
    @JsonProperty("first_name")
    private String firstName;
    
    @Size(max = 100, message = "Last name should not exceed 100 characters")
    @JsonProperty("last_name")
    private String lastName;
    
    @Size(max = 20, message = "Phone should not exceed 20 characters")
    private String phone;
    
    @Size(max = 500, message = "Address should not exceed 500 characters")
    private String address;
}

// ========================================
// src/main/java/com/springbootclient/dto/UserDto.java

package com.springbootclient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    
    @JsonProperty("user_id")
    private Long userId;
    
    private String email;
    
    @JsonProperty("first_name")
    private String firstName;
    
    @JsonProperty("last_name")
    private String lastName;
    
    private String phone;
    
    private String address;
    
    @JsonProperty("created_at")
    private String createdAt;
    
    @JsonProperty("updated_at")
    private String updatedAt;
    
    private String status;
}

// ========================================
// src/main/java/com/springbootclient/dto/ApiResponse.java

package com.springbootclient.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}

// ========================================
// src/main/java/com/springbootclient/service/UserRegistrationClientService.java

package com.springbootclient.service;

import com.springbootclient.dto.UserRegistrationRequest;
import com.springbootclient.dto.UserUpdateRequest;
import com.springbootclient.dto.UserDto;
import com.springbootclient.dto.ApiResponse;
import com.quarkuspoc.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class UserRegistrationClientService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserRegistrationClientService.class);
    
    @Autowired
    private UserRegistrationServiceGrpc.UserRegistrationServiceBlockingStub userRegistrationStub;
    
    public ApiResponse<UserDto> registerUser(UserRegistrationRequest request) {
        try {
            logger.info("Calling gRPC service to register user: {}", request.getEmail());
            
            RegisterUserRequest grpcRequest = RegisterUserRequest.newBuilder()
                    .setEmail(request.getEmail())
                    .setPassword(request.getPassword())
                    .setFirstName(request.getFirstName() != null ? request.getFirstName() : "")
                    .setLastName(request.getLastName() != null ? request.getLastName() : "")
                    .setPhone(request.getPhone() != null ? request.getPhone() : "")
                    .setAddress(request.getAddress() != null ? request.getAddress() : "")
                    .build();
            
            RegisterUserResponse response = userRegistrationStub
                    .withDeadlineAfter(10, TimeUnit.SECONDS)
                    .registerUser(grpcRequest);
            
            if (response.getSuccess()) {
                UserDto userDto = convertToUserDto(response.getUser());
                return ApiResponse.success(response.getMessage(), userDto);
            } else {
                return ApiResponse.error(response.getMessage());
            }
            
        } catch (Exception e) {
            logger.error("Error calling gRPC service for user registration: ", e);
            return ApiResponse.error("Failed to register user: " + e.getMessage());
        }
    }
    
    public ApiResponse<UserDto> updateUser(Long userId, UserUpdateRequest request) {
        try {
            logger.info("Calling gRPC service to update user: {}", userId);
            
            UpdateUserRequest grpcRequest = UpdateUserRequest.newBuilder()
                    .setUserId(userId)
                    .setEmail(request.getEmail() != null ? request.getEmail() : "")
                    .setFirstName(request.getFirstName() != null ? request.getFirstName() : "")
                    .setLastName(request.getLastName
