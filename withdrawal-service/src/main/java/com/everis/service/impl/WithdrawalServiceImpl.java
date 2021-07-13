package com.everis.service.impl;

import com.everis.model.Account;
import com.everis.model.Purchase;
import com.everis.model.Withdrawal;
import com.everis.repository.InterfaceRepository;
import com.everis.repository.InterfaceWithdrawalRepository;
import com.everis.service.InterfaceAccountService;
import com.everis.service.InterfacePurchaseService;
import com.everis.service.InterfaceWithdrawalService;
import com.everis.topic.producer.WithdrawalProducer;
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
 * Implementacion de Metodos del Service Withdrawal.
 */
@Slf4j
@Service
public class WithdrawalServiceImpl extends CrudServiceImpl<Withdrawal, String> 
    implements InterfaceWithdrawalService {

  static final String CIRCUIT = "withdrawalServiceCircuitBreaker";
  
  @Value("${msg.error.registro.notfound.all}")
  private String msgNotFoundAll;
  
  @Value("${msg.error.registro.positive}")
  private String msgPositive;
  
  @Value("${msg.error.registro.exceed}")
  private String msgExceed;
  
  @Value("${msg.error.registro.account.exists}")
  private String msgAccountNotExists;
  
  @Value("${msg.error.registro.card.exists}")
  private String msgCardNotExists;
  
  @Value("${msg.error.registro.notfound.create}")
  private String msgNotFoundCreate;
  
  @Autowired
  private InterfaceWithdrawalRepository repository;
  
  @Autowired
  private InterfaceWithdrawalService service;
  
  @Autowired
  private InterfacePurchaseService purchaseService;
  
  @Autowired
  private InterfaceAccountService accountService;
  
  @Autowired
  private WithdrawalProducer producer;

  @Override
  protected InterfaceRepository<Withdrawal, String> getRepository() {
    
    return repository;
  
  }
  
  @Override
  @CircuitBreaker(name = CIRCUIT, fallbackMethod = "findAllFallback")
  public Mono<List<Withdrawal>> findAllWithdrawal() {
    
    Flux<Withdrawal> withdrawalDatabase = service.findAll()
        .switchIfEmpty(Mono.error(new RuntimeException(msgNotFoundAll)));
    
    return withdrawalDatabase.collectList().flatMap(Mono::just);
  
  }
  
  @Override
  @CircuitBreaker(name = CIRCUIT, fallbackMethod = "createFallback")
  public Mono<Withdrawal> createWithdrawal(Withdrawal withdrawal) {
    
    Mono<Purchase> purchaseDatabase = purchaseService
        .findByCardNumber(withdrawal.getPurchase().getCardNumber())
        .switchIfEmpty(Mono.error(new RuntimeException(msgCardNotExists)));
    
    Mono<Account> accountDatabase = accountService
        .findByAccountNumber(withdrawal.getAccount().getAccountNumber())
        .switchIfEmpty(Mono.error(new RuntimeException(msgAccountNotExists)));
    
    return purchaseDatabase
        .flatMap(purchase -> {
          
          if (withdrawal.getAmount() < 0) {
            
            return Mono.error(new RuntimeException(msgPositive));
            
          }
        
          return accountDatabase
              .flatMap(account -> {
                
                if (withdrawal.getAmount() > account.getCurrentBalance()) {
                  
                  return Mono.error(new RuntimeException(msgExceed));
                  
                }
                
                account.setCurrentBalance(account.getCurrentBalance() - withdrawal.getAmount());
                withdrawal.setAccount(account);
                withdrawal.setPurchase(purchase);
                withdrawal.setWithdrawalDate(LocalDateTime.now());
                
                producer.sendWithdrawalAccountTopic(withdrawal); 
                
                if (purchase.getProduct().getCondition().getMonthlyTransactionLimit() > 0) {
                  
                  withdrawal.getPurchase().getProduct().getCondition().setMonthlyTransactionLimit(
                      purchase.getProduct().getCondition().getMonthlyTransactionLimit() - 1
                  );
                  
                } 
              
                return service.create(withdrawal)
                    .map(createdObject -> createdObject)
                    .switchIfEmpty(Mono.error(new RuntimeException(msgNotFoundCreate)));
                          
              });
                  
        });
      
  }
  
  /** Mensaje si no existen retiros. */
  public Mono<List<Withdrawal>> findAllFallback(Exception ex) {
    
    log.info("Retiros no encontradas, retornando fallback");
  
    List<Withdrawal> list = new ArrayList<>();
    
    list.add(Withdrawal
        .builder()
        .id(ex.getMessage())
        .build());
    
    return Mono.just(list);
    
  }
  
  /** Mensaje si falla el create. */
  public Mono<Withdrawal> createFallback(Withdrawal withdrawal, Exception ex) {
  
    log.info("Retiro con numero de cuenta {} no se pudo crear, "
        + "retornando fallback", withdrawal.getAccount().getAccountNumber());
  
    return Mono.just(Withdrawal
        .builder()
        .id(ex.getMessage())
        .amount(Double.parseDouble(withdrawal.getPurchase().getCardNumber()))
        .description(withdrawal.getAccount().getAccountNumber())
        .build());
    
  }

}
