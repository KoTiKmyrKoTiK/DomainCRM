package project.domaincrm_v1.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.domaincrm_v1.dto.LoginRequestDto;
import project.domaincrm_v1.dto.UserResponseDto;
import project.domaincrm_v1.entity.User;
import project.domaincrm_v1.dao.UserService;

@RestController
@RequestMapping("/api")
public class AuthController {
    @Autowired
    private UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody User user) {
        userService.addNewUser(user);
        var userRespDto = new UserResponseDto(user,   //TODO доп проверку на наичие юзера с таким же юзернеймом в базе
                "User successfully added to database",
                true);
        return ResponseEntity.ok(userRespDto);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequestDto) {
        return ResponseEntity.ok(userService.login(loginRequestDto.getEmail(), loginRequestDto.getPassword()));

    }
}

