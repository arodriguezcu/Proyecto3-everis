package com.everis.topic.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.everis.model.Account;
import com.everis.model.Purchase;
import com.everis.model.Transaction;
import com.everis.service.IAccountService;
import com.everis.service.IPurchaseService;
import com.everis.topic.producer.AccountProducer;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.Disposable;
import reactor.core.publisher.Mono;

@Component
public class AccountConsumer {

  @Autowired
  private ObjectMapper objectMapper;
  
  @Autowired
  private IAccountService accountService;

  @Autowired
  private IPurchaseService purchaseService;
  
  @Autowired
  private AccountProducer producer;
  
  @KafkaListener(topics = "created-purchase-topic", groupId = "account-group")
  public Disposable retrieveCreatedPurchase(String data) throws Exception {
  
    Purchase purchase = objectMapper.readValue(data, Purchase.class);
    
    if (purchase.getProduct().getProductType().equals("ACTIVO")) {
      
      return null;
        
    }
    
    return Mono.just(purchase)
      .log()
      .flatMap(purchaseService::update)
      .subscribe();
  
  }

  @KafkaListener(topics = "created-transaction-topic", groupId = "account-group")
  public Disposable retrieveCreatedTransaction(String data) throws Exception {
  
    Transaction transaction = objectMapper.readValue(data, Transaction.class);
    
    Mono<Account> monoAccount = Mono.just(Account.builder().build());
    
    if (!transaction.getPurchase().getProduct().getProductType().equals("PASIVO")) {
    
      return null;
      
    } else {
      
      monoAccount = accountService.findById(transaction.getAccount().getId());
      
    }
  
    Mono<Transaction> monoTransaction = Mono.just(transaction);
    
    return monoAccount
      .zipWith(monoTransaction, (a,b) -> {
        if(b.getTransactionType().equals("RETIRO")) {
        a.setCurrentBalance(a.getCurrentBalance() - b.getTransactionAmount());
        a.getPurchase().getProduct().getCondition().setMonthlyTransactionLimit(b.getPurchase().getProduct().getCondition().getMonthlyTransactionLimit());
        } else if (b.getTransactionType().equals("DEPOSITO")) {
        a.setCurrentBalance(a.getCurrentBalance() + b.getTransactionAmount());
        a.getPurchase().getProduct().getCondition().setMonthlyTransactionLimit(b.getPurchase().getProduct().getCondition().getMonthlyTransactionLimit());
        }
        producer.sendCreatedAccount(a);
        return a;
      })
      .flatMap(accountService::update)
      .subscribe();
  
  }
  
}
