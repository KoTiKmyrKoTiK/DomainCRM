package project.domaincrm_v1.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Table(name = "domain_providers")
public class DomainProvider extends BaseEntity {
    @Column(name = "name")
    private String name;
}
