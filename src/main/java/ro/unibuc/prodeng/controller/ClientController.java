package ro.unibuc.prodeng.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import ro.unibuc.prodeng.request.CreateClientRequest;
import ro.unibuc.prodeng.request.UpdateClientRequest;
import ro.unibuc.prodeng.response.ClientResponse;
import ro.unibuc.prodeng.service.ClientService;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    @Autowired
    private ClientService clientService;

    @GetMapping
    public ResponseEntity<List<ClientResponse>> getAllClients() {
        return ResponseEntity.ok(clientService.getAllClients());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getClientById(@PathVariable String id) {
        return ResponseEntity.ok(clientService.getClientById(id));
    }

    @PostMapping
    public ResponseEntity<ClientResponse> createClient(@Valid @RequestBody CreateClientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.createClient(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientResponse> updateClient(
            @PathVariable String id,
            @Valid @RequestBody UpdateClientRequest request) {
        return ResponseEntity.ok(clientService.updateClient(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable String id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }
}
