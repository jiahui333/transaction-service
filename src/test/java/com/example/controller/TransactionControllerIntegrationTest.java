package com.example.controller;

import com.example.dto.AccountResponse;
import com.example.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }

    @Test
    void whenTransfer_Success() throws Exception {
        AccountResponse sender = new AccountResponse();
        sender.setId(1L);
        sender.setName("Sender");
        sender.setEmail("sender@test.com");
        sender.setBalance(BigDecimal.valueOf(1000));

        AccountResponse recipient = new AccountResponse();
        recipient.setId(2L);
        recipient.setName("Recipient");
        recipient.setEmail("recipient@test.com");
        recipient.setBalance(BigDecimal.valueOf(500));

        when(restTemplate.getForObject(any(String.class), eq(AccountResponse.class)))
                .thenReturn(sender, recipient);
        doNothing().when(restTemplate).put(any(String.class), any(BigDecimal.class));

        mockMvc.perform(post("/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senderAccountId\":" + sender.getId() + ",\"recipientAccountId\":" + recipient.getId() + ",\"amount\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(100))
                .andExpect(jsonPath("$.senderAccountId", is(sender.getId().intValue())))
                .andExpect(jsonPath("$.recipientAccountId", is(recipient.getId().intValue())));
    }

    @Test
    void whenTransfer_invalidInput() throws Exception {
        AccountResponse recipient = new AccountResponse();
        recipient.setId(2L);
        recipient.setName("Recipient");
        recipient.setEmail("recipient@test.com");
        recipient.setBalance(BigDecimal.valueOf(500));

        when(restTemplate.getForObject(any(String.class), eq(AccountResponse.class)))
                .thenReturn(recipient);

        mockMvc.perform(post("/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientAccountId\":" + recipient.getId() + ",\"amount\":100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.senderAccountId").exists())
                .andExpect(jsonPath("$.senderAccountId", is("Send ID is required")));
    }

    @Test
    void whenTransfer_insufficientFunds() throws Exception {
        AccountResponse sender = new AccountResponse();
        sender.setId(1L);
        sender.setName("Sender");
        sender.setEmail("sender@test.com");
        sender.setBalance(BigDecimal.valueOf(50));

        AccountResponse recipient = new AccountResponse();
        recipient.setId(2L);
        recipient.setName("Recipient");
        recipient.setEmail("recipient@test.com");
        recipient.setBalance(BigDecimal.valueOf(500));

        when(restTemplate.getForObject(any(String.class), eq(AccountResponse.class)))
                .thenReturn(sender, recipient);

        mockMvc.perform(post("/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senderAccountId\":" + sender.getId() + ",\"recipientAccountId\":" + recipient.getId() + ",\"amount\":100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", is("Insufficient funds")));
    }

    @Test
    void whenTransfer_senderNotFound() throws Exception {
        AccountResponse recipient = new AccountResponse();
        recipient.setId(2L);
        recipient.setName("Recipient");
        recipient.setEmail("recipient@test.com");
        recipient.setBalance(BigDecimal.valueOf(500));

        when(restTemplate.getForObject(any(String.class), eq(AccountResponse.class)))
                .thenReturn(null, recipient);

        mockMvc.perform(post("/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senderAccountId\":999,\"recipientAccountId\":" + recipient.getId() + ",\"amount\":100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", is("Sender account not found")));
    }

    @Test
    void whenTransfer_recipientNotFound() throws Exception {
        AccountResponse sender = new AccountResponse();
        sender.setId(1L);
        sender.setName("Sender");
        sender.setEmail("sender@test.com");
        sender.setBalance(BigDecimal.valueOf(1000));

        when(restTemplate.getForObject(any(String.class), eq(AccountResponse.class)))
                .thenReturn(sender, null);

        mockMvc.perform(post("/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senderAccountId\":" + sender.getId() + ",\"recipientAccountId\":999,\"amount\":100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", is("Recipient account not found")));
    }
}