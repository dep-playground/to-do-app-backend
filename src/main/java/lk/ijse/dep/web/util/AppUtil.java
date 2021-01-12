package lk.ijse.dep.web.util;

import java.io.IOException;
import java.util.Properties;

public class AppUtil {

    public static String getAppSecretKey() throws IOException {
        Properties properties = new Properties();
        properties.load(AppUtil.class.getResourceAsStream("/application.properties"));
        return properties.getProperty("app.key");
    }
}
