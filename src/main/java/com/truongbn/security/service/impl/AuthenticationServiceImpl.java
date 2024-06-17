package com.truongbn.security.service.impl;

import com.truongbn.security.service.exception.InvalidLoginException;
import com.truongbn.security.service.exception.UserAlreadyExistsException;
import com.truongbn.security.service.exception.UserNotFoundException;
import java.util.Optional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.truongbn.security.dao.request.SignUpRequest;
import com.truongbn.security.dao.request.SigninRequest;
import com.truongbn.security.dao.response.JwtAuthenticationResponse;
import com.truongbn.security.entities.Role;
import com.truongbn.security.entities.User;
import com.truongbn.security.repository.UserRepository;
import com.truongbn.security.service.AuthenticationService;
import com.truongbn.security.service.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    @Override
    public JwtAuthenticationResponse signup(SignUpRequest request) {
        Optional<User> optional = userRepository.findByUserId(request.getUserId());
        optional.ifPresent(user -> {
            throw new UserAlreadyExistsException("User Id: " + request.getUserId() + " already exists.");
        });

        var user = User.builder()
                .name(request.getName())
                .userId(request.getUserId())
                .password(passwordEncoder.encode(request.getPassword()))
                .regNo(request.getRegNo())
                .role(Role.USER)
                .build();

        userRepository.save(user);

        var jwt = jwtService.generateToken(user);

        return JwtAuthenticationResponse.builder().token(jwt).build();
    }

    @Override
    public JwtAuthenticationResponse signin(SigninRequest request) {
        // 사용자 인증을 시도합니다.
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUserId(), request.getPassword()));
        } catch (AuthenticationException e) {
            throw new InvalidLoginException("Invalid userId or password.");
        }

        // 이메일로 사용자를 검색합니다.
        var user = userRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User Id: " + request.getUserId() + " not found."));

        // JWT 토큰을 생성합니다.
        var jwt = jwtService.generateToken(user);

        // JWT 토큰을 반환합니다.
        return JwtAuthenticationResponse.builder().token(jwt).build();
    }
}
