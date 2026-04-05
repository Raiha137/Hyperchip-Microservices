(function () {
  'use strict';

  try {
    document.addEventListener('DOMContentLoaded', function () {
      const cards = Array.from(document.querySelectorAll('.product-card'));
      if (!cards.length) return;

      cards.forEach(card => {
        try {
          const img = card.querySelector('.product-img');
          if (!img) return;

          const namesCsv = card.getAttribute('data-images') || '';
          const names = namesCsv.split(',').map(s => s.trim()).filter(Boolean);
          const base = (card.getAttribute('data-master-base-url') || '').replace(/\/+$/, '');

          const urls = names
            .map(name => (base ? (base + '/uploads/products/' + name) : name))
            .filter(Boolean);

          let timer = null;
          let idx = 0;
          const originalSrc = img.getAttribute('src') || '/img/default-product.png';

          function safeSetSrc(url) {
            if (!url) return;
            img.style.transition = 'opacity 220ms ease';
            img.style.opacity = '0.02';

            setTimeout(() => {
              try {
                img.src = url;
              } catch (_) {}
              img.style.opacity = '1';
            }, 220);
          }

          function startCycle() {
            if (urls.length < 2) return;
            if (timer) return;

            idx = 0;
            timer = setInterval(() => {
              idx = (idx + 1) % urls.length;
              safeSetSrc(urls[idx]);
            }, 700);
          }

          function stopCycle() {
            if (timer) {
              clearInterval(timer);
              timer = null;
            }

            if (urls.length) {
              safeSetSrc(urls[0]);
            } else {
              safeSetSrc(originalSrc);
            }
          }

          card.addEventListener('mouseenter', function () {
            card.classList.add('hc-hovered');
            startCycle();
          });

          card.addEventListener('mouseleave', function () {
            card.classList.remove('hc-hovered');
            stopCycle();
          });

          const wishBtn = card.querySelector('.hc-wishlist');
          if (wishBtn) {
            wishBtn.addEventListener('click', function (e) {
              e.stopPropagation();
              wishBtn.classList.toggle('active');
              wishBtn.style.transform = 'scale(1.05)';
              setTimeout(() => {
                wishBtn.style.transform = '';
              }, 220);
            });
          }

          const cartBtn = card.querySelector('.hc-addcart');
          if (cartBtn) {
            cartBtn.addEventListener('click', function (e) {
              e.stopPropagation();
              const originalHtml = cartBtn.innerHTML;
              cartBtn.innerHTML = '<i class="fa fa-check"></i>';
              setTimeout(() => {
                cartBtn.innerHTML = originalHtml;
              }, 1000);
            });
          }

          img.addEventListener('error', function () {
            if (!img.dataset.fallback) {
              img.dataset.fallback = 'true';
              img.src = '/img/default-product.png';
            }
          });
        } catch (inner) {
          console.warn('card init failed', inner);
        }
      });
    });
  } catch (e) {
    console.warn('home.js failed:', e);
  }
})();