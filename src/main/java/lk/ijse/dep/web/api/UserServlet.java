package lk.ijse.dep.web.api;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lk.ijse.dep.web.dto.UserDTO;
import lk.ijse.dep.web.util.AppUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.dbcp2.BasicDataSource;

import javax.crypto.SecretKey;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.Date;


@WebServlet(name = "UserServlet", urlPatterns = {"/api/v1/users/*","/api/v1/auth"})
public class UserServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Jsonb jsonb = JsonbBuilder.create();
        try {
            UserDTO userDTO = jsonb.fromJson(req.getReader(), UserDTO.class);
            if (userDTO.getUsername() == null || userDTO.getPassword() == null || userDTO.getUsername().length() < 3 || userDTO.getUsername().trim().isEmpty() || userDTO.getPassword().trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
            try (Connection connection = cp.getConnection()) {
                if (req.getServletPath().equals("/api/v1/auth")){
                    PreparedStatement pstm = connection.prepareStatement("SELECT * FROM `user` WHERE username=?");
                    pstm.setObject(1,userDTO.getUsername());
                    ResultSet rst = pstm.executeQuery();
                    if (rst.next()){
                        String sha256Hex = DigestUtils.sha256Hex(userDTO.getPassword());
                        if (sha256Hex.equals(rst.getString("password"))){

                            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(AppUtil.getAppSecretKey()));
                            String jws = Jwts.builder()
                                    .setIssuer("ijse")
                                    .setExpiration(new Date(new Date().getTime() + (1000 * 60 * 60 * 24)))
                                    .setIssuedAt(new Date())
                                    .claim("name", userDTO.getUsername())
                                    .signWith(key)
                                    .compact();

                            resp.setContentType("text/plain");
                            resp.getWriter().println(jws);
                        }else{
                            System.out.println("Horek");
                        }
                    }else{
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    }
                }else {
                    PreparedStatement pstm = connection.prepareStatement("SELECT * FROM `user` WHERE username=?");
                    pstm.setObject(1,userDTO.getUsername());
                    if (pstm.executeQuery().next()) {
                        resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    }
                    pstm = connection.prepareStatement("INSERT INTO `user` VALUES (?,?)");
                    pstm.setObject(1, userDTO.getUsername());
                    String sha256Hex = DigestUtils.sha256Hex(userDTO.getPassword());
                    pstm.setObject(2, sha256Hex);
                    if (pstm.executeUpdate() > 0) {
                        resp.setStatus(HttpServletResponse.SC_CREATED);
                    } else {
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    }
                }
            } catch (SQLException throwables) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                throwables.printStackTrace();
            }
        } catch (JsonbException exp) {
            exp.printStackTrace();
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Jsonb jsonb = JsonbBuilder.create();
        try {
            UserDTO userDTO = jsonb.fromJson(req.getReader(), UserDTO.class);
            if (userDTO.getPassword() == null || userDTO.getPassword().trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
            try (Connection connection = cp.getConnection()) {
                PreparedStatement pstm = connection.prepareStatement("SELECT * FROM `user` WHERE username=?");
                pstm.setObject(1, req.getAttribute("user"));
                ResultSet rst = pstm.executeQuery();
                if (!rst.next()) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                } else {
                    String sha256Hex = DigestUtils.sha256Hex(userDTO.getPassword());
                    if (sha256Hex.equals(rst.getString("password"))) {
                        pstm = connection.prepareStatement("DELETE FROM `user` WHERE username=?");
                        pstm.setObject(1, req.getAttribute("user"));
                        boolean success = pstm.executeUpdate() > 0;
                        if (success) {
                            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                        } else {
                            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        }
                    }else{
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    }
                }
            } catch (NumberFormatException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                e.printStackTrace();
            } catch (SQLException throwables) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                throwables.printStackTrace();
            }
        }catch (JsonbException exp) {
            exp.printStackTrace();
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        
    }
}
