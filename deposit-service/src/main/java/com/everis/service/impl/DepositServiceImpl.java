package com.everis.service.impl;

import com.everis.model.Account;
import com.everis.model.Deposit;
import com.everis.model.Purchase;
import com.everis.repository.InterfaceDepositRepository;
import com.everis.repository.InterfaceRepository;
import com.everis.service.InterfaceAccountService;
import com.everis.service.InterfaceDepositService;
import com.everis.service.InterfacePurchaseService;
import com.everis.topic.producer.DepositProducer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Implementacion de Metodos del Service Deposit.
 */
@Slf4j
@Service
public class DepositServiceImpl extends CrudServiceImpl<Deposit, String> 
    implements InterfaceDepositService {

  static final String CIRCUIT = "depositServiceCircuitBreaker";

  @Value("${msg.error.registro.notfound.all}")
  private String msgNotFoundAll;
  
  @Value("${msg.error.registro.positive}")
  private String msgPositive;
  
  @Value("${msg.error.registro.account.exists}")
  private String msgAccountNotExists;
  
  @Value("${msg.error.registro.card.exists}")
  private String msgCardNotExists;
  
  @Value("${msg.error.registro.notfound.create}")
  private String msgNotFoundCreate;
  
  @Autowired
  private InterfaceDepositRepository repository;
  
  @Autowired
  private InterfaceDepositService service;
  
  @Autowired
  private InterfacePurchaseService purchaseService;
  
  @Autowired
  private InterfaceAccountService accountService;
  
  @Autowired
  private DepositProducer producer;
  
  @Override
  protected InterfaceRepository<Deposit, String> getRepository() {
  
    return repository;
  
  }
  
  @Override
  @CircuitBreaker(name = CIRCUIT, fallbackMethod = "findAllFallback")
  public Mono<List<Deposit>> findAllDeposit() {
    
    Flux<Deposit> depositDatabase = service.findAll()
        .switchIfEmpty(Mono.error(new RuntimeException(msgNotFoundAll)));
    
    return depositDatabase.collectList().flatMap(Mono::just);
  
  }

  @Override
  @CircuitBreaker(name = CIRCUIT, fallbackMethod = "createFallback")
  public Mono<Deposit> createDeposit(Deposit deposit) {
    
    Mono<Purchase> purchaseDatabase = purchaseService
        .findByCardNumber(deposit.getPurchase().getCardNumber())
        .switchIfEmpty(Mono.error(new RuntimeException(msgCardNotExists)));
    
    Mono<Account> accountDatabase = accountService
        .findByAccountNumber(deposit.getAccount().getAccountNumber())
        .switchIfEmpty(Mono.error(new RuntimeException(msgAccountNotExists)));
    
    return purchaseDatabase
        .flatMap(purchase -> accountDatabase
            .flatMap(account -> {
                          
              if (deposit.getAmount() < 0) {
                
                return Mono.error(new RuntimeException(msgPositive));
            
              }
            
              account.setCurrentBalance(account.getCurrentBalance() + deposit.getAmount());
              deposit.setAccount(account);        
              deposit.setPurchase(purchase);
              deposit.setDepositDate(LocalDateTime.now());
            
              producer.sendDepositAccountTopic(deposit); 
              
              if (purchase.getProduct().getCondition().getMonthlyTransactionLimit() > 0) {
                
                deposit.getPurchase().getProduct().getCondition().setMonthlyTransactionLimit(
                    purchase.getProduct().getCondition().getMonthlyTransactionLimit() - 1
                );
                
              } 
        
              return service.create(deposit)
                .map(createdObject -> createdObject)
                .switchIfEmpty(Mono.error(new RuntimeException(msgNotFoundCreate)));
                             
            }));
    
  }
  
  /** Mensaje si no existen depositos. */
  public Mono<List<Deposit>> findAllFallback(Exception ex) {
    
    log.info("Depositos no encontradas, retornando fallback");
  
    List<Deposit> list = new ArrayList<>();
    
    list.add(Deposit
        .builder()
        .id(ex.getMessage())
        .build());
    
    return Mono.just(list);
    
  }
  
  /** Mensaje si falla el create. */
  public Mono<Deposit> createFallback(Deposit deposit, Exception ex) {
  
    log.info("Deposito con numero de cuenta {} no se pudo crear, "
        + "retornando fallback", deposit.getAccount().getAccountNumber());
  
    return Mono.just(Deposit
        .builder()
        .id(ex.getMessage())
        .amount(Double.parseDouble(deposit.getPurchase().getCardNumber()))
        .description(deposit.getAccount().getAccountNumber())
        .build());
    
  }
  
}
