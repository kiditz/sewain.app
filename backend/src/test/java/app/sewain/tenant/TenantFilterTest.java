package app.sewain.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantFilterTest {
    @AfterEach void clearContexts() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void membershipDenialReturnsForbiddenWithoutReachingControllerAndClearsTenantContext() throws Exception {
        var user = UUID.randomUUID();
        var access = mock(TenantAccess.class);
        when(access.getSchema("other-store")).thenReturn("other_store");
        doThrow(new AccessDeniedException("merchant.access.denied")).when(access).validateAccess(user, "other_store");
        var jwt = Jwt.withTokenValue("test").header("alg", "RS256").claim("user", user.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        TenantContext.set("stale_tenant");
        var request = new MockHttpServletRequest("GET", "/api/products");
        request.setServletPath("/api/products");
        request.addHeader("X-Tenant", "other-store");
        var response = new MockHttpServletResponse();
        var reached = new AtomicBoolean();

        new TenantFilter(access).doFilter(request, response, (req, res) -> reached.set(true));

        assertEquals(403, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertTrue(response.getContentAsString().contains("merchant.access.denied"));
        assertFalse(reached.get());
        assertNull(TenantContext.get());
    }
}
