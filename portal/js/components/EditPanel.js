/**
 * EditPanel Component - Right sidebar for editing coil slots
 *
 * Usage:
 *   import { showEditPanel, hideEditPanel } from './components/EditPanel.js';
 *   showEditPanel(coilData);
 */

import { updateCoilInventory } from '../api/admin.js';
import { showToast } from './Toast.js';
import { refreshGrid } from './CoilGrid.js';
import { escapeHtml } from '../utils/validation.js';

let currentCoil = null;

/**
 * Show the edit panel with coil data
 * @param {object} coil - Coil data object
 */
export function showEditPanel(coil) {
  const panel = document.getElementById('edit-panel');
  if (!panel) {
    console.error('Edit panel not found');
    return;
  }

  currentCoil = coil;
  panel.classList.remove('hidden');
  renderEditPanel(panel, coil);
}

/**
 * Hide the edit panel
 */
export function hideEditPanel() {
  const panel = document.getElementById('edit-panel');
  if (panel) {
    panel.classList.add('hidden');
  }
  currentCoil = null;
}

/**
 * Render edit panel content
 * @param {HTMLElement} panel - Panel container
 * @param {object} coil - Coil data
 */
function renderEditPanel(panel, coil) {
  const statusColor = getStatusColor(coil);
  const statusText = getStatusText(coil);

  panel.innerHTML = `
    <div class="flex flex-col h-full">
      <!-- Header -->
      <div class="p-6 border-b border-slate-200 dark:border-slate-800 flex justify-between items-center bg-slate-50 dark:bg-[#1e2732]">
        <h3 class="font-bold text-lg">Edit Slot ${escapeHtml(coil.id)}</h3>
        <button id="close-edit-panel" class="text-slate-400 hover:text-slate-600 dark:hover:text-white transition-colors">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>

      <!-- Content -->
      <div class="flex-1 overflow-y-auto p-6 flex flex-col gap-6">
        <!-- Product Preview -->
        <div class="flex flex-col items-center gap-3">
          <div class="w-32 h-32 bg-slate-100 dark:bg-[#283039] rounded-xl flex items-center justify-center p-4">
            <div class="bg-gradient-to-br from-blue-400 to-blue-600 rounded-lg h-full w-full flex items-center justify-center text-white font-bold text-2xl">
              ${escapeHtml(coil.id)}
            </div>
          </div>
          <div class="text-center">
            <h4 class="text-xl font-bold">Slot ${escapeHtml(coil.id)}</h4>
            <p class="text-slate-500 text-sm">${escapeHtml(statusText)}</p>
          </div>
        </div>

        <!-- Stats -->
        <div class="grid grid-cols-2 gap-3">
          <div class="bg-slate-50 dark:bg-[#1e2732] p-3 rounded-lg border border-slate-200 dark:border-slate-800 text-center">
            <span class="block text-xs text-slate-500 uppercase tracking-wide">Stock</span>
            <span class="block text-lg font-bold ${statusColor}">${coil.inventory}/10</span>
          </div>
          <div class="bg-slate-50 dark:bg-[#1e2732] p-3 rounded-lg border border-slate-200 dark:border-slate-800 text-center">
            <span class="block text-xs text-slate-500 uppercase tracking-wide">Status</span>
            <span class="block text-lg font-bold">${escapeHtml(coil.status || 'N/A')}</span>
          </div>
        </div>

        <hr class="border-slate-200 dark:border-slate-800"/>

        <!-- Edit Form -->
        <div class="flex flex-col gap-4">
          <label class="flex flex-col gap-1.5">
            <span class="text-sm font-medium text-slate-700 dark:text-slate-300">Update Quantity</span>
            <div class="flex items-center gap-2">
              <button id="decrement-btn" class="size-10 flex items-center justify-center bg-slate-100 dark:bg-[#283039] rounded-lg text-lg hover:bg-slate-200 dark:hover:bg-[#323b46] transition-colors font-bold">
                −
              </button>
              <input id="inventory-input" class="flex-1 bg-slate-50 dark:bg-[#283039] border border-slate-200 dark:border-slate-700 rounded-lg px-3 py-2 text-center font-bold focus:ring-2 focus:ring-primary focus:border-primary" type="number" min="0" max="10" value="${escapeHtml(coil.inventory)}"/>
              <button id="increment-btn" class="size-10 flex items-center justify-center bg-slate-100 dark:bg-[#283039] rounded-lg text-lg hover:bg-slate-200 dark:hover:bg-[#323b46] transition-colors font-bold">
                +
              </button>
            </div>
          </label>

          ${coil.link_group_id ? `
            <div class="bg-purple-50 dark:bg-purple-900/20 border border-purple-200 dark:border-purple-800 rounded-lg p-3">
              <div class="flex items-start gap-2">
                <span class="material-symbols-outlined text-purple-600 dark:text-purple-400 text-sm">link</span>
                <div class="flex-1">
                  <p class="text-xs font-medium text-purple-900 dark:text-purple-100">Linked Product</p>
                  <p class="text-xs text-purple-700 dark:text-purple-300">This coil is part of a product link group</p>
                </div>
              </div>
            </div>
          ` : ''}
        </div>

        <!-- Quick Actions -->
        <div class="flex flex-col gap-2">
          <button id="refill-btn" class="w-full px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg font-medium transition-colors flex items-center justify-center gap-2">
            <span class="material-symbols-outlined text-[18px]">add_circle</span>
            Refill to 10
          </button>
          <button id="empty-btn" class="w-full px-4 py-2 bg-slate-200 dark:bg-slate-700 hover:bg-slate-300 dark:hover:bg-slate-600 text-slate-700 dark:text-slate-200 rounded-lg font-medium transition-colors flex items-center justify-center gap-2">
            <span class="material-symbols-outlined text-[18px]">remove_circle</span>
            Set to 0
          </button>
        </div>
      </div>

      <!-- Footer -->
      <div class="p-6 border-t border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-[#1e2732] flex gap-3">
        <button id="save-btn" class="flex-1 py-2.5 bg-primary text-white rounded-lg font-bold text-sm shadow-lg shadow-primary/25 hover:bg-blue-600 transition-colors">
          Save Changes
        </button>
        <button id="cancel-btn" class="px-4 py-2.5 border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 bg-white dark:bg-transparent rounded-lg font-bold text-sm hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors">
          Cancel
        </button>
      </div>
    </div>
  `;

  // Add event listeners
  attachEventListeners(coil);
}

