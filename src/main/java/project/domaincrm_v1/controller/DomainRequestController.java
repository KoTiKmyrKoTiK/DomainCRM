package project.domaincrm_v1.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.domaincrm_v1.entity.DomainRequest;
import project.domaincrm_v1.repository.DomainRequestRepository;

@RestController
@RequestMapping("/api")
public class DomainRequestController {
    @Autowired
    DomainRequestRepository domainRequestRepository;

    @GetMapping("/domain_requests/{id}")
    public ResponseEntity<?> getDomainProviderById(@PathVariable UUID id) {
        var domain = domainRequestRepository.findById(id);

        if (domain == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(domain);
    }

    @GetMapping("/domain_requests")
    public ResponseEntity<?> getAllDomainProviders() {
        return ResponseEntity.ok(domainRequestRepository.findAll());
    }

    @PostMapping("/domain_requests")
    public ResponseEntity<?> addNewDomainProvider(@RequestBody DomainRequest domain_provider) {
        return ResponseEntity.ok(domainRequestRepository.save(domain_provider));
    }

    @PutMapping("/domain_requests/{id}")
    public ResponseEntity<?> UpdateDomainProvider(@PathVariable UUID id, @RequestBody DomainRequest domain_provider) {
        domain_provider.setId(id);
        return ResponseEntity.ok(domainRequestRepository.save(domain_provider));
    }

    @DeleteMapping("/domain_requests/{id}")
    public ResponseEntity<?> deleteDomainProvider(@PathVariable UUID id) {
        domainRequestRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}



