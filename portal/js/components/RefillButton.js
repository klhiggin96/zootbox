/**
 * Refill Button Component - Bulk refill all coils
 *
 * Usage:
 *   import { initRefillButton } from './components/RefillButton.js';
 *   initRefillButton();
 */

import { refillAllCoils } from '../api/admin.js';
import { confirmDialog } from './Modal.js';
import { showToast } from './Toast.js';
import { refreshGrid } from './CoilGrid.js';
import { getCurrentMachine } from '../state/machines.js';

/**
 * Initialize refill button
 */
export function initRefillButton() {
  const button = document.getElementById('refill-all-btn');

  if (!button) {
    console.warn('Refill button not found');
    return;
  }

  // Check if machine is selected
  const machine = getCurrentMachine();
  if (machine) {
    button.disabled = false;
  }

  // Add click handler
  button.addEventListener('click', handleRefillClick);
}

/**
 * Handle refill button click
 */
async function handleRefillClick() {
  const button = document.getElementById('refill-all-btn');
  const machine = getCurrentMachine();

  if (!machine) {
    showToast('Please select a machine first', 'warning');
    return;
  }

  // Confirm action
  const confirmed = await confirmDialog(
    'Set all 100 coils to inventory = 10?\n\nThis will reset all coils regardless of current inventory levels.',
    'Refill All Coils',
    {
      confirmText: 'Refill All',
      confirmClass: 'btn-success'
    }
  );

  if (!confirmed) {
    return;
  }

  // Disable button during operation
  button.disabled = true;
  const originalText = button.innerHTML;
  button.innerHTML = '<span class="loading"></span> Refilling...';

  try {
    // Call API
    const result = await refillAllCoils();

    // Show success toast
    showToast(
      `Successfully refilled ${result.refilled_count || 100} coils to inventory = 10`,
      'success'
    );

    // Refresh grid to show updated inventory
    await refreshGrid();

  } catch (error) {
    console.error('Refill failed:', error);

    // Show error toast
    showToast(
      `Refill failed: ${error.getUserMessage ? error.getUserMessage() : error.message}`,
      'error'
    );

  } finally {
    // Re-enable button
    button.disabled = false;
    button.innerHTML = originalText;
  }
}

/**
 * Enable refill button (called when machine is selected)
 */
export function enableRefillButton() {
  const button = document.getElementById('refill-all-btn');
  if (button) {
    button.disabled = false;
  }
}

/**
 * Disable refill button (called when no machine selected)
 */
export function disableRefillButton() {
  const button = document.getElementById('refill-all-btn');
  if (button) {
    button.disabled = true;
  }
}
