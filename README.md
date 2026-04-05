# Foody - Application de Livraison

Foody est une application Android moderne pour la commande et la livraison de repas, intégrant un système de suivi GPS en temps réel. L'application gère trois rôles d'utilisateurs : Clients, Livreurs et Administrateurs.

## Fonctionnalités Clés

### Côté Client
- Catalogue de repas : Parcourir les menus et ajouter des plats au panier.
- Commande intelligente : Sélection de l'adresse de livraison directement sur une interface Google Maps.
- Suivi en temps réel : Visualisation de la position du livreur sur une carte avec une icône de voiture animée et le tracé de l'itinéraire (Polyline).
- Historique des commandes : Consultation des commandes passées et de leur statut en temps réel.

### Côté Livreur (Driver)
- Gestion du statut : Activation/Désactivation de la disponibilité (En ligne/Hors ligne).
- Gestion des livraisons : Acceptation et gestion des commandes en attente à proximité.
- Guidage GPS : Calcul automatique de la distance et des gains. Navigation intégrée via Google Maps.
- Suivi actif : Mise à jour automatique de la position GPS pour le client pendant la livraison.

### Côté Administration
- Tableau de bord centralisé : Vue d'ensemble de l'activité de la plateforme.
- Gestion des utilisateurs : Contrôle et suivi des comptes clients et livreurs.
- Supervision des commandes : Suivi de tous les flux de commandes actifs et de leurs statuts respectifs.

## Stack Technique

- Langage : Kotlin
- Architecture : MVVM (Model-View-ViewModel)
- Injection de dépendances : Hilt / Dagger
- Backend & Base de données :
    - Firebase Auth : Authentification des utilisateurs.
    - Firebase Realtime Database : Suivi de localisation en temps réel.
    - Firebase Firestore : Stockage de données structurées pour les commandes et les utilisateurs.
- Composants UI/UX :
    - Jetpack Navigation Component
    - View Binding / Data Binding
    - Material Design 3
    - MotionLayout : Animations du tiroir de navigation.
- Cartographie : Google Maps SDK & Fused Location Provider API

## Structure du Projet

```text
com.imadev.foody
├── adapter       # Adaptateurs RecyclerView (Commandes, Plats, Livreurs, Utilisateurs)
├── model         # Modèles de données (Order, Meal, Client, DeliveryUser, Address)
├── repository    # Couche d'accès aux données (Implémentations Firebase)
├── ui            # Fragments et Activités
│   ├── admin     # Fragments de gestion administrative et dashboard
│   ├── auth      # Authentification Login, SignUp et OTP
│   ├── checkout  # Logique du panier et de validation de commande
│   ├── delivery  # Interface livreur et logique de suivi
│   ├── map       # Logique de sélection d'adresse
│   └── user      # Profil client et suivi de commande
└── utils         # Extensions, Constantes et Helpers
```

## Installation et Configuration

1. Cloner le projet :
   ```bash
   git clone https://github.com/ahlamelbechari-cmd/Foody-Android-App.git
   ```
2. Configuration Firebase :
   - Placez votre fichier `google-services.json` dans le répertoire `app/`.
   - Activez l'Authentification (Email/Téléphone), Firestore et Realtime Database dans la console Firebase.
3. API Google Maps :
   - Ajoutez votre clé API Google Maps dans le fichier `local.properties` :
     ```properties
     MAPS_API_KEY=VOTRE_CLE_API
     ```
4. Build : Compilez et lancez le projet via Android Studio.

## Licence
Ce projet est réalisé dans un but éducatif dans le cadre du développement d'une plateforme de livraison de repas.
