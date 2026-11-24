// ===== FRAMECUT MAIN JS =====

// Loading Screen
(function() {
  const screen = document.getElementById('loading-screen');
  if (!screen) return;

  // Has loading screen been shown already in this session?
  if (sessionStorage.getItem('fc_loaded')) {
    screen.style.display = 'none';
    document.body.style.overflow = '';
    return;
  }

  document.body.style.overflow = 'hidden';

  setTimeout(() => {
    screen.classList.add('loading-screen-exit');
    setTimeout(() => {
      screen.style.display = 'none';
      document.body.style.overflow = '';
      sessionStorage.setItem('fc_loaded', '1');
    }, 500);
  }, 2800);
})();

// Navbar scroll behavior
(function() {
  const nav = document.getElementById('navbar');
  if (!nav) return;

  const onScroll = () => {
    nav.classList.toggle('scrolled', window.scrollY > 50);
  };

  window.addEventListener('scroll', onScroll, { passive: true });
  onScroll();
})();

// Scroll-triggered section reveal
(function() {
  const sections = document.querySelectorAll('.section');
  if (!sections.length) return;

  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('visible');
        // Stagger card animations
        const cards = entry.target.querySelectorAll('.movie-card');
        cards.forEach((card, i) => {
          card.style.animationDelay = `${i * 0.05}s`;
        });
      }
    });
  }, { threshold: 0.08 });

  sections.forEach(s => observer.observe(s));
})();

// Hero backdrop slideshow
(function() {
  const slides = document.querySelectorAll('.hero-slide');
  if (slides.length < 2) return;

  let current = 0;
  slides[0].classList.add('active');

  setInterval(() => {
    slides[current].classList.remove('active');
    current = (current + 1) % slides.length;
    slides[current].classList.add('active');
  }, 6000);
})();

// Draggable horizontal scroll rows
document.querySelectorAll('.scroll-row').forEach(row => {
  let isDown = false, startX, scrollLeft;

  row.addEventListener('mousedown', e => {
    isDown = true;
    row.style.cursor = 'grabbing';
    startX = e.pageX - row.offsetLeft;
    scrollLeft = row.scrollLeft;
  });

  row.addEventListener('mouseleave', () => { isDown = false; row.style.cursor = 'grab'; });
  row.addEventListener('mouseup', () => { isDown = false; row.style.cursor = 'grab'; });
  row.addEventListener('mousemove', e => {
    if (!isDown) return;
    e.preventDefault();
    const x = e.pageX - row.offsetLeft;
    const walk = (x - startX) * 1.5;
    row.scrollLeft = scrollLeft - walk;
  });
});

// Row arrow buttons
document.querySelectorAll('.row-arrow').forEach(btn => {
  btn.addEventListener('click', () => {
    const row = btn.closest('.scroll-row-wrapper').querySelector('.scroll-row');
    const dir = btn.classList.contains('right') ? 1 : -1;
    row.scrollBy({ left: dir * 600, behavior: 'smooth' });
  });
});

// Live search / autocomplete
(function() {
  const input = document.getElementById('nav-search-input');
  const results = document.getElementById('autocomplete-results');
  if (!input || !results) return;

  let debounceTimer;
  let abortController = null;

  input.addEventListener('input', () => {
    const q = input.value.trim();
    clearTimeout(debounceTimer);

    if (abortController) {
      abortController.abort();
      abortController = null;
    }

    if (q.length < 2) {
      results.classList.remove('show');
      results.innerHTML = '';
      return;
    }

    debounceTimer = setTimeout(async () => {
      abortController = new AbortController();
      try {
        const res = await fetch(`/api/search/autocomplete?q=${encodeURIComponent(q)}`, {
          signal: abortController.signal
        });
        const data = await res.json();

        if (!data.length) {
          results.classList.remove('show');
          return;
        }

        results.innerHTML = data.map((item, i) => `
          <a href="/movies/${item.id}" class="autocomplete-item" style="animation-delay:${i*0.05}s">
            <img src="${item.poster}" alt="${item.title}" onerror="this.src='/images/no-poster.jpg'">
            <div class="autocomplete-item-info">
              <div class="autocomplete-item-title">${escapeHtml(item.title)}</div>
              <div class="autocomplete-item-meta">
                ${item.year || ''} ${item.genre ? '· ' + item.genre : ''}
                <span style="margin-left:auto">${item.type === 'tv' ? '📺' : '🎬'}</span>
              </div>
            </div>
            <span class="rating">★ ${item.rating ? item.rating.toFixed(1) : 'N/A'}</span>
          </a>
        `).join('');

        results.classList.add('show');
      } catch (e) {
        if (e.name !== 'AbortError') {
          console.error('Autocomplete error:', e);
        }
      }
    }, 200);
  });

  // Enter key → search page
  input.addEventListener('keydown', e => {
    if (e.key === 'Enter' && input.value.trim()) {
      window.location.href = `/search?q=${encodeURIComponent(input.value.trim())}`;
    }
    if (e.key === 'Escape') {
      results.classList.remove('show');
      input.blur();
    }
  });

  // Close on outside click
  document.addEventListener('click', e => {
    if (!input.contains(e.target) && !results.contains(e.target)) {
      results.classList.remove('show');
    }
  });
})();

