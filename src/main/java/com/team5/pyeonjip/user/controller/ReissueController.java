package com.team5.pyeonjip.user.controller;


import com.team5.pyeonjip.global.jwt.JWTUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;


@RequiredArgsConstructor
@Controller
@ResponseBody
public class ReissueController {

    private final JWTUtil jwtUtil;


    // Todo: 추후 제네릭 수정 + Service단으로 로직 분리해보기.
    // Access 토큰 재발급을 위한 컨트롤러
    @PostMapping("/reissue")
    public ResponseEntity<?> reissue(HttpServletRequest request, HttpServletResponse response) {

        // Refresh 토큰을 가져온다.
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {

            if (cookie.getName().equals("refresh")) {

                refreshToken = cookie.getValue();
            }
        }

        // Refresh 토큰이 없는 경우
        if (refreshToken == null) {

            return new ResponseEntity<>("refresh token null", HttpStatus.BAD_REQUEST);
        }

        // Refresh 토큰이 만료되었는지 확인
        try {
            jwtUtil.isExpired(refreshToken);
        } catch (ExpiredJwtException e) {

            return new ResponseEntity<>("refresh token expired", HttpStatus.BAD_REQUEST);
        }

        // Refresh 토큰인지 확인 (발급시 페이로드에 명시된다)
        String category = jwtUtil.getCategory(refreshToken);

        if (!category.equals("refresh")) {

            return new ResponseEntity<>("invalid refresh token", HttpStatus.BAD_REQUEST);
        }

        /* 여기까지 Refresh 토큰 검증 로직 */

        String email = jwtUtil.getEmail(refreshToken);
        String role = jwtUtil.getRole(refreshToken);

        // Access, Refresh JWT를 새로 생성.
        String newAccessToken = jwtUtil.createJwt("access", email, role, 600000L);
        String newRefreshToken = jwtUtil.createJwt("refresh", email, role, 86400000L);

        response.setHeader("access", newAccessToken);
        response.addCookie(createCookie("refresh", newRefreshToken));

        return new ResponseEntity<>(HttpStatus.OK);
    }


    private Cookie createCookie(String key, String value) {

        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24 * 60 * 60);
        // cookie.setSecure(true);
        // cookie.setPath("/");
        cookie.setHttpOnly(true);

        return cookie;
    }
}
