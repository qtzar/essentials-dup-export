package com.qtzar.essentialsexport.model.essential.response;

import lombok.Data;

@Data
public class BearerTokenResponse {
    String bearerToken;
    String refreshToken;
    int expiresInMinutes;
    int refreshTokenExpiresInMinutes;
}
