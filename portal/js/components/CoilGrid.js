/**
 * CoilGrid Component - 10x10 grid renderer for inventory display
 *
 * Usage:
 *   import { initCoilGrid, refreshGrid } from './components/CoilGrid.js';
 *   initCoilGrid();
 */

import { getCoils } from '../api/coils.js';
import { getCoilStatusClass, isCoilLinked } from '../api/coils.js';
import { getCurrentMachineId } from '../state/machines.js';
import { cacheInventory, getCachedInventory, detectInventoryChanges } from '../state/inventory.js';
import { showToast } from './Toast.js';

let currentCoils = [];

/**
 * Initialize coil grid on page load
 */
export function initCoilGrid() {
  const gridElement = document.getElementById('coil-grid');
  if (!gridElement) {
    console.error('Grid element not found');
    return;
  }

  // Generate grid structure
  renderGridStructure(gridElement);

  // Load initial data
  refreshGrid();
}

/**
 * Generate 10x10 grid structure with labels
 */
function renderGridStructure(gridElement) {
  gridElement.innerHTML = '';

  // Top-left corner (empty)
  const corner = document.createElement('div');
  corner.className = 'grid-corner';
  gridElement.appendChild(corner);

  // Column labels (1-10)
  for (let col = 1; col <= 10; col++) {
    const label = document.createElement('div');
    label.className = 'grid-col-label';
    label.textContent = col;
    gridElement.appendChild(label);
  }

  // Rows A-J with row labels
  const rows = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'];

  rows.forEach((row, rowIndex) => {
    // Row label
    const rowLabel = document.createElement('div');
    rowLabel.className = 'grid-row-label';
    rowLabel.textContent = row;
    gridElement.appendChild(rowLabel);

    // 10 cells in this row
    for (let col = 1; col <= 10; col++) {
      const coilId = `${row}${col}`;
      const cell = createCoilCell(coilId);
      gridElement.appendChild(cell);
    }
  });
}

/**
 * Create a coil cell element
 */
function createCoilCell(coilId) {
  const cell = document.createElement('div');
  cell.className = 'coil-cell coil-empty';
  cell.dataset.coilId = coilId;
  cell.id = `cell-${coilId}`;

  // Coil ID label
  const idLabel = document.createElement('div');
  idLabel.className = 'coil-id';
  idLabel.textContent = coilId;
  cell.appendChild(idLabel);

  // Inventory count
  const inventory = document.createElement('div');
  inventory.className = 'coil-inventory';
  inventory.textContent = '0';
  cell.appendChild(inventory);

  // Status icon (will be populated later)
  const statusIcon = document.createElement('div');
  statusIcon.className = 'coil-status-icon';
  cell.appendChild(statusIcon);

  // Click handler
  cell.addEventListener('click', () => handleCoilClick(coilId));

  return cell;
}

/**
 * Handle coil cell click
 */
async function handleCoilClick(coilId) {
  const coil = currentCoils.find(c => c.id === coilId);

  if (!coil) {
    return;
  }

  // Show coil edit modal
  const { showCoilEditModal } = await import('./CoilEditModal.js');
  showCoilEditModal(coil);
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

    // Update grid
    updateGrid(coils);

    // Update current coils reference
    currentCoils = coils;

    // Update status bar
    updateStatusBar(coils);

    // Update last sync time
    updateLastSyncTime();

    return coils;

  } catch (error) {
    console.error('Failed to refresh grid:', error);

    // Try to load from cache
    const cachedCoils = getCachedInventory(machineId);

    if (cachedCoils) {
      console.log('Loading from cache');
      updateGrid(cachedCoils);
      currentCoils = cachedCoils;
      updateStatusBar(cachedCoils);
    } else {
      showToast('Failed to load inventory: ' + error.message, 'error');
    }

    throw error;
  }
}

/**
 * Update grid cells with coil data
 */
