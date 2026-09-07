package com.rajeswaran.account.repository;

import com.rajeswaran.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Account a where a.accountNumber = :number")
    Optional<Account> findByAccountNumberWithLock(@org.springframework.data.repository.query.Param("number") String number);

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByUserId(String userId);

    List<Account> findByUserName(String userName);
}
