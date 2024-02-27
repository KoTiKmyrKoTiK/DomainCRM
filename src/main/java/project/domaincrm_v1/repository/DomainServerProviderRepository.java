package project.domaincrm_v1.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.domaincrm_v1.entity.DomainServerProvider;

@Repository
public interface DomainServerProviderRepository extends JpaRepository<DomainServerProvider, UUID> {
  
}