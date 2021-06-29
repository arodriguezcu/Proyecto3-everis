package com.everis.repository;

import com.everis.model.Product;

import reactor.core.publisher.Mono;

public interface IProductRepository extends IRepository<Product, String> {
  
  Mono<Product> findByProductName(String productName);
  
}