// Infinite scroll
(function() {
  const grid = document.getElementById('infinite-grid');
  const trigger = document.getElementById('load-more-trigger');
  if (!grid || !trigger) return;

  let page = 0;
  let loading = false;
  let hasMore = true;

  const loadMore = async () => {
    if (loading || !hasMore) return;
    loading = true;
    trigger.innerHTML = '<div class="loading-dots"><span></span><span></span><span></span></div>';

    const params = window.infiniteScrollParams || {};

    try {
      const queryParams = new URLSearchParams({
        page: ++page,
        size: 20,
        ...(params.type && { type: params.type }),
        ...(params.genre && { genre: params.genre }),
        ...(params.section && { section: params.section }),
        ...(params.mood && { mood: params.mood })
      });

      const res = await fetch(`/api/movies?${queryParams}`);
      const data = await res.json();

      data.content.forEach((movie, i) => {
        const card = createMovieCard(movie, i);
        grid.appendChild(card);
      });

      hasMore = data.hasMore;
      trigger.innerHTML = hasMore ? '' : '<span style="opacity:0.3;letter-spacing:0.1em;font-size:12px">— END —</span>';
    } catch (e) {
      console.error('Infinite scroll error:', e);
      trigger.innerHTML = '';
    }
    loading = false;
  };

  const observer = new IntersectionObserver(entries => {
    if (entries[0].isIntersecting) loadMore();
  }, { rootMargin: '200px' });

  observer.observe(trigger);
})();

function createMovieCard(movie, index) {
  const card = document.createElement('a');
  card.href = `/movies/${movie.id}`;
  card.className = 'movie-card';
  card.style.animationDelay = `${(index % 20) * 0.04}s`;
  card.setAttribute('data-id', movie.id);
  card.setAttribute('data-title', movie.title || '');
  card.setAttribute('data-poster', movie.poster || '');
  card.setAttribute('data-backdrop', movie.backdrop || movie.poster || '');
  card.setAttribute('data-rating', movie.rating ? movie.rating.toFixed(1) : 'N/A');
  card.setAttribute('data-year', movie.year || '');
  card.setAttribute('data-type', movie.type || 'movie');
  card.setAttribute('data-genres', movie.genre || '');
  card.setAttribute('data-overview', movie.overview || '');
  card.innerHTML = `
    <img class="movie-card-poster"
         src="${movie.poster}"
         alt="${escapeHtml(movie.title)}"
         loading="lazy"
         onerror="this.src='/images/no-poster.jpg'">
    <div class="movie-card-overlay">
      <div class="movie-card-title">${escapeHtml(movie.title)}</div>
      <div class="movie-card-meta">
        <span class="movie-card-rating">★ ${movie.rating ? movie.rating.toFixed(1) : 'N/A'}</span>
        <span>${movie.year || ''}</span>
        ${movie.type === 'tv' ? '<span>Series</span>' : ''}
      </div>
    </div>
    <div class="movie-card-bottom">
      <div class="movie-card-bottom-title">${escapeHtml(movie.title)}</div>
      <div class="movie-card-bottom-meta">${movie.year || ''} ${movie.genre ? '· ' + movie.genre.split(',')[0] : ''}</div>
    </div>
  `;
  return card;
}

