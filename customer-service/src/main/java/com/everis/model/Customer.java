package com.everis.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Omitir campos que son nulos.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "customer")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Customer {
  
  @Id
  private String id;

  @Size(min = 3)
  @Field(name = "name")
  private String name;

  @Field(name = "identityType")
  private String identityType;

  @Field(name = "identityNumber")
  private String identityNumber;

  @Field(name = "customerType")
  private String customerType;

  @Field(name = "address")
  private String address;

  @Field(name = "phoneNumber")
  private String phoneNumber;
  
}
