package me.ncexce.manager.service;

import lombok.RequiredArgsConstructor;

import me.ncexce.manager.entity.*;
import me.ncexce.manager.exceptions.InvalidCredentialsException;
import me.ncexce.manager.exceptions.UserNotFoundException;
import me.ncexce.manager.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AssetService {
    private final UserRepository userRepo;

    public UserEntity getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new InvalidCredentialsException("No authentication found");

        String username = auth.getName();
        return userRepo.findByUsername(username).orElseThrow(() -> new UserNotFoundException("User not found"));
    }

}
