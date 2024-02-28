package project.domaincrm_v1.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.domaincrm_v1.entity.DomainRequest;

@Repository
public interface DomainRequestRepository extends JpaRepository<DomainRequest, UUID> {
  
}