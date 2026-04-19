package com.microbiz.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "saas_subscription", uniqueConstraints = {
        @UniqueConstraint(name = "uk_saas_subscription_tenant", columnNames = "tenant_key")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaasSubscription {

    public enum PlanCode {
        FREE, PRO, BUSINESS
    }

    public enum SubscriptionStatus {
        TRIAL, ACTIVE, PAST_DUE, CANCELED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_key", nullable = false, length = 120)
    private String tenantKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PlanCode planCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubscriptionStatus status;

    @Column(nullable = false)
    private Double monthlyPrice;

    @Column(nullable = false, length = 8)
    private String currency;

    @Column(nullable = false)
    private LocalDate startsAt;

    private LocalDate trialEndsAt;
    private LocalDate endsAt;

    @Column(nullable = false)
    private Boolean autoRenew;

    @PrePersist
    void prePersist() {
        if (tenantKey == null || tenantKey.isBlank()) tenantKey = "default";
        if (planCode == null) planCode = PlanCode.FREE;
        if (status == null) status = SubscriptionStatus.ACTIVE;
        if (monthlyPrice == null) monthlyPrice = 0.0;
        if (currency == null || currency.isBlank()) currency = "XAF";
        if (startsAt == null) startsAt = LocalDate.now();
        if (autoRenew == null) autoRenew = Boolean.TRUE;
    }
}
