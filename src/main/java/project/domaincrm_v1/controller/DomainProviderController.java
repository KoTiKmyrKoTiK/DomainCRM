package project.domaincrm_v1.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.domaincrm_v1.entity.DomainProvider;
import project.domaincrm_v1.repository.DomainProviderRepository;

@RestController
@RequestMapping("/api")
public class DomainProviderController {
    @Autowired
    DomainProviderRepository domainProviderRepository;

    @GetMapping("/domain_providers/{id}")
    public ResponseEntity<?> getDomainById(@PathVariable UUID id) {
        var domain = domainProviderRepository.findById(id);

        if (domain == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(domain);
    }

    @GetMapping("/domain_providers")
    public ResponseEntity<?> getAllDomains() {
        return ResponseEntity.ok(domainProviderRepository.findAll());
    }

    @PostMapping("/domain_providers")
    public ResponseEntity<?> addNewDomain(@RequestBody DomainProvider domain_provider) {
        return ResponseEntity.ok(domainProviderRepository.save(domain_provider));
    }

    @PutMapping("/domain_providers/{id}")
    public ResponseEntity<?> UpdateDomain(@PathVariable UUID id, @RequestBody DomainProvider domain_provider) {
        domain_provider.setId(id);
        return ResponseEntity.ok(domainProviderRepository.save(domain_provider));
    }

    @DeleteMapping("/domain_providers/{id}")
    public ResponseEntity<?> deleteDomain(@PathVariable UUID id) {
        domainProviderRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}



