package project.domaincrm_v1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.domaincrm_v1.entity.User;

@Repository
public interface UserRepository  extends JpaRepository<User, Long> {
 User findByEmail(String email);


}
