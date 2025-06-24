// Project Structure: quarkus-poc
// src/main/proto/user-registration.proto

syntax = "proto3";

option java_multiple_files = true;
option java_package = "com.quarkuspoc.grpc";
option java_outer_classname = "UserRegistrationProto";

package userregistration;

service UserRegistrationService {
  rpc RegisterUser(RegisterUserRequest) returns (RegisterUserResponse);
  rpc UpdateUser(UpdateUserRequest) returns (UpdateUserResponse);
  rpc GetUser(GetUserRequest) returns (GetUserResponse);
  rpc DeleteUser(DeleteUserRequest) returns (DeleteUserResponse);
}

message RegisterUserRequest {
  string email = 1;
  string password = 2;
  string first_name = 3;
  string last_name = 4;
  string phone = 5;
  string address = 6;
}

message RegisterUserResponse {
  bool success = 1;
  string message = 2;
  UserDto user = 3;
}

message UpdateUserRequest {
  int64 user_id = 1;
  string email = 2;
  string first_name = 3;
  string last_name = 4;
  string phone = 5;
  string address = 6;
}

message UpdateUserResponse {
  bool success = 1;
  string message = 2;
  UserDto user = 3;
}

message GetUserRequest {
  int64 user_id = 1;
}

message GetUserResponse {
  bool success = 1;
  string message = 2;
  UserDto user = 3;
}

message DeleteUserRequest {
  int64 user_id = 1;
}

message DeleteUserResponse {
  bool success = 1;
  string message = 2;
}

message UserDto {
  int64 user_id = 1;
  string email = 2;
  string first_name = 3;
  string last_name = 4;
  string phone = 5;
  string address = 6;
  string created_at = 7;
  string updated_at = 8;
  string status = 9;
}

// ========================================
// src/main/java/com/quarkuspoc/entity/UserRegistration.java

package com.quarkuspoc.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_registrations")
public class UserRegistration extends PanacheEntityBase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    public Long userId;
    
    @Column(name = "email", unique = true, nullable = false, length = 255)
    public String email;
    
    @Column(name = "password_hash", nullable = false)
    public String passwordHash;
    
    @Column(name = "first_name", length = 100)
    public String firstName;
    
    @Column(name = "last_name", length = 100)
    public String lastName;
    
    @Column(name = "phone", length = 20)
    public String phone;
    
    @Column(name = "address", columnDefinition = "TEXT")
    public String address;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public UserStatus status = UserStatus.ACTIVE;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    public LocalDateTime updatedAt;
    
    public enum UserStatus {
        ACTIVE, INACTIVE, SUSPENDED, DELETED
    }
    
    public static UserRegistration findByUserId(Long userId) {
        return find("userId = ?1 and status != ?2", userId, UserStatus.DELETED).firstResult();
    }
    
    public static UserRegistration findByEmail(String email) {
        return find("email = ?1 and status != ?2", email, UserStatus.DELETED).firstResult();
    }
    
    public static boolean emailExists(String email) {
        return count("email = ?1 and status != ?2", email, UserStatus.DELETED) > 0;
    }
}

// ========================================
// src/main/java/com/quarkuspoc/repository/UserRegistrationRepository.java

package com.quarkuspoc.repository;

import com.quarkuspoc.entity.UserRegistration;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

@ApplicationScoped
public class UserRegistrationRepository implements PanacheRepository<UserRegistration> {
    
    @Transactional
    public UserRegistration registerUser(String email, String password, String firstName, 
                                       String lastName, String phone, String address) {
        
        // Check if email already exists
        if (UserRegistration.emailExists(email)) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        UserRegistration user = new UserRegistration();
        user.email = email;
        user.passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
        user.firstName = firstName;
        user.lastName = lastName;
        user.phone = phone;
        user.address = address;
        user.status = UserRegistration.UserStatus.ACTIVE;
        
        user.persist();
        return user;
    }
    
