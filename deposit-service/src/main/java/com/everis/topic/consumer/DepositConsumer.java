package com.everis.topic.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.everis.model.Account;
import com.everis.model.Deposit;
import com.everis.model.Purchase;
import com.everis.service.IAccountService;
import com.everis.service.IDepositService;
import com.everis.service.IPurchaseService;
import com.everis.topic.producer.DepositProducer;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.Disposable;
import reactor.core.publisher.Mono;

@Component
public class DepositConsumer {
  
  @Autowired
  private IAccountService accountService;
  
  @Autowired
  private IPurchaseService purchaseService;
  
  @Autowired
  private IDepositService depositService;

  @Autowired
  private DepositProducer depositProducer;
  
  ObjectMapper objectMapper = new ObjectMapper();
  
  @KafkaListener(topics = "created-account-topic", groupId = "deposit-group")
  public Disposable retrieveCreatedAccount(String data) throws Exception {
  
  Account account = objectMapper.readValue(data, Account.class);
    
  return Mono.just(account)
    .log()
    .flatMap(accountService::update)
    .subscribe();
  
  }
  
  @KafkaListener(topics = "created-purchase-topic", groupId = "deposit-group")
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
  
  @KafkaListener(topics = "created-transfer-deposit-topic", groupId = "deposit-group")
  public Disposable retrieveCreatedDeposit(String data) throws Exception {
  
  Deposit deposit = objectMapper.readValue(data, Deposit.class);
  
  depositProducer.sendDepositAccountTopic(deposit);
  
  return Mono.just(deposit)
    .log()
    .flatMap(depositService::update)
    .subscribe();
  
  }
  
}
