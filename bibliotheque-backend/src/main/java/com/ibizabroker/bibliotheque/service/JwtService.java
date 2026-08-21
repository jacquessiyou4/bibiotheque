package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.AdherentRepository;
import com.ibizabroker.bibliotheque.dao.AdministratorRepository;
import com.ibizabroker.bibliotheque.dto.ForgotPasswordResponse;
import com.ibizabroker.bibliotheque.dto.RefreshTokenRequest;
import com.ibizabroker.bibliotheque.dto.RegisterRequest;
import com.ibizabroker.bibliotheque.dto.RegisterResponse;
import com.ibizabroker.bibliotheque.dto.ResetPasswordRequest;
import com.ibizabroker.bibliotheque.entity.Adherent;
import com.ibizabroker.bibliotheque.entity.Administrator;
import com.ibizabroker.bibliotheque.entity.JwtRequest;
import com.ibizabroker.bibliotheque.entity.JwtResponse;
import com.ibizabroker.bibliotheque.exceptions.BadRequestException;
import com.ibizabroker.bibliotheque.exceptions.ConflictException;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import com.ibizabroker.bibliotheque.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class JwtService implements UserDetailsService {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AdministratorRepository administratorRepository;

    @Autowired
    private AdherentRepository adherentRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public JwtResponse createJwtToken(JwtRequest jwtRequest) throws Exception {
        String username = jwtRequest.getUsername();
        String password = jwtRequest.getPassword();
        authenticate(username, password);

        UserDetails userDetails = loadUserByUsername(username);
        String accessToken = jwtUtil.generateToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(username);

        return buildJwtResponse(username, accessToken, refreshToken);
    }

    public JwtResponse refreshToken(RefreshTokenRequest request) throws Exception {
        String token = request.getRefreshToken();
        if (token == null || !"refresh".equals(readTokenType(token))) {
            throw new BadRequestException("Token invalide pour cette opération (un refresh token est attendu).");
        }

        String username = jwtUtil.getUsernameFromToken(token);
        UserDetails userDetails = loadUserByUsername(username);
        String accessToken = jwtUtil.generateToken(userDetails);
        String newRefreshToken = jwtUtil.generateRefreshToken(username);

        return buildJwtResponse(username, accessToken, newRefreshToken);
    }

    public RegisterResponse register(RegisterRequest request) {
        if (request.getUsername() == null || request.getPassword() == null || request.getRole() == null) {
            throw new BadRequestException("username, password et role sont obligatoires");
        }

        String encryptedPassword = passwordEncoder.encode(request.getPassword());

        if ("Administrator".equalsIgnoreCase(request.getRole())) {
            Administrator administrator = new Administrator();
            administrator.setUsername(request.getUsername());
            administrator.setName(request.getName());
            administrator.setPassword(encryptedPassword);
            administrator.setMatricule(request.getMatricule());
            Administrator saved = administratorRepository.save(administrator);
            return new RegisterResponse(saved.getUserId(), saved.getUsername(), saved.getName(), saved.getMatricule(), "Admin");
        }

        if ("Adherent".equalsIgnoreCase(request.getRole())) {
            Adherent adherent = new Adherent();
            adherent.setUsername(request.getUsername());
            adherent.setName(request.getName());
            adherent.setPassword(encryptedPassword);
            adherent.setMatricule(request.getMatricule());
            Adherent saved = adherentRepository.save(adherent);
            return new RegisterResponse(saved.getUserId(), saved.getUsername(), saved.getName(), saved.getMatricule(), "User");
        }

        throw new BadRequestException("role doit être 'Administrator' ou 'Adherent'");
    }

    public ForgotPasswordResponse forgotPassword(String username) {
        boolean exists = administratorRepository.findByUsername(username).isPresent()
                || adherentRepository.findByUsername(username).isPresent();
        if (!exists) {
            throw new NotFoundException("Aucun compte avec le nom d'utilisateur \"" + username + "\".");
        }
        return new ForgotPasswordResponse(jwtUtil.generateResetToken(username));
    }

    public void resetPassword(ResetPasswordRequest request) {
        String token = request.getResetToken();
        if (token == null || !"reset".equals(readTokenType(token))) {
            throw new BadRequestException("Token invalide pour cette opération (un reset token est attendu).");
        }

        String username = jwtUtil.getUsernameFromToken(token);
        String encryptedPassword = passwordEncoder.encode(request.getNewPassword());

        Optional<Administrator> administrator = administratorRepository.findByUsername(username);
        if (administrator.isPresent()) {
            Administrator a = administrator.get();
            a.setPassword(encryptedPassword);
            administratorRepository.save(a);
            return;
        }

        Adherent adherent = adherentRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Compte introuvable pour \"" + username + "\"."));
        adherent.setPassword(encryptedPassword);
        adherentRepository.save(adherent);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<Administrator> administrator = administratorRepository.findByUsername(username);
        if (administrator.isPresent()) {
            Administrator a = administrator.get();
            return new org.springframework.security.core.userdetails.User(
                    a.getUsername(), a.getPassword(), Collections.singleton(new SimpleGrantedAuthority("ROLE_Admin")));
        }

        Optional<Adherent> adherent = adherentRepository.findByUsername(username);
        if (adherent.isPresent()) {
            Adherent u = adherent.get();
            return new org.springframework.security.core.userdetails.User(
                    u.getUsername(), u.getPassword(), Collections.singleton(new SimpleGrantedAuthority("ROLE_User")));
        }

        throw new UsernameNotFoundException("User not found with username: " + username);
    }

    private JwtResponse buildJwtResponse(String username, String accessToken, String refreshToken) {
        Optional<Administrator> administrator = administratorRepository.findByUsername(username);
        if (administrator.isPresent()) {
            Administrator a = administrator.get();
            return new JwtResponse(a.getUserId(), a.getUsername(), a.getName(), a.getMatricule(), "Admin", accessToken, refreshToken);
        }

        Adherent adherent = adherentRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        return new JwtResponse(adherent.getUserId(), adherent.getUsername(), adherent.getName(), adherent.getMatricule(), "User", accessToken, refreshToken);
    }

    private String readTokenType(String token) {
        try {
            return jwtUtil.getTokenType(token);
        } catch (ExpiredJwtException e) {
            throw new ConflictException("Token expiré, veuillez recommencer.");
        }
    }

    private void authenticate(String userName, String userPassword) throws Exception {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userName, userPassword));
        } catch (DisabledException e) {
            throw new Exception("USER_DISABLED", e);
        } catch (BadCredentialsException e) {
            throw new Exception("INVALID_CREDENTIALS", e);
        }
    }
}
