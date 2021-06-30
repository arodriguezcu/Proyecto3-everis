package com.everis.service.impl;

import com.everis.model.Product;
import com.everis.repository.InterfaceProductRepository;
import com.everis.repository.InterfaceRepository;
import com.everis.service.InterfaceProductService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 
 */
@Slf4j
@Service
public class ProductService extends CrudServiceImpl<Product, String> 
    implements InterfaceProductService {

  private final String circuitBreaker = "productServiceCircuitBreaker";
  
  @Value("${msg.error.registro.notfound}")
  private String msgNotFound;
  
  @Value("${msg.error.registro.notfound.update}")
  private String msgNotFoundUpdate;
  
  @Autowired
  private InterfaceProductRepository repository;

  @Override
  protected InterfaceRepository<Product, String> getRepository() {
  
    return repository;
  
  }

  @Override
  @CircuitBreaker(name = circuitBreaker, fallbackMethod = "findByProductNameFallback")
  public Mono<Product> findByProductName(String productName) {
      
    return repository.findByProductName(productName)
        .switchIfEmpty(Mono.error(new RuntimeException(msgNotFound)));
      
  }
  
  /** */
  public Mono<Product> findByProductNameFallback(String productName, Exception ex) {
      
    log.info("ups producto {} no encontrado para actualizar, retornando fallback", productName);
      
    return Mono.just(Product
        .builder()
        .id("0")
        .productName(msgNotFound)
        .build());
      
  }

  @Override
  @CircuitBreaker(name = circuitBreaker, fallbackMethod = "updateProductFallback")
  public Mono<Product> updateProduct(Product product, String productName) {
      
    Mono<Product> productModification = Mono.just(product);
      
    Mono<Product> productDatabase = repository.findByProductName(productName);
            
    return productDatabase
        .zipWith(productModification, (a, b) -> {
          
          a.getCondition().setCustomerTypeTarget(product.getCondition()
              .getCustomerTypeTarget());
          a.getCondition().setHasMaintenanceFee(product.getCondition()
              .isHasMaintenanceFee());
          a.getCondition().setHasMonthlyTransactionLimit(product.getCondition()
              .isHasMonthlyTransactionLimit());
          a.getCondition().setHasDailyMonthlyTransactionLimit(product.getCondition()
              .isHasDailyMonthlyTransactionLimit());
          a.getCondition().setProductPerPersonLimit(product.getCondition()
              .getProductPerPersonLimit());
          a.getCondition().setProductPerBusinessLimit(product.getCondition()
              .getProductPerBusinessLimit());
                  
          return a;
              
        })
        .flatMap(repository::save)
        .switchIfEmpty(Mono.error(new RuntimeException(msgNotFoundUpdate)));
    
  }
  
  /** */
  public Mono<Product> updateProductFallback(Product product, String productName, Exception ex) {
    
    log.info("ups producto {} no encontrado para actualizar, retornando fallback", productName);
      
    return Mono.just(Product
        .builder()
        .id("0")
        .productName(msgNotFoundUpdate)
        .build());
    
  }

  @Override
  @CircuitBreaker(name = circuitBreaker, fallbackMethod = "findByIdProductFallback")
  public Mono<Product> findByIdProduct(String id) {
       
    return repository.findById(id)
        .switchIfEmpty(Mono.error(new RuntimeException(msgNotFound)));
    
  }
  
  /** */
  public Mono<Product> findByIdProductFallback(String id, Exception ex) {
      
    log.info("ups producto {} no encontrado , retornando fallback", id);
      
    return Mono.just(Product
        .builder()
        .id("0")
        .productName(msgNotFound)
        .build());
    
  }

}
