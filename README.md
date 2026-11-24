<h1 align="center">🎬 FrameCut: Your Cinema, Your Story</h1>

<p align="center">
  <img src="docs/screenshot.png" alt="FrameCut Banner" width="100%">
</p>

<p align="center">
  <a href="https://framecut-kmem.onrender.com/"><img src="https://img.shields.io/badge/Live%20Application-framecut--kmem.onrender.com-red?style=for-the-badge&logo=render" alt="Live Platform"></a>
  <img src="https://img.shields.io/badge/Status-Active%20Production-brightgreen?style=for-the-badge" alt="Production">
  <img src="https://img.shields.io/badge/Java-17-blue?style=for-the-badge&logo=openjdk" alt="Java 17">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen?style=for-the-badge&logo=springboot" alt="Spring Boot 3.2">
  <img src="https://img.shields.io/badge/PostgreSQL-15-336791?style=for-the-badge&logo=postgresql" alt="PostgreSQL">
</p>

<p align="center">
  🌐 <strong>Official Website:</strong> <a href="https://framecut-kmem.onrender.com/">https://framecut-kmem.onrender.com/</a>
</p>

---

## 🌟 About FrameCut

**FrameCut** is a bespoke, full-stack cinema discovery platform and movie tracking diary designed for film enthusiasts. Built as a high-performance web application, FrameCut combines rich media exploration, automated catalog synchronization, personalized film journaling, and deep-dive director archives into one unified experience.

Operating as a live, fully responsive production platform, FrameCut features a sleek dark-mode aesthetic, fluid micro-interactions, and continuous data pipelines that keep thousands of titles, ratings, and cast networks automatically updated.

---

## 🍿 Platform Experience & Features

### 🎬 Immersive Cinema Discovery & Browsing
- **Fluid Interactive Catalog:** Browse through New Releases, Trending Cinema, Top Rated classics, and Hidden Gems with seamless, deterministic infinite scrolling.
- **Rich Media Previews:** Interactive titles provide instant previews with ambient backdrop blur, key art, rating badges, genre tags, and synopses with one-click direct access.
- **Radiant Genre Exploration:** Curated visual category hubs across 20+ genres with individualized radiant color themes, icons, and ambient lighting.

### ⚡ Sub-15ms Live Search
- **Instant Local Autocomplete:** Search queries check the indexed database first, delivering matching titles and poster thumbnails in **5–15ms**.
- **Parallel Real-Time Discovery:** For newly released or regional films not yet in the local database, parallel non-blocking WebClient requests query external registries in real time and store them on the fly.
- **Zero-Latency Typing:** Uses frontend `AbortController` and debounced event dispatching to eliminate request lag.

### 📖 Personal Cinema Diary & Watchlists
- Rate films and television series on 5-star and 10-point scales.
- Write community reviews with built-in **Spoiler Warning Masks** that viewers can toggle to reveal.
- Curate and track a personalized cinema watchlist linked to user accounts.

### 🌌 The Cinematic Director Universe
- Dedicated hub to explore the filmographies of legendary directors (Christopher Nolan, Quentin Tarantino, Hayao Miyazaki, Karthik Subbaraj, and more).
- Visualizes recurring actor collaborations, career timelines, and directorial styles in an interactive network canvas.

### 🎭 Mood Matcher
- Personalized recommendation engine matching curated cinema selections to the viewer's current emotional vibe (*Thrilled*, *Emotional*, *Mind-Blown*, *Feel-Good*, *Nostalgic*).

### 🛡️ Production Security Architecture
- Dual credential authentication: sign in via either **Username or Email**.
- BCrypt password hashing with adaptive salt work factors.
- Secure session management with `HttpOnly`, `SameSite=Lax`, and SSL/TLS protection.
- Hardened against unauthorized privilege escalation and brute-force attacks.

---

## ⚙️ Automated Backend Architecture & Data Pipeline

```
 [User Browser] 
       │
       ▼
 [Spring Security Filter Chain] (BCrypt, Session Cookies, HTTPS)
       │
       ▼
 [Controllers] ──► [Service Layer] ──► [PostgreSQL Database] (Local Cache)
                          │
                          ▼ (Reactive WebClient)
                   [TMDB REST API]
```

- **Continuous 6-Hour Automated Synchronization:** A persistent background scheduler (`TmdbSyncScheduler`) runs every 6 hours, pulling the latest weekly trending cinema (`/trending/movie/week`, `/trending/tv/week`) and current theatrical/OTT releases (`/movie/now_playing`, `/tv/on_the_air`).
- **Persistent Database Sync State:** Catalog pagination progress is tracked directly in the PostgreSQL `sync_state` table, ensuring the ingest pipeline continuously advances and safely recycles without losing state across deployments.
- **Deterministic Infinite Scrolling:** Multi-layered SQL sorting ensures zero duplicate records during continuous infinite scrolling.

---

## 🛠️ Technology Stack

| Layer | Technology |
|---|---|
| **Backend Framework** | Java 17, Spring Boot 3.2 (Web, Security, Data JPA, Scheduling, Validation) |
| **HTTP Client** | Spring WebFlux `WebClient` (non-blocking reactive consumer) |
| **Database** | PostgreSQL 15, Hibernate ORM / JPA |
| **Frontend** | Thymeleaf 3, HTML5, CSS3 (`Plus Jakarta Sans` & `Bebas Neue`), Vanilla ES6+ JavaScript |
| **Infrastructure** | Render Cloud Platform, Docker, 24/7 Availability Monitoring |

---

<p align="center">
  <em>Copyright © 2026 FrameCut. All rights reserved.</em><br>
  <strong>Bespoke production web application built for cinema lovers.</strong>
</p>
