package com.ibizabroker.bibliotheque.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static final String SECRET_KEY = "learn_programming_yourself";

    private static final int TOKEN_VALIDITY = 3600 * 5;
    private static final int REFRESH_TOKEN_VALIDITY = 3600 * 24 * 7;
    private static final int RESET_TOKEN_VALIDITY = 60 * 15;
    private static final String TYPE_CLAIM = "type";

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public String generateToken(UserDetails userDetails) {

        Map<String, Object> claims = new HashMap<>();

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + TOKEN_VALIDITY * 1000))
                .signWith(SignatureAlgorithm.HS512, SECRET_KEY)
                .compact();
    }

    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TYPE_CLAIM, "refresh");
        return buildToken(username, claims, REFRESH_TOKEN_VALIDITY);
    }

    public String generateResetToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TYPE_CLAIM, "reset");
        return buildToken(username, claims, RESET_TOKEN_VALIDITY);
    }

    public String getTokenType(String token) {
        return (String) getAllClaimsFromToken(token).get(TYPE_CLAIM);
    }

    private String buildToken(String subject, Map<String, Object> claims, int validitySeconds) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + validitySeconds * 1000L))
                .signWith(SignatureAlgorithm.HS512, SECRET_KEY)
                .compact();
    }
}