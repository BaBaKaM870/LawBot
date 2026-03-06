# ⚖️ LawBot — Simulation de Procès

> **Incarnez l'avocat de la défense.** Interrogez les témoins, contestez les preuves et convainquez le jury de l'innocence de votre client.

---

## 📸 Aperçu

```
┌──────────────────────────────────────────────────────────────────┐
│  ⚖ LAWBOT     [Ouverture › Accusation › Défense › …]   Jury 58% │
├──────────────┬───────────────────────────────────────────────────┤
│              │                                                   │
│  📁 Affaire  │        Phase active (injecté par JS)             │
│  👤 Accusé   │                                                   │
│              │   Témoins · Preuves · Questions · Verdict        │
│  📋 Journal  │                                                   │
│              │                                                   │
└──────────────┴───────────────────────────────────────────────────┘
```

---

## 🎮 Comment jouer

Le jeu se déroule en **6 phases successives** :

| # | Phase | Ce que vous faites |
|---|-------|--------------------|
| 1 | 📜 **Ouverture** | Découvrez l'affaire, l'accusé et les charges retenues |
| 2 | ⚔️ **Accusation** | Contestez les preuves présentées par la partie adverse |
| 3 | 🛡️ **Défense** | Interrogez vos témoins pour établir l'innocence |
| 4 | 🔥 **Contre-interrogatoire** | Déstabilisez les témoins adverses, cherchez les contradictions |
| 5 | 🎤 **Plaidoirie** | Confrontez deux témoins ou plaidez devant le jury |
| 6 | ⚖️ **Verdict** | Le jury délibère — acquittement ou condamnation ? |

### 💡 Conseils stratégiques

- Le jury commence à **50 %** de conviction — chaque action penche la balance
- Les preuves marquées **"Douteux"** peuvent être fausses : contestez-les en priorité
- **Confronter deux témoins** peut révéler des contradictions décisives (gros bonus)
- Ciblez les témoins avec une **faible crédibilité** pour les questions risquées
- Votre score final reflète le ratio d'actions réussies sur le total

---

## 🗂️ Affaires disponibles

12 affaires générées aléatoirement, réparties sur 5 types de crimes :

| Type | Affaires |
|------|----------|
| 🔪 **Meurtre** | Le Meurtre au Manoir, L'Hôtel des Arts, L'Incendie Criminel, Le Journaliste Disparu |
| 💰 **Fraude** | L'Escroquerie Financière, La Fraude à l'Héritage |
| 🏦 **Vol** | Le Musée National, La Bijouterie Leclerc, Le Cambriolage Nocturne |
| 👊 **Agression** | L'Agression dans le Parc, La Rixe au Bar |
| 🏛️ **Corruption** | La Corruption Municipale, L'Espionnage Industriel |

Chaque affaire dispose de **témoins uniques**, de **preuves spécifiques** (authentiques ou falsifiées) et d'un accusé nommé.

---

## 🚀 Lancer le projet

### Prérequis

- **Java 17+** (testé avec Java 25)
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

## 🛠️ Stack technique

| Couche | Technologie |
|--------|-------------|
| Backend | **Spring Boot 3.2** + Java 25 |
| Persistance | **H2** (base en mémoire, `create-drop`) |
| ORM | **JPA / Hibernate** |
| Réduction boilerplate | **Lombok 1.18** |
| Frontend | **HTML5 / CSS3 / Vanilla JS** (SPA) |
| Polices | Playfair Display (serif) + Inter (sans-serif) |
| Thème | Dark courtroom — Navy `#0d1520` / Gold `#c9a227` |

---

## 📡 API REST

Le frontend communique avec le backend via ces endpoints :

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
│   ├── LawBotApplication.java          # Point d'entrée Spring Boot
│   ├── controller/
│   │   ├── GameController.java         # API jeu web (7 endpoints)
│   │   ├── TrialController.java
│   │   ├── WitnessController.java
│   │   └── EvidenceController.java
│   ├── service/
│   │   ├── CaseGeneratorService.java   # 12 affaires aléatoires
│   │   ├── TrialService.java           # Logique de procès (état en mémoire)
│   │   ├── WitnessService.java         # Gestion des témoins et réponses
│   │   └── VerdictService.java         # Calcul du verdict final
│   ├── model/
│   │   ├── Case.java / Trial.java / Witness.java
│   │   ├── Evidence.java / JuryMember.java
│   │   └── ...
│   ├── dto/
│   │   ├── GameStateDTO.java           # État complet de la partie
│   │   ├── ActionResultDTO.java        # Résultat d'une action joueur
│   │   ├── WitnessInfoDTO.java
│   │   └── EvidenceInfoDTO.java
│   └── game/
│       └── TerminalGameRunner.java     # Mode terminal (désactivé par défaut)
└── resources/
    ├── application.properties
    └── static/
        ├── index.html                  # SPA — accueil, tutoriel, jeu, verdict
        ├── style.css                   # Thème dark courtroom
        └── game.js                     # Logique JS + appels API fetch
```

---

## 🖥️ Mode terminal (optionnel)

Il est possible de jouer directement dans le terminal :

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