/**
 * Attach event listeners to edit panel controls
 * @param {object} coil - Coil data
 */
function attachEventListeners(coil) {
  const panel = document.getElementById('edit-panel');

  // Close button
  panel.querySelector('#close-edit-panel')?.addEventListener('click', hideEditPanel);

  // Cancel button
  panel.querySelector('#cancel-btn')?.addEventListener('click', hideEditPanel);

  // Increment/Decrement
  const input = panel.querySelector('#inventory-input');
  panel.querySelector('#increment-btn')?.addEventListener('click', () => {
    if (input.value < 10) {
      input.value = parseInt(input.value) + 1;
    }
  });

  panel.querySelector('#decrement-btn')?.addEventListener('click', () => {
    if (input.value > 0) {
      input.value = parseInt(input.value) - 1;
    }
  });

  // Quick actions
  panel.querySelector('#refill-btn')?.addEventListener('click', () => {
    input.value = 10;
  });

  panel.querySelector('#empty-btn')?.addEventListener('click', () => {
    input.value = 0;
  });

  // Save button
  panel.querySelector('#save-btn')?.addEventListener('click', async () => {
    await handleSave(coil, parseInt(input.value));
  });
}

/**
 * Handle save action
 * @param {object} coil - Original coil data
 * @param {number} newInventory - New inventory value
 */
async function handleSave(coil, newInventory) {
  if (newInventory === coil.inventory) {
    showToast('No changes to save', 'info');
    hideEditPanel();
    return;
  }

  const saveBtn = document.querySelector('#save-btn');
  if (saveBtn) {
    saveBtn.disabled = true;
    saveBtn.innerHTML = '<span class="loading-spinner"></span> Saving...';
  }

  try {
    // Update inventory via API
    await updateCoilInventory(coil.id, newInventory);

    showToast(`Updated ${coil.id} inventory to ${newInventory}`, 'success');

    // Refresh grid
    await refreshGrid();

    // Close panel
    hideEditPanel();

  } catch (error) {
    console.error('Failed to update coil:', error);
    showToast(`Failed to update ${coil.id}: ${error.message}`, 'error');

    if (saveBtn) {
      saveBtn.disabled = false;
      saveBtn.innerHTML = 'Save Changes';
    }
  }
}

/**
 * Get status color class
 * @param {object} coil - Coil data
 * @returns {string} Tailwind color class
 */
function getStatusColor(coil) {
  if (coil.status === 'jammed') return 'text-red-500';
  if (coil.inventory === 0) return 'text-slate-400';
  if (coil.inventory <= 2) return 'text-orange-500';
  return 'text-primary';
}

/**
 * Get status text
 * @param {object} coil - Coil data
 * @returns {string} Status description
 */
function getStatusText(coil) {
  if (coil.status === 'jammed') return 'Jammed';
  if (coil.inventory === 0) return 'Empty';
  if (coil.inventory <= 2) return 'Low Stock';
  return 'Available';
}

/**
 * Initialize edit panel (if it exists in DOM)
 */
export function initEditPanel() {
  const panel = document.getElementById('edit-panel');
  if (panel) {
    // Initially hidden
    panel.classList.add('hidden');
  }
}
