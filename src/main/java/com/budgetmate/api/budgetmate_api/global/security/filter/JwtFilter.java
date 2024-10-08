package com.budgetmate.api.budgetmate_api.global.security.filter;

import static com.budgetmate.api.budgetmate_api.global.error.ErrorCode.AUTHENTICATION_FAILED;
import static com.budgetmate.api.budgetmate_api.global.error.ErrorCode.COMMON_SYSTEM_ERROR;

import com.budgetmate.api.budgetmate_api.domain.user.entity.User;
import com.budgetmate.api.budgetmate_api.global.error.ErrorCode;
import com.budgetmate.api.budgetmate_api.global.exception.JwtAuthenticationException;
import com.budgetmate.api.budgetmate_api.global.security.userDetails.CustomUserDetails;
import com.budgetmate.api.budgetmate_api.global.security.util.TokenManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final TokenManager tokenManager;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        try {
            //Authorization 헤더 검증
            if (authorization == null|| !authorization.startsWith("Bearer ")){
                log.info("[ERROR] Authorization header 정보 없음");
                throw new JwtAuthenticationException(ErrorCode.AUTHORIZATION_HEADER_MISSING);
            }

            //Bearer 제거
            String token = authorization.split(" ")[1];
            //토큰 유효성 검증
            tokenManager.validateToken(token);

            //토큰에서 category와 username 획득
            if (!tokenManager.isAccessToken(token)){
                throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN);
            }

            String username = tokenManager.getUsername(token);
            Long userId = tokenManager.getUserId(token);

            //userEntity를 생성하여 값 set
            User user = User.builder()
                .id(userId)
                .email(username)
                .password("temp")
                .build();

            //UserDetails에 회원 정보 객체 담기
            CustomUserDetails customUserDetails = new CustomUserDetails(user);

            //스프링 시큐리티 인증 토큰 생성
            Authentication authToken = new UsernamePasswordAuthenticationToken(
                customUserDetails, null,
                customUserDetails.getAuthorities());

            //세션에 사용자 등록
            SecurityContextHolder.getContext().setAuthentication(authToken);

            filterChain.doFilter(request, response);
        } catch (JwtAuthenticationException e) {
            log.info("[ERROR] JWT 인증 실패: {}", e.getMessage());
            request.setAttribute("tokenError",AUTHENTICATION_FAILED);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.info("[ERROR] 기타 인증 오류 발생: {}", e.getMessage());
            request.setAttribute("tokenError",COMMON_SYSTEM_ERROR);
            filterChain.doFilter(request, response);
        }
    }
}

