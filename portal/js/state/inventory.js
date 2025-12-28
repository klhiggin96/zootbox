/**
 * Inventory State Management - Session cache for coils, jams, and product links
 *
 * Usage:
 *   import { cacheInventory, getCachedInventory, clearCache } from './state/inventory.js';
 *   cacheInventory(machineId, coils);
 */

// Session cache (in-memory, cleared on page refresh)
const cache = {
  inventory: new Map(),      // machineId → coils array
  jamEvents: new Map(),       // machineId → jam events array
  productLinks: new Map(),    // machineId → product links array
  lastFetch: new Map(),       // cacheKey → timestamp
  staleness: new Map()        // cacheKey → staleness info
};

const CACHE_TTL = 30 * 60 * 1000; // 30 minutes (for offline mode)

// ========== Inventory Cache ==========

/**
 * Cache inventory for a machine
 *
 * @param {string} machineId - Machine UUID
 * @param {Array} coils - Array of coil objects
 */
export function cacheInventory(machineId, coils) {
  if (!machineId || !Array.isArray(coils)) {
    return;
  }

  cache.inventory.set(machineId, coils);
  cache.lastFetch.set(`inventory-${machineId}`, Date.now());
}

/**
 * Get cached inventory for a machine
 *
 * @param {string} machineId - Machine UUID
 * @returns {Array|null} Cached coils array or null if not found/expired
 */
export function getCachedInventory(machineId) {
  if (!machineId) {
    return null;
  }

  const cacheKey = `inventory-${machineId}`;
  const lastFetch = cache.lastFetch.get(cacheKey);

  // Check if cache exists and is not expired
  if (!lastFetch || Date.now() - lastFetch > CACHE_TTL) {
    return null;
  }

  return cache.inventory.get(machineId) || null;
}

/**
 * Detect changes between old and new inventory
 * Returns array of changed coils for efficient DOM updates
 *
 * @param {Array} oldCoils - Previous coils array
 * @param {Array} newCoils - New coils array
 * @returns {Object} { added, updated, removed }
 */
export function detectInventoryChanges(oldCoils, newCoils) {
  const changes = {
    added: [],
    updated: [],
    removed: []
  };

  if (!oldCoils || !newCoils) {
    return changes;
  }

  // Create maps for fast lookup
  const oldMap = new Map(oldCoils.map(c => [c.id, c]));
  const newMap = new Map(newCoils.map(c => [c.id, c]));

  // Find added and updated coils
  newCoils.forEach(newCoil => {
    const oldCoil = oldMap.get(newCoil.id);

    if (!oldCoil) {
      changes.added.push(newCoil);
    } else if (hasCoilChanged(oldCoil, newCoil)) {
      changes.updated.push({
        old: oldCoil,
        new: newCoil
      });
    }
  });

  // Find removed coils
  oldCoils.forEach(oldCoil => {
    if (!newMap.has(oldCoil.id)) {
      changes.removed.push(oldCoil);
    }
  });

  return changes;
}

/**
 * Check if coil has changed (inventory, status, link_group_id)
 *
 * @param {Object} oldCoil - Previous coil object
 * @param {Object} newCoil - New coil object
 * @returns {boolean} True if changed
 */
function hasCoilChanged(oldCoil, newCoil) {
  return (
    oldCoil.inventory !== newCoil.inventory ||
    oldCoil.status !== newCoil.status ||
    oldCoil.link_group_id !== newCoil.link_group_id
  );
}

// ========== Jam Events Cache ==========

/**
 * Cache jam events for a machine
 *
 * @param {string} machineId - Machine UUID
 * @param {Array} jamEvents - Array of jam event objects
 */
export function cacheJamEvents(machineId, jamEvents) {
  if (!machineId || !Array.isArray(jamEvents)) {
    return;
  }

  cache.jamEvents.set(machineId, jamEvents);
  cache.lastFetch.set(`jams-${machineId}`, Date.now());
}

/**
 * Get cached jam events for a machine
 *
 * @param {string} machineId - Machine UUID
 * @returns {Array|null} Cached jam events or null
 */
export function getCachedJamEvents(machineId) {
  if (!machineId) {
    return null;
  }

  const cacheKey = `jams-${machineId}`;
  const lastFetch = cache.lastFetch.get(cacheKey);

  if (!lastFetch || Date.now() - lastFetch > CACHE_TTL) {
    return null;
  }

  return cache.jamEvents.get(machineId) || null;
}

// ========== Product Links Cache ==========

/**
 * Cache product links for a machine
 *
 * @param {string} machineId - Machine UUID
 * @param {Array} productLinks - Array of product link objects
 */
export function cacheProductLinks(machineId, productLinks) {
  if (!machineId || !Array.isArray(productLinks)) {
    return;
  }

  cache.productLinks.set(machineId, productLinks);
  cache.lastFetch.set(`products-${machineId}`, Date.now());
}

/**
 * Get cached product links for a machine
 *
 * @param {string} machineId - Machine UUID
 * @returns {Array|null} Cached product links or null
 */
export function getCachedProductLinks(machineId) {
  if (!machineId) {
    return null;
  }

  const cacheKey = `products-${machineId}`;
  const lastFetch = cache.lastFetch.get(cacheKey);

  if (!lastFetch || Date.now() - lastFetch > CACHE_TTL) {
    return null;
  }

  return cache.productLinks.get(machineId) || null;
}

// ========== Cache Invalidation ==========

/**
 * Clear all cache for a specific machine
 *
 * @param {string} machineId - Machine UUID
 */
