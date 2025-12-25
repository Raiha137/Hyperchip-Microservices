// admin-ledger.js
(function () {
  const tbody = document.getElementById('ledger-tbody');
  const statusEl = document.getElementById('ledger-status');
  const btnFilter = document.getElementById('btn-ledger-filter');

  async function loadLedger() {
    if (!tbody) return;

    const fromDate = document.getElementById('fromDate').value;
    const toDate = document.getElementById('toDate').value;
    const entryType = document.getElementById('entryType').value;

    let url = '/api/admin/ledger';
    const params = [];
    if (fromDate) params.push('fromDate=' + encodeURIComponent(fromDate));
    if (toDate) params.push('toDate=' + encodeURIComponent(toDate));
    if (entryType) params.push('type=' + encodeURIComponent(entryType));
    if (params.length > 0) {
      url += '?' + params.join('&');
    }

    tbody.innerHTML = `
      <tr>
        <td colspan="8" class="text-center text-muted">Loading...</td>
      </tr>
    `;
    if (statusEl) statusEl.innerText = '';

    try {
      const res = await fetch(url, { credentials: 'same-origin' });
      if (!res.ok) {
        tbody.innerHTML = `
          <tr>
            <td colspan="8" class="text-center text-danger">
              Failed to load ledger
            </td>
          </tr>`;
        return;
      }
      const list = await res.json();
      render(list || []);
    } catch (e) {
      console.error(e);
      tbody.innerHTML = `
        <tr>
          <td colspan="8" class="text-center text-danger">
            Error loading ledger
          </td>
        </tr>`;
    }
  }

  function render(entries) {
    if (!entries || entries.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="8" class="text-center text-muted">
            No ledger entries found
          </td>
        </tr>`;
      if (statusEl) statusEl.innerText = '';
      return;
    }

    const rows = entries
      .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
      .map((e, idx) => {
        const dt = e.createdAt
          ? new Date(e.createdAt).toLocaleString()
          : '';
        const userText = (e.userEmail || '') + (e.userId ? (' (#' + e.userId + ')') : '');
        const methodSource = e.paymentMethod
          ? e.paymentMethod
          : (e.source || '');
        const amount = typeof e.amount === 'number'
          ? e.amount.toFixed(2)
          : '0.00';

        return `
          <tr>
            <td>${idx + 1}</td>
            <td>${dt}</td>
            <td>${e.type || ''}</td>
            <td>${e.direction || ''}</td>
            <td>${e.orderNumber || (e.orderId || '')}</td>
            <td>${userText}</td>
            <td>${methodSource}</td>
            <td class="text-end">${amount}</td>
          </tr>`;
      });

    tbody.innerHTML = rows.join('');
    if (statusEl) statusEl.innerText = `Total entries: ${entries.length}`;
  }

  if (btnFilter) {
    btnFilter.addEventListener('click', function (e) {
      e.preventDefault();
      loadLedger();
    });
  }

  // initial load
  loadLedger();
})();
