# MICROBIZ PRO — Rapport d'Amélioration & Roadmap Fonctionnelle

- **Version analysée** : `microbiz-pro2` (Spring Boot 3.3 / Java 17 / MySQL)
- **Date du rapport** : 18/04/2026
- **Portée** : Fonctionnalités à améliorer + roadmap de 5 mois complets

## Synthèse exécutive

L'analyse du code source de Microbiz Pro révèle une base solide (**multi-tenant**, **RBAC**, **PDF**, **multi-devises**, **OHADA**, **webhooks**). Cependant, plusieurs lacunes techniques et fonctionnelles freinent la valeur métier pour les PME africaines.

Ce rapport classe :

1. les améliorations urgentes ;
2. les améliorations non urgentes ;
3. une roadmap de 5 mois incluant 5 nouvelles fonctionnalités complètes.

---

## SECTION 1 — AMÉLIORATIONS URGENTES (Impact critique)

Ces points doivent être traités immédiatement car ils génèrent des bugs silencieux, des risques de sécurité, ou une mauvaise expérience utilisateur bloquante.

### U-01 — Filtrage tenant côté mémoire (`FactureService` / `DepenseService`)

- **Problème** : `findAll()` récupère toutes les lignes BDD puis filtre en mémoire par `tenantKey`.
- **Impact** : fuite de données multi-tenant + `OutOfMemoryError` en production.
- **Correction** : ajouter des prédicats SQL dans les requêtes JPA (`findAllByTenantKeyOrderByDateEmissionDesc(String tenantKey)`), idem pour `DepenseService.findAll()` et `search()`.

### U-02 — Numérotation de facture non atomique (`FactureService`)

- **Problème** : `nextNumero()` lit le dernier ID, calcule le suivant, et teste les doublons en boucle.
- **Impact** : doublon de numéro de facture sous charge concurrente.
- **Correction** : utiliser une séquence BDD dédiée ou un verrou pessimiste (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) sur le compteur.

### U-03 — Mot de passe admin en clair dans `application.properties`

- **Problème** : `MICROBIZ_ADMIN_PASSWORD` et `MICROBIZ_USER_PASSWORD` sont lus depuis les propriétés sans chiffrement.
- **Impact** : compromission potentielle totale du compte admin.
- **Correction** : utiliser Spring Cloud Config + Vault (ou secrets d'environnement chiffrés) et ne jamais versionner de vrais secrets.

### U-04 — Absence de gestion CSRF sur les API REST (`SecurityConfig`)

- **Problème** : les endpoints `/api/v1/**` acceptent des requêtes JSON sans stratégie CSRF explicitée.
- **Impact** : possibilité d'exécution de requêtes malveillantes depuis un site tiers (selon le mode d'authentification/session).
- **Correction** : configurer la politique CSRF explicitement (`ignoringRequestMatchers` pour API stateless) et documenter le modèle d'authentification.

### U-05 — `ClientService` non filtré par tenant

- **Problème** : `ClientService.findAll()` et `rechercherActifs()` ne filtrent pas par `tenantKey`.
- **Impact** : fuite de données clients entre entreprises (risque RGPD).
- **Correction** : ajouter `tenantKey` dans l'entité `Client` et adapter toutes les requêtes repository.

### U-06 — `FournisseurService` sans isolation tenant

- **Problème** : le modèle `Fournisseur` ne contient pas de `tenantKey`.
- **Impact** : données fournisseurs partagées entre tenants.
- **Correction** : ajouter `tenantKey` dans `Fournisseur`, créer `findAllByTenantKey()` dans le repository, adapter le service.

### U-07 — PDF facture sans TVA ni remise (`FactureService.genererPdf`)

- **Problème** : le PDF affiche uniquement le montant TTC global.
- **Impact** : non-conformité fiscale (mention TVA attendue).
- **Correction** : ajouter `tauxTva`, `montantHt`, `remisePourcent` et afficher HT / TVA / TTC dans le PDF.

### U-08 — Prévisions de ventes par moyenne simple (`PredictiveSalesService`)

- **Problème** : `previsionMensuelle()` repose sur une tendance trop basique, sans saisonnalité.
- **Impact** : prévisions potentiellement erronées sur le dashboard.
- **Correction** : implémenter un lissage exponentiel (Holt-Winters) ; court terme : limiter à 3 mois + afficher intervalle de confiance.

---

## SECTION 2 — AMÉLIORATIONS NON URGENTES (Qualité & Confort)

À traiter dans les 2 à 3 prochains sprints.

| ID | Amélioration | Module | Détail & Recommandation | Priorité |
|---|---|---|---|---|
| NU-01 | Pagination absente sur `VenteService.findAll()` | VenteService | Ajouter `Pageable` et généraliser `findByFiltres()` | Haute |
| NU-02 | Logs SQL activés en production | `application.properties` | Déplacer `spring.jpa.show-sql=true` vers profil dev | Moyenne |
| NU-03 | Taux de change codés en dur | `CurrencyRateService` | Remplacer fallback par source externe avec retry | Haute |
| NU-04 | OHADA — comptes limités | `OhadaAccountingService` | Étendre le mapping classes 1 à 9 + config par tenant | Moyenne |
| NU-05 | `SaasAdminController` vide | SaaS Admin | Implémenter la gestion réelle des abonnements | Moyenne |
| NU-06 | `UtilisateurController` sans service layer | Utilisateurs | Introduire `UtilisateurService` | Moyenne |
| NU-07 | Absence de cache sur statistiques | `StatistiqueService` | Ajouter `@Cacheable` (Caffeine) | Haute |
| NU-08 | Soft-delete client non isolé | `ClientService` | Filtrer la corbeille `findTrash()` par tenant | Haute |
| NU-09 | CSV rapport non structuré | `RapportService` | Passer à un export structuré (idéalement XLSX) | Basse |
| NU-10 | Tests insuffisants | `src/test/` | Ajouter charge + viser 70% de couverture (JaCoCo) | Moyenne |