// Star rating picker
(function() {
  const stars = document.querySelectorAll('.star-pick');
  const input = document.getElementById('rating-input');
  if (!stars.length || !input) return;

  stars.forEach((star, i) => {
    star.addEventListener('mouseenter', () => {
      stars.forEach((s, j) => s.classList.toggle('active', j <= i));
    });
    star.addEventListener('click', () => {
      input.value = i + 1;
      stars.forEach((s, j) => s.classList.toggle('active', j <= i));
    });
  });

  document.querySelector('.star-picker')?.addEventListener('mouseleave', () => {
    const val = parseInt(input.value) || 0;
    stars.forEach((s, j) => s.classList.toggle('active', j < val));
  });

  // Set initial value if editing
  const initial = parseInt(input.value) || 0;
  if (initial) stars.forEach((s, j) => s.classList.toggle('active', j < initial));
})();

// Page transition
document.querySelectorAll('a[href]').forEach(link => {
  if (link.hostname !== window.location.hostname) return;
  if (link.getAttribute('href').startsWith('#')) return;
  link.addEventListener('click', e => {
    // Just let normal navigation happen, CSS handles entrance
  });
});

// Add page-transition-enter class on load
document.addEventListener('DOMContentLoaded', () => {
  document.querySelector('.page-wrapper')?.classList.add('page-transition-enter');
});

// Prime Video / Netflix Style Hover Preview Controller
(function() {
  const hoverCard = document.getElementById('prime-hover-card');
  if (!hoverCard) return;

  const cardImg = document.getElementById('hover-card-img');
  const cardBgBlur = document.getElementById('hover-card-bg-blur');
  const cardBadge = document.getElementById('hover-card-badge');
  const cardTitle = document.getElementById('hover-card-title');
  const playBtn = document.getElementById('hover-card-play-btn');
  const infoBtn = document.getElementById('hover-card-info-btn');
  const cardRating = document.getElementById('hover-card-rating');
  const cardYear = document.getElementById('hover-card-year');
  const cardGenres = document.getElementById('hover-card-genres');
  const cardOverview = document.getElementById('hover-card-overview');

  let openTimer = null;
  let closeTimer = null;
  let activeCard = null;

  function showPreview(targetCard) {
    if (!targetCard) return;
    if (window.innerWidth <= 768) return;

    if (activeCard && activeCard !== targetCard) {
      activeCard.classList.remove('hover-hidden');
    }
    activeCard = targetCard;
    targetCard.classList.add('hover-hidden');

    const href = targetCard.getAttribute('href') || '#';
    const poster = targetCard.getAttribute('data-backdrop') ||
                   targetCard.getAttribute('data-poster') ||
                   targetCard.querySelector('.movie-card-poster')?.getAttribute('src') || '';
    const title = targetCard.getAttribute('data-title') ||
                  targetCard.querySelector('.movie-card-title')?.textContent ||
                  targetCard.querySelector('.movie-card-bottom-title')?.textContent || 'Film';
    let rating = targetCard.getAttribute('data-rating') ||
                 targetCard.querySelector('.movie-card-rating')?.textContent || '★ N/A';
    const year = targetCard.getAttribute('data-year') || '';
    const type = targetCard.getAttribute('data-type') || (targetCard.textContent.includes('Series') ? 'tv' : 'movie');
    let genres = targetCard.getAttribute('data-genres') ||
                 targetCard.querySelector('.movie-card-bottom-meta')?.textContent || '';
    const overview = targetCard.getAttribute('data-overview') ||
                     'Click explore to view full cast, trailer, and community reviews.';

    // Populate data
    cardImg.src = poster;
    if (cardBgBlur && poster) {
      cardBgBlur.style.backgroundImage = `url("${poster}")`;
    }
    cardTitle.textContent = title;
    cardBadge.textContent = (type && type.toLowerCase().includes('tv')) ? 'SERIES' : 'MOVIE';

    if (!rating.includes('★')) rating = '★ ' + rating;
    cardRating.textContent = rating;

    cardYear.textContent = year || '';
    genres = genres.replace(/^[·\s\d]+/, '').replace(/^·\s*/, '').trim();
    cardGenres.textContent = genres || 'Cinema';
    cardOverview.textContent = overview;

    playBtn.setAttribute('href', href);
    infoBtn.setAttribute('href', href);

    // Calculate dimensions & position over the hovered card (Prime Video scale)
    const rect = targetCard.getBoundingClientRect();
    const cardWidth = Math.min(Math.max(Math.round(rect.width * 1.95), 380), 420);
    const cardHeight = Math.round(cardWidth * 1.12);

    let left = rect.left - (cardWidth - rect.width) / 2;
    let top = rect.top - (cardHeight - rect.height) / 2;

    // Viewport collision bounds with safe padding
    if (left < 16) {
      left = 16;
    } else if (left + cardWidth > window.innerWidth - 16) {
      left = window.innerWidth - cardWidth - 16;
    }

    if (top < 72) {
      top = 72;
    } else if (top + cardHeight > window.innerHeight - 16) {
      top = Math.max(72, window.innerHeight - cardHeight - 16);
    }

    hoverCard.style.left = `${left}px`;
    hoverCard.style.top = `${top}px`;
    hoverCard.style.width = `${cardWidth}px`;

    hoverCard.classList.add('visible');
    hoverCard.setAttribute('aria-hidden', 'false');
  }

  function hidePreview() {
    hoverCard.classList.remove('visible');
    hoverCard.setAttribute('aria-hidden', 'true');
    if (activeCard) {
      activeCard.classList.remove('hover-hidden');
      activeCard = null;
    }
  }

  // Event Delegation for hover over .movie-card
  document.addEventListener('mouseover', (e) => {
    if (window.innerWidth <= 768) return;
    const card = e.target.closest('.movie-card');
    if (card) {
      clearTimeout(closeTimer);
      if (activeCard === card) return;
      clearTimeout(openTimer);
      openTimer = setTimeout(() => {
        showPreview(card);
      }, 200); // 200ms hover debounce
    } else if (e.target.closest('#prime-hover-card')) {
      clearTimeout(closeTimer);
    }
  });

  document.addEventListener('mouseout', (e) => {
    if (window.innerWidth <= 768) return;
    const card = e.target.closest('.movie-card');
    if (card) {
      clearTimeout(openTimer);
      closeTimer = setTimeout(() => {
        hidePreview();
      }, 150);
    } else if (e.target.closest('#prime-hover-card')) {
      closeTimer = setTimeout(() => {
        hidePreview();
      }, 150);
    }
  });

  hoverCard.addEventListener('mouseenter', () => {
    clearTimeout(closeTimer);
  });
  hoverCard.addEventListener('mouseleave', () => {
    closeTimer = setTimeout(() => {
      hidePreview();
    }, 150);
  });

  // Clicking anywhere on the hover card navigates directly to the movie/show page
  hoverCard.addEventListener('click', (e) => {
    const targetAnchor = e.target.closest('a');
    if (targetAnchor && targetAnchor.getAttribute('href') && targetAnchor.getAttribute('href') !== '#') {
      return; // Link handles standard click
    }
    const href = playBtn.getAttribute('href');
    if (href && href !== '#' && href !== '') {
      window.location.href = href;
    }
  });

  window.addEventListener('scroll', () => {
    if (activeCard) hidePreview();
  }, { passive: true });

  document.querySelectorAll('.scroll-row').forEach(row => {
    row.addEventListener('scroll', () => {
      if (activeCard) hidePreview();
    }, { passive: true });
  });
})();

