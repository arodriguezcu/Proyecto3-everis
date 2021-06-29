package com.everis.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.everis.model.Withdrawal;
import com.everis.repository.IRepository;
import com.everis.repository.IWithdrawalRepository;
import com.everis.service.IWithdrawalService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class WithdrawalServiceImpl extends CRUDServiceImpl<Withdrawal, String> implements IWithdrawalService {

  private final String CIRCUIT_BREAKER = "accountServiceCircuitBreaker";
  
  @Value("${msg.error.registro.notfound}")
  private String msgNotFound;
  
  @Value("${msg.error.registro.notfound.update}")
  private String msgNotFoundUpdate;
  
  @Autowired
  private IWithdrawalRepository repository;

  @Override
  protected IRepository<Withdrawal, String> getRepository() {
    return repository;
  }
  
  @Override
  @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "findByIdWithdrawalFallback")
  public Mono<Withdrawal> findByIdWithdrawal(String id) {
      return repository.findById(id)
              .switchIfEmpty( Mono.error(new RuntimeException(msgNotFound) ) );
  }
  
  public Mono<Withdrawal> findByIdWithdrawalFallback(String id, Exception ex) {
      
      log.info("ups retiro con id{} no encontrado, retornando fallback",id);
      return Mono.just(Withdrawal.builder()
              .id(msgNotFound)
              .build());
  }

}
