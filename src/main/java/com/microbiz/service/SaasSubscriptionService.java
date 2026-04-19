package com.microbiz.service;

import com.microbiz.model.SaasSubscription;
import com.microbiz.repository.SaasSubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class SaasSubscriptionService {

    @Autowired
    private SaasSubscriptionRepository repository;

    public List<SaasSubscription> findAll() {
        return repository.findAllByOrderByTenantKeyAsc();
    }

    public Map<SaasSubscription.PlanCode, Double> getPlanPrices(String currency) {
        Map<SaasSubscription.PlanCode, Double> prices = new EnumMap<>(SaasSubscription.PlanCode.class);
        prices.put(SaasSubscription.PlanCode.FREE, 0.0);
        prices.put(SaasSubscription.PlanCode.PRO, 15000.0);
        prices.put(SaasSubscription.PlanCode.BUSINESS, 45000.0);
        return prices;
    }

    public SaasSubscription upsert(String tenantKey,
                                   SaasSubscription.PlanCode planCode,
                                   SaasSubscription.SubscriptionStatus status,
                                   LocalDate startsAt,
                                   LocalDate trialEndsAt,
                                   Boolean autoRenew,
                                   String currency) {
        String normalizedTenant = (tenantKey == null || tenantKey.isBlank()) ? "default" : tenantKey.trim();
        String normalizedCurrency = (currency == null || currency.isBlank()) ? "XAF" : currency.trim().toUpperCase();
        Map<SaasSubscription.PlanCode, Double> prices = getPlanPrices(normalizedCurrency);

        SaasSubscription subscription = repository.findByTenantKey(normalizedTenant)
                .orElseGet(() -> SaasSubscription.builder().tenantKey(normalizedTenant).build());

        subscription.setPlanCode(planCode == null ? SaasSubscription.PlanCode.FREE : planCode);
        subscription.setStatus(status == null ? SaasSubscription.SubscriptionStatus.ACTIVE : status);
        subscription.setCurrency(normalizedCurrency);
        subscription.setMonthlyPrice(prices.getOrDefault(subscription.getPlanCode(), 0.0));
        subscription.setStartsAt(startsAt == null ? LocalDate.now() : startsAt);
        subscription.setTrialEndsAt(trialEndsAt);
        subscription.setAutoRenew(autoRenew == null || autoRenew);
        if (subscription.getStatus() == SaasSubscription.SubscriptionStatus.CANCELED) {
            subscription.setEndsAt(LocalDate.now());
            subscription.setAutoRenew(false);
        } else {
            subscription.setEndsAt(null);
        }
        return repository.save(subscription);
    }

    public void cancel(String tenantKey) {
        repository.findByTenantKey(tenantKey).ifPresent(subscription -> {
            subscription.setStatus(SaasSubscription.SubscriptionStatus.CANCELED);
            subscription.setEndsAt(LocalDate.now());
            subscription.setAutoRenew(false);
            repository.save(subscription);
        });
    }
}
