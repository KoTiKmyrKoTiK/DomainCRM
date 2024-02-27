package project.domaincrm_v1.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.domaincrm_v1.entity.DomainServerProvider;
import project.domaincrm_v1.repository.DomainServerProviderRepository;

@RestController
@RequestMapping("/api")
public class DomainServerProviderController {
    @Autowired
    DomainServerProviderRepository domainServerProviderRepository;

    @GetMapping("/domain_server_providers/{id}")
    public ResponseEntity<?> getDomainById(@PathVariable UUID id) {
        var domain = domainServerProviderRepository.findById(id);

        if (domain == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(domain);
    }

    @GetMapping("/domain_server_providers")
    public ResponseEntity<?> getAllDomains() {
        return ResponseEntity.ok(domainServerProviderRepository.findAll());
    }

    @PostMapping("/domain_server_providers")
    public ResponseEntity<?> addNewDomain(@RequestBody DomainServerProvider domain_server_provider) {
        return ResponseEntity.ok(domainServerProviderRepository.save(domain_server_provider));
    }

    @PutMapping("/domain_server_providers/{id}")
    public ResponseEntity<?> UpdateDomain(@PathVariable UUID id, @RequestBody DomainServerProvider domain_server_provider) {
        domain_server_provider.setId(id);
        return ResponseEntity.ok(domainServerProviderRepository.save(domain_server_provider));
    }

    @DeleteMapping("/domain_server_providers/{id}")
    public ResponseEntity<?> deleteDomain(@PathVariable UUID id) {
        domainServerProviderRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}



