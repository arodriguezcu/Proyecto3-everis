package com.everis.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Clase Product.
 */
@Document(collection = "product")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Product {
  
  @Id
  private String id;
  
  @Field(name = "productName")
  private String productName;
  
  @Field(name = "productType")
  private String productType;
  
  @Field(name = "condition")
  private Condition condition;

}
