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
import com.everis.model.CreditPayment;
import com.everis.model.Purchase;
import com.everis.service.ICreditPaymentService;
import com.everis.service.IPurchaseService;
import com.everis.topic.producer.CreditPaymentProducer;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/payment")
public class CreditPaymentController {

  @Autowired
  private ICreditPaymentService service;
  
  @Autowired
  private IPurchaseService purchaseService;
  
  @Autowired
  private CreditPaymentProducer creditPaymentProducer;

  @GetMapping
  public Mono<ResponseEntity<List<CreditPayment>>> findAll() {
  
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
  public Mono<ResponseEntity<CreditPayment>> findById(@PathVariable("id") String id) {
  
  return service.findById(id)
    .map(objectFound -> ResponseEntity
      .ok()
      .contentType(MediaType.APPLICATION_JSON)
      .body(objectFound));
  
  }
  
  @PostMapping
  public Mono<ResponseEntity<Response>> create(@RequestBody CreditPayment creditPayment, final ServerHttpRequest request) {
  
  Mono<Purchase> purchaseDB = purchaseService.findByCardNumber(creditPayment.getPurchase().getCardNumber());
  
  return purchaseDB
    .flatMap(purchase -> {
      Double monto = purchase.getAmountIni() - purchase.getAmountFin();      
      if(creditPayment.getAmount() < monto) {
      return Mono.just(ResponseEntity
        .badRequest()
        .body(Response
          .builder()
          .error("El monto a pagar es : " + monto)
          .build()));
      }
      Double cambio = creditPayment.getAmount() - (purchase.getAmountIni() - purchase.getAmountFin());
      creditPayment.setAmount(purchase.getAmountIni() - purchase.getAmountFin());
      purchase.setAmountFin(purchase.getAmountIni());
      creditPayment.setPurchase(purchase);
      creditPayment.setPaymentDate(LocalDateTime.now());
      return service.create(creditPayment)
        .flatMap(created -> {
        creditPaymentProducer.sendCreditPaymentTransactionTopic(creditPayment);
        return Mono.just(ResponseEntity
          .ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(Response
            .builder()
            .error("Su cambio es : " + cambio)
            .data(creditPayment)
            .build()));
        });
    })
    .defaultIfEmpty(ResponseEntity
      .badRequest()
      .body(Response
        .builder()
        .error("No es posible realizar el pago, la tarjeta no existe")
        .build()));
  
  }
  
}
