/**
 * CoilGrid Component - Row-based inventory grid renderer (1-10 rows showing A-J columns)
 *
 * Usage:
 *   import { initCoilGrid, refreshGrid } from './components/CoilGrid.js';
 *   initCoilGrid();
 */

import { getCoils } from '../api/coils.js';
import { getCurrentMachineId } from '../state/machines.js';
import { cacheInventory, getCachedInventory } from '../state/inventory.js';
import { showToast } from './Toast.js';
import { showEditPanel } from './EditPanel.js';
import { updateStatCards } from './StatCards.js';
import { escapeHtml } from '../utils/validation.js';

let currentCoils = [];
let currentRows = [];

/**
 * Initialize coil grid on page load
 */
export function initCoilGrid() {
  const gridElement = document.getElementById('coil-grid');
  if (!gridElement) {
    console.error('Grid element not found');
    return;
  }

  // Load initial data
  refreshGrid();
}

/**
 * Refresh grid data from API
 */
export async function refreshGrid() {
  const machineId = getCurrentMachineId();

  if (!machineId) {
    console.warn('No machine selected');
    return;
  }

  const gridElement = document.getElementById('coil-grid');

  try {
    // Fetch coils from API
    const coils = await getCoils();

    // Cache for offline mode
    cacheInventory(machineId, coils);

    // Transform to row-based structure
    const rows = transposeToRows(coils);

    // Update grid
    renderGrid(gridElement, rows);

    // Update current data references
    currentCoils = coils;
    currentRows = rows;

    // Update stat cards
    updateStatCards(coils);

    return coils;

  } catch (error) {
    console.error('Failed to refresh grid:', error);

    // Try to load from cache
    const cachedCoils = getCachedInventory(machineId);

    if (cachedCoils) {
      console.log('Loading from cache');
      const rows = transposeToRows(cachedCoils);
      renderGrid(gridElement, rows);
      currentCoils = cachedCoils;
      currentRows = rows;
      updateStatCards(cachedCoils);
    } else {
      showToast('Failed to load inventory: ' + error.message, 'error');
    }

    throw error;
  }
}

/**
 * Transpose coil data to row-based structure
 * Machine has 10 rows, each row is 1 coil that holds max 10 units
 * @param {Array} coils - Array of coil objects
 * @returns {Array} Array of row objects
 */
function transposeToRows(coils) {
  const rows = [];

  // We only have 10 coils total (A1 through J1)
  // Each coil is displayed as one row
  const coilIds = ['A1', 'B1', 'C1', 'D1', 'E1', 'F1', 'G1', 'H1', 'I1', 'J1'];

  coilIds.forEach((coilId, index) => {
    const coil = coils.find(c => c.id === coilId);

    if (!coil) {
      console.warn(`Coil ${coilId} not found`);
      return;
    }

    const inventory = coil.inventory || 0;
    const maxStock = 10; // Each coil holds max 10 units
    const capacityPercent = Math.round((inventory / maxStock) * 100);

    // Determine status
    let status = 'ACTIVE';
    let statusClass = 'text-emerald-500 bg-emerald-500/10';

    if (coil.status === 'jammed') {
      status = 'JAMMED';
      statusClass = 'text-red-500 bg-red-500/10';
    } else if (inventory === 0) {
      status = 'EMPTY';
      statusClass = 'text-slate-500 bg-slate-500/10';
    } else if (inventory <= 2) {
      status = 'LOW STOCK';
      statusClass = 'text-orange-500 bg-orange-500/10';
    }

    rows.push({
      rowNumber: index + 1,
      coil: coil,
      product: determineRowProduct(coil, index + 1),
      totalStock: inventory,
      maxStock,
      capacityPercent,
      status,
      statusClass
    });
  });

  return rows;
}

/**
 * Determine which product is assigned to a row
 * @param {object} coil - Coil object
 * @param {number} rowNum - Row number (1-10)
 * @returns {object} Product info
 */
