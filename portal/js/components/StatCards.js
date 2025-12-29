/**
 * StatCards Component - Dashboard statistics cards
 *
 * Usage:
 *   import { renderStatCards, updateStatCards } from './components/StatCards.js';
 *   renderStatCards(container, coilsData);
 */

/**
 * Render stat cards
 * @param {HTMLElement} container - Container for stat cards
 * @param {Array} coils - Array of coil data
 */
export function renderStatCards(container, coils = []) {
  if (!container) {
    console.error('Stat cards container not found');
    return;
  }

  const stats = calculateStats(coils);

  container.innerHTML = `
    <!-- Total Capacity -->
    <div class="flex flex-col p-5 bg-white dark:bg-[#1e2732] rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm">
      <div class="flex justify-between items-start mb-2">
        <span class="text-slate-500 dark:text-slate-400 text-sm font-medium">Total Capacity</span>
        <span class="material-symbols-outlined text-primary bg-primary/10 p-1 rounded">inventory_2</span>
      </div>
      <div class="flex items-baseline gap-2">
        <span class="text-2xl font-bold">${stats.capacity}%</span>
        <span class="text-xs ${stats.capacityTrend >= 0 ? 'text-emerald-500' : 'text-red-500'} font-medium flex items-center">
          <span class="material-symbols-outlined text-[16px] mr-0.5">${stats.capacityTrend >= 0 ? 'trending_up' : 'trending_down'}</span>
          ${Math.abs(stats.capacityTrend)}%
        </span>
      </div>
    </div>

    <!-- Needs Restock -->
    <div class="flex flex-col p-5 bg-white dark:bg-[#1e2732] rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm">
      <div class="flex justify-between items-start mb-2">
        <span class="text-slate-500 dark:text-slate-400 text-sm font-medium">Needs Restock</span>
        <span class="material-symbols-outlined text-orange-500 bg-orange-500/10 p-1 rounded">warning</span>
      </div>
      <div class="flex items-baseline gap-2">
        <span class="text-2xl font-bold">${stats.needsRestock} Items</span>
        ${stats.restockChange !== 0 ? `
          <span class="text-xs ${stats.restockChange > 0 ? 'text-red-500' : 'text-emerald-500'} font-medium flex items-center">
            <span class="material-symbols-outlined text-[16px] mr-0.5">${stats.restockChange > 0 ? 'trending_up' : 'trending_down'}</span>
            ${stats.restockChange > 0 ? '+' : ''}${stats.restockChange}
          </span>
        ` : ''}
      </div>
    </div>

    <!-- Empty Slots -->
    <div class="flex flex-col p-5 bg-white dark:bg-[#1e2732] rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm">
      <div class="flex justify-between items-start mb-2">
        <span class="text-slate-500 dark:text-slate-400 text-sm font-medium">Empty Slots</span>
        <span class="material-symbols-outlined text-red-500 bg-red-500/10 p-1 rounded">block</span>
      </div>
      <div class="flex items-baseline gap-2">
        <span class="text-2xl font-bold">${stats.emptySlots}</span>
        ${stats.emptyChange !== 0 ? `
          <span class="text-xs ${stats.emptyChange > 0 ? 'text-red-500' : 'text-emerald-500'} font-medium flex items-center">
            <span class="material-symbols-outlined text-[16px] mr-0.5">${stats.emptyChange > 0 ? 'trending_up' : 'trending_down'}</span>
            ${stats.emptyChange > 0 ? '+' : ''}${stats.emptyChange}
          </span>
        ` : ''}
      </div>
    </div>

    <!-- Top Seller -->
    <div class="flex flex-col p-5 bg-white dark:bg-[#1e2732] rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm">
      <div class="flex justify-between items-start mb-2">
        <span class="text-slate-500 dark:text-slate-400 text-sm font-medium">Top Product</span>
        <span class="material-symbols-outlined text-emerald-500 bg-emerald-500/10 p-1 rounded">stars</span>
      </div>
      <div class="flex items-baseline gap-2">
        <span class="text-2xl font-bold truncate w-full">${stats.topProduct}</span>
      </div>
    </div>
  `;
}

