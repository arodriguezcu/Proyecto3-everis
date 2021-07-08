package com.everis.topic;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * Configuracion del Topico.
 */
@Configuration
public class CustomerTopic {
  
//  @Value("${kafka.server.hostname}")
//  private String hostName;
//  
//  @Value("${kafka.server.port}")
//  private String port;
  
  @Value("${spring.kafka.bootstrap-servers}")
  private String host;
    
  /** Crea una instancia de esta clase. */
  @Bean
  public NewTopic topicCustomer() {
  
    return TopicBuilder
        .name("saved-customer-topic")
        .partitions(1)
        .replicas(1)
        .build();
    
  }

  /** Crea una instancia de esta clase. */
  @Bean
  public ProducerFactory<String, Object> producerFactory() {
  
    Map<String, Object> config = new HashMap<>();
  
//    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, hostName + ":" + port);
  
//    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, host);
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "52.255.82.239:29092");
    
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
  
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
  
    return new DefaultKafkaProducerFactory<>(config);
  
  }
  
  /** Crea una instancia de esta clase. */
  @Bean
  public KafkaTemplate<String, Object> kafkaTemplate() {
  
    return new KafkaTemplate<>(producerFactory());
    
  }
  
}
