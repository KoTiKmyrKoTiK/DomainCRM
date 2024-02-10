package project.domaincrm_v1.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Table(name = "domains")
@Entity
public class Domain {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "name")
    String name;

    @Column(name = "status")
    String status;

    @Column(name = "subdomain")
    String subdomain;

    @Column(name = "ip")
    String ip;

    @ManyToOne
    @JoinColumn(name = "user_id")
    User user;

    @Column(name = "registrator")
    String registrator;

    @Column(name = "creation_date")
    String creationDate;
    @Column(name = "archived_date")
    String archivedDate;//TODO мб на стороне бекенда даты делать
}