/**
 * Calculate statistics from coils data
 * @param {Array} coils - Array of coil data
 * @returns {object} Statistics
 */
function calculateStats(coils) {
  if (!Array.isArray(coils) || coils.length === 0) {
    return {
      capacity: 0,
      capacityTrend: 0,
      needsRestock: 0,
      restockChange: 0,
      emptySlots: 0,
      emptyChange: 0,
      topProduct: 'N/A'
    };
  }

  // Total capacity (10 coils × 10 units = 100 max)
  const totalInventory = coils.reduce((sum, coil) => sum + (coil.inventory || 0), 0);
  const maxCapacity = 100; // 10 coils, each holds max 10 units
  const capacity = Math.round((totalInventory / maxCapacity) * 100);

  // Needs restock (inventory <= 2 but > 0)
  const needsRestock = coils.filter(c => c.inventory <= 2 && c.inventory > 0).length;

  // Empty slots
  const emptySlots = coils.filter(c => c.inventory === 0).length;

  // Top product - find most common product (or just use row 1 column 1 for now)
  const topProduct = findTopProduct(coils);

  // For trends, we'd need historical data
  // For now, use placeholder values
  const previousStats = loadPreviousStats();
  const capacityTrend = previousStats ? capacity - previousStats.capacity : 0;
  const restockChange = previousStats ? needsRestock - previousStats.needsRestock : 0;
  const emptyChange = previousStats ? emptySlots - previousStats.emptySlots : 0;

  // Save current stats for next comparison
  saveCurrentStats({ capacity, needsRestock, emptySlots });

  return {
    capacity,
    capacityTrend,
    needsRestock,
    restockChange,
    emptySlots,
    emptyChange,
    topProduct
  };
}

/**
 * Find the top product based on which column has the most inventory
 * @param {Array} coils - Array of coil data
 * @returns {string} Top product name
 */
function findTopProduct(coils) {
  // Group by column (A-J) and find which has most total inventory
  const columnInventory = {};

  coils.forEach(coil => {
    const column = coil.id.charAt(0); // A, B, C, etc.
    if (!columnInventory[column]) {
      columnInventory[column] = 0;
    }
    columnInventory[column] += coil.inventory || 0;
  });

  // Find column with max inventory
  let maxColumn = null;
  let maxInventory = 0;

  for (const [column, inventory] of Object.entries(columnInventory)) {
    if (inventory > maxInventory) {
      maxInventory = inventory;
      maxColumn = column;
    }
  }

  if (!maxColumn) return 'N/A';

  // Return column name as product (could be enhanced with actual product names)
  return `Column ${maxColumn}`;
}

/**
 * Load previous stats from session storage
 * @returns {object|null} Previous stats
 */
function loadPreviousStats() {
  try {
    const stored = sessionStorage.getItem('previous-stats');
    return stored ? JSON.parse(stored) : null;
  } catch (error) {
    console.error('Failed to load previous stats:', error);
    return null;
  }
}

/**
 * Save current stats to session storage
 * @param {object} stats - Current statistics
 */
function saveCurrentStats(stats) {
  try {
    sessionStorage.setItem('previous-stats', JSON.stringify(stats));
  } catch (error) {
    console.error('Failed to save stats:', error);
  }
}

/**
 * Update stat cards with new data
 * @param {Array} coils - Array of coil data
 */
export function updateStatCards(coils) {
  const container = document.getElementById('stat-cards');
  if (container) {
    renderStatCards(container, coils);
  }
}

/**
 * Initialize stat cards
 * @param {HTMLElement} container - Container element
 */
export function initStatCards(container) {
  if (!container) return;
  renderStatCards(container, []);
}
