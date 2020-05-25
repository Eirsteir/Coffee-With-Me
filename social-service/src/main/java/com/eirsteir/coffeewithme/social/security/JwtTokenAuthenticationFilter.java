package com.eirsteir.coffeewithme.social.security;

import com.eirsteir.coffeewithme.commons.security.JwtConfig;
import com.eirsteir.coffeewithme.commons.security.UserDetailsImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@AllArgsConstructor
public class JwtTokenAuthenticationFilter extends OncePerRequestFilter {

    private final JwtConfig jwtConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(jwtConfig.getHeader());
        if (header == null || !header.startsWith(jwtConfig.getPrefix())) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.replace(jwtConfig.getPrefix(), "");
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtConfig.getSecret().getBytes())
                    .parseClaimsJws(token)
                    .getBody();

            // Probably need to get Id from here?
            String id = claims.getSubject();
            if (id != null) {
                @SuppressWarnings("unchecked")
                List<String> authorities = (List<String>) claims.get("authorities");
                UserDetailsImpl principal = UserDetailsImpl.builder()
                        .id(Long.parseLong(id))
                        .email((String) claims.get("email"))
                        .username((String) claims.get("username"))
                        .build();

                UsernamePasswordAuthenticationToken auth =  new UsernamePasswordAuthenticationToken(
                        principal, null, authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));

                log.info("[x] Authenticating user: {}", auth);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception e) {
            log.error("[x] Error while authenticating user, clearing security context: ", e);
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}