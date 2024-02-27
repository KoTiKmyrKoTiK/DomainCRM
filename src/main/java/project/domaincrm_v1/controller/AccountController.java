package project.domaincrm_v1.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.domaincrm_v1.entity.Account;
import project.domaincrm_v1.repository.AccountRepository;


@RestController
@RequestMapping("/api")
public class AccountController {
    @Autowired
    AccountRepository accountRepository;

    @GetMapping("/fb_accounts")
    public ResponseEntity<?> getAllAccounts() {
        return ResponseEntity.ok(accountRepository.findAll());
    }

    @GetMapping("/fb_accounts/{id}")
    public ResponseEntity<?> getAccountById(@PathVariable UUID id) {
        var fb_account = accountRepository.findById(id);

        if (fb_account == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(fb_account);
    }

    @PostMapping("/fb_accounts")
    public ResponseEntity<?> addNewAccount(@RequestBody Account account) {
        return ResponseEntity.ok(accountRepository.save(account));
    }

    @PutMapping("/fb_accounts/{id}")
    public ResponseEntity<?> UpdateAccount(@PathVariable UUID id, @RequestBody Account account) {
        account.setId(id);
        return ResponseEntity.ok(accountRepository.save(account));
    }
    @DeleteMapping("/fb_accounts/{id}")
    public ResponseEntity<?> deleteAccount(@PathVariable UUID id) {
        accountRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
