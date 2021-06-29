package com.everis.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.everis.dto.Response;
import com.everis.model.Deposit;
import com.everis.model.Purchase;
import com.everis.service.IAccountService;
import com.everis.service.IDepositService;
import com.everis.service.IPurchaseService;
import com.everis.topic.producer.DepositProducer;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/deposit")
public class DepositController {

  @Autowired
  private IDepositService service;
  
  @Autowired
  private IAccountService accountService;

  @Autowired
  private IPurchaseService purchaseService;
  
  @Autowired
  private DepositProducer depositProducer;
  
  @GetMapping
  public Mono<ResponseEntity<List<Deposit>>> findAll() { 
  
  return service.findAll()
      .collectList()
      .flatMap(list -> {
        
        return list.size() > 0 
            ?
                Mono.just(ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(list))
            :
                Mono.just(ResponseEntity
                    .noContent()
                    .build());
        
    });
    
  }
  
  @GetMapping("/{id}")
  public Mono<ResponseEntity<Deposit>> findById(@PathVariable("id") String id) {
  
  return service.findById(id)
    .map(objectFound -> ResponseEntity
      .ok()
      .contentType(MediaType.APPLICATION_JSON)
      .body(objectFound));
      
  }
  
  @PostMapping
  public Mono<ResponseEntity<Response>> create(@RequestBody Deposit deposit, final ServerHttpRequest request) {
  
  Mono<Purchase> purchaseDB = purchaseService.findByCardNumber(deposit.getPurchase().getCardNumber());
  
  return purchaseDB
    .flatMap(purchase -> {
      
      return accountService.findByAccountNumber(deposit.getAccount().getAccountNumber())
        .flatMap(account -> {
        if(deposit.getAmount() < 0) {
          return Mono.just(ResponseEntity
            .badRequest()
            .body(Response
              .builder()
              .error("El monto debe ser positivo")
              .build()));
        }
        account.setCurrentBalance(account.getCurrentBalance() + deposit.getAmount());
        deposit.setAccount(account);        
        deposit.setPurchase(purchase);
        deposit.setDepositDate(LocalDateTime.now());
        
        depositProducer.sendDepositAccountTopic(deposit); 
        
        if (purchase.getProduct().getCondition().getMonthlyTransactionLimit() > 0) {
          
          deposit.getPurchase().getProduct().getCondition().setMonthlyTransactionLimit(
              purchase.getProduct().getCondition().getMonthlyTransactionLimit() - 1
          );
          
        } 
        
        return service.create(deposit)
          .flatMap(created -> {
            return Mono.just(ResponseEntity
              .ok()
              .contentType(MediaType.APPLICATION_JSON)
              .body(Response
                .builder()
                .data(deposit)
                .build()));
          });
        })
        .defaultIfEmpty(ResponseEntity
          .badRequest()
          .body(Response
            .builder()
            .error("No es posible realizar el retiro, el número de cuenta no existe")
            .build()));
    })
    .defaultIfEmpty(ResponseEntity
      .badRequest()
      .body(Response
        .builder()
        .error("El numero de tarjeta no existe")
        .build()));
  }
  
  @PutMapping("/{id}")
  public Mono<ResponseEntity<Deposit>> update(@RequestBody Deposit deposit, @PathVariable("id") String id) {
  
    return service.updateDeposit(deposit, id)
        .map(objectUpdated -> ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectUpdated));
  
  }
  
  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<String>> delete(@PathVariable("id") String id) {
  
  return service.delete(id)
    .map(objectDeleted -> ResponseEntity
      .ok()
      .contentType(MediaType.APPLICATION_JSON)
      .body(""))
    .defaultIfEmpty(ResponseEntity
      .noContent()
      .build());
    
  }
  
}