    @Transactional
    public UserRegistration updateUser(Long userId, String email, String firstName, 
                                     String lastName, String phone, String address) {
        
        UserRegistration user = UserRegistration.findByUserId(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        
        // Check email uniqueness if email is being changed
        if (email != null && !email.equals(user.email) && UserRegistration.emailExists(email)) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        if (email != null && !email.trim().isEmpty()) {
            user.email = email;
        }
        if (firstName != null && !firstName.trim().isEmpty()) {
            user.firstName = firstName;
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            user.lastName = lastName;
        }
        if (phone != null && !phone.trim().isEmpty()) {
            user.phone = phone;
        }
        if (address != null && !address.trim().isEmpty()) {
            user.address = address;
        }
        
        user.persist();
        return user;
    }
    
    public UserRegistration findByUserId(Long userId) {
        return UserRegistration.findByUserId(userId);
    }
    
    public UserRegistration findByEmail(String email) {
        return UserRegistration.findByEmail(email);
    }
    
    @Transactional
    public boolean deleteUser(Long userId) {
        UserRegistration user = UserRegistration.findByUserId(userId);
        if (user != null) {
            user.status = UserRegistration.UserStatus.DELETED;
            user.persist();
            return true;
        }
        return false;
    }
    
    public List<UserRegistration> findActiveUsers() {
        return list("status", UserRegistration.UserStatus.ACTIVE);
    }
}

// ========================================
// src/main/java/com/quarkuspoc/service/UserRegistrationGrpcService.java

package com.quarkuspoc.service;

import com.quarkuspoc.entity.UserRegistration;
import com.quarkuspoc.grpc.*;
import com.quarkuspoc.repository.UserRegistrationRepository;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class UserRegistrationGrpcService implements UserRegistrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserRegistrationGrpcService.class);
    
    @Inject
    UserRegistrationRepository userRepository;
    
    @Override
    public Uni<RegisterUserResponse> registerUser(RegisterUserRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                logger.info("Registering new user with email: {}", request.getEmail());
                
                // Validation
                if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                    return RegisterUserResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage("Email is required")
                            .build();
                }
                
                if (request.getPassword() == null || request.getPassword().length() < 6) {
                    return RegisterUserResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage("Password must be at least 6 characters")
                            .build();
                }
                
                // Register user
                UserRegistration newUser = userRepository.registerUser(
                    request.getEmail(),
                    request.getPassword(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getPhone(),
                    request.getAddress()
                );
                
                UserDto userDto = convertToUserDto(newUser);
                
                return RegisterUserResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("User registered successfully")
                        .setUser(userDto)
                        .build();
                        
            } catch (IllegalArgumentException e) {
                logger.warn("Registration failed: {}", e.getMessage());
                return RegisterUserResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage(e.getMessage())
                        .build();
            } catch (Exception e) {
                logger.error("Error registering user: ", e);
                return RegisterUserResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Registration failed: " + e.getMessage())
                        .build();
            }
        });
    }
    
    @Override
    public Uni<UpdateUserResponse> updateUser(UpdateUserRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                logger.info("Updating user with ID: {}", request.getUserId());
                
                if (request.getUserId() <= 0) {
                    return UpdateUserResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage("Invalid user ID")
                            .build();
                }
                
                UserRegistration updatedUser = userRepository.updateUser(
                    request.getUserId(),
                    request.getEmail(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getPhone(),
                    request.getAddress()
                );
                
                UserDto userDto = convertToUserDto(updatedUser);
                
                return UpdateUserResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("User updated successfully")
                        .setUser(userDto)
                        .build();
                        
            } catch (IllegalArgumentException e) {
                logger.warn("Update failed: {}", e.getMessage());
                return UpdateUserResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage(e.getMessage())
                        .build();
            } catch (Exception e) {
                logger.error("Error updating user: ", e);
                return UpdateUserResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Update failed: " + e.getMessage())
                        .build();
            }
        });
    }
    
    @Override
    public Uni<GetUserResponse> getUser(GetUserRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                logger.info("Getting user with ID: {}", request.getUserId());
                
                UserRegistration user = userRepository.findByUserId(request.getUserId());
                
                if (user == null) {
                    return GetUserResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage("User not found")
                            .build();
                }
                
                UserDto userDto = convertToUserDto(user);
                
                return GetUserResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("User retrieved successfully")
                        .setUser(userDto)
                        .build();
                        
            } catch (Exception e) {
                logger.error("Error getting user: ", e);
                return GetUserResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Failed to get user: " + e.getMessage())
                        .build();
            }
        });
    }
    
    @Override
    public Uni<DeleteUserResponse> deleteUser(DeleteUserRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                logger.info("Deleting user with ID: {}", request.getUserId());
                
                boolean deleted = userRepository.deleteUser(request.getUserId());
                
                if (deleted) {
                    return DeleteUserResponse.newBuilder()
                            .setSuccess(true)
                            .setMessage("User deleted successfully")
                            .build();
                } else {
                    return DeleteUserResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage("User not found")
                            .build();
                }
                
            } catch (Exception e) {
                logger.error("Error deleting user: ", e);
                return DeleteUserResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Failed to delete user: " + e.getMessage())
                        .build();
            }
        });
    }
    
    private UserDto convertToUserDto(UserRegistration user) {
        return UserDto.newBuilder()
                .setUserId(user.userId)
                .setEmail(user.email != null ? user.email : "")
                .setFirstName(user.firstName != null ? user.firstName : "")
                .setLastName(user.lastName != null ? user.lastName : "")
                .setPhone(user.phone != null ? user.phone : "")
                .setAddress(user.address != null ? user.address : "")
                .setCreatedAt(user.createdAt != null ? user.createdAt.toString() : "")
                .setUpdatedAt(user.updatedAt != null ? user.updatedAt.toString() : "")
                .setStatus(user.status != null ? user.status.toString() : "ACTIVE")
                .build();
    }
}

