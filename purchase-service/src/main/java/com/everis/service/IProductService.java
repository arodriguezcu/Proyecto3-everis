package com.everis.service;

import com.everis.model.Product;

import reactor.core.publisher.Mono;

public interface IProductService extends ICRUDService<Product, String> {
  
  Mono<Product> findByProductName(String productName); 

}
