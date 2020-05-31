package com.eirsteir.coffeewithme.commons.security;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;

@Getter
public class JwtConfig {

    @Value("${security.jwt.uri:/auth/**}")
    private String Uri;

    // Tomcat converts all headers to lowercase
    @Value("${security.jwt.header:authorization}")
    private String header;

    @Value("${security.jwt.prefix:Bearer }")
    private String prefix;

    @Value("${security.jwt.expiration:#{24*60*60}}")
    private int expiration;

    @Value("${security.jwt.secret:JwtSecretKey}")
    private String secret;

}
