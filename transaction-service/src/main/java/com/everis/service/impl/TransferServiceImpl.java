package com.everis.service.impl;

import com.everis.model.Account;
import com.everis.model.Deposit;
import com.everis.model.Transfer;
import com.everis.model.Withdrawal;
import com.everis.repository.InterfaceRepository;
import com.everis.repository.InterfaceTransferRepository;
import com.everis.service.InterfaceAccountService;
import com.everis.service.InterfaceTransferService;
import com.everis.topic.producer.TransactionProducer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Implementacion de Metodos del Service Transfer.
 */
@Slf4j
@Service
public class TransferServiceImpl extends CrudServiceImpl<Transfer, String> 
    implements InterfaceTransferService {

  private final String circuitBreaker = "transferServiceCircuitBreaker";

  @Value("${msg.error.registro.accountini.exists}")
  public String msgAccountIniNotExists;

  @Value("${msg.error.registro.accountfin.exists}")
  public String msgAccountFinNotExists;
  
  @Value("${msg.error.registro.positive}")
  public String msgPositive;
  
  @Value("${msg.error.registro.exceed}")
  public String msgExceed;
  
  @Autowired
  private InterfaceTransferRepository repository;

  @Autowired
  private InterfaceAccountService accountService;

  @Autowired
  private TransactionProducer producer;
  
  @Override
  protected InterfaceRepository<Transfer, String> getRepository() {
  
    return repository;
  
  }

  @Override
  @CircuitBreaker(name = circuitBreaker, fallbackMethod = "createFallback")
  public Mono<Withdrawal> createTransfer(Transfer transfer) {
    
    Mono<Account> sendAccount = accountService
        .findByAccountNumber(transfer.getSendAccount().getAccountNumber())
        .switchIfEmpty(Mono.error(new RuntimeException(msgAccountIniNotExists)));

    Mono<Account> receiveAccount = accountService
        .findByAccountNumber(transfer.getReceiveAccount().getAccountNumber())
        .switchIfEmpty(Mono.error(new RuntimeException(msgAccountFinNotExists)));
    
    Withdrawal withdrawal = Withdrawal.builder().build();
    
    Deposit deposit = Deposit.builder().build();
    
    return sendAccount
        .flatMap(send -> {
          
          return receiveAccount
              .flatMap(receive -> {
                
                if (transfer.getAmount() < 0) {
                  
                  return Mono.error(new RuntimeException(msgPositive));
                  
                }
                        
                withdrawal.setAccount(receive);
                withdrawal.getAccount().setCurrentBalance(receive.getCurrentBalance() 
                    - transfer.getAmount());
                withdrawal.setPurchase(receive.getPurchase());
                withdrawal.setAmount(transfer.getAmount());
                
                deposit.setAccount(send);
                deposit.getAccount().setCurrentBalance(send.getCurrentBalance() 
                    + transfer.getAmount());
                deposit.setPurchase(send.getPurchase());
                deposit.setAmount(transfer.getAmount());
                
                if (withdrawal.getAccount().getCurrentBalance() < 0) {
                  
                  return Mono.error(new RuntimeException(msgExceed));
                  
                }
                
                producer.sendCreatedTransferWithdrawalTopic(withdrawal);
                producer.sendCreatedTransferDepositTopic(deposit);
                
                return Mono.just(withdrawal);
                                  
              });
          
        });
    
  }
  
  /** Mensaje si falla el transfer. */
  public Mono<Withdrawal> createFallback(Transfer transfer, Exception ex) {
  
    log.info("Transferencia de la cuenta {} hacia la cuenta {} no se pudo realizar, "
        + "retornando fallback", transfer.getSendAccount(), transfer.getReceiveAccount());
  
    return Mono.just(Withdrawal
        .builder()
        .id(ex.getMessage())
        .description(transfer.getSendAccount().getAccountNumber())
        .description2(transfer.getReceiveAccount().getAccountNumber())
        .build());
    
  }

}
