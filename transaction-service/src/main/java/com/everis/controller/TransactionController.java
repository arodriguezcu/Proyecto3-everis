package com.everis.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.everis.dto.Response;
import com.everis.model.Transaction;
import com.everis.model.Transfer;
import com.everis.service.ITransactionService;
import com.everis.service.ITransferService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/transaction")
public class TransactionController {
   
  @Autowired
  private ITransactionService service;
  
  @Autowired
  private ITransferService transferService;
  
  @GetMapping
  public Mono<ResponseEntity<List<Transaction>>> findAll(){ 
  
  return service.findAll()
    .collectList()
    .flatMap(list -> {
      return list.size() > 0 ? 
        Mono.just(ResponseEntity
          .ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(list)) :
        Mono.just(ResponseEntity
          .noContent()
          .build());
    });
    
  }
  
  @GetMapping("/{cardNumber}")
  public Mono<ResponseEntity<Response>> findAllByCardNumber(@PathVariable("cardNumber") String cardNumber) { 
  
    return service.findAll().filter(list -> list.getPurchase().getCardNumber().equals(cardNumber))
        .collectList()
        .flatMap(list -> {
        
          return list.size() > 0 
              ?
                  Mono.just(ResponseEntity
                      .ok()
                      .contentType(MediaType.APPLICATION_JSON)
                      .body(Response
                          .builder()
                          .data(list)
                          .build()))
              :
                  Mono.just(ResponseEntity
                      .badRequest()
                      .body(Response
                          .builder()
                          .error("El numero de tarjeta " + cardNumber + " no existe.")
                          .build()));
          
      });
    
  }
  
  //Implementar un reporte con los últimos 10 movimientos de la tarjeta de débito y de crédito.
  @GetMapping("list10/{cardNumber}")
  public Mono<ResponseEntity<Response>> findTop10ByCardNumber(@PathVariable("cardNumber") String cardNumber) { 
  
    return service.findAll().filter(list -> list.getPurchase().getCardNumber().equals(cardNumber))
        .takeLast(10)
        .collectList()
        .flatMap(list -> {
        
          return list.size() > 0 
              ?
                  Mono.just(ResponseEntity
                      .ok()
                      .contentType(MediaType.APPLICATION_JSON)
                      .body(Response
                          .builder()
                          .data(list)
                          .build()))
              :
                  Mono.just(ResponseEntity
                      .badRequest()
                      .body(Response
                          .builder()
                          .error("El numero de tarjeta " + cardNumber + " no existe.")
                          .build()));
          
      });
    
  }
  
  //Consultar el saldo de la cuenta principal asociada a la tarjeta de débito
  @GetMapping("current-balance/{cardNumber}")
  public Mono<Object> currentBalance(@PathVariable("cardNumber") String cardNumber) { 
  
    /*return service.findAll().filter(list -> list.getPurchase().getCardNumber().equals(cardNumber))
            .takeLast(1);*/
    
    return service.findAll().filter(list -> list.getPurchase().getCardNumber().equals(cardNumber))
        .takeLast(1)
        .collectList()
        .flatMap(list -> {
        
          return list.size() > 0 
              ?
                  Mono.just(ResponseEntity
                      .ok()
                      .contentType(MediaType.APPLICATION_JSON)
                      .body(Response
                          .builder()
                          .data(list)
                          .build()))
              :
                  Mono.just(ResponseEntity
                      .badRequest()
                      .body(Response
                          .builder()
                          .error("El numero de tarjeta " + cardNumber + " no existe.")
                          .build()));
          
      });
    
  }
  
  @GetMapping("/commission/{cardNumber}")
  public Mono<ResponseEntity<Response>> findAllByCommission(@PathVariable("cardNumber") String cardNumber) { 
  
    return service.findAll()
        .filter(list -> list.getPurchase().getCardNumber().equals(cardNumber))
        .filter(list -> list.getCommission() > 0)
        .collectList()
        .flatMap(list -> {
        
          return list.size() > 0 
              ?
                  Mono.just(ResponseEntity
                      .ok()
                      .contentType(MediaType.APPLICATION_JSON)
                      .body(Response
                          .builder()
                          .data(list)
                          .build()))
              :
                  Mono.just(ResponseEntity
                      .badRequest()
                      .body(Response
                          .builder()
                          .error("El numero de tarjeta " + cardNumber + " no existe.")
                          .build()));
          
      });
    
  }
  
  @PostMapping
  public Mono<ResponseEntity<Response>> createTransfer(@RequestBody Transfer transfer) {
    
    return transferService.createTransfer(transfer)
        .map(objectCreate -> ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(Response
                .builder()
                .data(objectCreate)
                .build()));
    
  }
  
}
