// Node.js script (requires node >= 18 or install node-fetch / axios)
const fetch = global.fetch || require('node-fetch');

const USER_SERVICE_EXPORT_URL = process.env.USER_EXPORT_URL || 'http://localhost:8083/internal/migrate/addresses/export';
const ADDRESS_SERVICE_URL = process.env.ADDRESS_SERVICE_URL || 'http://localhost:8090/api/addresses';
const BATCH_DELAY_MS = 100; // small delay between requests

(async () => {
  let addresses;
  // If local file exported_addresses.json exists, you can set NODE_LOCAL_FILE=1
  const useLocal = process.env.NODE_LOCAL_FILE === '1';
  if (useLocal) {
    const fs = require('fs');
    addresses = JSON.parse(fs.readFileSync('exported_addresses.json', 'utf8'));
  } else {
    console.log('Fetching exported addresses from user-service...');
    const resp = await fetch(USER_SERVICE_EXPORT_URL);
    if (!resp.ok) {
      console.error('Failed to fetch export:', resp.status, await resp.text());
      process.exit(1);
    }
    addresses = await resp.json();
  }

  console.log(`Got ${addresses.length} addresses. Starting import...`);

  for (const a of addresses) {
    try {
      // if userId missing skip
      if (!a.userId) {
        console.warn('Skipping address with missing userId', a);
        continue;
      }

      const resp = await fetch(ADDRESS_SERVICE_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-User-Id': String(a.userId) // important header
        },
        body: JSON.stringify(a)
      });

      if (!resp.ok) {
        console.error('Failed import for userId', a.userId, 'status', resp.status, await resp.text());
      } else {
        console.log('Imported address for userId', a.userId);
      }
    } catch (err) {
      console.error('Error importing', err);
    }
    // small delay
    await new Promise(r => setTimeout(r, BATCH_DELAY_MS));
  }

  console.log('Import finished');
})();