// Mobile Navigation Toggle
(function() {
  const hamburger = document.getElementById('nav-hamburger');
  const navLinks = document.querySelector('.nav-links');
  if (!hamburger || !navLinks) return;

  hamburger.addEventListener('click', (e) => {
    e.stopPropagation();
    hamburger.classList.toggle('open');
    navLinks.classList.toggle('open');
  });

  // Close mobile menu when clicking outside
  document.addEventListener('click', (e) => {
    if (!navLinks.contains(e.target) && !hamburger.contains(e.target) && navLinks.classList.contains('open')) {
      hamburger.classList.remove('open');
      navLinks.classList.remove('open');
    }
  });

  // Mobile Genre dropdown toggle
  const genreToggle = document.querySelector('.nav-dropdown-toggle');
  const genreDropdown = document.querySelector('.nav-dropdown');
  if (genreToggle && genreDropdown) {
    genreToggle.addEventListener('click', (e) => {
      if (window.innerWidth <= 768) {
        e.preventDefault();
        e.stopPropagation();
        genreDropdown.classList.toggle('active');
      }
    });
  }

  // Mobile user profile dropdown toggle
  const navUser = document.querySelector('.nav-user');
  if (navUser) {
    navUser.addEventListener('click', (e) => {
      if (window.innerWidth <= 768) {
        e.stopPropagation();
        navUser.classList.toggle('active');
      }
    });
  }
})();

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
