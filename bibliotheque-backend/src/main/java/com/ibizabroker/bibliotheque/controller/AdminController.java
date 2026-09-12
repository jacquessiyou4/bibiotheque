package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.dto.UserCreateRequest;
import com.ibizabroker.bibliotheque.dto.UserResponse;
import com.ibizabroker.bibliotheque.entity.Role;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Tag(name = "Utilisateurs", description = "Gestion des utilisateurs (admin)")
@CrossOrigin("http://localhost:4200/")
@RestController
@RequestMapping("/admin")
@Slf4j
public class AdminController {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Operation(summary = "Créer un nouvel utilisateur")
    @PostMapping("/users")
    @PreAuthorize("hasRole('Admin')")
    public Users addUserByAdmin(@Valid @RequestBody UserCreateRequest request) {
        log.info("Requête POST /admin/users — création de l'utilisateur '{}'", request.getUsername());
        Users user = new Users();
        user.setUsername(request.getUsername());
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Set<Role> roles = new HashSet<>();
        if (request.getRoles() != null) {
            for (String roleName : request.getRoles()) {
                Role role = new Role();
                role.setRoleName(roleName);
                roles.add(role);
            }
        }
        user.setRole(roles);

        return usersRepository.save(user);
    }

    @Operation(summary = "Lister les utilisateurs (pagination)")
    @GetMapping("/users")
    @PreAuthorize("hasRole('Admin')")
    public Page<UserResponse> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "userId") String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        log.info("Requête GET /admin/users — {} utilisateurs en base", usersRepository.count());
        return usersRepository.findAll(pageable).map(this::toUserResponse);
    }

    @Operation(summary = "Obtenir un utilisateur par son identifiant")
    @PreAuthorize("hasRole('Admin')")
    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Integer id) {
        log.info("Requête GET /admin/users/{}", id);
        Users user = usersRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User with id " + id + " does not exist."));
        return ResponseEntity.ok(toUserResponse(user));
    }

    @Operation(summary = "Modifier un utilisateur existant")
    @PreAuthorize("hasRole('Admin')")
    @PutMapping("/users/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Integer id, @Valid @RequestBody UserCreateRequest userDetails) {
        log.info("Requête PUT /admin/users/{}", id);
        Users user = usersRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User with id " + id + " does not exist."));

        user.setName(userDetails.getName());
        user.setUsername(userDetails.getUsername());
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }

        Set<Role> roles = new HashSet<>();
        if (userDetails.getRoles() != null) {
            for (String roleName : userDetails.getRoles()) {
                Role role = new Role();
                role.setRoleName(roleName);
                roles.add(role);
            }
        }
        user.setRole(roles);

        Users updatedUser = usersRepository.save(user);
        return ResponseEntity.ok(toUserResponse(updatedUser));
    }

    private UserResponse toUserResponse(Users user) {
        return new UserResponse(
            user.getUserId(),
            user.getUsername(),
            user.getName(),
            user.getRole().stream()
                .map(Role::getRoleName)
                .collect(Collectors.toList())
        );
    }
}
