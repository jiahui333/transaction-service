package com.example.controller;

import com.example.dto.TransactionRequest;
import com.example.entity.Transaction;
import com.example.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/transactions")
public class TransactionController {
    @Autowired
    private TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer (@Valid @RequestBody TransactionRequest transactionRequest) {
        try {
            Transaction transaction = transactionService.transfer(transactionRequest.getSenderAccountId(), transactionRequest.getRecipientAccountId(), transactionRequest.getAmount());
            return ResponseEntity.ok(transaction);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
