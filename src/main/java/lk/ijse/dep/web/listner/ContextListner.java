package lk.ijse.dep.web.listner;

import org.apache.commons.dbcp2.BasicDataSource;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.HttpSessionBindingEvent;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

@WebListener()
public class ContextListner implements ServletContextListener,
        HttpSessionListener, HttpSessionAttributeListener {

    public ContextListner() {

    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Properties properties = new Properties();
        try {
            properties.load(this.getClass().getResourceAsStream("/application.properties"));
            BasicDataSource bds = new BasicDataSource();
            bds.setUsername(properties.getProperty("mysql.username"));
            bds.setPassword(properties.getProperty("mysql.password"));
            bds.setUrl(properties.getProperty("mysql.url"));
            bds.setDriverClassName(properties.getProperty("mysql.driver_class"));
            bds.setInitialSize(5);
            bds.setMaxTotal(10);
            ServletContext ctx = sce.getServletContext();
            ctx.setAttribute("cp",bds);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        BasicDataSource cp = (BasicDataSource) sce.getServletContext().getAttribute("cp");
        try {
            cp.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }


}
