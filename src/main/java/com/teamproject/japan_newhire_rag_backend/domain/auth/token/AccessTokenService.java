package com.teamproject.japan_newhire_rag_backend.domain.auth.token;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

import com.teamproject.japan_newhire_rag_backend.domain.auth.config.AuthTokenProperties;

@Component
public class AccessTokenService {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access";

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final AuthTokenProperties properties;
    private final Clock clock;

    public AccessTokenService(
            JwtEncoder jwtEncoder,
            JwtDecoder jwtDecoder,
            AuthTokenProperties properties,
            Clock clock
    ) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.properties = properties;
        this.clock = clock;
    }

    public String issue(Long appUserId) {
        if (appUserId == null) {
            throw new IllegalArgumentException("appUserId must not be null");
        }

        Instant issuedAt = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(appUserId.toString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(properties.accessTokenExpiration()))
                .id(UUID.randomUUID().toString())
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public Long validateAndExtractAppUserId(String token) {
        Jwt jwt = jwtDecoder.decode(token);
        if (!ACCESS_TOKEN_TYPE.equals(jwt.getClaimAsString(TOKEN_TYPE_CLAIM))) {
            throw new BadJwtException("JWT token_type must be access");
        }

        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException | NullPointerException exception) {
            throw new BadJwtException("JWT subject must be a valid appUserId", exception);
        }
    }
}
