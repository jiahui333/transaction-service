package com.example.service;

import com.example.dto.AccountResponse;
import com.example.entity.Transaction;
import com.example.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
public class TransactionServiceImpl implements TransactionService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionServiceImpl.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TransactionRepository transactionRepository;

    @Value("${account.service.url}")
    private String accountServiceUrl;

    @Override
    public Transaction transfer(Long senderAccountId, Long recipientAccountId, BigDecimal amount) {
        AccountResponse senderAccount;
        AccountResponse recipientAccount;

        try {
            senderAccount = restTemplate.getForObject(accountServiceUrl + senderAccountId, AccountResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            logger.error("Sender account not found by Id: {}", senderAccountId);
            throw new IllegalArgumentException("Sender account not found");
        }

       try {
           recipientAccount = restTemplate.getForObject(accountServiceUrl + recipientAccountId, AccountResponse.class);
       } catch (HttpClientErrorException.NotFound e) {
           logger.error("Recipient account not found by Id: {}", recipientAccountId);
           throw new IllegalArgumentException("Recipient account not found");
       }

        if (senderAccount.getBalance().compareTo(amount) <= 0) {
            logger.error("Insufficient funds in sender account with id: {}", senderAccountId);
            throw new IllegalStateException("Insufficient funds");
        }

        senderAccount.setBalance(senderAccount.getBalance().subtract(amount));
        recipientAccount.setBalance(recipientAccount.getBalance().add(amount));

        restTemplate.put(accountServiceUrl + senderAccountId + "/balance", senderAccount.getBalance());
        restTemplate.put(accountServiceUrl + recipientAccountId + "/balance", recipientAccount.getBalance());

        Transaction transaction = new Transaction();
        transaction.setSenderAccountId(senderAccountId);
        transaction.setRecipientAccountId(recipientAccountId);
        transaction.setAmount(amount);
        transaction.setTimestamp(OffsetDateTime.now());
        Transaction savedTransaction = transactionRepository.save(transaction);
        logger.info("Transaction successful with transaction id: {}", savedTransaction.getId());
        return savedTransaction;
    }
}
