package com.everis.service;

import com.everis.model.Product;
import reactor.core.publisher.Mono;

/**
 * 
 */
public interface InterfaceProductService extends InterfaceCrudService<Product, String> {
  
  Mono<Product> findByProductName(String productName);
  
  Mono<Product> updateProduct(Product product, String productName);
  
  Mono<Product> findByIdProduct(String id);
  
}
