package com.rajeswaran.account.service;

import com.rajeswaran.account.entity.Account;
import com.rajeswaran.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }


    //Get account by account number
    public Optional<Account> getAccountByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }

    public Account createAccount(Account account) {
        // Set created timestamp if not already set
        if (account.getCreatedTimestamp() == null) {
            account.setCreatedTimestamp(LocalDateTime.now());
        }
        
        return accountRepository.save(account);
    }

    public void deleteAccount(String id) {
        accountRepository.deleteById(id);
    }

    // Get accounts by user ID
    public List<Account> getAccountsByUserId(String userId) {
        return accountRepository.findByUserId(userId);
    }

    // Get accounts by user name
    public List<Account> getAccountsByUserName(String userName) {
        return accountRepository.findByUserName(userName);
    }

    // Validate if account belongs to the specified user
    public boolean validateAccountOwnership(String accountNumber, String userName) {
        Optional<Account> accountOpt = getAccountByAccountNumber(accountNumber);
        if (accountOpt.isPresent()) {
            Account account = accountOpt.get();
            return account.getUserName().equals(userName);
        }
        return false;
    }
}
