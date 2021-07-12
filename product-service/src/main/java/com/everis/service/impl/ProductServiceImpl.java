package com.everis.service.impl;

import com.everis.dto.Response;
import com.everis.model.Product;
import com.everis.repository.InterfaceProductRepository;
import com.everis.repository.InterfaceRepository;
import com.everis.service.InterfaceProductService;
import com.everis.topic.producer.ProductProducer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Implementacion de Metodos del Service Product.
 */
@Slf4j
@Service
public class ProductServiceImpl extends CrudServiceImpl<Product, String>
    implements InterfaceProductService {

  static final String CIRCUIT = "productServiceCircuitBreaker";

  @Value("${msg.error.registro.notfound.all}")
  private String msgNotFoundAll;

  @Value("${msg.error.registro.notfound}")
  private String msgNotFound;

  @Value("${msg.error.registro.if.exists}")
  private String msgIfExists;

  @Value("${msg.error.registro.notfound.create}")
  private String msgNotFoundCreate;

  @Value("${msg.error.registro.notfound.update}")
  private String msgNotFoundUpdate;

  @Value("${msg.error.registro.notfound.delete}")
  private String msgNotFoundDelete;

  @Value("${msg.error.registro.product.delete}")
  private String msgProductDelete;

  @Autowired
  private InterfaceProductRepository repository;

  @Autowired
  private InterfaceProductService service;

  @Autowired
  private ProductProducer producer;

  @Override
  protected InterfaceRepository<Product, String> getRepository() {

    return repository;

  }

  @Override
  @CircuitBreaker(name = CIRCUIT, fallbackMethod = "findAllFallback")
  public Mono<List<Product>> findAllProduct() {

    Flux<Product> productDatabase = service.findAll()
        .switchIfEmpty(Mono.error(new RuntimeException(msgNotFoundAll)));

    return productDatabase.collectList().flatMap(Mono::just);

  }

  @Override
  @CircuitBreaker(name = CIRCUIT, fallbackMethod = "productFallback")
  public Mono<Product> findByProductName(String productName) {

    return repository.findByProductName(productName)
        .switchIfEmpty(Mono.error(new RuntimeException(msgNotFound)));

  }

  @Override
  @CircuitBreaker(name = CIRCUIT, fallbackMethod = "createFallback")
  public Mono<Product> createProduct(Product product) {

    Flux<Product> productDatabase = service.findAll()
        .filter(list -> list.getProductName().equals(product.getProductName()));

    return productDatabase
        .collectList()
        .flatMap(list -> {

          if (list.size() > 0) {

            return Mono.error(new RuntimeException(msgIfExists));

          }

          return service.create(product)
              .map(createdObject -> {

                producer.sendSavedProductTopic(createdObject);
                return createdObject;

              })
              .switchIfEmpty(Mono.error(new RuntimeException(msgNotFoundCreate)));

        });

  }

  @Override
  @CircuitBreaker(name = CIRCUIT, fallbackMethod = "updateFallback")
  public Mono<Product> updateProduct(Product product, String productName) {

    Mono<Product> productModification = Mono.just(product);

    Mono<Product> productDatabase = findByProductName(productName);

    return productDatabase
        .zipWith(productModification, (a, b) -> {

          if (b.getCondition().getCustomerTypeTarget() != null) {
            a.getCondition().setCustomerTypeTarget(b.getCondition()
                .getCustomerTypeTarget());
          }

          a.getCondition().setHasMaintenanceFee(b.getCondition()
              .isHasMaintenanceFee());
          a.getCondition().setHasMonthlyTransactionLimit(b.getCondition()
                .isHasMonthlyTransactionLimit());
          a.getCondition().setHasDailyMonthlyTransactionLimit(b.getCondition()
                .isHasDailyMonthlyTransactionLimit());

          if (b.getCondition().getProductPerPersonLimit() != null) {
            a.getCondition().setProductPerPersonLimit(b.getCondition()
                .getProductPerPersonLimit());
          }

          if (b.getCondition().getProductPerBusinessLimit() != null) {
            a.getCondition().setProductPerBusinessLimit(b.getCondition()
                .getProductPerBusinessLimit());
          }

          return a;

        })
        .flatMap(service::update)
        .map(objectUpdated -> {

          producer.sendSavedProductTopic(objectUpdated);
          return objectUpdated;

        })
        .switchIfEmpty(Mono.error(new RuntimeException(msgNotFoundUpdate)));

  }

  @Override
  @CircuitBreaker(name = CIRCUIT, fallbackMethod = "deleteFallback")
  public Mono<Response> deleteProduct(String productName) {

    Mono<Product> productDatabase = findByProductName(productName);

    return productDatabase
        .flatMap(objectDelete -> service.delete(objectDelete.getId())
            .then(Mono.just(Response.builder().data(msgProductDelete).build())))
        .switchIfEmpty(Mono.error(new RuntimeException(msgNotFoundDelete)));

  }

  /** Mensaje si no existen productos. */
  public Mono<List<Product>> findAllFallback(Exception ex) {

    log.info("Productos no encontrados.");

    List<Product> list = new ArrayList<>();

    list.add(Product
        .builder()
        .productName(ex.getMessage())
        .build());

    return Mono.just(list);

  }

  /** Mensaje si no encuentra el producto. */
  public Mono<Product> productFallback(String productName, Exception ex) {

    log.info("Producto {} no encontrado.", productName);

    return Mono.just(Product
        .builder()
        .productName(productName)
        .productType(ex.getMessage())
        .build());

  }

  /** Mensaje si falla el create. */
  public Mono<Product> createFallback(Product product, Exception ex) {

    log.info("Producto {} no se pudo crear.", product.getProductName());

    return Mono.just(Product
        .builder()
        .productName(product.getProductName())
        .productType(ex.getMessage())
        .build());

  }

  /** Mensaje si falla el update. */
  public Mono<Product> updateFallback(Product product, String productName, Exception ex) {

    log.info("Producto {} no encontrado para actualizar.", product.getProductName());

    return Mono.just(Product
        .builder()
        .productName(productName)
        .productType(ex.getMessage())
        .build());

  }

  /** Mensaje si falla el delete. */
  public Mono<Response> deleteFallback(String productName, Exception ex) {

    log.info("Product {} no encontrado para eliminar.", productName);

    return Mono.just(Response
        .builder()
        .data(productName)
        .error(ex.getMessage())
        .build());

  }

}
