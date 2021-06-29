package com.everis.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.everis.model.Purchase;
import com.everis.repository.IPurchaseRepository;
import com.everis.repository.IRepository;
import com.everis.service.IPurchaseService;
import com.everis.topic.producer.PurchaseProducer;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class PurchseServiceImpl extends CRUDServiceImpl<Purchase, String> implements IPurchaseService {

  private final String CIRCUIT_BREAKER = "purchaseServiceCircuitBreaker";
  
  @Value("${msg.error.registro.notfound}")
  private String msgNotFound;
  
  @Value("${msg.error.registro.notfound.update}")
  private String msgNotFoundUpdate;
  
  @Autowired
  private IPurchaseRepository repository;
  
  @Autowired
  private PurchaseProducer producer;
  
  @Override
  protected IRepository<Purchase, String> getRepository() {
  
  return repository;
  
  }

//  @Override
//  public Flux<Purchase> findByCustomerOwner(List<Customer> customers) {
//  
//  return repository.findByCustomerOwner(customers);
//  
//  }

  @Override
  public Mono<List<Purchase>> findByIdentityNumberAndProductID(String identityNumber, String idProduct) {
      return repository.findByIdentityNumberAndProductID(identityNumber,idProduct);
  }

  @Override
  public Mono<Purchase> findByCardNumber(String cardNumber) {
      
      return repository.findByCardNumber(cardNumber);
      
  }

  @Override
  @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "updatePurchaseFallback")
  public Mono<Purchase> updatePurchase(Purchase purchase, String id) {
      Mono<Purchase> customerModification = Mono.just(purchase);
      
      Mono<Purchase> customerDatabase = repository.findById(id);
      
      return customerDatabase
              .zipWith(customerModification, (a,b) -> {
                  a.setId(id);
                  a.setProduct(purchase.getProduct());
                  a.setCustomerOwner(purchase.getCustomerOwner());
                  a.setAuthorizedSigner(purchase.getAuthorizedSigner());
                  a.setPurchaseDate(purchase.getPurchaseDate());
                  return a;
              })
              .flatMap(repository::save)
              .map(objectUpdated -> {
                  producer.sendCreatePurchase(objectUpdated);
                  return objectUpdated;
              }).switchIfEmpty(Mono.error( new RuntimeException(msgNotFoundUpdate) ));
              
  }
  
  public Mono<Purchase> updatePurchaseFallback(Purchase purchase, String id, Exception ex) {
      
      log.info("ups purchase con id{} no encontrado para actualizar, retornando fallback",id);
      return Mono.just(Purchase.builder()
              .id(msgNotFoundUpdate)
              .build());
      
  }
  
}
