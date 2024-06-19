package com.example.service;

import com.example.entity.Transaction;

import java.math.BigDecimal;

public interface TransactionService{
    Transaction transfer(Long senderAccountId, Long recipientAccountId, BigDecimal amount);
}
