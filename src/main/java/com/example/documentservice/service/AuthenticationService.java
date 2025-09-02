package com.example.documentservice.service;

import com.example.documentservice.dto.JwtAuthenticationResponse;
import com.example.documentservice.dto.SignInRequest;
import com.example.documentservice.dto.SignUpRequest;

public interface AuthenticationService {

    JwtAuthenticationResponse signup(SignUpRequest request);
    JwtAuthenticationResponse signin(SignInRequest request);
}
