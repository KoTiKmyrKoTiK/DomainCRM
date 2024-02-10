package project.domaincrm_v1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.domaincrm_v1.entity.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {


}
