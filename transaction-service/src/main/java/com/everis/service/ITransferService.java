package com.everis.service;

import org.springframework.http.ResponseEntity;

import com.everis.dto.Response;
import com.everis.model.Transfer;

import reactor.core.publisher.Mono;

public interface ITransferService extends ICRUDService<Transfer, String> {
  
  Mono<ResponseEntity<Response>> createTransfer(Transfer transfer);
    
}