function determineRowProduct(coil, rowNum) {
  // For now, use placeholder product names based on row
  // In the future, this could pull from product links API
  const productNames = [
    'Cola Classic',
    'Snickers Bar',
    'Lays Classic',
    'Dasani Water',
    'M&Ms Peanut',
    'Doritos Nacho',
    'Sprite',
    'KitKat',
    'Cheetos',
    'Dr Pepper'
  ];

  return {
    name: productNames[rowNum - 1] || `Product ${rowNum}`,
    icon: 'inventory_2' // Material icon name
  };
}

/**
 * Render the grid with row-based layout
 * @param {HTMLElement} gridElement - Grid container
 * @param {Array} rows - Array of row objects
 */
function renderGrid(gridElement, rows) {
  if (!gridElement || !Array.isArray(rows)) {
    console.error('Invalid grid or rows data');
    return;
  }

  // Build HTML for grid
  let html = `
    <!-- Grid Header -->
    <div class="grid grid-cols-[260px_1fr] border-b border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-[#1e2732] sticky top-0 z-10">
      <div class="h-10 flex items-center px-4 font-bold text-slate-500 dark:text-slate-400 border-r border-slate-200 dark:border-slate-800 text-xs uppercase tracking-wide">
        Product Assignment
      </div>
      <div class="h-10 flex items-center justify-center text-xs font-bold text-slate-500 dark:text-slate-400">Inventory</div>
    </div>

    <!-- Grid Body -->
    <div class="flex flex-col divide-y divide-slate-200 dark:divide-slate-800">
  `;

  // Render each row
  rows.forEach(row => {
    html += renderRow(row);
  });

  html += `</div>`;

  gridElement.innerHTML = html;

  // Attach event listeners
  attachCoilClickHandlers();
}

/**
 * Render a single row
 * @param {object} row - Row data
 * @returns {string} HTML string
 */
function renderRow(row) {
  const coil = row.coil;
  const statusColor = getCoilStatusColor(coil);
  const barHeight = Math.round((coil.inventory / 10) * 100);

  return `
    <div data-row="${escapeHtml(row.rowNumber)}" class="grid grid-cols-[260px_1fr] h-28 group/row cursor-pointer hover:bg-slate-50 dark:hover:bg-[#0d1117]" data-coil-id="${escapeHtml(coil.id)}">
      <!-- Left: Product Info -->
      <div class="relative flex flex-col justify-center p-4 border-r border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-[#1e2732] transition-colors">
        <div class="flex justify-between items-start mb-2">
          <span class="text-xl font-bold text-slate-400 dark:text-slate-500">Row ${escapeHtml(row.rowNumber)}</span>
          <span class="text-[10px] uppercase font-bold ${row.statusClass} px-1.5 py-0.5 rounded">${escapeHtml(row.status)}</span>
        </div>
        <div class="flex items-center gap-3">
          <div class="h-10 w-10 shrink-0 flex items-center justify-center bg-slate-200 dark:bg-slate-700 rounded">
            <span class="material-symbols-outlined text-slate-600 dark:text-slate-400">${escapeHtml(row.product.icon)}</span>
          </div>
          <div class="flex flex-col min-w-0">
            <span class="font-bold text-sm text-slate-900 dark:text-white truncate">${escapeHtml(row.product.name)}</span>
            <span class="text-xs text-slate-500">Coil: <span class="font-semibold">${escapeHtml(coil.id)}</span> • Stock: <span class="font-semibold text-primary">${row.totalStock}/${row.maxStock}</span></span>
          </div>
        </div>
        <div class="w-full bg-slate-200 dark:bg-slate-700 h-1 rounded-full overflow-hidden mt-3">
          <div class="bg-primary h-full transition-all duration-300" style="width: ${row.capacityPercent}%"></div>
        </div>
      </div>

      <!-- Right: Single Coil Visual (0-10 units) -->
      <div class="relative p-6 flex items-center justify-center" data-inventory="${coil.inventory}">
        <!-- Horizontal bar showing 0-10 units -->
        <div class="w-full max-w-md">
          <div class="flex justify-between items-center mb-2">
            <span class="text-xs font-bold text-slate-400">${escapeHtml(coil.id)}</span>
            <span class="text-sm font-bold ${coil.inventory === 0 ? 'text-red-500' : 'text-slate-700 dark:text-slate-300'}">${coil.inventory} / 10</span>
          </div>
          <div class="w-full bg-slate-100 dark:bg-slate-700/50 rounded-full h-8 relative overflow-hidden">
            <div class="h-full ${statusColor} transition-all duration-300" style="width: ${barHeight}%"></div>
          </div>
          ${coil.status === 'jammed' ? `
            <div class="flex items-center gap-1 mt-2 text-red-500 text-xs">
              <span class="material-symbols-outlined text-[14px]">error</span>
              <span>Jammed</span>
            </div>
          ` : ''}
        </div>
      </div>
    </div>
  `;
}

