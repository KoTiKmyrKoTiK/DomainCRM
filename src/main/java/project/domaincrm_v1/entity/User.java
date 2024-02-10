package project.domaincrm_v1.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;


import java.util.List;


@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;
    @Column(name = "name")
    String name;
    @Column(name = "email")
    String email;
    @Column(name = "password")
    String password;
    @Column(name = "role")
    String role;

    @OneToMany(mappedBy = "user")
    List<Domain> domains;

    @OneToMany(mappedBy = "user")
    List<Account> accounts;


    public User(long id, String name, String email, String password, String role, String domain) {
    }
}