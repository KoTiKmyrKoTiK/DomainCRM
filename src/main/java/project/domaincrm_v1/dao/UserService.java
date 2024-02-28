package project.domaincrm_v1.dao;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import project.domaincrm_v1.dto.UserResponseDto;
import project.domaincrm_v1.entity.User;
import project.domaincrm_v1.repository.UserRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

@Slf4j
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Service
public class UserService {
    @Autowired
    UserRepository userRepository;

    @Autowired
    DatasourceConfig datasourceProps;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.datasourceProps = new DatasourceConfig();
    }

    String ADD_NEW_USER = "INSERT INTO users  (email, password, role, name) VALUES (?, ?, ?, ?)";
    String GET_USER_BY_EMAIL = "SELECT * FROM users WHERE email = ?";
    String GET_USER_BY_ID = "SELECT * FROM users WHERE id = ?";

    public User getUserInfo(String email) {
        try (var conn = ConnectionUtils.connect(datasourceProps)) {
            var statement = conn.prepareStatement(GET_USER_BY_ID);
            statement.setString(1, email);
            var rs = statement.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(UUID.fromString(rs.getString("id")));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                user.setName(rs.getString("name"));
                user.setRole(rs.getString("role"));
                return user;
            } else {
                throw new RuntimeException("User not found");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public UUID addNewUser(User user) {
        var conn = ConnectionUtils.connect(datasourceProps);
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
                    return UUID.fromString(generatedKeys.getString(1));
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
            userRespDto = new UserResponseDto(user, "Пользователь успешно добавлен", true);
        } else {
            userRespDto = new UserResponseDto(null, "Пользователь с таким e-mail уже создан", false);
        }
        return userRespDto;
    }

    public UserResponseDto login(String email, String password) {
        var userRep = userRepository.findByEmail(email);
        User user;

        if (userRep.isPresent()) {
            user = userRep.get();
        } else {
            return new UserResponseDto(null, "Пользователь с таким e-mail не найден", false);
        }

        if (user.isRestricted()) {
            return new UserResponseDto(null, "Пользователь заблокирован", false);
        } else if (!isPasswordCorrect(user, password)) {
            return new UserResponseDto(null, "Неправильный пароль", false);
        }

        return new UserResponseDto(user, "Success Login", true);
    }

    private boolean isPasswordCorrect(User user, String password) {
        try (var conn = ConnectionUtils.connect(datasourceProps)) {
            var storedPassword = user.getPassword();
            return User.encoder.matches(password, storedPassword);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}