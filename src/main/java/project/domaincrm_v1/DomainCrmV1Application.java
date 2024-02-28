package project.domaincrm_v1;

import java.io.Serializable;
import java.util.UUID;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import project.domaincrm_v1.dao.UserService;
import project.domaincrm_v1.entity.User;
import project.domaincrm_v1.repository.UserRepository;

@SpringBootApplication
@EnableConfigurationProperties
@EnableTransactionManagement
@EnableJpaRepositories
@EnableJpaAuditing
public class DomainCrmV1Application {
    private static UserRepository userRepository;

    public DomainCrmV1Application(UserRepository userRepository) {
        DomainCrmV1Application.userRepository = userRepository;
    }

    public static void main(String[] args) {
        SpringApplication.run(DomainCrmV1Application.class, args);

        var userId = UUID.fromString("f344ec9b-6153-45fc-8fe1-696f1987d29b");
        var userEmail = System.getenv("ADMIN_EMAIL") != null ? System.getenv("ADMIN_EMAIL") : "test@gmail.com";
        var userPassword = System.getenv("ADMIN_PASSWORD") != null ? System.getenv("ADMIN_PASSWORD") : "23423423432423";
        var userName = System.getenv("ADMIN_USERNAME") != null ? System.getenv("ADMIN_USERNAME") : "zabych";

        User user;

        var existsUser = userRepository.findById(userId);
        user = existsUser.isPresent() ? existsUser.get() : new User();

        user.setId(userId);
        user.setEmail(userEmail);
        user.setPassword(userPassword);
        user.setRole("admin");
        user.setName(userName);
        user.setStatus("active");

        System.out.println("ID " + userRepository.save(user));
    }

}
