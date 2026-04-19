package com.microbiz.repository;

import com.microbiz.model.SaasSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SaasSubscriptionRepository extends JpaRepository<SaasSubscription, Long> {
    Optional<SaasSubscription> findByTenantKey(String tenantKey);
    List<SaasSubscription> findAllByOrderByTenantKeyAsc();
}
