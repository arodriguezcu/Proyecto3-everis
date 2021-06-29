package com.everis.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.everis.model.CreditConsumer;
import com.everis.repository.ICreditConsumerRepository;
import com.everis.repository.IRepository;
import com.everis.service.ICreditConsumerService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class CreditConsumerService extends CRUDServiceImpl<CreditConsumer, String> implements ICreditConsumerService {

  private final String CIRCUIT_BREAKER = "creditConsumerServiceCircuitBreaker";
  
  @Value("${msg.error.registro.notfound}")
  private String msgNotFound;
  
  @Value("${msg.error.registro.notfound.update}")
  private String msgNotFoundUpdate;
  
  @Autowired
  private ICreditConsumerRepository repository;
  
  @Override
  protected IRepository<CreditConsumer, String> getRepository() {
  
    return repository;
  
  }

  @Override
  @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "findByIdCreditConsumerFallback")
  public Mono<CreditConsumer> findByIdCreditConsumer(String id) {
     
    return repository.findById(id)
        .switchIfEmpty(Mono.error(new RuntimeException(msgNotFound)));
    
  }
  
  public Mono<CreditConsumer> findByIdCreditConsumerFallback(String id, Exception ex) {
      
    log.info("ups consumo con id{} no encontrado, retornando fallback", id);
      
    return Mono.just(CreditConsumer
        .builder()
        .id(msgNotFound)
        .build());
    
  }
  
}
