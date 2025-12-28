/**
 * Machine Selector Component - Dropdown for switching between machines
 *
 * Usage:
 *   import { initMachineSelector } from './components/MachineSelector.js';
 *   initMachineSelector();
 */

import { getMachines, getCurrentMachineId, setCurrentMachine } from '../state/machines.js';
import { clearMachineCache } from '../state/inventory.js';
import { clearGrid, refreshGrid } from './CoilGrid.js';
import { apiClient } from '../api/client.js';
import { stopAutoRefresh, startAutoRefresh } from '../state/sync.js';
import { showToast } from './Toast.js';

/**
 * Initialize machine selector
 */
export function initMachineSelector() {
  const selector = document.getElementById('machine-select');

  if (!selector) {
    console.warn('Machine selector not found');
    return;
  }

  // Load machines into dropdown
  refreshMachineList();

  // Set current selection
  const currentMachineId = getCurrentMachineId();
  if (currentMachineId) {
    selector.value = currentMachineId;
  }

  // Handle selection change
  selector.addEventListener('change', handleMachineChange);
}

/**
 * Refresh machine list in dropdown
 */
export function refreshMachineList() {
  const selector = document.getElementById('machine-select');

  if (!selector) {
    return;
  }

  const machines = getMachines();
  const currentSelection = selector.value;

  // Clear existing options
  selector.innerHTML = '<option value="">Select a machine...</option>';

  // Add machine options
  machines.forEach(machine => {
    const option = document.createElement('option');
    option.value = machine.id;
    option.textContent = machine.name;
    selector.appendChild(option);
  });

  // Restore selection if still valid
  if (currentSelection && machines.find(m => m.id === currentSelection)) {
    selector.value = currentSelection;
  }
}

/**
 * Handle machine selection change
 */
async function handleMachineChange(event) {
  const newMachineId = event.target.value;
  const currentMachineId = getCurrentMachineId();

  // No change
  if (newMachineId === currentMachineId) {
    return;
  }

  // Switching to no machine
  if (!newMachineId) {
    await switchToNoMachine();
    return;
  }

  // Switching to a different machine
  await switchMachine(newMachineId);
}

/**
 * Switch to a different machine
 */
async function switchMachine(newMachineId) {
  const selector = document.getElementById('machine-select');
  const previousMachineId = getCurrentMachineId();

  try {
    // Confirm switch if there are unsaved changes
    if (previousMachineId) {
      const confirmed = confirm('Switch to a different machine? Any in-progress operations will be cancelled.');
      if (!confirmed) {
        // Revert selection
        selector.value = previousMachineId;
        return;
      }
    }

    // Disable selector during switch
    selector.disabled = true;

    // Stop auto-refresh
    stopAutoRefresh();

    // Cancel all in-flight API requests
    apiClient.cancelAllRequests();

    // Clear cache for previous machine
    if (previousMachineId) {
      clearMachineCache(previousMachineId);
    }

    // Clear grid
    clearGrid();

    // Update current machine
    setCurrentMachine(newMachineId);

    // Get new machine details
    const machines = getMachines();
    const newMachine = machines.find(m => m.id === newMachineId);

    if (!newMachine) {
      throw new Error('Machine not found');
    }

    // Update API client base URL
    apiClient.setBaseURL(newMachine.endpointUrl);

    // Load inventory for new machine
    await refreshGrid();

    // Start auto-refresh
    startAutoRefresh();

    // Show success toast
    showToast(`Switched to ${newMachine.name}`, 'success');

  } catch (error) {
    console.error('Machine switch failed:', error);
    showToast(`Failed to switch machines: ${error.message}`, 'error');

    // Revert selection
    if (previousMachineId) {
      selector.value = previousMachineId;
      setCurrentMachine(previousMachineId);
    } else {
      selector.value = '';
      setCurrentMachine(null);
    }

  } finally {
    // Re-enable selector
    selector.disabled = false;
  }
}

/**
 * Switch to no machine (clear selection)
 */
async function switchToNoMachine() {
  const selector = document.getElementById('machine-select');

  // Stop auto-refresh
  stopAutoRefresh();

  // Cancel all in-flight requests
  apiClient.cancelAllRequests();

  // Clear current machine
  setCurrentMachine(null);

  // Clear grid
  clearGrid();

  // Show empty state
  const gridElement = document.getElementById('coil-grid');
  if (gridElement) {
    gridElement.innerHTML = `
      <div class="empty-state">
        <h2>No Machine Selected</h2>
        <p>Please select a machine from the dropdown above or add a new machine in <a href="machine-settings.html">Machine Settings</a>.</p>
      </div>
    `;
  }

  // Disable refill button
  const refillBtn = document.getElementById('refill-all-btn');
  if (refillBtn) {
    refillBtn.disabled = true;
  }
}

/**
 * Get currently selected machine
 */
export function getSelectedMachine() {
  const machines = getMachines();
  const currentMachineId = getCurrentMachineId();

  return machines.find(m => m.id === currentMachineId) || null;
}
