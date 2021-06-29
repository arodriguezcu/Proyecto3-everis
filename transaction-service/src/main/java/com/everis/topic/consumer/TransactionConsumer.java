package com.everis.topic.consumer;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.everis.model.Account;
import com.everis.model.CreditConsumer;
import com.everis.model.CreditPayment;
import com.everis.model.Deposit;
import com.everis.model.Transaction;
import com.everis.model.Withdrawal;
import com.everis.service.IAccountService;
import com.everis.service.ITransactionService;
import com.everis.topic.producer.TransactionProducer;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.Disposable;
import reactor.core.publisher.Mono;

@Component
public class TransactionConsumer {
  
  @Autowired
  private ObjectMapper objectMapper;
  
  @Autowired
  private ITransactionService transactionService;

  @Autowired
  private IAccountService accountService;
    
  @Autowired
  private TransactionProducer producer;

  @KafkaListener(topics = "created-account-topic", groupId = "transaction-group")
  public Disposable retrieveCreatedAccount(String data) throws Exception {
  
    Account account = objectMapper.readValue(data, Account.class);
    
    return Mono.just(account)
        .log()
        .flatMap(accountService::update)
        .subscribe();
  
  }

  @KafkaListener(topics = "created-withdrawal-topic", groupId = "transaction-group")
  public Disposable retrieveCreatedWithdrawal(String data) throws Exception {
  
    Withdrawal withdrawal = objectMapper.readValue(data, Withdrawal.class);
  
    Double commission = 0.5;
    
    return Mono.just(withdrawal)
      .map(w -> {
        
        Double commissionFin = withdrawal.getAmount();
        
        if (withdrawal.getPurchase().getProduct().getCondition().getMonthlyTransactionLimit().equals(0)) {
          
          int cantidad = (int) Math.round(w.getAmount() / 100);
          
          w.setAmount(w.getAmount() + (commission * cantidad));
                    
        }
        
        if (withdrawal.getPurchase().getProduct().getCondition().getMonthlyTransactionLimit() > 0) {
          
          w.getPurchase().getProduct().getCondition().setMonthlyTransactionLimit(
              w.getPurchase().getProduct().getCondition().getMonthlyTransactionLimit() - 1
          );
          
        } 
        
        commissionFin = w.getAmount() - commissionFin;
        
        w.getAccount().setCurrentBalance(w.getAccount().getCurrentBalance() - commissionFin);
        
        return transactionService
          .create(Transaction
            .builder()
            .account(w.getAccount())
            .purchase(w.getPurchase())
            .description(w.getDescription())
            .transactionType("RETIRO")
            .transactionAmount(w.getAmount())
            .commission(commissionFin)
            .transactionDate(LocalDateTime.now())
            .build())
          .block();
        
      })
      .flatMap(a -> {
        producer.sendCreatedTransactionTopic(a);
        return Mono.just(a);
      })
      .subscribe();
  
  }

  @KafkaListener(topics = "created-deposit-topic", groupId = "transaction-group")
  public Disposable retrieveCreatedDeposit(String data) throws Exception {
  
    Deposit deposit = objectMapper.readValue(data, Deposit.class);
    
    Double commission = 0.5;
    
    return Mono.just(deposit)
        .map(d -> {
          
          Double commissionFin = deposit.getAmount();
          
          if (deposit.getPurchase().getProduct().getCondition().getMonthlyTransactionLimit().equals(0)) {
            
            int cantidad = (int) Math.round(d.getAmount() / 100);
            
            d.setAmount(d.getAmount() - (commission * cantidad));
                                 
          }
        
          if (deposit.getPurchase().getProduct().getCondition().getMonthlyTransactionLimit() > 0) {
            
            d.getPurchase().getProduct().getCondition().setMonthlyTransactionLimit(
                d.getPurchase().getProduct().getCondition().getMonthlyTransactionLimit() - 1
            );
            
          }
          
          commissionFin = commissionFin - d.getAmount();
          
          d.getAccount().setCurrentBalance(d.getAccount().getCurrentBalance() - commissionFin);
          
          return transactionService
              .create(Transaction
                  .builder()
                  .account(d.getAccount())
                  .purchase(d.getPurchase())
                  .description(d.getDescription())
                  .transactionType("DEPOSITO")
                  .transactionAmount(d.getAmount())
                  .commission(commissionFin)
                  .transactionDate(LocalDateTime.now())
                  .build())
              .block();
          
        })
        .flatMap(a -> {
                  
          producer.sendCreatedTransactionTopic(a);
          return Mono.just(a);
          
        })
        .subscribe();
  
  }

  @KafkaListener(topics = "created-credit-consumer-topic", groupId = "transaction-group")
  public Disposable retrieveCreatedCreditConsumer(String data) throws Exception {
  
  CreditConsumer creditConsumer = objectMapper.readValue(data, CreditConsumer.class);
  
  return Mono.just(creditConsumer)
    .map(c -> {
      return transactionService
        .create(Transaction
          .builder()
          .purchase(c.getPurchase())
          .description(c.getDescription())
          .transactionType("CONSUMO TARJETA CREDITO")
          .transactionAmount(c.getAmount())
          .transactionDate(c.getConsumDate())
          .build())
        .block();
    })
    .flatMap(a -> {
      producer.sendCreatedTransactionTopic(a);
      return Mono.just(a);
    })
    .subscribe();
  
  }

  @KafkaListener(topics = "created-credit-payment-topic", groupId = "transaction-group")
  public Disposable retrieveCreatedCreditPayment(String data) throws Exception {
  
  CreditPayment creditPayment = objectMapper.readValue(data, CreditPayment.class);
  
  return Mono.just(creditPayment)
    .map(p -> {
      return transactionService
        .create(Transaction
          .builder()
          .purchase(p.getPurchase())
          .description(p.getDescription())
          .transactionType("PAGO TARJETA CREDITO")
          .transactionAmount(p.getAmount())
          .transactionDate(p.getPaymentDate())
          .build())
        .block();
    })
    .flatMap(a -> {
      producer.sendCreatedTransactionTopic(a);
      return Mono.just(a);
    })
    .subscribe();
  
  }
  
}

