# ⚖️ LawBot — Simulation de Procès 
Lucas Patin, Sriraam Peroumal, Yohan Dechamps

> **Incarnez l'avocat de la défense.** Interrogez les témoins, contestez les preuves et convainquez un jury hostile de l'innocence de votre client. Bonne chance — vous en aurez besoin.

---

## Comment jouer ?

Le jeu se déroule en **6 phases successives** :

| # | Phase | Ce que vous faites | Actions |
|---|-------|--------------------|---------|
| 1 | 📜 **Ouverture** | Découvrez l'affaire, l'accusé et les charges retenues | — |
| 2 | ⚔️ **Accusation** | Contestez les preuves présentées par la partie adverse | 3 |
| 3 | 🛡️ **Défense** | Interrogez les témoins via des questions prédéfinies | 5 |
| 4 | 🔍 **Contre-interrogatoire** | Confrontez deux témoins pour trouver des contradictions | 2 |
| 5 | 🎤 **Plaidoirie** | Choisissez votre stratégie de plaidoirie finale | — |
| 6 | ⚖️ **Verdict** | Le jury délibère — acquittement ou condamnation ? | — |

### 💡 Conseils stratégiques

- Le jury **part biaisé vers la culpabilité** — il faut travailler dur pour le convaincre
- Les preuves marquées **"Douteux"** peuvent être fausses : contestez-les en priorité, pas les preuves authentiques
- Les témoins ont une **personnalité** (Nerveux, Menteur, Confiant, Coopératif) — leur style de réponse change selon les questions
- Posez des questions de **catégories différentes** pour obtenir des réponses variées
- En **Contre-interrogatoire**, confrontez un témoin crédible avec un témoin peu fiable pour maximiser les contradictions
- Votre **grade final** (S à F) reflète la qualité de toute votre plaidoirie

---

## Affaires disponibles

13 affaires générées aléatoirement, réparties sur 5 types de crimes :

| Type | Affaires |
|------|----------|
| 🔪 **Meurtre** | Meurtre au manoir, Hôtel des arts, Incendie criminel, Meurtre du journaliste |
| 💰 **Fraude** | Escroquerie financière, Fraude pour l'héritage |
| 🏦 **Vol** | Vol au Musée, Braquage de la bijouterie, Le cambriolage nocturne |
| 👊 **Agression** | Agression dans le Parc, Rixe au bar |
| 🏛️ **Corruption** | La corruption municipale, espionnage industriel |

Chaque affaire dispose de **témoins uniques** (2 à 3), de **preuves spécifiques** (authentiques ou falsifiées) et d'un accusé nommé. La culpabilité réelle de l'accusé est révélée après le verdict.

---

## Système de témoins

Chaque témoin possède :

- Une **déclaration initiale** visible sur sa carte
- Une **personnalité** déterminée par sa fiabilité :
  - **Nerveux** — hésite, se contredit facilement, paniqué sous pression
  - **Menteur** — trop précis, défensif, prétend tout savoir
  - **Confiant** — agressif, repousse les questions, intimidant
  - **Coopératif** — honnête, répond directement, facile à questionner
- Un **niveau de stress** qui augmente à chaque question — au-delà de 70%, le témoin peut se contredire spontanément
- Un **score de crédibilité** qui baisse si des contradictions sont détectées

Les témoins répondent différemment selon la **catégorie de question** posée (alibi, crédibilité, observation, motif, chronologie).

---

## Lancer le projet !

### Prérequis

- **Java 17+**
- **Maven 3.8+**

### Démarrage rapide

```bash
# Cloner le projet
git clone https://github.com/votre-repo/lawbot.git
cd lawbot

# Lancer le serveur
mvn spring-boot:run
```

Puis ouvrir **http://localhost:8080** dans votre navigateur.

### Packaging en JAR autonome

```bash
mvn package -DskipTests
java -jar target/lawbot-0.0.1-SNAPSHOT.jar
```

---

