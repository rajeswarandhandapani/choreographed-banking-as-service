package com.rajeswaran.transaction.repository;

import com.rajeswaran.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByReference(String reference);
    List<Transaction> findByUsername(String username);
}
