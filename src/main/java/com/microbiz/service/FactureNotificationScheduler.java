package com.microbiz.service;

import com.microbiz.model.Facture;
import com.microbiz.repository.FactureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class FactureNotificationScheduler {

    @Autowired private FactureRepository factureRepository;
    @Autowired private EmailNotificationService emailNotificationService;

    @Scheduled(cron = "${microbiz.notifications.impayees-cron:0 30 8 * * *}")
    public void notifierFacturesImpayeesJ30() {
        LocalDate limit = LocalDate.now().minusDays(30);
        factureRepository.findByStatutAndDateEcheanceBefore(Facture.StatutFacture.IMPAYEE, limit)
                .forEach(emailNotificationService::sendFactureImpayee);
    }
}
