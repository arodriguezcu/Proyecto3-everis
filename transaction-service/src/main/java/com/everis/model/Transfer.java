package com.everis.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "transfer")
@Data
public class Transfer {

  @Id
  private String id;

  @Field(name = "sendAccount")
  private Account sendAccount;

  @Field(name = "receiveAccount")
  private Account receiveAccount;

  @Field(name = "amount")
  private Double amount;
  
}
