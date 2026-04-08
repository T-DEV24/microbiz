package com.microbiz.service;

import com.microbiz.model.Client;
import com.microbiz.repository.ClientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    public List<Client> findAll() {
        return clientRepository.findAll().stream()
                .filter(c -> c.getDeletedAt() == null)
                .toList();
    }

    public Page<Client> findAll(Pageable pageable) {
        return clientRepository.findByDeletedAtIsNull(pageable);
    }

    public Page<Client> findTrash(Pageable pageable) {
        return clientRepository.findByDeletedAtIsNotNull(pageable);
    }

    public Page<Client> rechercherActifs(String q, Pageable pageable) {
        if (q == null || q.isBlank()) {
            return clientRepository.findByDeletedAtIsNull(pageable);
        }
        return clientRepository.findByDeletedAtIsNullAndNomContainingIgnoreCase(q.trim(), pageable);
    }

    public Page<Client> findAll(Pageable pageable) {
        return clientRepository.findAll(pageable);
    }

    public Optional<Client> findById(Long id) {
        return clientRepository.findById(id);
    }

    public Client save(Client client) {
        return clientRepository.save(client);
    }

    public void deleteById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client introuvable"));
        client.setDeletedAt(LocalDateTime.now());
        clientRepository.save(client);
    }

    public void restoreById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client introuvable"));
        client.setDeletedAt(null);
        clientRepository.save(client);
    }

    public List<Client> rechercher(String nom) {
        return clientRepository.findByNomContainingIgnoreCase(nom);
    }

    public long countAll() {
        return clientRepository.count();
    }

    public boolean emailExiste(String email) {
        return clientRepository.findByEmail(email).isPresent();
    }
}
