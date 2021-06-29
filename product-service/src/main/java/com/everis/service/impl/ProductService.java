package com.everis.service.impl;

import com.everis.model.Product;
import com.everis.repository.IProductRepository;
import com.everis.repository.IRepository;
import com.everis.service.IProductService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class ProductService extends CRUDServiceImpl<Product, String> implements IProductService {

  private final String CIRCUIT_BREAKER = "productServiceCircuitBreaker";
  
  @Value("${msg.error.registro.notfound}")
  private String msgNotFound;
  
  @Value("${msg.error.registro.notfound.update}")
  private String msgNotFoundUpdate;
  
  @Autowired
  private IProductRepository repository;

  @Override
  protected IRepository<Product, String> getRepository() {
  
    return repository;
  
  }

  @Override
  @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "findByProductNameFallback")
  public Mono<Product> findByProductName(String productName) {
      return repository.findByProductName(productName)
              .switchIfEmpty(Mono.error( new RuntimeException(msgNotFound) ));
      
  }
  
  public Mono<Product> findByProductNameFallback(String productName, Exception ex) {
      log.info("ups producto {} no encontrado para actualizar, retornando fallback",productName);
      return Mono.just(Product.builder()
              .id("0")
              .productName(msgNotFound).build());
      
  }

  @Override
  @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "updateProductFallback")
  public Mono<Product> updateProduct(Product product, String productName) {
      Mono<Product> ProductModification = Mono.just(product);
      Mono<Product> ProductDatabase = repository.findByProductName(productName);
      
      return ProductDatabase
              .zipWith(ProductModification, (a,b) -> {
                  a.getCondition().setCustomerTypeTarget(product.getCondition().getCustomerTypeTarget());
                  a.getCondition().setHasMaintenanceFee(product.getCondition().isHasMaintenanceFee());
                  a.getCondition().setHasMonthlyTransactionLimit(product.getCondition().isHasMonthlyTransactionLimit());
                  a.getCondition().setHasDailyMonthlyTransactionLimit(product.getCondition().isHasDailyMonthlyTransactionLimit());
                  a.getCondition().setProductPerPersonLimit(product.getCondition().getProductPerPersonLimit());
                  a.getCondition().setProductPerBusinessLimit(product.getCondition().getProductPerBusinessLimit());
                  return a;
              })
              .flatMap(repository::save)
              .switchIfEmpty(Mono.error( new RuntimeException(msgNotFoundUpdate) ));
  }
  
  public Mono<Product> updateProductFallback(Product product, String productName, Exception ex) {
      log.info("ups producto {} no encontrado para actualizar, retornando fallback",productName);
      return Mono.just(Product.builder()
              .id("0")
              .productName(msgNotFoundUpdate).build());
  }

  @Override
  @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "findByIdProductFallback")
  public Mono<Product> findByIdProduct(String id) {
       return repository.findById(id)
               .switchIfEmpty(Mono.error( new RuntimeException(msgNotFound) ));
  }
  
  public Mono<Product> findByIdProductFallback(String id, Exception ex) {
      log.info("ups producto {} no encontrado , retornando fallback",id);
      return Mono.just(Product.builder()
              .id("0")
              .productName(msgNotFound).build());
  }

}
