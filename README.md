# 🛴 NAVEE Companion — Application Android pour NAVEE NT5 Max

[![Build & Release APK](https://github.com/USER/REPO/actions/workflows/build-apk.yml/badge.svg)](https://github.com/USER/REPO/actions/workflows/build-apk.yml)
[![Platform](https://img.shields.io/badge/Platform-Android_8.0+_(API_24+)-brightgreen.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-purple.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Apache_2.0-orange.svg)](LICENSE)

**NAVEE Companion** est une application Android native indépendante, conçue en **Kotlin** et **Jetpack Compose (Material 3)**, spécialement adaptée à la trottinette électrique **NAVEE NT5 Max** (architecture contrôleur *Brightway*).

Elle permet de contrôler la trottinette via **Bluetooth Low Energy (BLE)** sans passer par l'application constructeur officielle, tout en apportant des fonctionnalités avancées : profils de vitesse personnalisés, tentative de démarrage sans poussée (0 km/h), télémétrie complète, tableau de bord 3D interactif, historique des trajets et console de diagnostic réseau.

---

## 📥 Téléchargement de l'APK (Automatisé via GitHub Actions)

Grâce au workflow CI/CD intégré dans ce dépôt, **chaque mise à jour génère automatiquement le fichier APK installable**, sans que vous n'ayez besoin d'installer Android Studio.

### Option 1 — Via les Artefacts GitHub Actions (Le plus récent)
1. Rendez-vous sur l'onglet **[Actions](../../actions)** de votre dépôt GitHub.
2. Cliquez sur le dernier workflow exécuté avec succès (marqué d'une coche verte ✅).
3. Descendez tout en bas de la page dans la section **Artifacts**.
4. Cliquez sur **`NAVEE-Companion-debug-apk`** pour télécharger le fichier ZIP contenant l'APK.
5. Décompressez le ZIP et transférez le fichier `.apk` sur votre téléphone Android.

### Option 2 — Via les Releases (Versions étiquetées)
1. Rendez-vous dans la section **[Releases](../../releases)** à droite de la page GitHub.
2. Téléchargez directement le fichier **`NAVEE-Companion-debug.apk`** attaché à la dernière version.

---

## ✨ Fonctionnalités Principales

### 1. Tableau de Bord & Modèle Visuel 3D Interactif
- **Représentation 2.5D/3D détaillée** de la NAVEE NT5 Max :
  - Fourche double bras oscillant (*Damping Arm™*).
  - Potence inclinée à 15° avec afficheur digital au guidon.
  - Large deck avec bande grip signature orange NAVEE.
  - Pneus larges 10 pouces, feu arrière et béquille.
- **Interactivité tactile** : rotation panoramique 360°, inclinaison et zoom par pincement.
- **Animations en temps réel** : rotation des rayons de roue proportionnelle à la vitesse, faisceau lumineux des phares LED projeté au sol, bouclier de verrouillage moteur.
- **Compteur numérique haute précision** : arc lumineux dynamique, vitesse instantanée en km/h et jauge de puissance électrique en **Watts**.
- **Bascule instantanée** vers la vue photographique de studio haute définition.

### 2. Profils de Vitesse & Réglage Fin (CMD `0x6E`)
- **15 km/h** : Mode Éco Urbain (zones piétonnes tolérées).
- **20 km/h** : Norme eKFV (Allemagne / Suisse).
- **25 km/h** : Limite légale France & Union Européenne (Décret EDPm n°2019-1082).
- **Boost (>25 km/h)** : Consigne débridée pour usage sur voie privée uniquement.
- **Curseur de précision (Slider)** : Ajustement personnalisé au km/h près (de 10 à 35 km/h).
- **Affichage transparent du retour contrôleur (ESC)** : L'application distingue toujours la consigne envoyée de la limite réellement acceptée et appliquée par le microcontrôleur de la trottinette.

### 3. Démarrage Direct à 0 km/h (Zero-Start)
- Contrairement à l'application officielle qui restreint le kick-start à 3, 4 ou 5 km/h (norme de sécurité EN 17128), NAVEE Companion transmet la commande de démarrage direct à **0 km/h** via la trame `0x66`.
- Indicateur matériel en direct : informe l'utilisateur si le firmware de la trottinette accepte le 0 km/h ou si le contrôleur européen applique la sécurité minimale de 3 km/h.

### 4. Télémétrie & Surveillance Énergie
- Tension précise du pack batterie 13S Li-ion 48V (plage de 42.0V à 54.6V).
- Courant de décharge instantané (A) et récupération d'énergie au freinage (eABS négatif).
- Puissance instantanée consommée en Watts (jusqu'à 1000W crête).
- Températures en direct : Sonde interne moteur (°C) et MOSFETs contrôleur ESC (°C).
- Estimation dynamique de l'autonomie restante en kilomètres.
- Odomètre total constructeur et odomètre de trajet.

### 5. Historique Local des Trajets (Room Database)
- Enregistreur de session : distance parcourue, durée, vitesse maximale, vitesse moyenne, énergie consommée en Watt-heures (Wh) et delta de batterie.
- Base de données locale SQLite via Android Room (aucune dépendance cloud requise).

### 6. Diagnostic BLE & Renifleur de Trames
- **Sniffer Bluetooth en temps réel** : visualisation des trames hexadécimales émises (`TX`) et reçues (`RX`) avec horodatage à la milliseconde.
- **Répertoire officiel des codes d'erreur Brightway** (`E1` accélérateur, `E2` frein, `E3` capteurs Hall, `E4` surintensité, `E5` communication UART, `E6` surchauffe ESC, `E7` tension batterie, `E8` surchauffe moteur).
- **Cartographie GATT** : inspection des services et caractéristiques découverts.

### 7. Mode Simulateur Haute Fidélité
- Intègre un moteur physique simulant l'accélération, le freinage eABS, la décharge de batterie et la chauffe moteur.
- Permet de tester toute l'interface sans nécessiter la présence physique de la trottinette.

---

## 🔬 Analyse du Protocole & Reverse-Engineering

Le protocole de communication repose sur les travaux communautaires (*pepperonas/navee-st3-pro*, *scooterteam* et analyses de firmware Brightway) :

### Services & Caractéristiques BLE
```
Service Principal Brightway : 00001530-1212-efde-1523-785feabcd123
├── Caractéristique TX (Write)  : 0000155f-1212-efde-1523-785feabcd123
├── Caractéristique RX (Notify) : 0000155e-1212-efde-1523-785feabcd123
├── Auth Nonce (Read)           : 00002556-1212-efde-1523-785feabcd123
└── Auth Response (Write)       : 00002557-1212-efde-1523-785feabcd123

Service Fallback Nordic UART (NUS) : 6e400001-b5a3-f393-e0a9-e50e24dcca9e
```

### Structure des Trames (UART / BLE)
```
[0x5A] [Longueur] [Commande] [Payload (0..N octets)] [CRC16-L] [CRC16-H]
```
- **En-tête** : Fixe à `0x5A`.
- **Checksum** : CRC-16 CCITT (Polynôme `0x1021`, initialisé à `0x0000`).
- **Commandes principales** :
  - `0x12` : Requête périodique de télémétrie (Réponse `0x21`).
  - `0x6E` : Écriture de la consigne de vitesse maximale.
  - `0x66` : Seuil de démarrage kick-start (`0x00` = 0 km/h, `0x03` = 3 km/h).
  - `0x65` : Commande d'allumage des phares (`0x01` on, `0x00` off).
  - `0x64` : Verrouillage électronique moteur (`0x01` lock, `0x00` unlock).

---

## 🛠️ Compilation Locale

### Sous Windows
1. Ouvrez un terminal **PowerShell** ou **Invite de commandes** dans le dossier du projet :
   ```powershell
   .\gradlew.bat assembleDebug
   ```
2. L'APK généré sera disponible dans :
   ```
   app\build\outputs\apk\debug\app-debug.apk
   ```

### Sous Linux / macOS
```bash
chmod +x ./gradlew
./gradlew assembleDebug
```

### Exécution des Tests Unitaires
```bash
./gradlew testDebugUnitTest
```

---

## ⚖️ Avertissements Légaux & Sécurité

1. **Réglementation Routière** : En France et dans l'Union Européenne, les engins de déplacement personnel motorisés (EDPm) sont limités par construction à **25 km/h** sur la voie publique (Décret n° 2019-1082). Le mode Boost est strictement réservé à une utilisation sur terrain privé fermé à la circulation.
2. **Sécurité Matérielle** : L'application n'altère en aucun cas les sécurités matérielles critiques du BMS (Battery Management System), des sondes thermiques et du coupe-circuit de freinage.
3. **Marque** : NAVEE® et Brightway® sont des marques déposées de leurs propriétaires respectifs. Ce projet est un développement communautaire indépendant open-source.

---

## 📄 Licence

Ce projet est distribué sous licence **Apache 2.0**. Consultez le fichier [LICENSE](LICENSE) pour plus de détails.
