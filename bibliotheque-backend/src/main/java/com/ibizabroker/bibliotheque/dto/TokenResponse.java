package com.ibizabroker.bibliotheque.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class TokenResponse {

    @Schema(description = "Jeton d'accès à coller dans « Authorize » > bearerAuth")
    private String accessToken;

    @Schema(description = "Affiché pour information : non utilisable depuis Swagger. "
            + "Quand l'access token expire, relancer POST /auth/token.")
    private String refreshToken;

    @Schema(example = "Bearer")
    private String tokenType;

    @Schema(description = "Durée de validité de l'access token, en secondes", example = "1800")
    private long expiresIn;

    @Schema(description = "Durée de validité du refresh token, en secondes", example = "1800")
    private long refreshExpiresIn;

    @Schema(example = "profile email")
    private String scope;

    public TokenResponse() {}

    public TokenResponse(String accessToken, String refreshToken, String tokenType,
                         long expiresIn, long refreshExpiresIn, String scope) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.refreshExpiresIn = refreshExpiresIn;
        this.scope = scope;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }

    public long getRefreshExpiresIn() { return refreshExpiresIn; }
    public void setRefreshExpiresIn(long refreshExpiresIn) { this.refreshExpiresIn = refreshExpiresIn; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
}
