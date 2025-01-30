package com.cms.dto;

import com.cms.enums.UserRole;

public class AuthenticationResponse {
    private final String jwt;
    private final UserRole userRole;

    public AuthenticationResponse(String jwt, UserRole userRole) {
        this.jwt = jwt;
        this.userRole = userRole;
    }

    public String getJwt() {
        return jwt;
    }

    public UserRole getUserRole() {
        return userRole;
    }
}

