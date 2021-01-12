package lk.ijse.dep.web.api;

import lk.ijse.dep.web.dto.TodoitemDTO;
import lk.ijse.dep.web.util.Priority;
import lk.ijse.dep.web.util.Status;
import org.apache.commons.dbcp2.BasicDataSource;

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
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "TodoitemServlet", urlPatterns = "/api/v1/items/*")
public class TodoitemServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
        Jsonb jsonb = JsonbBuilder.create();
        if (req.getPathInfo() == null || req.getPathInfo().equals("/")){
            try (Connection connection = cp.getConnection()){
//                req.setAttribute("user","suchira");
                PreparedStatement pstm = connection.prepareStatement("SELECT * FROM todo_item WHERE username=?");
                pstm.setObject(1,req.getAttribute("user"));
                ResultSet rst = pstm.executeQuery();
                List<TodoitemDTO> items = new ArrayList<>();
                while (rst.next()){
                    items.add(new TodoitemDTO(rst.getInt("id"),
                            rst.getString("text"),
                            Priority.valueOf(rst.getString("priority")),
                            Status.valueOf(rst.getString("status")),
                            rst.getString("username")));
                }
                resp.setContentType("application/json");
                resp.getWriter().println(jsonb.toJson(items));
            } catch (SQLException | JsonbException throwables) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                throwables.printStackTrace();
            }
        }else {
            try(Connection connection = cp.getConnection()) {
              int id = Integer.parseInt(req.getPathInfo().replace("/", ""));
//              req.setAttribute("user", "suchira");
                PreparedStatement pstm = connection.prepareStatement("SELECT * FROM todo_item WHERE id=? AND username=?");
                pstm.setObject(1,id);
                pstm.setObject(2,req.getAttribute("user"));
                ResultSet rst = pstm.executeQuery();
                if (!rst.next()){
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                }else{
                    resp.setContentType("application/json");
                    TodoitemDTO item = new TodoitemDTO(rst.getInt("id"),
                            rst.getString("text"),
                            Priority.valueOf(rst.getString("priority")),
                            Status.valueOf(rst.getString("status")),
                            rst.getString("username"));
                    resp.getWriter().println(jsonb.toJson(item));

                }
            }catch (NumberFormatException e){
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                e.printStackTrace();
            }catch (SQLException throwables){
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                throwables.printStackTrace();
            }
        }

        }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Jsonb jsonb = JsonbBuilder.create();
        try {
            TodoitemDTO item = jsonb.fromJson(req.getReader(), TodoitemDTO.class);
            if (item.getId() != null || item.getText() == null ||
            item.getText().trim().isEmpty()){
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
            try (Connection connection = cp.getConnection()){
                PreparedStatement pstm = connection.prepareStatement("SELECT * FROM `user` WHERE username=?");
                pstm.setObject(1,req.getAttribute("user"));
                if (!pstm.executeQuery().next()){
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.setContentType("text/plain");
                    resp.getWriter().println("invalid user");
                    return;
                }
                pstm = connection.prepareStatement("INSERT INTO todo_item (`text`, `priority`, `status`, `username`) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
                pstm.setObject(1,item.getText());
                pstm.setObject(2,item.getPriority().toString());
                pstm.setObject(3,item.getStatus().toString());
                pstm.setObject(4,req.getAttribute("user"));
                if (pstm.executeUpdate()>0){
                    resp.setStatus(HttpServletResponse.SC_CREATED);
                    ResultSet generatedKeys = pstm.getGeneratedKeys();
                    generatedKeys.next();
                    int generatedId = generatedKeys.getInt(1);
                    item.setId(generatedId);
                    item.setUsername(req.getAttribute("user").toString());
                    resp.setContentType("application/json");
                    resp.getWriter().println(jsonb.toJson(item));
                }else{
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }

        }catch (JsonbException | SQLException exp){
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            exp.printStackTrace();
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
        try(Connection connection = cp.getConnection()) {
            int id = Integer.parseInt(req.getPathInfo().replace("/", ""));
            PreparedStatement pstm = connection.prepareStatement("SELECT * FROM todo_item WHERE id=? AND username=?");
            pstm.setObject(1,id);
            pstm.setObject(2,req.getAttribute("user"));
            ResultSet rst = pstm.executeQuery();
            if (!rst.next()){
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }else{
                pstm = connection.prepareStatement("DELETE FROM todo_item WHERE id=? AND username=?");
                pstm.setObject(1,id);
                pstm.setObject(2,req.getAttribute("user"));
                boolean success = pstm.executeUpdate() > 0;
                if (success) {
                    resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                } else {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }

            }
        }catch (NumberFormatException e){
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            e.printStackTrace();
        }catch (SQLException throwables){
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throwables.printStackTrace();
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Jsonb jsonb = JsonbBuilder.create();
        try {
            TodoitemDTO item = jsonb.fromJson(req.getReader(), TodoitemDTO.class);
            if (item.getId() != null || item.getText() == null ||
                    item.getText().trim().isEmpty()){
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
            try (Connection connection = cp.getConnection()){
                int id = Integer.parseInt(req.getPathInfo().replace("/", ""));
                PreparedStatement pstm = connection.prepareStatement("SELECT * FROM todo_item WHERE id=? AND username=?");
                pstm.setObject(1,id);
                pstm.setObject(2,req.getAttribute("user"));
                ResultSet rst = pstm.executeQuery();
                if (!rst.next()){
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                }else {
                    pstm = connection.prepareStatement("UPDATE todo_item SET text=?, priority=?, status=? WHERE id=? AND username=?");
                    pstm.setObject(1, item.getText());
                    pstm.setObject(2, item.getPriority().toString());
                    pstm.setObject(3, item.getStatus().toString());
                    pstm.setObject(4, id);
                    pstm.setObject(5, req.getAttribute("user"));
                    boolean success = pstm.executeUpdate() > 0;
                    if (success) {
                        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    } else {
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    }
                }
            }
        }catch (JsonbException | SQLException exp){
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            exp.printStackTrace();
        }
    }
}

