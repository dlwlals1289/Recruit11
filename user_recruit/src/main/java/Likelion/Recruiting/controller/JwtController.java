package Likelion.Recruiting.controller;

import Likelion.Recruiting.config.auth.CustomOauthUserImpl;
import Likelion.Recruiting.config.auth.jwt.JwtTokenProvider;
import Likelion.Recruiting.exception.ErrorCode;
import Likelion.Recruiting.exception.RefreshException;
import Likelion.Recruiting.model.User;
import Likelion.Recruiting.model.dto.NavbarDto;
import Likelion.Recruiting.model.dto.JWTDto;
import Likelion.Recruiting.service.TokenService;
import Likelion.Recruiting.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@CrossOrigin("*")
@RestController
@RequiredArgsConstructor
public class JwtController {

    private final UserService userService;
    private final TokenService tokenService;
    private final JwtTokenProvider jwtTokenProvider;

    // 여기서 JWT를 돌려줄 것이다.
    @PostMapping("/v1/token")
    public NavbarDto testOAuthLogin(HttpServletRequest request, HttpServletResponse response, @RequestBody Long uid) throws IOException {
        // login
        NavbarDto navbarDto = tokenService.login(uid);
        // refresh_token 발행
        String refresh_token = tokenService.makeRt(uid);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refresh_token)
                .maxAge(7 * 24 * 60 * 60)
                .path("/") // 해당 쿠키를 도메인 전체에서 사용하고 싶다면 9번 라인같이 "/"를 Path로 준다.
                .secure(true)
                .sameSite("None")
                .httpOnly(true)
                .build();
        response.setHeader("Set-Cookie", cookie.toString());

        return navbarDto;
    }

    @GetMapping("/refresh")
    public JWTDto takeRefreshToken(@AuthenticationPrincipal CustomOauthUserImpl customOauthUser, @CookieValue("refreshToken") String cookie) {
//    public JWTDto takeRefreshToken(HttpServletRequest httpServletRequest) {
//    public void takeRefreshToken(HttpServletRequest httpServletRequest) {
//        System.out.println("httpServletRequest.getCookies() = " + httpServletRequest.getCookies());

        if (jwtTokenProvider.validateRT(cookie) == "ok") { // rt가 올바르다면 == 만료가 되지 않았거나 && null 값이 아니라면
            User user = tokenService.getUser(cookie);

            return tokenService.makeJwt(user.getId());
        }
//        else if (jwtTokenProvider.validateRT(cookie) == "expired") {
        else {
            tokenService.deleteRt(cookie);
            throw new RefreshException(ErrorCode.EXPIRED_RT.getErrorCode(), "refresh_token 만료");
        }
    }

}
