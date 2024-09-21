package com.developer.config;


import com.developer.user.security.CustomUsernameFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{


        // permitall() => 접근 모두 허용
        // hasRole => 하나의 권한만 접근 가능하게 설정
        // hasAnyRole => 여러 개의 권한 접근 가능하게 설정 가능

        // CSRF 설정 Disable
        http.csrf(AbstractHttpConfigurer::disable)

                //HTTP Basic 인증 방식 disable
                .httpBasic(AbstractHttpConfigurer::disable)



                        .headers((headers) -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))


                // 초기 Security 환경으로만 사용 (세션 사용)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                                .maximumSessions(1)
                                .maxSessionsPreventsLogin(false))


                // 로그인, 회원가입 API는 토큰이 없는 상태에서 요청이 들어오기 때문에 permitAll 설정
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers("/user/**", "/**").permitAll()
                        .requestMatchers("/email/**").hasAnyRole("ADMIN", "USER")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated());

        // 로그인 로그아웃 설정
        http.logout(AbstractHttpConfigurer::disable);
        http.formLogin((AbstractHttpConfigurer::disable));

        // 사용자 정보 사전 검증 및 SecurityContext에 사용자 정보 저장
        http.addFilterBefore(new CustomUsernameFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
