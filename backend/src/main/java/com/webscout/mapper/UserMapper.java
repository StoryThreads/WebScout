package com.webscout.mapper;

import com.webscout.dto.UserResponse;
import com.webscout.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {

        UserResponse response = new UserResponse();

        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setStatus(user.getStatus());

        return response;
    }
}