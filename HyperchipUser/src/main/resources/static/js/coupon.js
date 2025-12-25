// coupon.js – handles applying and removing coupon codes
(function () {
  'use strict';

  // Elements
  const userIdEl = document.getElementById('page-user-id');
  const userId = userIdEl ? parseInt(userIdEl.value, 10) : null;
  const couponInput   = document.getElementById('coupon-code-input');
  const applyBtn      = document.getElementById('apply-coupon-btn');
  const removeBtn     = document.getElementById('remove-coupon-btn');
  const feedbackEl    = document.getElementById('coupon-feedback');
  const discountRow   = document.getElementById('coupon-discount-row');
  const appliedCodeEl = document.getElementById('applied-coupon-code');
  const discountEl    = document.getElementById('cart-discount');
  const totalEl       = document.getElementById('cart-total');

  // API endpoints (proxied through user service)
  const endpoints = {
    apply:  () => '/user/api/proxy/coupons/apply',
    remove: () => '/user/api/proxy/coupons/remove'
  };

  // Fetch CSRF headers (copied from checkout.js)
  function getCsrfHeaders() {
    const headers = {};
    const tokenMeta  = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');
    if (tokenMeta && headerMeta) {
      headers[headerMeta.getAttribute('content')] = tokenMeta.getAttribute('content');
    } else {
      const token = document.querySelector('meta[name="csrf-token"]')?.getAttribute('content');
      if (token) headers['X-CSRF-TOKEN'] = token;
    }
    return headers;
  }

  // Safely parse JSON
  async function safeJson(res) {
    const text = await res.text();
    try { return JSON.parse(text); } catch { return text || null; }
  }

  // Format money according to global settings
  function formatMoney(val) {
    const curr = (window.APP_CURRENCY || 'INR').toUpperCase();
    const sym  = (window.APP_CURRENCY_SYMBOL || (curr + ' '));
    return sym + Number(val || 0).toFixed(2);
  }

  // Display feedback
  function showFeedback(msg, isError = true) {
    if (!feedbackEl) return;
    feedbackEl.innerText  = msg || '';
    feedbackEl.style.display = msg ? 'block' : 'none';
    feedbackEl.classList.remove('text-success','text-danger');
    feedbackEl.classList.add(isError ? 'text-danger' : 'text-success');
  }

  // Compute new total including tax, shipping and discount
  function computeTotal(subtotal, discount) {
    const tax      = +(subtotal * 0.05).toFixed(2);
    const shipping = subtotal > 200 ? 0 : 15;
    return +(subtotal + tax + shipping - discount).toFixed(2);
  }

  // Apply coupon
  async function applyCoupon() {
    const code = couponInput?.value.trim() || '';
    if (!code) { showFeedback('Please enter a coupon code.', true); return; }
    if (!userId) { showFeedback('Please login to apply a coupon.', true); return; }
    if (window.checkoutData?.couponCode === code) {
      showFeedback('This coupon is already applied.', true); return;
    }
    try {
      const payload = {
        userId:    userId,
        couponCode: code,
        totalAmount: window.checkoutData ? window.checkoutData.lastTotal : 0
      };
      const res = await fetch(endpoints.apply(), {
        method: 'POST',
        credentials: 'same-origin',
        headers: Object.assign({ 'Content-Type':'application/json' }, getCsrfHeaders()),
        body: JSON.stringify(payload)
      });
      const data = await safeJson(res);
      if (!res.ok || !data || data.success === false) {
        showFeedback(data?.message || 'Failed to apply coupon', true);
        return;
      }
      // update checkoutData
      const discount = Number(data.discountAmount || 0);
      window.checkoutData.couponCode    = code;
      window.checkoutData.discountAmount = discount;
      // recompute total
      const subtotal = parseFloat(document.getElementById('cart-subtotal').innerText.replace(/[^\d.]/g,'')) || 0;
      const newTotal = computeTotal(subtotal, discount);
      window.checkoutData.lastTotal = newTotal;
      // update DOM
      discountRow?.classList.remove('d-none');
      appliedCodeEl.innerText = code;
      discountEl.innerText    = formatMoney(discount);
      totalEl.innerText       = formatMoney(newTotal);
      applyBtn.classList.add('d-none');
      removeBtn.classList.remove('d-none');
      showFeedback('Coupon applied successfully!', false);
    } catch (err) {
      console.error('applyCoupon error', err);
      showFeedback(err?.message || 'Failed to apply coupon', true);
    }
  }

  // Remove coupon
  async function removeCoupon() {
    if (!window.checkoutData?.couponCode) {
      showFeedback('No coupon to remove.', true); return;
    }
    try {
      const res = await fetch(endpoints.remove(), {
        method: 'POST',
        credentials: 'same-origin',
        headers: Object.assign({ 'Content-Type':'application/json' }, getCsrfHeaders()),
        body: JSON.stringify({ userId })
      });
      // clear coupon locally regardless of response
      window.checkoutData.couponCode    = null;
      window.checkoutData.discountAmount = 0;
      // recompute totals without discount
      const subtotal = parseFloat(document.getElementById('cart-subtotal').innerText.replace(/[^\d.]/g,'')) || 0;
      if (window.checkoutData.updateSummary) {
        window.checkoutData.updateSummary(subtotal);
      }
      // reset UI
      discountRow?.classList.add('d-none');
      applyBtn.classList.remove('d-none');
      removeBtn.classList.add('d-none');
      appliedCodeEl.innerText = '';
      discountEl.innerText    = formatMoney(0);
      couponInput.value       = '';
      showFeedback('Coupon removed.', false);
    } catch (err) {
      console.error('removeCoupon error', err);
      showFeedback('Failed to remove coupon', true);
    }
  }

  // Attach listeners
  applyBtn?.addEventListener('click', (e) => { e.preventDefault(); applyCoupon(); });
  removeBtn?.addEventListener('click',(e) => { e.preventDefault(); removeCoupon(); });
})();
