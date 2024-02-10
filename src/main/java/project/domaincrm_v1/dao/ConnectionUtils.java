package project.domaincrm_v1.dao;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@UtilityClass
public class ConnectionUtils {

    public Connection connect() {
        try {
            return DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "zabych", "1111");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void close(Connection connection) {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}