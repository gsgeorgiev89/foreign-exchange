package com.example.fx.controller;

import com.example.fx.dto.BalanceDto;
import com.example.fx.exception.FxException;
import com.example.fx.repository.BalanceRepository;
import com.example.fx.repository.ClientRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class BalanceController {

    private final ClientRepository clientRepository;
    private final BalanceRepository balanceRepository;

    @GetMapping("/{clientId}/balances")
    public ResponseEntity<List<BalanceDto>> getBalances(@PathVariable String clientId) {

        if (!clientRepository.existsById(clientId)) {
            throw new FxException(
                FxException.ErrorCode.CLIENT_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Client not found: " + clientId
            );
        }

        List<BalanceDto> balances = balanceRepository.findAllByClientId(clientId)
                .stream()
                .map(b -> new BalanceDto(b.getCurrency(), b.getAmount()))
                .toList();

        return ResponseEntity.ok(balances);
    }
}