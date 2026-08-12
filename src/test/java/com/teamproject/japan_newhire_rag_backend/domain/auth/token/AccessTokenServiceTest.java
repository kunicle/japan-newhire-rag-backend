package com.teamproject.japan_newhire_rag_backend.domain.auth.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtException;

import com.teamproject.japan_newhire_rag_backend.domain.auth.config.AuthTokenProperties;
import com.teamproject.japan_newhire_rag_backend.domain.auth.config.JwtInfrastructureConfig;

class AccessTokenServiceTest {

    private static final String SECRET_A = "0123456789abcdef0123456789abcdef";
    private static final String SECRET_B = "abcdef0123456789abcdef0123456789";
    private static final Instant NOW = Instant.parse("2026-08-11T12:00:00Z");

    @Test
    void issueStoresAppUserIdAndRequiredAccessClaims() {
        TokenFixture fixture = fixture(SECRET_A, Clock.fixed(NOW, ZoneOffset.UTC));

        String token = fixture.service().issue(42L);
        Jwt jwt = fixture.decoder().decode(token);

        assertEquals("42", jwt.getSubject());
        assertEquals("access", jwt.getClaimAsString("token_type"));
        assertEquals(NOW, jwt.getIssuedAt());
        assertEquals(NOW.plus(Duration.ofMinutes(15)), jwt.getExpiresAt());
        assertNotNull(jwt.getId());
        assertFalse(jwt.getId().isBlank());
        assertEquals(42L, fixture.service().validateAndExtractAppUserId(token));
    }

    @Test
    void validateSucceedsImmediatelyBeforeExpiration() {
        MutableClock clock = new MutableClock(NOW, ZoneOffset.UTC);
        TokenFixture fixture = fixture(SECRET_A, clock);
        String token = fixture.service().issue(42L);
        clock.setInstant(NOW.plus(Duration.ofMinutes(15)).minusMillis(1));

        assertEquals(42L, fixture.service().validateAndExtractAppUserId(token));
    }

    @Test
    void validateRejectsExpiredToken() {
        MutableClock clock = new MutableClock(NOW, ZoneOffset.UTC);
        TokenFixture fixture = fixture(SECRET_A, clock);
        String token = fixture.service().issue(42L);
        clock.setInstant(NOW.plus(Duration.ofMinutes(16)).plusSeconds(1));

        assertThrows(
                JwtException.class,
                () -> fixture.service().validateAndExtractAppUserId(token));
    }

    @Test
    void validateRejectsTokenSignedWithDifferentSecret() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        TokenFixture issuer = fixture(SECRET_A, clock);
        TokenFixture validator = fixture(SECRET_B, clock);
        String token = issuer.service().issue(42L);

        assertThrows(
                JwtException.class,
                () -> validator.service().validateAndExtractAppUserId(token));
    }

    @Test
    void validateRejectsTamperedToken() {
        TokenFixture fixture = fixture(SECRET_A, Clock.fixed(NOW, ZoneOffset.UTC));
        String token = fixture.service().issue(42L);
        int signatureStart = token.lastIndexOf('.') + 1;
        int changeIndex = signatureStart + 3;
        char replacement = token.charAt(changeIndex) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, changeIndex)
                + replacement
                + token.substring(changeIndex + 1);

        assertThrows(
                JwtException.class,
                () -> fixture.service().validateAndExtractAppUserId(tampered));
    }

    @Test
    void validateRejectsTokenWhoseTypeIsNotAccess() {
        AuthTokenProperties properties = properties(SECRET_A);
        JwtInfrastructureConfig config = new JwtInfrastructureConfig();
        JwtEncoder encoder = config.jwtEncoder(config.jwtSecretKey(properties));
        JwtDecoder decoder = mock(JwtDecoder.class);
        Jwt jwt = mock(Jwt.class);
        when(decoder.decode("refresh-token")).thenReturn(jwt);
        when(jwt.getClaimAsString("token_type")).thenReturn("refresh");
        AccessTokenService service = new AccessTokenService(
                encoder,
                decoder,
                properties,
                Clock.systemUTC());

        assertThrows(
                JwtException.class,
                () -> service.validateAndExtractAppUserId("refresh-token"));
    }

    private TokenFixture fixture(String secret, Clock clock) {
        AuthTokenProperties properties = properties(secret);
        JwtInfrastructureConfig config = new JwtInfrastructureConfig();
        SecretKey secretKey = config.jwtSecretKey(properties);
        JwtEncoder encoder = config.jwtEncoder(secretKey);
        JwtDecoder decoder = config.jwtDecoder(secretKey, clock);
        AccessTokenService service = new AccessTokenService(
                encoder,
                decoder,
                properties,
                clock);
        return new TokenFixture(service, decoder);
    }

    private AuthTokenProperties properties(String secret) {
        return new AuthTokenProperties(
                secret,
                Duration.ofMinutes(15),
                Duration.ofDays(14));
    }

    private record TokenFixture(AccessTokenService service, JwtDecoder decoder) {
    }

    private static final class MutableClock extends Clock {

        private Instant instant;
        private final ZoneId zone;

        private MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        private void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
