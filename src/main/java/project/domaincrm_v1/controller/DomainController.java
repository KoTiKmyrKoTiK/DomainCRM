package project.domaincrm_v1.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.domaincrm_v1.entity.Domain;
import project.domaincrm_v1.repository.DomainRepository;

@RestController
@RequestMapping("/api")
public class DomainController {
    @Autowired
    DomainRepository domainRepository;

    @GetMapping("/domains/{id}")
    public ResponseEntity<?> getDomainById(@PathVariable UUID id) {
        var domain = domainRepository.findById(id);

        if (domain == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(domain);
    }

    @GetMapping("/domains")
    public ResponseEntity<?> getAllDomains() {
        return ResponseEntity.ok(domainRepository.findAll());
    }

    @PostMapping("/domains")
    public ResponseEntity<?> addNewDomain(@RequestBody Domain domain) {
        return ResponseEntity.ok(domainRepository.save(domain));
    }

    @PutMapping("/domains/{id}")
    public ResponseEntity<?> UpdateDomain(@PathVariable UUID id, @RequestBody Domain domain) {
        domain.setId(id);
        return ResponseEntity.ok(domainRepository.save(domain));
    }

    @DeleteMapping("/domains/{id}")
    public ResponseEntity<?> deleteDomain(@PathVariable UUID id) {
        domainRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}



