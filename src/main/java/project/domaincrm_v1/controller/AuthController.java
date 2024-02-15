package project.domaincrm_v1.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.domaincrm_v1.dao.UserService;
import project.domaincrm_v1.dto.LoginRequestDto;
import project.domaincrm_v1.entity.User;

@RestController
@RequestMapping("/api")
public class AuthController {
    @Autowired
    private UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody User user) {
            return ResponseEntity.ok(userService.createNewUser(user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequestDto) {
        return ResponseEntity.ok(userService.login(loginRequestDto.getEmail(), loginRequestDto.getPassword()));

    }
}

