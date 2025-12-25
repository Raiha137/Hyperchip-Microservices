// admin-orders.js
(async function(){
  const tbody = document.getElementById('orders-tbody');
  const pagination = document.getElementById('pagination');
  const searchInput = document.getElementById('search-input');
  document.getElementById('btn-search').addEventListener('click', ()=> load(0));

  async function load(page = 0) {
    const q = encodeURIComponent(searchInput.value || '');
    const r = await fetch('/api/admin/orders?page=' + page + '&size=20&q=' + q);
    if (!r.ok) { tbody.innerHTML = '<tr><td colspan="7">Failed to load</td></tr>'; return; }
    const data = await r.json();
    render(data);
  }
function render(data) {
  const content = data || [];   // data is already a list
  tbody.innerHTML = content.map(o => `
      <tr>
        <td>${o.orderNumber}</td>
        <td>${o.userId}</td>
        <td>${o.items ? o.items.length : 0}</td>
        <td>${o.total}</td>
        <td>${o.status}</td>
        <td>${new Date(o.createdAt).toLocaleString()}</td>
        <td>
          <a class="btn btn-sm btn-outline-primary" href="/admin/orders/${o.orderId}">View</a>
        </td>
      </tr>
    `).join('');

  // 👇 for now, remove pagination logic or keep it simple:
  pagination.innerHTML = '';
}

  await load(0);
})();
