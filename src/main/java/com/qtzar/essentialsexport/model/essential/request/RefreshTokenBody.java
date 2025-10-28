package com.qtzar.essentialsexport.model.essential.request;

import lombok.Data;

@Data
public class RefreshTokenBody {
    String grantType;
    String refreshToken;
}
