package com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class LoginTokenPairTest {

    @Test
    void toStringRedactsBothTokenValuesWithoutChangingAccessors() {
        String accessToken = "actual-access-token";
        String refreshToken = "actual-refresh-token";
        LoginTokenPair tokenPair = new LoginTokenPair(accessToken, refreshToken);

        String result = tokenPair.toString();

        assertEquals(
                "LoginTokenPair[accessToken=[REDACTED], refreshToken=[REDACTED]]",
                result);
        assertFalse(result.contains(accessToken));
        assertFalse(result.contains(refreshToken));
        assertEquals(accessToken, tokenPair.accessToken());
        assertEquals(refreshToken, tokenPair.refreshToken());
    }
}
