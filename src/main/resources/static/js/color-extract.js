// ===== MOVIE DETAIL - COLOR EXTRACT =====
// Uses ColorThief to extract dominant color from backdrop

(function() {
  const backdropEl = document.getElementById('detail-backdrop');
  const accentOverlay = document.getElementById('detail-accent-overlay');
  const backdropUrl = window.movieBackdropUrl;

  if (!backdropUrl || !accentOverlay) return;

  // Load ColorThief
  const script = document.createElement('script');
  script.src = 'https://cdnjs.cloudflare.com/ajax/libs/color-thief/2.3.0/color-thief.umd.js';
  script.onload = () => {
    const img = new Image();
    img.crossOrigin = 'Anonymous';
    img.src = backdropUrl.replace('/original/', '/w300/'); // Use smaller size for speed

    img.onload = () => {
      try {
        const thief = new ColorThief();
        const [r, g, b] = thief.getColor(img);

        // Apply extracted color as accent
        const rgb = `${r}, ${g}, ${b}`;
        accentOverlay.style.background =
          `radial-gradient(ellipse at top left, rgba(${rgb}, 0.3) 0%, transparent 60%)`;

        // Glow on rating
        const ratingEl = document.querySelector('.detail-tmdb-rating');
        if (ratingEl) {
          ratingEl.style.borderColor = `rgba(${rgb}, 0.4)`;
          ratingEl.style.background = `rgba(${rgb}, 0.1)`;
        }

        // Accent the type badge
        const badge = document.querySelector('.detail-type-badge');
        if (badge && isVibrant(r, g, b)) {
          badge.style.background = `rgb(${rgb})`;
        }

        // Smooth CSS variable transition
        document.documentElement.style.setProperty('--accent-r', r);
        document.documentElement.style.setProperty('--accent-g', g);
        document.documentElement.style.setProperty('--accent-b', b);
        document.documentElement.style.setProperty('--accent',
          `rgb(${r}, ${g}, ${b})`);

      } catch(e) {
        console.log('ColorThief failed, using default red accent');
      }
    };
  };
  document.head.appendChild(script);

  function isVibrant(r, g, b) {
    const max = Math.max(r, g, b);
    const min = Math.min(r, g, b);
    return (max - min) > 80; // Only use if actually colorful
  }
})();