// ========================================
// Complete pom.xml for quarkus-poc

<?xml version="1.0"?>
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" 
         xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.quarkuspoc</groupId>
  <artifactId>quarkus-poc</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  
  <properties>
    <compiler-plugin.version>3.11.0</compiler-plugin.version>
    <maven.compiler.release>17</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
    <quarkus.platform.artifact-id>quarkus-bom</quarkus.platform.artifact-id>
    <quarkus.platform.group-id>io.quarkus.platform</quarkus.platform.group-id>
    <quarkus.platform.version>3.6.4</quarkus.platform.version>
    <skipITs>true</skipITs>
    <surefire-plugin.version>3.0.0</surefire-plugin.version>
  </properties>
  
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>${quarkus.platform.group-id}</groupId>
        <artifactId>${quarkus.platform.artifact-id}</artifactId>
        <version>${quarkus.platform.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  
  <dependencies>
    <!-- Quarkus gRPC -->
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-grpc</artifactId>
    </dependency>
    
    <!-- Database -->
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-hibernate-orm-panache</artifactId>
    </dependency>
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-jdbc-postgresql</artifactId>
    </dependency>
    
    <!-- Password Hashing -->
    <dependency>
      <groupId>org.mindrot</groupId>
      <artifactId>jbcrypt</artifactId>
      <version>0.4</version>
    </dependency>
    
    <!-- Validation -->
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-hibernate-validator</artifactId>
    </dependency>
    
    <!-- Core -->
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-arc</artifactId>
    </dependency>
    
    <!-- Testing -->
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-junit5</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
  
  <build>
    <plugins>
      <plugin>
        <groupId>${quarkus.platform.group-id}</groupId>
        <artifactId>quarkus-maven-plugin</artifactId>
        <version>${quarkus.platform.version}</version>
        <extensions>true</extensions>
        <executions>
          <execution>
            <goals>
              <goal>build</goal>
              <goal>generate-code</goal>
              <goal>generate-code-tests</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
      
      <plugin>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>${compiler-plugin.version}</version>
        <configuration>
          <compilerArgs>
            <arg>-parameters</arg>
          </compilerArgs>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>

// ========================================
// src/main/resources/application.properties

# Database Configuration
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=${DB_USERNAME:quarkus}
quarkus.datasource.password=${DB_PASSWORD:quarkus}
quarkus.datasource.jdbc.url=${DB_URL:jdbc:postgresql://localhost:5432/quarkus_poc}

# Connection Pool
quarkus.datasource.jdbc.min-size=5
quarkus.datasource.jdbc.max-size=20

# Hibernate Configuration
quarkus.hibernate-orm.database.generation=update
quarkus.hibernate-orm.log.sql=true
quarkus.hibernate-orm.sql-load-script=import.sql

# gRPC Configuration
quarkus.grpc.server.port=9000
quarkus.grpc.server.host=0.0.0.0
quarkus.grpc.server.enable-reflection-service=true

# Logging
quarkus.log.level=INFO
quarkus.log.category."com.quarkuspoc".level=DEBUG
quarkus.log.category."org.hibernate.SQL".level=DEBUG

# Application
quarkus.application.name=quarkus-poc
quarkus.application.version=1.0.0

// ========================================
// Database Schema: src/main/resources/import.sql

DROP TABLE IF EXISTS user_registrations;

CREATE TABLE user_registrations (
    user_id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    address TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_email ON user_registrations(email);
CREATE INDEX idx_user_status ON user_registrations(status);

-- Sample test user (password: "password123")
INSERT INTO user_registrations (email, password_hash, first_name, last_name, phone, address, status) 
VALUES ('test@quarkuspoc.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye2J9YB4/YQ3cLMAKoP4hA5u6w2XM5.3u', 'Test', 'User', '+1234567890', '123 Test St, Test City', 'ACTIVE');
