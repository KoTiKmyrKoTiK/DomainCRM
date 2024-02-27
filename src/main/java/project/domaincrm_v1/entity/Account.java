package project.domaincrm_v1.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Table(name = "accounts")
public class Account extends BaseEntity {
    @Column(name = "name")
    private String name;

    @Column(name = "currency")
    private String currency;

    @Column(name = "account_limit")
    private String account_limit;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
