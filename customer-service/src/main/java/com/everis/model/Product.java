package com.everis.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Omitr campos no enviados.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Product {
  
  private String id;
  
  private Double amount;
  
}
