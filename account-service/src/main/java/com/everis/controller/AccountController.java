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
import com.everis.model.Account;
import com.everis.model.Purchase;
import com.everis.service.IAccountService;
import com.everis.service.IPurchaseService;
import com.everis.topic.producer.AccountProducer;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/account")
public class AccountController {
  
  @Autowired
  private IAccountService service;
  
  @Autowired
  private IPurchaseService purchaseService;
  
  @Autowired
  private AccountProducer producer;
  
  @GetMapping("/welcome")
  public Mono<ResponseEntity<String>> welcome(){
  
    return Mono.just(ResponseEntity
      .ok()
      .contentType(MediaType.APPLICATION_JSON)
      .body("Welcome Account"));
    
    }
  
  @GetMapping
  public Mono<ResponseEntity<List<Account>>> findAll(){ 
  
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
  public Mono<ResponseEntity<Account>> findById(@PathVariable("id") String id){
  
    return service.findById(id)
        .map(objectFound -> ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectFound))
        .defaultIfEmpty(ResponseEntity
            .noContent()
            .build());
  
  }
  
  @PostMapping
  public Mono<ResponseEntity<Response>> create(@RequestBody Account account, final ServerHttpRequest request){
    
    Mono<Purchase> purchaseDB = purchaseService.findByCardNumber(account.getPurchase().getCardNumber());
    
    Flux<Account> accountDB = service.findAll().filter(list -> list.getAccountNumber().equals(account.getAccountNumber()))
        .mergeWith(service.findAll().filter(list -> list.getPurchase().getCardNumber().equals(account.getPurchase().getCardNumber())));
    
    return purchaseDB
        .flatMap(purchase -> {
          
          account.setPurchase(purchase);
          
          return accountDB
              .collectList()
              .flatMap(list -> {
                
                account.setCurrentBalance(purchase.getAmountIni());
                account.setDateOpened(LocalDateTime.now());
                
                return list.size() > 0
                    ?
                        Mono.just(ResponseEntity
                            .badRequest()
                            .body(Response
                              .builder()
                              .data("Ya existe una cuenta para esta tarjeta.")
                              .build()))
                    :
                        service.create(account)
                        .flatMap(createdObject -> {
                          
                          producer.sendCreatedAccount(createdObject);
                          
                          return Mono.just(ResponseEntity
                              .ok()
                              .contentType(MediaType.APPLICATION_JSON)
                              .body(Response
                                  .builder()
                                  .data(createdObject)
                                  .build()));
                          
                        });
              
              });
    
        })
        .defaultIfEmpty(ResponseEntity
            .badRequest()
            .body(Response
                .builder()
                .error("El numero de tarjeta " + account.getPurchase().getCardNumber() + " no existe")
                .build()));
  
  }
  
  @PutMapping("/{id}")
  public Mono<ResponseEntity<Account>> update(@RequestBody Account account, @PathVariable("id") String id){
  
    return service.updateAccount(account, id)
        .map(objectUpdated -> ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectUpdated));
    
  }
  
  @DeleteMapping("/{accountNumber}")
  public Mono<ResponseEntity<Response>> delete(@PathVariable("accountNumber") String accountNumber){
    
    Mono<Account> customerDatabase = service.findByAccountNumber(accountNumber).filter(p -> p.getDateClosed() == null);
    
    return customerDatabase
        .flatMap(objectDelete -> {
          
          objectDelete.setDateClosed(LocalDateTime.now());
          
          return service.update(objectDelete)
              .then(Mono.just(ResponseEntity
                  .ok()
                  .contentType(MediaType.APPLICATION_JSON)
                  .body(Response
                      .builder()
                      .data("Cuenta eliminada")
                      .build())));
    
        })
        .defaultIfEmpty(ResponseEntity
            .badRequest()
            .body(Response
                .builder()
                .data("La cuenta ya ha sido eliminada")
                .build()));
    
  }
  
}