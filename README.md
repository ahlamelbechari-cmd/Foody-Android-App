# Foody - Delivery Application

Foody is a modern Android application for meal ordering and delivery, featuring a real-time GPS tracking system. The application manages three user roles: Clients, Delivery Drivers, and Administrators.

## Key Features

### Client Side
- Meal Catalog: Browse menus and add dishes to the cart.
- Smart Ordering: Select the delivery address directly on a Google Maps interface.
- Real-time Tracking: Visualize the driver's position on a map with an animated car icon and the route (Polyline).
- Order History: Consult past orders and their real-time status.

### Delivery Side (Driver)
- Status Management: Toggle availability status (Online/Offline).
- Delivery Management: Accept and manage pending orders in the vicinity.
- GPS Guidance: Automatic distance and earnings calculation. Integrated navigation via Google Maps.
- Active Tracking: Automatic GPS position updates for the client during delivery.

### Administration Side
- Centralized Dashboard: Overview of platform activity.
- User Management: Control and monitor client and driver accounts.
- Order Supervision: Monitor all active order flows and their respective statuses.

## Technical Stack

- Language: Kotlin
- Architecture: MVVM (Model-View-ViewModel)
- Dependency Injection: Hilt / Dagger
- Backend & Database:
    - Firebase Auth: User authentication.
    - Firebase Realtime Database: Real-time location tracking.
    - Firebase Firestore: Structured data storage for orders and users.
- UI/UX Components:
    - Jetpack Navigation Component
    - View Binding / Data Binding
    - Material Design 3
    - MotionLayout: Navigation drawer animations.
- Mapping: Google Maps SDK & Fused Location Provider API

## Project Structure

```text
com.imadev.foody
├── adapter       # RecyclerView Adapters (Orders, Meals, Drivers, Users)
├── model         # Data Models (Order, Meal, Client, DeliveryUser, Address)
├── repository    # Data access layer (Firebase implementations)
├── ui            # Fragments and Activities
│   ├── admin     # Dashboard and Admin management fragments
│   ├── auth      # Login, SignUp, and OTP authentication
│   ├── checkout  # Cart and order validation logic
│   ├── delivery  # Driver interface and tracking logic
│   ├── map       # Address selection logic
│   └── user      # Client profile and order tracking
└── utils         # Extensions, Constants, and Helpers
```

## Installation and Setup

1. Clone the project:
   ```bash
   git clone https://github.com/ahlamelbechari-cmd/Foody-Android-App.git
   ```
2. Firebase Configuration:
   - Place your `google-services.json` file in the `app/` directory.
   - Enable Authentication (Email/Phone), Firestore, and Realtime Database in the Firebase Console.
3. Google Maps API:
   - Add your Google Maps API Key to the `local.properties` file:
     ```properties
     MAPS_API_KEY=YOUR_API_KEY
     ```
4. Build: Compile and run the project using Android Studio.

