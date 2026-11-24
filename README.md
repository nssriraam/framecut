<h1 align="center">🎬 FrameCut</h1>
<p align="center">
  <strong>Your Cinema, Your Story</strong><br>
  <em>A modern, high-performance streaming discovery & cinema diary platform.</em>
</p>

<p align="center">
  <a href="https://framecut-kmem.onrender.com/"><img src="https://img.shields.io/badge/Live%20Demo-Render-red?style=for-the-badge&logo=render" alt="Live Demo"></a>
  <img src="https://img.shields.io/badge/Java-17-blue?style=for-the-badge&logo=openjdk" alt="Java 17">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen?style=for-the-badge&logo=springboot" alt="Spring Boot 3.2">
  <img src="https://img.shields.io/badge/PostgreSQL-15-336791?style=for-the-badge&logo=postgresql" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/License-MIT-lightgrey?style=for-the-badge" alt="License">
</p>

<p align="center">
  🌐 <strong>Live Application:</strong> <a href="https://framecut-kmem.onrender.com/">https://framecut-kmem.onrender.com/</a>
</p>

---

## 🌟 Overview

**FrameCut** is a production-grade cinema discovery platform and movie tracking diary built for cinephiles. Inspired by streaming interfaces like **Amazon Prime Video** and social film platforms like **Letterboxd**, FrameCut combines rich media exploration with robust catalog architecture.

Whether you are logging your film journal, navigating the filmographies of legendary directors, exploring fresh theatrical releases, or discovering a movie tailored to your mood, FrameCut delivers a fluid, dark-mode streaming experience.

---

## 🍿 Core Features

### 🎬 Prime Video-Style Hover Preview Cards
- **Cinematic Popups:** Hovering over any movie or series tile smoothly expands into a high-resolution preview with ambient backdrop blur, key art, rating, genre tags, and synopsis.
- **One-Click Navigation:** Directly click anywhere on the expanded preview card to navigate seamlessly into the title's detail page.
- **Hardware-Accelerated Animation:** Uses decelerated cubic-bezier curves with GPU rasterization to eliminate stutter or sudden layout shifts.

### 🎨 Vibrant "Explore by Genre" Categories
- **Dynamic Category Themes:** Replaced dull static boxes with vibrant, individualized color gradients (Action crimson, Sci-Fi cyan, Animation magenta, Horror blood-red, etc.).
- **Interactive Glyphs & Glow:** Micro-animations and ambient lighting sweep across genre tiles on hover.

### ⚡ Sub-15ms Live Autocomplete
- **Local-First Caching:** Keystroke autocomplete checks the local database first, returning instant suggestions in **5–15ms**.
- **Parallel TMDB Fallback:** If a newly released title (such as upcoming or regional 2026 films) isn't cached yet, parallel WebClient requests query TMDB in the background and insert it on the fly.
- **Stale Request Cancellation:** Employs frontend `AbortController` and 200ms debounce to prevent input lag.

### 📖 Personal Cinema Diary & Watchlists
- Rate titles on a 5-star or 10-point scale.
- Write reviews with optional **Spoiler Warning Masks** that other users can click to reveal.
- Curate a personal watchlist directly tied to your authenticated profile.

### 🌌 The Cinematic Director Universe
- Dedicated director hubs (Christopher Nolan, Quentin Tarantino, Hayao Miyazaki, Karthik Subbaraj, and more).
- Visualizes recurring actor collaborations, film chronologies, and signature directorial motifs.

### 🎭 Mood Matcher & Curated Shelves
- Recommends titles based on your current emotional vibe (*Thrilled*, *Emotional*, *Mind-Blown*, *Feel-Good*, *Nostalgic*).
- Highlights **Hidden Gems**, **New Releases**, and **Top Rated** classics with deterministic sorting to prevent duplicate records during infinite scroll.

### 🛡️ Production-Grade Security
- Dual credential authentication: sign in via either **Username or Email**.
- BCrypt password encryption with adaptive work factor.
- Secure HTTP session management with `HttpOnly`, `SameSite=Lax`, and HTTPS protection.
- Configurable environment-driven administrator bootstrapping.

---

## ⚙️ Architecture & Data Pipeline

```
 [User Browser] 
       │
       ▼
 [Spring Security Filter Chain] (BCrypt, Session Cookies, CSRF protection)
       │
       ▼
 [Controllers] ──► [Service Layer] ──► [PostgreSQL Database] (Local Cache)
                          │
                          ▼ (Reactive WebClient)
                   [TMDB REST API]
```

- **Automated 6-Hour Background Sync:** A persistent `TmdbSyncScheduler` regularly pulls weekly trending titles (`/trending/movie/week`, `/trending/tv/week`) and theatrical/OTT releases (`/movie/now_playing`, `/tv/on_the_air`).
- **Persistent State Pagination:** Catalog pagination is tracked in the database (`sync_state` table) across application reboots and safely cycles when reaching catalog limits.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Java 17, Spring Boot 3.2 (Web, Security, Data JPA, Scheduling, Validation) |
| **HTTP Client** | Spring WebFlux `WebClient` (non-blocking reactive TMDB consumer) |
| **Database** | PostgreSQL 15 / Render Managed PostgreSQL, Hibernate / JPA |
| **Frontend** | Thymeleaf 3, HTML5, CSS3 (`Plus Jakarta Sans` & `Bebas Neue`), Vanilla ES6+ JS |
| **Container & Cloud** | Docker, Docker Compose, Render PaaS |

---

## 🚀 Running Locally

### 1. Prerequisites
- **JDK 17** or later
- **Docker & Docker Compose** (or a local PostgreSQL instance)
- A free **TMDB API Key** from [themoviedb.org](https://www.themoviedb.org/documentation/api)

### 2. Configure Environment
Copy the example environment file:
```bash
cp .env.example .env
```
Update `.env` with your TMDB API Key and database credentials:
```properties
TMDB_API_KEY=your_tmdb_api_key_here
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/framecut
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
ADMIN_PASSWORD=your_secure_admin_password
```

### 3. Start with Docker Compose
```bash
docker compose up --build -d
```
Access the application at `http://localhost:8080`.

### 4. Or Run with Maven Directly
```bash
./mvnw clean spring-boot:run
```

---

## 📄 License
Distributed under the MIT License. See `LICENSE` for details.

*Copyright © 2026 FrameCut. Built for cinema lovers.*
