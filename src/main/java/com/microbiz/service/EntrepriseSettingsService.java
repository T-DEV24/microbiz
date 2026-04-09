package com.microbiz.service;

import com.microbiz.model.EntrepriseSettings;
import com.microbiz.repository.EntrepriseSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class EntrepriseSettingsService {

    @Autowired private EntrepriseSettingsRepository repository;

    public EntrepriseSettings getSettings() {
        return repository.findAll().stream().findFirst()
                .orElseGet(() -> repository.save(EntrepriseSettings.builder()
                        .nomEntreprise("MicroBiz Pro")
                        .build()));
    }

    public EntrepriseSettings save(EntrepriseSettings payload, MultipartFile logoFile) {
        EntrepriseSettings current = getSettings();
        current.setNomEntreprise(payload.getNomEntreprise());
        current.setSiret(payload.getSiret());
        current.setRccm(payload.getRccm());
        current.setAdresse(payload.getAdresse());
        current.setMentionsLegales(payload.getMentionsLegales());

        if (logoFile != null && !logoFile.isEmpty()) {
            try {
                current.setLogo(logoFile.getBytes());
                current.setLogoContentType(logoFile.getContentType());
            } catch (Exception e) {
                throw new RuntimeException("Impossible de charger le logo", e);
            }
        }
        return repository.save(current);
    }
}
