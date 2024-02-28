package project.domaincrm_v1.entity;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Table(name = "domain_requests")
public class DomainRequest extends BaseEntity {
    @Column(name = "subdomain")
    private String subdomain;

    @Column(name = "domain_id")
    private UUID domain_id;

    @Column(name = "status")
    private String status;

    @OneToOne()
    @JoinColumn(name = "domain_id", insertable=false, updatable=false)
    private Domain domain;
}
