package com.microbiz.service;

import com.microbiz.model.Client;
import com.microbiz.repository.ClientRepository;
import com.microbiz.security.TenantContext;
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
        return clientRepository.findByTenantKeyAndDeletedAtIsNull(TenantContext.getTenant());
    }

    public Page<Client> findAll(Pageable pageable) {
        return clientRepository.findByTenantKeyAndDeletedAtIsNull(TenantContext.getTenant(), pageable);
    }

    public Page<Client> findTrash(Pageable pageable) {
        return clientRepository.findByTenantKeyAndDeletedAtIsNotNull(TenantContext.getTenant(), pageable);
    }

    public Page<Client> rechercherActifs(String q, Pageable pageable) {
        String tenant = TenantContext.getTenant();
        if (q == null || q.isBlank()) {
            return clientRepository.findByTenantKeyAndDeletedAtIsNull(tenant, pageable);
        }
        return clientRepository.findByTenantKeyAndDeletedAtIsNullAndNomContainingIgnoreCase(tenant, q.trim(), pageable);
    }

    public Optional<Client> findById(Long id) {
        return clientRepository.findByIdAndTenantKey(id, TenantContext.getTenant());
    }

    public Client save(Client client) {
        client.setTenantKey(TenantContext.getTenant());
        return clientRepository.save(client);
    }

    public void deleteById(Long id) {
        Client client = clientRepository.findByIdAndTenantKey(id, TenantContext.getTenant())
                .orElseThrow(() -> new RuntimeException("Client introuvable"));
        client.setDeletedAt(LocalDateTime.now());
        clientRepository.save(client);
    }

    public void restoreById(Long id) {
        Client client = clientRepository.findByIdAndTenantKey(id, TenantContext.getTenant())
                .orElseThrow(() -> new RuntimeException("Client introuvable"));
        client.setDeletedAt(null);
        clientRepository.save(client);
    }

    public List<Client> rechercher(String nom) {
        return clientRepository.findByTenantKeyAndNomContainingIgnoreCaseAndDeletedAtIsNull(TenantContext.getTenant(), nom);
    }

    public long countAll() {
        return clientRepository.countByTenantKeyAndDeletedAtIsNull(TenantContext.getTenant());
    }

    public boolean emailExiste(String email) {
        return clientRepository.findByTenantKeyAndEmail(TenantContext.getTenant(), email).isPresent();
    }
}
