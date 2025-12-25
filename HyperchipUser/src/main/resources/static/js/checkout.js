// checkout.js – COD + Razorpay
(function () {
  'use strict';

const endpoints = {
  addresses: (userId) => `/user/api/proxy/addresses/${encodeURIComponent(userId)}`,
  cart: (userId) => `/user/api/proxy/cart/${encodeURIComponent(userId)}`,
  placeOrder: () => '/user/api/proxy/checkout/place',
  createRazor: (appOrderId) => `/user/api/proxy/payment/create/${encodeURIComponent(appOrderId)}`,
  verifyRazor: (appOrderId) => `/user/api/proxy/payment/verify/${encodeURIComponent(appOrderId)}`,

  // ✅ correct proxy URL (no "order/api" here)
  deliveryCharge: (pinCode) => `/user/api/proxy/delivery/charge?pinCode=${encodeURIComponent(pinCode)}`
};





  // ===== Elements
  const el = {
    userId: document.getElementById('page-user-id'),
    userEmail: document.getElementById('page-user-email'),
    addressesArea: document.getElementById('addresses-area'),
    itemsArea: document.getElementById('checkout-items-area'),
    placeBtn: document.getElementById('place-order-btn') || document.getElementById('placeBtn'),
    feedback: document.getElementById('place-order-feedback'),
    subtotalEl: document.getElementById('cart-subtotal'),
    taxEl: document.getElementById('cart-tax'),
    shippingEl: document.getElementById('cart-shipping'),
    totalEl: document.getElementById('cart-total'),
  };

  // ===== Currency
  const CURRENCY = (window.APP_CURRENCY || 'INR').toUpperCase(); // set ₹ here if needed
  const CURRENCY_SYMBOL = (window.APP_CURRENCY_SYMBOL || (CURRENCY + ' '));

  // ===== Coupon Integration: Expose data for coupon.js =====
  // This block exposes the last total, coupon code, and discount amount so the
  // coupon.js script can update them. It also exposes updateSummary for reuse.
  window.checkoutData = {
    // getter/setter for the last computed total (including coupons)
    get lastTotal() { return lastComputedTotalDecimal; },
    set lastTotal(val) { lastComputedTotalDecimal = val; },
    // currently applied coupon code (if any)
    couponCode: null,
    // discount amount applied by the coupon
    discountAmount: 0,
    // expose updateSummary so coupon.js can recompute totals when removing a coupon
 updateSummary: function(subtotal) { return updateSummary(subtotal); }

  };
  // ===== End Coupon Integration =====

let currentCart = null;
let lastComputedTotalDecimal = 0.00;
let lastSubtotalDecimal = 0.00;
let addressesById = {};
  const userId = el.userId ? (el.userId.value || null) : null;

  // ===== CSRF headers (Spring)
  function getCsrfHeaders() {
    const headers = {};
    const tokenMeta = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');
    if (tokenMeta && headerMeta) {
      headers[headerMeta.getAttribute('content')] = tokenMeta.getAttribute('content');
    } else {
      const token = document.querySelector('meta[name="csrf-token"]')?.getAttribute('content');
      if (token) headers['X-CSRF-TOKEN'] = token;
    }
    return headers;
  }

  // ===== helpers
  function formatMoney(val) {
    return CURRENCY_SYMBOL + Number(val || 0).toFixed(2);
  }
  async function safeJson(response) {
    if (!response) return null;
    const txt = await response.text();
    try { return JSON.parse(txt); } catch { return txt || null; }
  }
  function escapeHtml(unsafe) {
    if (unsafe === null || unsafe === undefined) return '';
    return String(unsafe)
      .replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;').replaceAll("'", '&#039;');
  }
  function showFeedback(msg, isError = true) {
    if (!el.feedback) return;
    el.feedback.innerText = msg || '';
    el.feedback.style.display = msg ? 'block' : 'none';
    el.feedback.classList.remove('alert-info', 'alert-danger', 'alert');
    el.feedback.classList.add('alert', isError ? 'alert-danger' : 'alert-info');
  }
  function setButtonLoading(on, text) {
    if (!el.placeBtn) return;
    el.placeBtn.disabled = !!on;
    if (on) {
      el.placeBtn.dataset.orig = el.placeBtn.innerText;
      el.placeBtn.innerText = text || 'Processing...';
    } else if (el.placeBtn.dataset.orig) {
      el.placeBtn.innerText = el.placeBtn.dataset.orig;
      delete el.placeBtn.dataset.orig;
    }
  }
// ===== Addresses
async function loadAddresses() {
  if (!el.addressesArea || !userId) return;
  try {
    const res = await fetch(endpoints.addresses(userId), { credentials: 'same-origin' });
    if (!res.ok) {
      el.addressesArea.innerHTML = '<div class="alert alert-warning w-100">No addresses found.</div>';
      return;
    }
    const list = await res.json();
    if (!Array.isArray(list) || list.length === 0) {
      el.addressesArea.innerHTML = '<div class="alert alert-info w-100">You have no saved addresses. Add one from profile.</div>';
      return;
    }

    // 🔹 keep them in a map for later (shipping)
    addressesById = {};
    list.forEach(a => {
      if (!a || a.id == null) return;
      addressesById[String(a.id)] = a;
    });

    el.addressesArea.innerHTML = list.map(a => `
      <div class="col-12 address-card card p-3 address-item mb-2" data-id="${a.id}">
        <div class="d-flex align-items-start">
          <div class="me-3">
            <input class="form-check-input" type="radio" name="address" ${a.isDefault ? 'checked' : ''} value="${a.id}">
          </div>
          <div class="flex-grow-1">
            <strong>${escapeHtml(a.label || 'Address')}</strong>
            ${a.isDefault ? '<span class="badge bg-success ms-2">Default</span>' : ''}
            <div class="text-muted small mt-1">${escapeHtml([a.addressLine1, a.addressLine2, a.city, a.state, a.pincode].filter(Boolean).join(', '))}</div>
          </div>
        </div>
      </div>
    `).join('');

    // 🔹 Add click events for selecting an address
    document.querySelectorAll('.address-card').forEach(card => {
      card.addEventListener('click', function (ev) {
        if (ev.target.tagName.toLowerCase() === 'button' || ev.target.tagName.toLowerCase() === 'a') return;
        const r = this.querySelector('input[type=radio]');
        if (r) {
          r.checked = true;
          // 🔹 recalc shipping when address changes
          updateSummary(lastSubtotalDecimal).catch(console.error);
        }
      });
    });

    // 🔹 EXACT PLACE TO ADD THIS LINE
    // Recalculate summary once addresses are loaded (default address)
    updateSummary(lastSubtotalDecimal).catch(console.error);


  } catch (err) {
    console.error('loadAddresses error', err);
    el.addressesArea.innerHTML = '<div class="alert alert-danger w-100">Error loading addresses</div>';
  }
}

  // ===== Cart / Summary
  async function loadCart() {
    if (!el.itemsArea || !userId) return;
    try {
      const res = await fetch(endpoints.cart(userId), { credentials: 'same-origin' });
      if (!res.ok) {
        el.itemsArea.innerHTML = '<div class="alert alert-danger">Cannot load cart</div>';
        updateSummary(0.00);
        return;
      }
      const cart = await res.json();
      currentCart = cart;

      if (!cart.items || cart.items.length === 0) {
        el.itemsArea.innerHTML = '<div class="alert alert-info">Your cart is empty.</div>';
        updateSummary(0.00);
        return;
      }

      el.itemsArea.innerHTML = cart.items.map(it => `
        <div class="d-flex align-items-center border rounded p-2 mb-2">
          <img src="${escapeHtml(it.productImage || '/img/default-product.png')}" class="product-thumb me-3" alt="">
          <div class="flex-grow-1">
            <div class="fw-bold">${escapeHtml(it.productTitle || ('Product #' + it.productId))}</div>
            <div class="text-muted small">Unit ${formatMoney(it.unitPrice)} • Qty ${it.quantity}</div>
          </div>
          <div class="text-end">
            <div class="fw-bold">${formatMoney(it.total || (it.unitPrice * it.quantity))}</div>
          </div>
        </div>
      `).join('');

      const subtotal = (typeof cart.subtotal === 'number') ? cart.subtotal :
        cart.items.reduce((s, it) => s + (Number(it.total || (it.unitPrice * it.quantity)) || 0), 0);

    updateSummary(subtotal).catch(console.error);

    } catch (err) {
      console.error('loadCart error', err);
      el.itemsArea.innerHTML = '<div class="alert alert-danger">Error loading cart</div>';
      updateSummary(0.00);
    }
  }



  // ===== Helpers for location-based shipping =====

  function getSelectedAddress() {
    const sel = document.querySelector('input[name="address"]:checked');
    if (!sel) return null;
    const id = sel.value;
    return addressesById[id] || null;
  }

async function fetchBackendShipping(addr) {
  if (!addr) return 0.0;

  const pin = (addr.pincode ?? addr.pinCode ?? '').toString().trim();
  if (!pin) return 0.0;

  try {
    const url = endpoints.deliveryCharge(pin);
    console.log('📦 delivery charge url:', url);

    const res = await fetch(url, { credentials: 'same-origin' });

    // ✅ safe JSON parse
    const text = await res.text();
    let data = null;
    try { data = JSON.parse(text); } catch { data = null; }

    if (!res.ok) {
      console.warn('❌ delivery charge API failed:', res.status, text);
      return 0.0;
    }

    return Number(data?.deliveryCharge ?? data?.charge ?? 0) || 0.0;
  } catch (e) {
    console.error('❌ fetchBackendShipping error:', e);
    return 0.0;
  }
}


async function updateSummary(subtotalDecimal) {

  // 🔹 store raw subtotal separately
  lastSubtotalDecimal = Number(subtotalDecimal || 0);

  // 5% tax on subtotal
  const tax = +(lastSubtotalDecimal * 0.05).toFixed(2);

  // 🔹 Address-based shipping
  const selectedAddress = getSelectedAddress();
 let shipping = await fetchBackendShipping(selectedAddress);

 if (lastSubtotalDecimal >= 1500) {
   shipping = 0.0;
 }


  // 🔹 Same rule as backend: free shipping above 1500


    // ===== Coupon Integration: adjust total with discount =====
    let total = +(lastSubtotalDecimal + tax + shipping).toFixed(2);
    if (window.checkoutData && window.checkoutData.discountAmount) {
      total = +(total - Number(window.checkoutData.discountAmount || 0)).toFixed(2);
    }
    // ===== End Coupon Integration =====

    // 🔹 update DOM
    if (el.subtotalEl) el.subtotalEl.innerText = formatMoney(lastSubtotalDecimal);
    if (el.taxEl) el.taxEl.innerText = formatMoney(tax);
    if (el.shippingEl) el.shippingEl.innerText = shipping === 0 ? 'Free' : formatMoney(shipping);
    if (el.totalEl) el.totalEl.innerText = formatMoney(total);

    // 🔒 COD LIMIT: same logic as before, based on final total
    const codRadio   = document.getElementById('pay-cod');
    const codLabel   = document.querySelector('label[for="pay-cod"]');
    const codWarning = document.getElementById('cod-limit-warning');

    if (codRadio) {
      if (total > 1000) {
        if (codRadio.checked) {
          const alt = document.querySelector('input[name="paymentMethod"]:not(#pay-cod)');
          if (alt) alt.checked = true;
        }
        codRadio.disabled = true;
        if (codLabel)   codLabel.classList.add('text-muted');
        if (codWarning) codWarning.style.display = 'block';
      } else {
        codRadio.disabled = false;
        if (codLabel)   codLabel.classList.remove('text-muted');
        if (codWarning) codWarning.style.display = 'none';
      }
    }

    // 🔹 keep final total for coupon + payload
    lastComputedTotalDecimal = total;
  }




// ===== Improved placeAppOrder with clear error handling (COD + WALLET + Razorpay)
async function placeAppOrder(payload) {
  const headers = Object.assign(
    { 'Content-Type': 'application/json' },
    getCsrfHeaders()
  );

const res = await fetch('/user/api/proxy/checkout/place', {
  method: 'POST',
  headers,
  credentials: 'same-origin',
  body: JSON.stringify(payload)
});



  const body = await safeJson(res);

  // ⚠️ If backend returned success:false (wallet insufficient etc.)
  if (body && body.success === false) {
    throw new Error(body.message || 'Order could not be placed');
  }

  // ⚠️ If backend returned non-200
  if (!res.ok) {
    throw new Error(
      typeof body === 'string'
        ? body
        : body?.message || 'Order creation failed'
    );
  }

  return body;
}

  // ===== Razorpay: ask server to create Razorpay order
  // ask server to create Razorpay order (GET → computes amount)
// ===== Razorpay: ask server to create Razorpay order
// ask server to create Razorpay order (POST → computes amount)
async function createRazorOrderOnServer(appOrderId) {
  // For GET we don't need a JSON body
  const res = await fetch(endpoints.createRazor(appOrderId), {
    method: 'GET',
    credentials: 'same-origin'
    // you can add CSRF headers if your user-service requires it for GET,
    // but usually CSRF is only checked for POST/PUT/DELETE.
  });

  if (!res.ok) {
    const body = await safeJson(res);
    throw new Error(typeof body === 'string' ? body : (body?.message || 'Failed to create order on payment service'));
  }

  return await res.json();
}




async function verifyRazorOnServer(appOrderId, payload) {
  const headers = Object.assign({ 'Content-Type': 'application/json' }, getCsrfHeaders());
const res = await fetch(endpoints.verifyRazor(appOrderId), {
  method: 'POST',
  headers,
  credentials: 'same-origin',
  body: JSON.stringify({
    orderId: appOrderId,
    razorpay_payment_id: payload.razorpay_payment_id,
    razorpay_order_id: payload.razorpay_order_id,
    razorpay_signature: payload.razorpay_signature
  })
});


  if (!res.ok) {
    const body = await safeJson(res);
    throw new Error(typeof body === 'string' ? body : (body?.message || 'Verification failed'));
  }
  return await res.json();
}

  // ===== Event: Place Order
  async function onPlaceBtnClick(ev) {
    ev.preventDefault();
    showFeedback('', false);
    if (!userId) return alert('Please login');

    const selAddr = document.querySelector('input[name="address"]:checked');
    if (!selAddr) return alert('Select delivery address');

    const paymentMethod = document.querySelector('input[name="paymentMethod"]:checked')?.value || 'COD';

    const payload = {
      userId: parseInt(userId, 10),
      addressId: parseInt(selAddr.value, 10),
      paymentMethod,
      userEmail: el.userEmail?.value || null,
      // ===== Coupon Integration: use updated total with discount =====
      totalAmount: window.checkoutData ? window.checkoutData.lastTotal : lastComputedTotalDecimal,
      // ===== End Coupon Integration =====
      items: currentCart?.items || []
    };
    // ===== Coupon Integration: include coupon info in payload =====
    if (window.checkoutData && window.checkoutData.couponCode) {
      payload.couponCode = window.checkoutData.couponCode;
      payload.discountAmount = window.checkoutData.discountAmount;
    }
    // ===== End Coupon Integration =====

    try {
      setButtonLoading(true, 'Placing order...');
      const placed = await placeAppOrder(payload);
      const appOrderId = placed?.orderId || placed?.id;
      const appOrderNumber = placed?.orderNumber || placed?.orderNo;

      if (!appOrderId) throw new Error('Invalid order id from server');

           // COD & WALLET → backend already handled payment logic.
           // If placeOrder() succeeded, show success page.
           if (paymentMethod === 'COD' || paymentMethod === 'WALLET') {
             window.location.href =
               '/user/order/success-page?orderNumber=' + encodeURIComponent(appOrderNumber || '');
             return;
           }


// 🧾 Fake Razorpay flow (for learning/demo only)
if (paymentMethod === 'RAZORPAY' || paymentMethod === 'ONLINE' || paymentMethod === 'PAY_ONLINE') {
  const razor = await createRazorOrderOnServer(appOrderId);

  // --- If server told us this is a fake/test order, do NOT open Razorpay widget ---
  if (razor && razor.fake) {
    try {
      const fakeVerify = {
        razorpay_payment_id: 'pay_test_' + Date.now(),
        razorpay_order_id: razor.orderId || ('order_test_' + appOrderId),
        razorpay_signature: 'fake_signature'
      };

      // call your backend verify endpoint
      await verifyRazorOnServer(appOrderId, fakeVerify);

      // show success page
      // 2. Fake Razorpay success
      window.location.href =
        '/user/order/success-page?orderNumber=' + encodeURIComponent(appOrderNumber || '');
      return;
    } catch (e) {
      console.error('fake verify error', e);
    // 4. Fake Razorpay failure (catch)
    window.location.href =
      '/user/order/payment-failed?orderNumber=' + encodeURIComponent(appOrderNumber || '');
      return;
    }
  }

  // --- otherwise (real/test key Razorpay mode) continue normal flow ---
  const rzpOptions = {

    key: razor.razorpayKey,                                // from backend
    order_id: razor.razorpayOrderId,                       // from backend
    amount: razor.amountMinor || Math.round(razor.amount * 100), // use amountMinor if available
    currency: razor.currency || "USD",                     // use backend currency if set
    name: razor.name || 'Hyperchip',
    description: razor.description || ('Order ' + (appOrderNumber || appOrderId)),
    prefill: razor.prefill || { email: el.userEmail?.value || '' },
    notes: { appOrderId: String(appOrderId), appOrderNumber: String(appOrderNumber || '') },


 handler: async function (response) {
   try {
     const verifyPayload = {
       razorpay_payment_id: response.razorpay_payment_id,
       razorpay_order_id: response.razorpay_order_id,
       razorpay_signature: response.razorpay_signature
     };

     // If this returns 200, treat it as success
     await verifyRazorOnServer(appOrderId, verifyPayload);

     window.location.href =
       '/user/order/success-page?orderNumber=' + encodeURIComponent(appOrderNumber || '');
   } catch (e) {
     window.location.href =
       '/user/order/payment-failed?orderNumber=' + encodeURIComponent(appOrderNumber || '');
   }
 },


  modal: {
    ondismiss: function () {
      window.location.href =
        '/user/order/payment-failed?orderNumber=' + encodeURIComponent(appOrderNumber || '');
    }
  },
  theme: { color: '#F37254' }
};



  // --- open Razorpay popup only if not fake ---
  if (typeof Razorpay === 'undefined') {
    throw new Error('Razorpay SDK not loaded');
  }
  const rzp = new Razorpay(rzpOptions);
  rzp.open();
  setButtonLoading(false);
  return;
}


      // Unknown payment
      showFeedback('Selected payment method is not supported.', true);
      setButtonLoading(false);

    } catch (err) {
      console.error('place/order error', err);
      const msg = (err && err.message) ? err.message : 'Failed to place order';
      // your screenshot showed “Upstream service error”: that’s a 5xx from the proxy.
      // This will surface that message here too.
      showFeedback(msg, true);
      setButtonLoading(false);
    }
  }

  // ===== init
  (function init() {
    if (el.placeBtn) el.placeBtn.addEventListener('click', onPlaceBtnClick);
    if (userId) {
      loadAddresses().catch(e => console.warn('addresses load failed', e));
      loadCart().catch(e => console.warn('cart load failed', e));
    }
  })();
})();
