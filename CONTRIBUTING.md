# Contribuer à NAVEE Companion

Merci de votre intérêt pour le projet **NAVEE Companion** !

Ce projet est une initiative communautaire dédiée à la trottinette **NAVEE NT5 Max** et aux véhicules basés sur l'architecture contrôleur **Brightway**.

---

## 🛠️ Comment Contribuer

### 1. Remonter des Journaux Bluetooth (BLE)
Si vous disposez d'une trottinette NAVEE (NT5 Max ou autre modèle) :
- Rendez-vous dans l'onglet **Diagnostic BLE** de l'application.
- Effectuez des opérations (changement de mode, allumage des feux, tentative de démarrage à 0 km/h).
- Copiez les trames hexadécimales émises (`TX`) et reçues (`RX`) et ouvrez une **Issue** GitHub décrivant votre version de firmware.

### 2. Soumettre du Code (Pull Requests)
1. Forkez le dépôt.
2. Créez une branche thématique (`git checkout -b feature/amelioration-telemetrie`).
3. Écrivez du code propre en **Kotlin** et **Jetpack Compose** (Material 3).
4. Assurez-vous que les tests unitaires réussissent :
   ```bash
   ./gradlew testDebugUnitTest
   ```
5. Ouvrez une Pull Request avec une description claire de vos modifications.

---

## ⚖️ Règles de Sécurité

- Ne soumettez jamais de code désactivant les sécurités thermiques ou de surintensité de la batterie.
- Ne soumettez aucune clé privée ou certificat dans le dépôt public.
