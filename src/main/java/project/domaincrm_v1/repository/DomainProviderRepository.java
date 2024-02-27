package project.domaincrm_v1.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.domaincrm_v1.entity.DomainProvider;

@Repository
public interface DomainProviderRepository extends JpaRepository<DomainProvider, UUID> {
  
}