---

## SECTION 3 — ROADMAP FONCTIONNELLE (5 mois — 5 modules complets)

Chaque mois livre un module complet : modèle de données, service, contrôleur, vue Thymeleaf, tests unitaires.

### Mois 1 — Gestion des Paiements & Encaissements

- Entité `Paiement` (montant, devise, mode, date, référence, tenant).
- Service de cumul d'encaissements par facture.
- Statut automatique : `PAYEE` / `PAIEMENT_PARTIEL`.
- API : `POST /api/v1/paiements`, `GET /api/v1/paiements/{factureId}`.
- Historique des encaissements dans le PDF facture.

### Mois 2 — RH : Employés, Contrats & Paie

- Entités `Employe`, `Conge`, `FicheDePaie`.
- Calcul paie (CNPS + IRPP simplifié).
- Génération de bulletin PDF.
- Vues RH (liste, fiche, congés, dashboard).

### Mois 3 — Point de Vente (POS) & Caisse physique

- Refactor `Vente` en en-tête + lignes (`VenteLigne`).
- Interface POS (Thymeleaf + Alpine.js).
- Ticket de caisse PDF (format thermique).
- `SessionCaisse` (ouverture/fermeture/écart).
- Promotions et remises.

### Mois 4 — CRM & Fidélisation client

- Enrichissement de la fiche client.
- Historique 360 (ventes, factures, paiements, relances).
- Segmentation RFM (Champions, Fidèles, À risque, Perdus).
- Programme de points + relances automatiques.

### Mois 5 — BI & Rapports avancés

- Dashboard interactif (Chart.js / ApexCharts).
- Comparaisons N vs N-1.
- Rapports planifiés par email.
- Exports XLSX + JSON.
- Prévisions Holt-Winters + intervalle de confiance.

---

## SECTION 4 — Récapitulatif roadmap & matrice de priorité

### Vue d'ensemble — planning sur 5 mois

| Mois | Module | Complexité | Valeur métier | Dépendances | Effort |
|---|---|---|---|---|---|
| M1 | Paiements & Encaissements | Moyenne | ★★★★★ | Aucune | ~15 j.dev |
| M2 | RH — Employés & Paie | Haute | ★★★★☆ | M1 (dépenses auto) | ~25 j.dev |
| M3 | POS & Caisse Physique | Haute | ★★★★★ | Refactoring Vente | ~20 j.dev |
| M4 | CRM & Fidélisation | Moyenne | ★★★★☆ | M3 (ticket points) | ~20 j.dev |
| M5 | BI & Rapports avancés | Haute | ★★★★★ | M1 à M4 | ~20 j.dev |

### Matrice urgence / impact — Sections 1 & 2

| ID | Sujet | Urgence | Impact | Effort estimé |
|---|---|---|---|---|
| U-01 | Filtrage tenant en mémoire | Critique | 5/5 | 2j |
| U-02 | Numérotation facture non atomique | Critique | 5/5 | 1j |
| U-03 | Credentials en clair | Critique | 5/5 | 0.5j |
| U-04 | CSRF API REST | Critique | 4/5 | 1j |
| U-05 | Client non isolé par tenant | Critique | 5/5 | 2j |
| U-06 | Fournisseur non isolé par tenant | Critique | 4/5 | 1.5j |
| U-07 | PDF facture sans TVA | Urgente | 5/5 | 3j |
| U-08 | Prévisions ventes non fiables | Urgente | 3/5 | 4j |
| NU-01 | Pagination VenteService manquante | Haute | 4/5 | 1j |
| NU-03 | Taux de change codés en dur | Haute | 4/5 | 1j |
| NU-07 | Pas de cache statistiques | Haute | 3/5 | 1j |
| NU-02 | Logs SQL en production | Moyenne | 2/5 | 0.5j |
| NU-04 | OHADA incomplet | Moyenne | 3/5 | 2j |
| NU-06 | UtilisateurController direct repo | Moyenne | 2/5 | 1j |
| NU-09 | CSV rapport non structuré | Basse | 2/5 | 1j |

---

## Conclusion & recommandations finales

Microbiz Pro dispose d'une architecture solide et bien adaptée aux micro-entreprises d'Afrique francophone. Les correctifs prioritaires doivent cibler d'abord l'isolation multi-tenant, la sécurité des credentials, l'atomicité de numérotation, et la conformité de facturation.

### Priorités d'exécution

| Priorité | Action | Horizon |
|---|---|---|
| Immédiat (semaine 1) | Corriger U-01 à U-06 | 7 jours |
| Court terme (mois 1) | U-07, U-08, NU-01 + démarrage M1 | 30 jours |
| Moyen terme (mois 2-3) | M2, M3 + corrections NU-02 à NU-07 + couverture tests >70% | 90 jours |
| Long terme (mois 4-5) | M4, M5 + SaaS Admin complet + préparation cloud | 150 jours |

### Estimation globale d'effort

- Corrections urgentes (Section 1) : **~16 j.dev**
- Améliorations non urgentes (Section 2) : **~12 j.dev**
- Roadmap 5 modules (Section 3) : **~100 j.dev** (équipe type : 2 dev Java + 1 designer)
- **Total estimé : ~4 mois** avec 2 développeurs à temps plein.

---

_Rapport généré par analyse statique du code source — Microbiz Pro `v0.0.1-SNAPSHOT` — 18/04/2026._
