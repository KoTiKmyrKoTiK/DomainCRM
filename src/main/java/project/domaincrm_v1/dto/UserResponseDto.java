package project.domaincrm_v1.dto;



import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import project.domaincrm_v1.entity.User;

@RequiredArgsConstructor
@AllArgsConstructor
@Data

@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponseDto {
    User user;
    String message;
    boolean success;
}
