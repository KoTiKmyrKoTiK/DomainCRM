package project.domaincrm_v1.entity;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Filters;

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
@Table(name = "domains")
@Filters( {
    @Filter(name="status", condition="status = :status"),
})
public class Domain extends BaseEntity {
    @Column(name = "domain")
    private String domain;

    @Column(name = "subdomain")
    private String subdomain;

    @Column(name = "status")
    private String status;

    @Column(name = "server_ip")
    private String server_ip;

    @Column(name = "server_provider")
    private String server_provider;

    @ManyToOne
    @JoinColumn(name = "domain_server_provider_id")
    private DomainProvider domain_server_provider;

    @ManyToOne
    @JoinColumn(name = "domain_provider_id")
    private DomainProvider domain_provider;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}

