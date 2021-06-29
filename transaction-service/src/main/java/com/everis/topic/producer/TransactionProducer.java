package com.everis.topic.producer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.everis.model.Deposit;
import com.everis.model.Transaction;
import com.everis.model.Withdrawal;

@Component
public class TransactionProducer {
  
  @Autowired
  private KafkaTemplate<String, Object> kafkaTemplate;

  private String transactionTopic = "created-transaction-topic";

  private String transferWithdrawalTopic = "created-transfer-withdrawal-topic";

  private String transferDepositTopic = "created-transfer-deposit-topic";

  public void sendCreatedTransactionTopic(Transaction transaction) {
  
    kafkaTemplate.send(transactionTopic, transaction);
  
  }

  public void sendCreatedTransferWithdrawalTopic(Withdrawal withdrawal) {
  
    kafkaTemplate.send(transferWithdrawalTopic, withdrawal);
  
  }

  public void sendCreatedTransferDepositTopic(Deposit deposit) {
  
    kafkaTemplate.send(transferDepositTopic, deposit);
  
  }  
  
}
