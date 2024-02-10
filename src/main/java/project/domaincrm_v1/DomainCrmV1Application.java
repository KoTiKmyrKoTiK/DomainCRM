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

        user.setEmail("test@gmail.com");
        user.setPassword("23423423432423");
        user.setRole("admin");
        user.setName("zabych");

        System.out.println("ID " + userService.addNewUser(user));
    }

}
