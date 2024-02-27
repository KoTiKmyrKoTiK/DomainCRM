package project.domaincrm_v1.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Table(name = "domain_server_providers")
public class DomainServerProvider extends BaseEntity {
    @Column(name = "name")
    private String name;
}
