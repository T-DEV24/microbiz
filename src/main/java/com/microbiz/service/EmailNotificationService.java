package com.microbiz.service;

import com.microbiz.model.Facture;
import com.microbiz.model.Produit;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

@Service
public class EmailNotificationService {

    @Autowired(required = false)
    private JavaMailSender mailSender;
    @Autowired
    private TemplateEngine templateEngine;

    @Value("${microbiz.notifications.to:}")
    private String notificationTo;

    public void sendFactureEnvoyee(Facture facture) {
        send("Facture envoyée : " + facture.getNumero(),
                "La facture <strong>" + facture.getNumero() + "</strong> a été marquée ENVOYEE.",
                List.of("Client: " + facture.getClientNom(), "Montant: " + facture.getMontantTtc()));
    }

    public void sendFactureImpayee(Facture facture) {
        send("Facture impayée (J+30) : " + facture.getNumero(),
                "La facture <strong>" + facture.getNumero() + "</strong> est impayée depuis plus de 30 jours.",
                List.of("Client: " + facture.getClientNom(), "Échéance: " + facture.getDateEcheance()));
    }

    public void sendStockBas(List<Produit> produits) {
        send("Alerte stock bas", "Des produits sont passés sous le seuil de stock.",
                produits.stream().map(p -> p.getNom() + " : " + p.getStockActuel()).toList());
    }

    private void send(String subject, String message, List<String> items) {
        if (mailSender == null || notificationTo == null || notificationTo.isBlank()) {
            return;
        }
        try {
            Context ctx = new Context();
            ctx.setVariable("title", subject);
            ctx.setVariable("message", message);
            ctx.setVariable("items", items);
            String html = templateEngine.process("email/base-notification", ctx);

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");
            helper.setTo(notificationTo);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mime);
        } catch (MessagingException ignored) {
        }
    }
}
