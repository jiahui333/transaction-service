package com.example.service;

import com.example.dto.AccountResponse;
import com.example.entity.Transaction;
import com.example.repository.TransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class TransactionServiceImplTest {
    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Value("http://localhost:8081/accounts/")
    private String accountServiceUrl;

    private AutoCloseable autoCloseable;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
    }

    @Test
    void whenTransfer_createTransaction() {
        AccountResponse sender = new AccountResponse();
        sender.setId(1L);
        sender.setBalance(new BigDecimal("1000"));

        AccountResponse recipient = new AccountResponse();
        recipient.setId(2L);
        recipient.setBalance(new BigDecimal("600"));

        when(restTemplate.getForObject(accountServiceUrl + sender.getId(), AccountResponse.class)).thenReturn(sender);
        when(restTemplate.getForObject(accountServiceUrl + recipient.getId(), AccountResponse.class)).thenReturn(recipient);
        doNothing().when(restTemplate).put(anyString(), any(BigDecimal.class));

        Transaction transaction = new Transaction();
        transaction.setSenderAccountId(sender.getId());
        transaction.setRecipientAccountId(recipient.getId());
        transaction.setAmount(new BigDecimal("100"));
        transaction.setTimestamp(OffsetDateTime.now());

        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        Transaction createdTransaction = transactionService.transfer(sender.getId(), recipient.getId(), new BigDecimal("100"));

        assertNotNull(createdTransaction);
        assertThat(sender.getBalance(), comparesEqualTo(new BigDecimal("900")));
        assertThat(recipient.getBalance(), comparesEqualTo(new BigDecimal("700")));
        assertEquals(sender.getId(), createdTransaction.getSenderAccountId());
        assertEquals(recipient.getId(), createdTransaction.getRecipientAccountId());
        assertThat(createdTransaction.getAmount(), comparesEqualTo(new BigDecimal("100")));

        verify(restTemplate).put(accountServiceUrl + sender.getId() + "/balance", sender.getBalance());
        verify(restTemplate).put(accountServiceUrl + recipient.getId() + "/balance", recipient.getBalance());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void whenTransfer_InsufficientFunds() {
        AccountResponse sender = new AccountResponse();
        sender.setId(1L);
        sender.setBalance(new BigDecimal("60"));

        AccountResponse recipient = new AccountResponse();
        recipient.setId(2L);
        recipient.setBalance(new BigDecimal("600"));

        when(restTemplate.getForObject(accountServiceUrl + sender.getId(), AccountResponse.class)).thenReturn(sender);
        when(restTemplate.getForObject(accountServiceUrl + recipient.getId(), AccountResponse.class)).thenReturn(recipient);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                transactionService.transfer(sender.getId(), recipient.getId(), new BigDecimal("100")));

        assertEquals("Insufficient funds", exception.getMessage());

        verify(restTemplate, never()).put(anyString(), any(BigDecimal.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void whenTransfer_SenderAccountNotFound() {
        when(restTemplate.getForObject(accountServiceUrl + "1", AccountResponse.class)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                transactionService.transfer(1L, 2L, new BigDecimal("100")));

        assertEquals("Sender account not found", exception.getMessage());

        verify(restTemplate, never()).put(anyString(), any(BigDecimal.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void whenTransfer_RecipientAccountNotFound() {
        AccountResponse sender = new AccountResponse();
        sender.setId(1L);
        sender.setBalance(new BigDecimal("1000"));

        when(restTemplate.getForObject(accountServiceUrl + sender.getId(), AccountResponse.class)).thenReturn(sender);
        when(restTemplate.getForObject(accountServiceUrl + "2", AccountResponse.class)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                transactionService.transfer(1L, 2L, new BigDecimal("100")));

        assertEquals("Recipient account not found", exception.getMessage());

        verify(restTemplate, never()).put(anyString(), any(BigDecimal.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }
}
