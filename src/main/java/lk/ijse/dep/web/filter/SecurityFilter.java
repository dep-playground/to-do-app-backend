package lk.ijse.dep.web.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lk.ijse.dep.web.util.AppUtil;

import javax.crypto.SecretKey;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(filterName = "SecurityFilter", servletNames = {"TodoitemServlet", "UserServlet"})
public class SecurityFilter extends HttpFilter {
    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        if (req.getServletPath().equals("/api/v1/auth") && req.getMethod().equals("POST")){
            chain.doFilter(req, res);
        }else if (req.getServletPath().equals("/api/v1/users") && req.getMethod().equals("POST")){
            chain.doFilter(req, res);
        }
        else {
            String authorization = req.getHeader("Authorization");
            if (authorization == null || !authorization.startsWith("Bearer")) {
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                String token = authorization.replace("Bearer", "");
                Jws<Claims> jws;

                try {
                    SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(AppUtil.getAppSecretKey()));
                    jws = Jwts.parserBuilder()  // (1)
                            .setSigningKey(key)         // (2)
                            .build()                    // (3)
                            .parseClaimsJws(token); // (4)

                    req.setAttribute("user", jws.getBody().get("name"));
                    chain.doFilter(req, res);
                    // we can safely trust the JWT
                } catch (JwtException ex) {       // (5)
                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    ex.printStackTrace();
                    // we *cannot* use the JWT as intended by its creator
                }
            }
        }
    }
}
