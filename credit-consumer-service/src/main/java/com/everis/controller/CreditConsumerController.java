package com.everis.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.everis.dto.Response;
import com.everis.model.CreditConsumer;
import com.everis.model.Purchase;
import com.everis.service.ICreditConsumerService;
import com.everis.service.IPurchaseService;
import com.everis.topic.producer.CreditConsumerProducer;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/credit")
public class CreditConsumerController {

  @Autowired
  private ICreditConsumerService service;
  
  @Autowired
  private IPurchaseService purchaseService;
  
  @Autowired
  private CreditConsumerProducer creditConsumerProducer;

  @GetMapping
  public Mono<ResponseEntity<List<CreditConsumer>>> findAll() {
  
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
  
  @GetMapping("/{id}")
  public Mono<ResponseEntity<CreditConsumer>> findById(@PathVariable("id") String id) {
  
    return service.findById(id)
      .map(objectFound -> ResponseEntity
        .ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(objectFound));
  
  }
  
  @PostMapping
  public Mono<ResponseEntity<Response>> create(@RequestBody CreditConsumer creditConsumer, final ServerHttpRequest request) {
  
  Mono<Purchase> purchaseDB = purchaseService.findByCardNumber(creditConsumer.getPurchase().getCardNumber());
    
  return purchaseDB
    .flatMap(purchase -> {
      if (creditConsumer.getAmount() < 0) {
      return Mono.just(ResponseEntity
        .badRequest()
        .body(Response
          .builder()
          .error("El monto debe ser positivo.")
          .build()));      
      }      
      if(creditConsumer.getAmount() > purchase.getAmountFin()) {
      return Mono.just(ResponseEntity
        .badRequest()
        .body(Response
          .builder()
          .error("El monto a cobrar excede al saldo disponible")
          .build()));
      }
      purchase.setAmountFin(purchase.getAmountFin() - creditConsumer.getAmount());
      creditConsumer.setPurchase(purchase);
      creditConsumer.setConsumDate(LocalDateTime.now());
      return service.create(creditConsumer)
        .flatMap(created -> {
        creditConsumerProducer.sendCreditConsumerTransactionTopic(creditConsumer);
        return Mono.just(ResponseEntity
          .ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(Response
            .builder()
            .data(creditConsumer)
            .build()));
        });
    })
    .defaultIfEmpty(ResponseEntity
      .badRequest()
      .body(Response
        .builder()
        .error("El numero de tarjeta no existe")
        .build()));
  
  }
  
}