export function updateGrid(newCoils) {
  if (!Array.isArray(newCoils)) {
    console.error('Invalid coils data');
    return;
  }

  // Detect changes for efficient updates
  const changes = detectInventoryChanges(currentCoils, newCoils);

  // Update changed cells only
  const cellsToUpdate = [
    ...changes.added,
    ...changes.updated.map(u => u.new)
  ];

  cellsToUpdate.forEach(coil => {
    updateCoilCell(coil);
  });

  // If this is the first load, update all cells
  if (currentCoils.length === 0) {
    newCoils.forEach(coil => {
      updateCoilCell(coil);
    });
  }
}

/**
 * Update a single coil cell
 */
function updateCoilCell(coil) {
  const cellElement = document.getElementById(`cell-${coil.id}`);

  if (!cellElement) {
    console.warn(`Cell not found: ${coil.id}`);
    return;
  }

  // Get status class
  const statusClass = getCoilStatusClass(coil);

  // Update cell classes
  cellElement.className = `coil-cell coil-${statusClass}`;

  // Add linked indicator
  if (isCoilLinked(coil)) {
    cellElement.classList.add('coil-linked');
  }

  // Update inventory count
  const inventoryElement = cellElement.querySelector('.coil-inventory');
  if (inventoryElement) {
    inventoryElement.textContent = coil.inventory;
  }

  // Update status icon
  const statusIcon = cellElement.querySelector('.coil-status-icon');
  if (statusIcon) {
    statusIcon.innerHTML = getStatusIcon(coil);
  }

  // Store coil data on element
  cellElement.dataset.coil = JSON.stringify(coil);
}

/**
 * Get status icon SVG
 */
function getStatusIcon(coil) {
  if (coil.status === 'jammed') {
    return '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M8 1L1 15h14L8 1z" fill="#ef4444"/><text x="8" y="12" text-anchor="middle" fill="white" font-size="10" font-weight="bold">!</text></svg>';
  }

  if (coil.inventory <= 2 && coil.inventory > 0) {
    return '<svg width="16" height="16" viewBox="0 0 16 16" fill="#f59e0b"><path d="M8 2L3 14h10L8 2z"/></svg>';
  }

  return '';
}

/**
 * Update status bar with counts
 */
function updateStatusBar(coils) {
  if (!Array.isArray(coils)) {
    return;
  }

  const lowStockCount = coils.filter(c => c.inventory <= 2 && c.inventory > 0).length;
  const jammedCount = coils.filter(c => c.status === 'jammed').length;

  const lowStockElement = document.getElementById('low-stock-count');
  const jammedElement = document.getElementById('jammed-count');

  if (lowStockElement) {
    lowStockElement.textContent = `${lowStockCount} coil${lowStockCount !== 1 ? 's' : ''}`;
    lowStockElement.style.color = lowStockCount > 0 ? '#f59e0b' : 'inherit';
    lowStockElement.style.fontWeight = lowStockCount > 0 ? '700' : '500';
  }

  if (jammedElement) {
    jammedElement.textContent = `${jammedCount} coil${jammedCount !== 1 ? 's' : ''}`;
    jammedElement.style.color = jammedCount > 0 ? '#ef4444' : 'inherit';
    jammedElement.style.fontWeight = jammedCount > 0 ? '700' : '500';
  }
}

/**
 * Update last sync time display
 */
function updateLastSyncTime() {
  const lastSyncElement = document.getElementById('last-sync-text');

  if (lastSyncElement) {
    const now = new Date();
    lastSyncElement.textContent = now.toLocaleTimeString();
  }
}

/**
 * Get current coils
 */
export function getCurrentCoils() {
  return currentCoils;
}

/**
 * Clear grid (used when switching machines)
 */
export function clearGrid() {
  currentCoils = [];
  const gridElement = document.getElementById('coil-grid');
  if (gridElement) {
    renderGridStructure(gridElement);
  }
}
