package com.everis.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.everis.dto.Response;
import com.everis.model.Account;
import com.everis.model.Deposit;
import com.everis.model.Transfer;
import com.everis.model.Withdrawal;
import com.everis.repository.IRepository;
import com.everis.repository.ITransferRepository;
import com.everis.service.IAccountService;
import com.everis.service.ITransferService;
import com.everis.topic.producer.TransactionProducer;

import reactor.core.publisher.Mono;

@Service
public class TransferService extends CRUDServiceImpl<Transfer, String> implements ITransferService {

  @Autowired
  private ITransferRepository repository;

  @Autowired
  private IAccountService accountService;

  @Autowired
  private TransactionProducer producer;
  
  @Override
  protected IRepository<Transfer, String> getRepository() {
  
    return repository;
  
  }

  @Override
  public Mono<ResponseEntity<Response>> createTransfer(Transfer transfer) {
    
    Mono<Account> sendAccount = accountService.findByAccountNumber(transfer.getSendAccount().getAccountNumber());
    
    Mono<Account> receiveAccount = accountService.findByAccountNumber(transfer.getReceiveAccount().getAccountNumber());
                   
    Withdrawal withdrawal = Withdrawal.builder().build();
    
    Deposit deposit = Deposit.builder().build();
        
    return sendAccount
        .flatMap(send -> {
          
          return receiveAccount
          .flatMap(receive -> {
                        
            withdrawal.setAccount(receive);
            withdrawal.getAccount().setCurrentBalance(receive.getCurrentBalance() - transfer.getAmount());
            withdrawal.setPurchase(receive.getPurchase());
            withdrawal.setAmount(transfer.getAmount());
            
            deposit.setAccount(send);
            deposit.getAccount().setCurrentBalance(send.getCurrentBalance() + transfer.getAmount());
            deposit.setPurchase(send.getPurchase());
            deposit.setAmount(transfer.getAmount());
            
            if (withdrawal.getAccount().getCurrentBalance() < 0) {
              
              return Mono.just(ResponseEntity
                  .ok()
                  .contentType(MediaType.APPLICATION_JSON)
                  .body(Response
                      .builder()
                      .error("El monto excede al saldo disponible.")
                      .build()));
              
            }
            
            producer.sendCreatedTransferWithdrawalTopic(withdrawal);
            producer.sendCreatedTransferDepositTopic(deposit);
            
            return Mono.just(ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Response
                    .builder()
                    .data(withdrawal)
                    .build()));
                
              })
              .defaultIfEmpty(ResponseEntity
                  .badRequest()
                  .body(Response
                      .builder()
                      .error("La cuenta final no existe.")
                      .build()));
          
        })
        .defaultIfEmpty(ResponseEntity
            .badRequest()
            .body(Response
                .builder()
                .error("La cuenta inicial no existe.")
                .build()));
    
  }

}
