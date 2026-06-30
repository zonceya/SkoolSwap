# SkoolSwap Android

SkoolSwap is an Android marketplace application that enables users to buy and sell preloved school items such as uniforms, stationery, textbooks, accessories, and sports equipment. The application is designed to make school essentials more affordable while encouraging community trading between parents, learners, and schools.

This repository contains the native Android application built with modern Android development practices.

---

# Features

## Authentication

* Email & Password Authentication
* Google Sign-In
* Firebase Authentication
* Secure backend user synchronization with Ruby on Rails and PostgreSQL

## User Profile

* User onboarding
* School selection
* Profile management
* Automatic shop creation

## Marketplace

* Create listings
* Edit listings
* Delete listings
* Mark listings as sold
* Product detail screens
* Home feed
* Category browsing
* Search
* Search filters
* Favourite listings
* Share listings
* Recommendations (Essentials, Trending, Recent)

## Seller Contact

* "I'm Interested" contact seller flow
* Seller contact information
* Interest logging

## Media

* Upload images directly from Android
* Cloudflare R2 integration
* Image loading and local caching

---

# Architecture

The application follows **MVVM (Model–View–ViewModel)** architecture with a clean separation of concerns.

```
UI
│
├── Fragments
├── Activities
│
ViewModel
│
Repository
│
Retrofit API
│
Ruby on Rails Backend
├──────────────┬──────────────┐
│              │              │
PostgreSQL    Redis     Cloudflare R2
```

### Technologies

* Kotlin
* XML
* MVVM Architecture
* Navigation Component
* Hilt (Dependency Injection)
* Coroutines
* Retrofit
* Room Database
* DataStore
* Firebase Authentication
* Cloudflare R2
* Redis

---

# Tech Stack

| Component | Technology |
| ---------- | ---------- |
| Language | Kotlin |
| UI | XML |
| Architecture | MVVM |
| Dependency Injection | Hilt |
| Local Database | Room |
| Preferences | DataStore |
| Networking | Retrofit |
| Asynchronous Programming | Kotlin Coroutines |
| Authentication | Firebase Authentication |
| Backend | Ruby on Rails |
| Database | PostgreSQL |
| Cache / Background Processing | Redis |
| Image Storage | Cloudflare R2 |
| CI/CD | GitHub Actions |

---

# Current MVP

The current MVP supports:

* User registration
* User login
* Google Sign-In
* Email & Password authentication
* School onboarding
* Shop creation
* Listing creation
* Edit listings
* Delete listings
* Mark items as sold
* Image uploads
* Home feed
* Recommendations
* Search
* Search filters
* Favorites
* Share listings
* Seller contact flow
* Interest tracking
* Local caching with Room

---

# Planned Features

* Firebase Cloud Messaging
* Buyer/Seller Chat
* Payment workflow
* Collection PIN verification
* Ratings and Reviews
* Improved recommendation engine

---

# Backend

The Android application communicates with a Ruby on Rails REST API backed by PostgreSQL and Redis.

The backend provides:

* Authentication
* User management
* Shop management
* Listing management
* Search
* Recommendation endpoints
* Image metadata
* Redis caching and background processing

Images are uploaded to **Cloudflare R2**, while only the image URLs are stored in PostgreSQL.

---

# Project Structure

```
app/
├── data/
│   ├── api/
│   ├── database/
│   ├── model/
│   ├── repository/
│   └── datastore/
│
├── di/
│
├── ui/
│   ├── auth/
│   ├── onboarding/
│   ├── home/
│   ├── item/
│   ├── shop/
│   ├── profile/
│   └── common/
│
├── utils/
│
└── viewmodel/
```

---

# Getting Started

## Requirements

* Android Studio
* JDK 17+
* Android SDK
* Kotlin

## Installation

Clone the repository:

```bash
git clone https://github.com/zonceya/SkoolSwap.git
```

Open the project in Android Studio.

Configure the required environment variables and API endpoints.

Build and run the application on an emulator or Android device.

---

# Related Projects

## Android Application

https://github.com/zonceya/SkoolSwap

## Backend API

https://github.com/zonceya/SekeniROR

---

# Database Design

Core Marketplace

https://drawsql.app/teams/zama/diagrams/skoolswapcore

Storage & Media

https://drawsql.app/teams/zama/diagrams/sscomms-storage

---

# Live Links

Website

https://skoolswap.co.za

Waitlist

https://skoolswap.co.za/waitlist

Figma

https://bit.ly/skoolswap_demo

---

# My Role

I designed and developed the Android application end-to-end, including:

* Android architecture
* UI implementation
* API integration
* Authentication
* Local persistence
* Image upload integration
* Backend integration
* Database design
* Redis integration
* Deployment preparation

---

## License

This repository is shared for portfolio and demonstration purposes. Sensitive credentials and production secrets have been removed.