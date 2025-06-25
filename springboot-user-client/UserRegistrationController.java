package com.springbootclient.controller;

import com.springbootclient.dto.*;
import com.springbootclient.service.UserRegistrationClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserRegistrationController {

    @Autowired
    private UserRegistrationClientService clientService;

    @PostMapping("/register")
    public ApiResponse<UserDto> registerUser(@RequestBody UserRegistrationRequest request) {
        return clientService.registerUser(request);
    }

    @PutMapping("/{id}")
    public ApiResponse<UserDto> updateUser(@PathVariable("id") Long id,
                                           @RequestBody UserUpdateRequest request) {
        return clientService.updateUser(id, request);
    }
}
