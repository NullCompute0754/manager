package me.ncexce.manager.service;

import java.time.LocalDateTime;
import java.util.Optional;

import me.ncexce.manager.entity.BranchEntity;
import me.ncexce.manager.exceptions.UserExistsException;
import me.ncexce.manager.pojo.CommitHistoryItem;
import me.ncexce.manager.pojo.dto.RegisterRequestDTO;
import me.ncexce.manager.pojo.dto.RegisterResponseDTO;
import me.ncexce.manager.repository.BranchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import me.ncexce.manager.entity.UserEntity;
import me.ncexce.manager.pojo.dto.LoginRequestDTO;
import me.ncexce.manager.pojo.dto.LoginResponseDTO;
import me.ncexce.manager.repository.UserRepository;
import me.ncexce.manager.security.JwtService;
import me.ncexce.manager.exceptions.UserNotFoundException;
import me.ncexce.manager.exceptions.InvalidCredentialsException;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Autowired
    public AuthService(UserRepository userRepository, BranchRepository branchRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        Optional<UserEntity> optionalUser = userRepository.findByUsername(request.getUsername());
        if (!optionalUser.isPresent()) {
            throw new UserNotFoundException("User not found");
        }

        UserEntity user = optionalUser.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Wrong password");
        }
        String token = jwtService.generateToken(user.getUsername(), user.getRole(), user.getSecurityRole());

        LoginResponseDTO response = new LoginResponseDTO();
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        response.setToken(token);
        response.setStatusCode(HttpStatus.OK.value());

        return response;
    }

    @Transactional
    public RegisterResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByUsername(request.getUsername())) throw new UserExistsException("The username exists");
        UserEntity user = new UserEntity();
        BranchEntity branch = new BranchEntity();

        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setSecurityRole("ADMIN");

        userRepository.save(user);

        branch.setName(request.getUsername());
        branch.setUpdatedAt(LocalDateTime.now());
        branch.setMaster(false);
        branch.setHeadCommitId(null);

        branchRepository.save(branch);

        return new RegisterResponseDTO(user.getUsername(), 200, "User registered successfully");
    }

}