## 🏆 Système de score

À la fin du procès, vous recevez :

| Grade | Score | Titre |
|-------|-------|-------|
| **S** | 80–100 | Maître Défenseur |
| **A** | 65–79 | Excellent Avocat |
| **B** | 50–64 | Bon Défenseur |
| **C** | 35–49 | Défense Correcte |
| **D** | 20–34 | Défense Insuffisante |
| **F** | 0–19 | Plaidoirie Catastrophique |

Le score est calculé à partir de : l'état des preuves (40%), la crédibilité des témoins (30%), le niveau de conviction du jury (20%) et votre ratio d'actions réussies (10%).

---

## Stack technique

| Couche | Technologie |
|--------|-------------|
| Backend | **Spring Boot 3.2** + Java 17 |
| Persistance | **H2** (base en mémoire, `create-drop`) |
| ORM | **JPA / Hibernate** |
| Réduction boilerplate | **Lombok 1.18** |
| Frontend | **HTML5 / CSS3 / Vanilla JS** (SPA) |
| Polices | Playfair Display (serif) + Inter (sans-serif) |
| Thème | Dark courtroom — Navy `#0d1520` / Gold `#c9a227` |

---

## API REST

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/api/game/new` | Crée une nouvelle partie |
| `GET` | `/api/game/{id}` | Récupère l'état de la partie |
| `POST` | `/api/game/{id}/contest/{idx}` | Conteste une preuve (par index) |
| `POST` | `/api/game/{id}/question/{idx}` | Interroge un témoin |
| `POST` | `/api/game/{id}/confront` | Confronte deux témoins |
| `POST` | `/api/game/{id}/next` | Passe à la phase suivante |
| `POST` | `/api/game/{id}/verdict` | Demande le verdict final |

> La console H2 est accessible sur **http://localhost:8080/h2-console**
> (JDBC URL : `jdbc:h2:mem:lawbotdb`, utilisateur : `sa`, mot de passe : vide)

---

## 📁 Structure du projet

```
src/main/
├── java/
│   ├── LawBotApplication.java
│   ├── controller/
│   │   ├── GameController.java         # API jeu web (7 endpoints)
│   │   ├── TrialController.java
│   │   ├── WitnessController.java
│   │   └── EvidenceController.java
│   ├── service/
│   │   ├── CaseGeneratorService.java   # 13 affaires + assignation des personnalités
│   │   ├── TrialService.java           # Logique de procès (état en mémoire)
│   │   ├── WitnessService.java         # Réponses contextuelles par personnalité
│   │   └── VerdictService.java         # Calcul verdict + grade + feedback
│   ├── model/
│   │   ├── Case.java / Trial.java / Witness.java
│   │   ├── Evidence.java / JuryMember.java
│   │   └── ...
│   ├── dto/
│   │   ├── GameStateDTO.java
│   │   ├── VerdictDTO.java             # Inclut grade, feedback, vérité révélée
│   │   └── ...
│   └── game/
│       └── TerminalGameRunner.java     # Mode terminal (désactivé par défaut)
└── resources/
    ├── application.properties
    └── static/
        ├── index.html                  # SPA + tutoriel + overlay verdict
        ├── style.css                   # Thème dark courtroom
        └── game.js                     # Logique JS + appels API fetch
```

---

## Mode terminal (optionnel)

1. Ouvrir [src/main/java/game/TerminalGameRunner.java](src/main/java/game/TerminalGameRunner.java)
2. Décommenter `@Component` en haut de la classe
3. Ajouter `spring.main.web-application-type=none` dans `application.properties`
4. Packager et lancer le JAR :
   ```bash
   mvn package -DskipTests
   java -jar target/lawbot-0.0.1-SNAPSHOT.jar
   ```

> ⚠️ `mvn spring-boot:run` ne transmet pas `stdin` correctement — utiliser impérativement le JAR.

---

## 📜 Licence

Projet éducatif — libre d'utilisation et de modification.
