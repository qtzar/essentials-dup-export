package com.qtzar.essentialsexport.model.essential.request;

import lombok.Data;

@Data
public class BearerTokenBody {
    String grantType;
    String username;
    String password;
}
