package project.domaincrm_v1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import project.domaincrm_v1.dao.UserService;
import project.domaincrm_v1.entity.User;

@SpringBootApplication
public class DomainCrmV1Application {
    private static UserService userService;

    public DomainCrmV1Application(UserService userService) {
        DomainCrmV1Application.userService = userService;
    }

    public static void main(String[] args) {
        SpringApplication.run(DomainCrmV1Application.class, args);
        var user = new User();

        var userEmail = System.getenv("ADMIN_EMAIL") != null ? System.getenv("ADMIN_EMAIL") : "test@gmail.com";
        var userPassword = System.getenv("ADMIN_PASSWORD") != null ? System.getenv("ADMIN_EMAIL") : "23423423432423";
        var userName = System.getenv("ADMIN_USERNAME") != null ? System.getenv("ADMIN_EMAIL") : "zabych";

        user.setEmail(userEmail);
        user.setPassword(userPassword);
        user.setRole("admin");
        user.setName(userName);

        System.out.println("ID " + userService.addNewUser(user));
    }

}