export function clearMachineCache(machineId) {
  if (!machineId) {
    return;
  }

  cache.inventory.delete(machineId);
  cache.jamEvents.delete(machineId);
  cache.productLinks.delete(machineId);
  cache.lastFetch.delete(`inventory-${machineId}`);
  cache.lastFetch.delete(`jams-${machineId}`);
  cache.lastFetch.delete(`products-${machineId}`);
  cache.staleness.delete(machineId);
}

/**
 * Clear all cache (all machines)
 */
export function clearAllCache() {
  cache.inventory.clear();
  cache.jamEvents.clear();
  cache.productLinks.clear();
  cache.lastFetch.clear();
  cache.staleness.clear();
}

/**
 * Invalidate specific cache type for a machine
 *
 * @param {string} machineId - Machine UUID
 * @param {string} type - Cache type ('inventory', 'jams', 'products')
 */
export function invalidateCache(machineId, type) {
  if (!machineId || !type) {
    return;
  }

  switch (type) {
    case 'inventory':
      cache.inventory.delete(machineId);
      cache.lastFetch.delete(`inventory-${machineId}`);
      break;
    case 'jams':
      cache.jamEvents.delete(machineId);
      cache.lastFetch.delete(`jams-${machineId}`);
      break;
    case 'products':
      cache.productLinks.delete(machineId);
      cache.lastFetch.delete(`products-${machineId}`);
      break;
    default:
      console.warn(`Unknown cache type: ${type}`);
  }
}

// ========== Cache Staleness Tracking ==========

/**
 * Mark cache as stale (machine went offline)
 *
 * @param {string} machineId - Machine UUID
 */
export function markCacheStale(machineId) {
  if (!machineId) {
    return;
  }

  cache.staleness.set(machineId, {
    markedAt: Date.now(),
    lastSuccessfulFetch: cache.lastFetch.get(`inventory-${machineId}`) || null
  });
}

/**
 * Clear stale marker (machine came back online)
 *
 * @param {string} machineId - Machine UUID
 */
export function clearStaleMarker(machineId) {
  if (!machineId) {
    return;
  }

  cache.staleness.delete(machineId);
}

/**
 * Check if cache is stale
 *
 * @param {string} machineId - Machine UUID
 * @returns {boolean} True if cache is stale
 */
export function isCacheStale(machineId) {
  if (!machineId) {
    return false;
  }

  return cache.staleness.has(machineId);
}

/**
 * Get cache staleness info
 *
 * @param {string} machineId - Machine UUID
 * @returns {Object|null} Staleness info { markedAt, lastSuccessfulFetch, ageMinutes }
 */
export function getCacheStaleness(machineId) {
  if (!machineId) {
    return null;
  }

  const info = cache.staleness.get(machineId);
  if (!info) {
    return null;
  }

  const ageMs = Date.now() - (info.lastSuccessfulFetch || info.markedAt);
  const ageMinutes = Math.floor(ageMs / (60 * 1000));

  return {
    ...info,
    ageMinutes
  };
}

// ========== Cache Statistics ==========

/**
 * Get cache statistics for debugging
 *
 * @returns {Object} Cache stats
 */
export function getCacheStats() {
  return {
    inventory: cache.inventory.size,
    jamEvents: cache.jamEvents.size,
    productLinks: cache.productLinks.size,
    lastFetchKeys: cache.lastFetch.size,
    staleMachines: cache.staleness.size
  };
}

/**
 * Get last fetch time for a cache key
 *
 * @param {string} machineId - Machine UUID
 * @param {string} type - Cache type ('inventory', 'jams', 'products')
 * @returns {number|null} Timestamp or null
 */
export function getLastFetchTime(machineId, type) {
  if (!machineId || !type) {
    return null;
  }

  const cacheKey = `${type}-${machineId}`;
  return cache.lastFetch.get(cacheKey) || null;
}

/**
 * Check if cache exists for a machine
 *
 * @param {string} machineId - Machine UUID
 * @param {string} type - Cache type ('inventory', 'jams', 'products')
 * @returns {boolean} True if cache exists
 */
export function hasCachedData(machineId, type) {
  if (!machineId || !type) {
    return false;
  }

  switch (type) {
    case 'inventory':
      return cache.inventory.has(machineId);
    case 'jams':
      return cache.jamEvents.has(machineId);
    case 'products':
      return cache.productLinks.has(machineId);
    default:
      return false;
  }
}

// ========== Batch Operations ==========

/**
 * Prefetch and cache all data for a machine
 * Useful when switching machines
 *
 * @param {string} machineId - Machine UUID
 * @param {Object} data - { inventory, jamEvents, productLinks }
 */
export function cacheAllData(machineId, data) {
  if (!machineId || !data) {
    return;
  }

  if (data.inventory) {
    cacheInventory(machineId, data.inventory);
  }

  if (data.jamEvents) {
    cacheJamEvents(machineId, data.jamEvents);
  }

  if (data.productLinks) {
    cacheProductLinks(machineId, data.productLinks);
  }
}

/**
 * Get all cached data for a machine
 *
 * @param {string} machineId - Machine UUID
 * @returns {Object} { inventory, jamEvents, productLinks }
 */
export function getAllCachedData(machineId) {
  if (!machineId) {
    return {
      inventory: null,
      jamEvents: null,
      productLinks: null
    };
  }

  return {
    inventory: getCachedInventory(machineId),
    jamEvents: getCachedJamEvents(machineId),
    productLinks: getCachedProductLinks(machineId)
  };
}
