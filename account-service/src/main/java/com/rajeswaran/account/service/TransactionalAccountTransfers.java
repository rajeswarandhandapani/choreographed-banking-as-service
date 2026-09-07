package com.rajeswaran.account.service;

import com.rajeswaran.account.entity.Account;
import com.rajeswaran.account.entity.TransferReceipt;
import com.rajeswaran.account.repository.AccountRepository;
import com.rajeswaran.account.repository.TransferReceiptRepository;
import com.rajeswaran.common.events.PaymentInitiatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionalAccountTransfers implements AccountTransfers {
    private final AccountRepository accounts;
    private final TransferReceiptRepository receipts;

    @Override
    @Transactional
    public TransferReceipt transfer(PaymentInitiatedEvent request) {
        TransferPolicy.validate(request);
        String source = request.getSourceAccountNumber(), destination = request.getDestinationAccountNumber();
        String first = source.compareTo(destination) < 0 ? source : destination;
        Account a = find(first);
        Account b = find(first.equals(source) ? destination : source);
        // Check after locking: concurrent copies of the same payment serialize here.
        var previous = receipts.findById(request.getPaymentId());
        if (previous.isPresent()) {
            TransferReceipt receipt = previous.get();
            if (!source.equals(receipt.getSourceAccountNumber()) || !destination.equals(receipt.getDestinationAccountNumber())
                    || Double.compare(request.getAmount(), receipt.getAmount()) != 0 || !java.util.Objects.equals(request.getUsername(), receipt.getUsername())) {
                throw new IllegalArgumentException("Payment ID was already used for different transfer details");
            }
            return receipt;
        }
        Account debit = first.equals(source) ? a : b, credit = first.equals(source) ? b : a;
        TransferPolicy.apply(debit, credit, request);
        accounts.save(debit);
        accounts.save(credit);
        TransferReceipt receipt = new TransferReceipt();
        receipt.setPaymentId(request.getPaymentId());
        receipt.setSourceAccountNumber(source);
        receipt.setDestinationAccountNumber(destination);
        receipt.setUsername(request.getUsername());
        receipt.setRecipientUsername(credit.getUserName());
        receipt.setAmount(request.getAmount());
        receipt.setSourceBalance(debit.getBalance());
        receipt.setDestinationBalance(credit.getBalance());
        return receipts.saveAndFlush(receipt);
    }
    private Account find(String number) {
        return accounts.findByAccountNumberWithLock(number).orElseThrow(() -> new IllegalArgumentException("Account not found: " + number));
    }
}
