package app.sewain.tenant;

import app.sewain.merchants.TenantAccessService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantFilter extends OncePerRequestFilter {

	private static final String ROOT_DOMAIN = "sewain.app";
	private static final String PUBLIC = "public";
	private static final String TENANT_HEADER = "X-Tenant";

	private final TenantAccessService tenantAccessService;

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {

		try {
			String tenant;
            try {
                var path = request.getServletPath();
                tenant = path.equals("/api/merchants") || path.equals("/ws/provisioning")
                        ? PUBLIC : tenantAccessService.getSchema(resolveTenant(request));
            } catch (org.springframework.web.server.ResponseStatusException unavailable) {
                response.setStatus(unavailable.getStatusCode().value());
                response.setContentType("application/json");
                response.getWriter().write("""
                        {"message":"merchant.provisioning"}
                        """);
                return;
            }
			if (!tenant.equals(PUBLIC)) {
				UUID userId = getUserId();
				if (userId != null) {
					tenantAccessService.validateAccess(userId, tenant);
				}
			}
			log.info("Connected to tenant: {}", tenant);
			TenantContext.set(tenant.replace("-", "_"));
			filterChain.doFilter(request, response);
		} finally {
			TenantContext.clear();
		}
	}

	private UUID getUserId() {
		Authentication authentication =
				SecurityContextHolder
						.getContext()
						.getAuthentication();

		if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
			return null;
		}

		return UUID.fromString(Objects.requireNonNull(jwtAuth.getToken().getClaimAsString("user")));
	}

	private String resolveTenant(HttpServletRequest request) {
		String header = request.getHeader("X-Forwarded-Host");
		String host = header != null ? header : "";
		var head = request.getHeader(TENANT_HEADER);
		if (StringUtils.isNoneBlank(head)) {
			return head;
		}
		if (StringUtils.isBlank(host)) {
			host = request.getServerName();
		}
		if (StringUtils.isNoneBlank(host)) {
			host = host.replace(".localhost", "");
		}
		if (host.equals(ROOT_DOMAIN)
				|| host.equals("www." + ROOT_DOMAIN)) {
			return PUBLIC;
		}

		String suffix = "." + ROOT_DOMAIN;

		if (!host.endsWith(suffix)) {
			return PUBLIC;
		}

		String subdomain =
				host.substring(0, host.length() - suffix.length());

		if (subdomain.isBlank() || subdomain.contains(".")) {
			return PUBLIC;
		}

		return subdomain;
	}
}