package project.domaincrm_v1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.domaincrm_v1.entity.Domain;

@Repository
public interface DomainRepository extends JpaRepository<Domain, Long> {

}
