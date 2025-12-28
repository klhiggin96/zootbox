/**
 * Coil Edit Modal Component - Manual inventory adjustment
 *
 * Usage:
 *   import { showCoilEditModal } from './components/CoilEditModal.js';
 *   showCoilEditModal(coil);
 */

import { updateCoilInventory } from '../api/admin.js';
import { showModal } from './Modal.js';
import { showToast } from './Toast.js';
import { refreshGrid } from './CoilGrid.js';
import { validateInventory } from '../utils/validation.js';

/**
 * Show coil edit modal
 *
 * @param {Object} coil - Coil object to edit
 */
export function showCoilEditModal(coil) {
  if (!coil) {
    return;
  }

  // Create form content
  const content = createEditForm(coil);

  // Show modal
  const modal = showModal({
    title: `Edit Coil ${coil.id}`,
    content,
    buttons: [
      {
        text: 'Cancel',
        className: 'btn-secondary',
        onClick: () => {
          return true; // Close modal
        }
      },
      {
        text: 'Save',
        className: 'btn-primary',
        onClick: async (e, modalId) => {
          await handleSave(coil, modalId);
          return false; // Don't close modal yet (will close after save)
        }
      }
    ],
    closeOnBackdrop: false // Prevent accidental close
  });
}

/**
 * Create edit form HTML
 */
function createEditForm(coil) {
  const container = document.createElement('div');
  container.className = 'coil-edit-form';

  // Coil info
  const info = document.createElement('div');
  info.className = 'form-group';
  info.innerHTML = `
    <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; margin-bottom: 16px;">
      <div>
        <strong>Coil ID:</strong> ${coil.id}
      </div>
      <div>
        <strong>Status:</strong> <span style="color: ${coil.status === 'jammed' ? '#ef4444' : '#10b981'}">${coil.status}</span>
      </div>
      <div>
        <strong>Current Inventory:</strong> ${coil.inventory}
      </div>
      <div>
        <strong>Linked:</strong> ${coil.link_group_id ? 'Yes' : 'No'}
      </div>
    </div>
  `;
  container.appendChild(info);

  // Inventory input
  const formGroup = document.createElement('div');
  formGroup.className = 'form-group';

  const label = document.createElement('label');
  label.className = 'form-label';
  label.htmlFor = 'inventory-input';
  label.textContent = 'New Inventory (0-10)';
  formGroup.appendChild(label);

  const input = document.createElement('input');
  input.type = 'number';
  input.id = 'inventory-input';
  input.className = 'form-input';
  input.min = '0';
  input.max = '10';
  input.value = coil.inventory;
  input.required = true;
  formGroup.appendChild(input);

  const help = document.createElement('small');
  help.className = 'form-help';
  help.textContent = 'Enter a value between 0 and 10';
  formGroup.appendChild(help);

  container.appendChild(formGroup);

  // Focus input after modal opens
  setTimeout(() => {
    input.focus();
    input.select();

    // Submit on Enter key
    input.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        const saveButton = document.querySelector('.modal button.btn-primary');
        if (saveButton) {
          saveButton.click();
        }
      }
    });
  }, 100);

  return container;
}

/**
 * Handle save operation
 */
async function handleSave(coil, modalId) {
  const input = document.getElementById('inventory-input');
  const newInventory = parseInt(input.value, 10);

  // Validate input
  const validation = validateInventory(newInventory);

  if (!validation.valid) {
    // Show error
    input.classList.add('error');

    let errorElement = input.parentElement.querySelector('.form-error');
    if (!errorElement) {
      errorElement = document.createElement('span');
      errorElement.className = 'form-error';
      input.parentElement.appendChild(errorElement);
    }
    errorElement.textContent = validation.error;

    input.focus();
    return;
  }

  // Check if value changed
  if (newInventory === coil.inventory) {
    showToast('No changes to save', 'info');
    const { hideModal } = await import('./Modal.js');
    hideModal(modalId);
    return;
  }

  // Disable form during save
  input.disabled = true;
  const saveButton = document.querySelector('.modal button.btn-primary');
  if (saveButton) {
    saveButton.disabled = true;
    saveButton.innerHTML = '<span class="loading"></span> Saving...';
  }

  try {
    // Call API
    await updateCoilInventory(coil.id, newInventory);

    // Show success toast
    showToast(`Coil ${coil.id} inventory updated to ${newInventory}`, 'success');

    // Refresh grid
    await refreshGrid();

    // Close modal
    const { hideModal } = await import('./Modal.js');
    hideModal(modalId);

  } catch (error) {
    console.error('Save failed:', error);

    // Handle specific errors
    if (error.isStatus && error.isStatus(409)) {
      // Optimistic locking conflict
      showToast(
        'Coil was updated by another user. Please refresh and try again.',
        'error'
      );
    } else {
      showToast(
        `Failed to update inventory: ${error.getUserMessage ? error.getUserMessage() : error.message}`,
        'error'
      );
    }

    // Re-enable form
    input.disabled = false;
    if (saveButton) {
      saveButton.disabled = false;
      saveButton.textContent = 'Save';
    }
  }
}

/**
 * Show quick edit dialog (simplified version)
 */
export async function quickEditInventory(coil) {
  const { promptDialog } = await import('./Modal.js');

  const newValue = await promptDialog(
    `Set inventory for coil ${coil.id} (0-10):`,
    coil.inventory.toString(),
    `Edit Coil ${coil.id}`
  );

  if (newValue === null) {
    return; // Cancelled
  }

  const newInventory = parseInt(newValue, 10);
  const validation = validateInventory(newInventory);

  if (!validation.valid) {
    showToast(validation.error, 'error');
    return;
  }

  if (newInventory === coil.inventory) {
    showToast('No changes to save', 'info');
    return;
  }

  try {
    await updateCoilInventory(coil.id, newInventory);
    showToast(`Coil ${coil.id} updated to ${newInventory}`, 'success');
    await refreshGrid();
  } catch (error) {
    console.error('Quick edit failed:', error);
    showToast(`Failed to update: ${error.message}`, 'error');
  }
}
