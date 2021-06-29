package com.everis.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.everis.model.CreditPayment;
import com.everis.repository.ICreditPaymentRepository;
import com.everis.repository.IRepository;
import com.everis.service.ICreditPaymentService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class CreditPaymentService extends CRUDServiceImpl<CreditPayment, String> implements ICreditPaymentService {

  private final String CIRCUIT_BREAKER = "creditPaymentServiceCircuitBreaker";
  
  @Value("${msg.error.registro.notfound}")
  private String msgNotFound;
  
  @Value("${msg.error.registro.notfound.update}")
  private String msgNotFoundUpdate;
  
  @Autowired
  private ICreditPaymentRepository repository;
  
  @Override
  protected IRepository<CreditPayment, String> getRepository() {
  
    return repository;
  
  }

  @Override
  @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "findByIdCreditPaymentFallback")
  public Mono<CreditPayment> findByIdCreditPayment(String id) {
      
    return repository.findById(id)
        .switchIfEmpty(Mono.error(new RuntimeException(msgNotFound)));
    
  }
  
  public Mono<CreditPayment> findByIdCreditPaymentFallback(String id, Exception ex) {
      
    log.info("ups pago con id{} no encontrado, retornando fallback", id);
      
    return Mono.just(CreditPayment
        .builder()
        .id(msgNotFound)
        .build());
    
  }
  
}
