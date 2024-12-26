package com.team5.pyeonjip.global.config;

import com.team5.pyeonjip.global.exception.JWTAccessDeniedHandler;
import com.team5.pyeonjip.global.exception.JWTAuthenticationEntryPoint;
import com.team5.pyeonjip.global.jwt.CustomLogoutFilter;
import com.team5.pyeonjip.global.jwt.JWTFilter;
import com.team5.pyeonjip.global.jwt.JWTUtil;
import com.team5.pyeonjip.global.jwt.LoginFilter;
import com.team5.pyeonjip.user.repository.RefreshRepository;
import com.team5.pyeonjip.user.service.ReissueService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Collections;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;
    private final ReissueService reissueService;
    private final JWTAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JWTAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {

        return new BCryptPasswordEncoder();
    }

    // AuthenticationManager Bean
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {

        return configuration.getAuthenticationManager();
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .cors((cors) -> cors
                        .configurationSource(new CorsConfigurationSource() {
                            @Override
                            public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {

                                CorsConfiguration configuration = new CorsConfiguration();

                                // 데이터를 보내는 80번 포트를 허용
                                configuration.setAllowedOrigins(Collections.singletonList("http://43.202.4.58:3000"));

                                // 모든 메서드 허용
                                configuration.setAllowedMethods(Collections.singletonList("*"));

                                configuration.setAllowCredentials(true);

                                // 허용할 헤더
                                configuration.setAllowedHeaders(Collections.singletonList("*"));

                                // 허용 시간
//                                configuration.setMaxAge(3600L);

                                // Authorization 헤더 노출
                                configuration.setExposedHeaders(Collections.singletonList("Authorization"));

                                return configuration;
                            }
                        }));
        // csrf 비활성화. JWT는 세션을 stateless로 관리하기 때문
        http
                .csrf((auth) -> auth.disable());

        // Form Login 비활성화
        http
                .formLogin((auth) -> auth.disable());

        // http basic 인증 방식 비활성화
        http
                .httpBasic((auth) -> auth.disable());

        http
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)  // 인증 실패 처리
                        .accessDeniedHandler(jwtAccessDeniedHandler));

        // 경로별 인가
        http
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers("/api/auth/**",
                                         "/api/user/signup",
                                         "/not-found",
                                         "/").permitAll()
                        // 관리자만 접근 가능
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 토큰 리이슈
                        .requestMatchers("/api/user/reissue").permitAll()

                        /* 장바구니 */
                        .requestMatchers("/api/cart/**").permitAll()                     // 1
                        .requestMatchers("/api/cart/sync").authenticated()                 // 2
                        //.requestMatchers("/api/cart/**").hasAnyRole("ADMIN", "USER")     // 3

                        /* 쿠폰 */
                        .requestMatchers("/api/coupon/**").permitAll()
                        .requestMatchers("/api/coupon/custom/**").hasRole("ADMIN")

                        /* 댓글 */
                        .requestMatchers("/api/comments/product/**").permitAll()
                        .requestMatchers("/api/comments/product-rating/**").permitAll()
                        //.requestMatchers("/api/comments/**").authenticated()

                        /* 채팅 */
                        .requestMatchers("/api/chat/**").hasAnyRole("ADMIN", "USER")
                        .requestMatchers("/api/chat/waiting-room").hasRole("USER")
                        .requestMatchers("/api/chat/waiting-rooms").hasRole("ADMIN")
                        .requestMatchers("/api/chat/activate-room").hasRole("ADMIN")

                        /* 카테고리 */
                        .requestMatchers("/api/category/**").permitAll()

                        /* 주문 */
                        .requestMatchers("/api/orders/**").permitAll()

                        /* 유저 - 임시 전체 허용 */
//                        .requestMatchers("/api/user/**").permitAll()                      // 1
                        //.requestMatchers("/api/user/**").hasRole("USER")                  // 2
                        //.requestMatchers("/api/user/**").hasAnyRole("ADMIN", "USER")      // 3
                        .requestMatchers("/api/user/**").permitAll()                      // 1

                        /* 상품 */
                        .requestMatchers("/api/product/**").permitAll()

                        /* Swagger */
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // 임시로 permitAll()
                        .anyRequest().permitAll());         // 1. 모두 허용
//                        .anyRequest().authenticated());   // 2. 인증 필요

        // 필터 등록
//      JWTFilter
        http
                .addFilterBefore(new JWTFilter(jwtUtil), LoginFilter.class);
//      LoginFilter
        http
                .addFilterAt(new LoginFilter(authenticationManager(authenticationConfiguration), jwtUtil, refreshRepository, reissueService), UsernamePasswordAuthenticationFilter.class);
//      LogoutFilter
        http
                .addFilterBefore(new CustomLogoutFilter(jwtUtil, refreshRepository), LogoutFilter.class);
        // 세션 설정
        http
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}
