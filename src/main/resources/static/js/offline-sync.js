(function () {
  const DB_NAME = 'microbiz-offline';
  const STORE = 'pending-sales';

  function openDb() {
    return new Promise((resolve, reject) => {
      const req = indexedDB.open(DB_NAME, 1);
      req.onupgradeneeded = () => req.result.createObjectStore(STORE, { keyPath: 'id', autoIncrement: true });
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error);
    });
  }

  async function queueSale(payload) {
    const db = await openDb();
    const tx = db.transaction(STORE, 'readwrite');
    tx.objectStore(STORE).add({ payload, createdAt: Date.now() });
  }

  async function flushSales() {
    if (!navigator.onLine) return;
    const db = await openDb();
    const tx = db.transaction(STORE, 'readwrite');
    const store = tx.objectStore(STORE);
    const allReq = store.getAll();
    allReq.onsuccess = async () => {
      for (const item of allReq.result) {
        const body = new URLSearchParams(item.payload);
        try {
          const res = await fetch('/ventes/enregistrer', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body });
          if (res.ok) store.delete(item.id);
        } catch (e) {
          return;
        }
      }
    };
  }

  window.MicrobizOffline = { queueSale, flushSales };
  window.addEventListener('online', flushSales);
  flushSales();
})();
