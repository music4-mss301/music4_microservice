package vn.edu.fpt.sba.music4.configurations;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import vn.edu.fpt.sba.music4.entities.RefreshToken;
import vn.edu.fpt.sba.music4.entities.User;
import vn.edu.fpt.sba.music4.services.IUserService;
import vn.edu.fpt.sba.music4.services.impl.RefreshTokenServiceImpl;
import vn.edu.fpt.sba.music4.util.JwtUtil;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final IUserService userService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenServiceImpl refreshTokenService;

    @Value("${JWT_REFRESH_EXPIRES_IN}")
    private long refreshTokenExpiration;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        User user = userService.processOAuthPostLogin(oAuth2User, "GOOGLE");
        String accessToken = this.jwtUtil.generateToken(user);

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUsername(), "user");

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token", refreshToken.getToken())
                .httpOnly(true)       // Chống JavaScript (XSS)
                .secure(false)        // Đặt true nếu chạy HTTPS (Dev localhost để false)
                .path("/")            // Áp dụng cho toàn bộ domain Backend
                .maxAge(refreshTokenExpiration * 24 * 60 * 60)
                .sameSite("Lax")       // Chống CSRF (Hoặc "None" nếu Frontend/Backend khác domain hoàn toàn trên HTTPS)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/oauth2/redirect")
                .queryParam("token", accessToken)
                .queryParam("refreshToken", refreshToken.getToken())
                .build().toUriString();

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
