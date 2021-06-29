package com.everis.topic.producer;

import com.everis.model.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Anotacion generica.
 */
@Component
public class CustomerProducer {
  
  @Autowired
  private KafkaTemplate<String, Object> kafkaTemplate;
  
  private String createdCustomerTopic = "saved-customer-topic";

  /** Envia datos del customer al topico. */
  public void sendSavedCustomerTopic(Customer data) {
  
    kafkaTemplate.send(createdCustomerTopic, data);
  
  }
  
}
