(async function () {

  const periodSelect = document.getElementById('period-select');
  const topProductsBody = document.getElementById('top-products-body');
  const topCategoriesBody = document.getElementById('top-categories-body');
  const topBrandsBody = document.getElementById('top-brands-body');
  const ctx = document.getElementById('sales-chart');

  let chart;

  async function loadDashboard() {
    const period = periodSelect.value || 'MONTH';
    const res = await fetch('/api/admin/dashboard/data?periodType=' + period);
    if (!res.ok) {
      console.error('Dashboard fetch failed', res.status);
      return;
    }
    const data = await res.json();
    updateDashboard(data);
  }

  function updateDashboard(data) {
    renderChart(data.chartPoints || []);
    renderTopProducts(data.topProducts || []);
    renderTopCategories(data.topCategories || []);
    renderTopBrands(data.topBrands || []);
  }

  function renderChart(points) {
    const labels = points.map(p => p.label);
    const values = points.map(p => (p.totalAmount || 0));

    if (chart) chart.destroy();
    chart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: 'Sales',
          data: values
        }]
      }
    });
  }

  function renderTopProducts(list) {
    if (!list.length) {
      topProductsBody.innerHTML =
        `<tr><td colspan="4" class="text-center">No data</td></tr>`;
      return;
    }

    topProductsBody.innerHTML = list.map((p, idx) => `
      <tr>
        <td>${idx + 1}</td>
        <td>${p.productName || ('#' + p.productId)}</td>
        <td>${p.totalQuantity}</td>
        <td>${p.totalAmount}</td>
      </tr>
    `).join('');
  }

  function renderTopCategories(list) {
    if (!list.length) {
      topCategoriesBody.innerHTML =
        `<tr><td colspan="4" class="text-center">No data</td></tr>`;
      return;
    }

    topCategoriesBody.innerHTML = list.map((c, idx) => `
      <tr>
        <td>${idx + 1}</td>
        <td>${c.categoryName || ('#' + c.categoryId)}</td>
        <td>${c.totalQuantity}</td>
        <td>${c.totalAmount}</td>
      </tr>
    `).join('');
  }

  function renderTopBrands(list) {
    if (!list.length) {
      topBrandsBody.innerHTML =
        `<tr><td colspan="4" class="text-center">No data</td></tr>`;
      return;
    }

    topBrandsBody.innerHTML = list.map((b, idx) => `
      <tr>
        <td>${idx + 1}</td>
        <td>${b.brandName || ('#' + b.brandId)}</td>
        <td>${b.totalQuantity}</td>
        <td>${b.totalAmount}</td>
      </tr>
    `).join('');
  }

  periodSelect.addEventListener('change', loadDashboard);
  await loadDashboard();
})();
