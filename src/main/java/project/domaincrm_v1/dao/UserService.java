package project.domaincrm_v1.dao;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import project.domaincrm_v1.dto.UserResponseDto;
import project.domaincrm_v1.entity.User;
import project.domaincrm_v1.repository.UserRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


@Slf4j
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Service
public class UserService {
    @Autowired
    UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    String ADD_NEW_USER = "INSERT INTO users  (email, password, role, name) VALUES (?, ?, ?, ?)";
    String COMPARE_PASSWORD_TO_EMAIL = "SELECT * FROM users WHERE email = ? AND password = ?";
    String GET_USER_BY_EMAIL = "SELECT * FROM users WHERE email = ?";
    String GET_USER_BY_ID = "SELECT * FROM users WHERE id = ?";


    public User getUserInfo(String email) {
        try (var conn = ConnectionUtils.connect()) {
            var statement = conn.prepareStatement(GET_USER_BY_ID);
            statement.setString(1, email);
            var rs = statement.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getString("domain")


                );
            } else {
                throw new RuntimeException("User not found");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public long addNewUser(User user) {
        var conn = ConnectionUtils.connect();
        try {
            conn.setAutoCommit(false);

            var statement = conn.prepareStatement(ADD_NEW_USER, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, user.getEmail());
            statement.setString(2, user.getPassword());
            statement.setString(3, user.getRole());
            statement.setString(4, user.getName());

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creation of user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    conn.commit();
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            log.error("Error adding new user to database");
            ConnectionUtils.rollback(conn);
            throw new RuntimeException(e);
        } finally {
            ConnectionUtils.close(conn);
        }
    }

    public UserResponseDto createNewUser(User user) {
        UserResponseDto userRespDto;
        if (userRepository.findByEmail(user.getEmail()) == null) {
            userRepository.save(user);
           userRespDto = new UserResponseDto(user, "User successfully added to database", true);
        } else {
            userRespDto = new UserResponseDto(null, "User with this email already exists", false);
        }
        return userRespDto;
    }

    public UserResponseDto login(String email, String password) {
        if (isPasswordCorrect(email, password)) {
            return new UserResponseDto(userRepository.findByEmail(email), "Success Login", true);
        } else if (!isPasswordCorrect(email, password)) {
            return new UserResponseDto(new User(), "Incorrect password", false);
        } else {
            return new UserResponseDto(new User(), "User with this username does not exist!", false);//TODO empty user returns, fix it
        }
    }

    private boolean isPasswordCorrect(String email, String password) {

        try (var conn = ConnectionUtils.connect()) {
            conn.setAutoCommit(false);
            var statement = conn.prepareStatement(COMPARE_PASSWORD_TO_EMAIL);
            statement.setString(1, email);
            statement.setString(2, password);
            var rs = statement.executeQuery();
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                return storedPassword.equals(password);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return false;
    }
}