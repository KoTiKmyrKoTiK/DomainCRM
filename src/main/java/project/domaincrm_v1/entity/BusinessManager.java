package project.domaincrm_v1.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "business_manager")
public class BusinessManager {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "name")
    String name;

    @ElementCollection
    List<String> motherList;

     @OneToMany(mappedBy = "b_manager")
    List<Account> accounts;
}