/**
 * Render a single coil cell
 * @param {object} coil - Coil data
 * @returns {string} HTML string
 */
function renderCoilCell(coil) {
  const statusColor = getCoilStatusColor(coil);
  const barHeight = Math.round((coil.inventory / 10) * 100);

  return `
    <div class="relative p-2 border-r border-slate-200 dark:border-slate-800 flex flex-col items-center justify-end hover:bg-slate-50 dark:hover:bg-[#1e2732] transition-colors cursor-pointer group"
         data-coil-id="${coil.id}"
         data-inventory="${coil.inventory}">
      <span class="absolute top-1.5 left-2 text-[10px] font-bold text-slate-400 group-hover:text-primary">${coil.id}</span>

      <!-- Vertical bar -->
      <div class="w-3 bg-slate-100 dark:bg-slate-700/50 rounded-full h-12 relative overflow-hidden flex flex-col justify-end">
        <div class="w-full ${statusColor}" style="height: ${barHeight}%"></div>
      </div>

      <span class="mt-2 text-[10px] font-medium ${coil.inventory === 0 ? 'text-red-500' : 'text-slate-500'}">${coil.inventory}</span>

      ${coil.status === 'jammed' ? `
        <span class="absolute top-1.5 right-1.5 material-symbols-outlined text-red-500 text-[12px]">error</span>
      ` : ''}
    </div>
  `;
}

/**
 * Get status color for coil
 * @param {object} coil - Coil data
 * @returns {string} Tailwind color class
 */
function getCoilStatusColor(coil) {
  if (coil.status === 'jammed') return 'bg-red-500';
  if (coil.inventory === 0) return 'bg-slate-300 dark:bg-slate-600';
  if (coil.inventory <= 2) return 'bg-orange-500';
  return 'bg-primary';
}

/**
 * Attach click handlers to all coil cells
 */
function attachCoilClickHandlers() {
  const coilCells = document.querySelectorAll('[data-coil-id]');

  coilCells.forEach(cell => {
    cell.addEventListener('click', () => {
      const coilId = cell.dataset.coilId;
      handleCoilClick(coilId);
    });
  });
}

/**
 * Handle coil cell click
 * @param {string} coilId - Coil ID (e.g., "A1")
 */
function handleCoilClick(coilId) {
  const coil = currentCoils.find(c => c.id === coilId);

  if (!coil) {
    console.warn(`Coil not found: ${coilId}`);
    return;
  }

  // Show edit panel
  showEditPanel(coil);
}

/**
 * Update grid (called after data changes)
 * @param {Array} newCoils - Updated coils data
 */
export function updateGrid(newCoils) {
  if (!Array.isArray(newCoils)) {
    console.error('Invalid coils data');
    return;
  }

  currentCoils = newCoils;
  const rows = transposeToRows(newCoils);
  currentRows = rows;

  const gridElement = document.getElementById('coil-grid');
  if (gridElement) {
    renderGrid(gridElement, rows);
  }

  // Update stat cards
  updateStatCards(newCoils);
}

/**
 * Get current coils data
 * @returns {Array} Current coils
 */
export function getCurrentCoils() {
  return currentCoils;
}

/**
 * Clear grid (used when switching machines)
 */
export function clearGrid() {
  currentCoils = [];
  currentRows = [];

  const gridElement = document.getElementById('coil-grid');
  if (gridElement) {
    gridElement.innerHTML = `
      <div class="flex flex-col items-center justify-center gap-4 p-12">
        <span class="material-symbols-outlined text-6xl text-slate-300">inventory_2</span>
        <p class="text-slate-500">No inventory data loaded</p>
      </div>
    `;
  }
}
