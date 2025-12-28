/**
 * Modal Component - Reusable modal dialog
 *
 * Usage:
 *   import { showModal, hideModal, confirmDialog } from './components/Modal.js';
 *   const confirmed = await confirmDialog('Are you sure?');
 */

/**
 * Show modal dialog
 *
 * @param {Object} options - Modal options
 * @param {string} options.title - Modal title
 * @param {string|HTMLElement} options.content - Modal content (HTML string or element)
 * @param {Array} options.buttons - Button configurations
 * @param {Function} options.onClose - Callback when modal closes
 * @returns {HTMLElement} Modal element
 */
export function showModal(options = {}) {
  const {
    title = 'Dialog',
    content = '',
    buttons = [],
    onClose = null,
    closeOnBackdrop = true,
    closeOnEscape = true
  } = options;

  // Create modal overlay
  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay';
  overlay.id = `modal-${Date.now()}`;

  // Create modal container
  const modal = document.createElement('div');
  modal.className = 'modal';

  // Modal header
  const header = document.createElement('div');
  header.className = 'modal-header';

  const titleElement = document.createElement('h2');
  titleElement.className = 'modal-title';
  titleElement.textContent = title;
  header.appendChild(titleElement);

  const closeButton = document.createElement('button');
  closeButton.className = 'modal-close';
  closeButton.innerHTML = '&times;';
  closeButton.addEventListener('click', () => {
    hideModal(overlay.id);
    if (onClose) onClose(null);
  });
  header.appendChild(closeButton);

  modal.appendChild(header);

  // Modal body
  const body = document.createElement('div');
  body.className = 'modal-body';

  if (typeof content === 'string') {
    body.innerHTML = content;
  } else if (content instanceof HTMLElement) {
    body.appendChild(content);
  }

  modal.appendChild(body);

  // Modal footer (buttons)
  if (buttons.length > 0) {
    const footer = document.createElement('div');
    footer.className = 'modal-footer';

    buttons.forEach(buttonConfig => {
      const button = document.createElement('button');
      button.className = `btn ${buttonConfig.className || 'btn-secondary'}`;
      button.textContent = buttonConfig.text || 'Button';

      if (buttonConfig.onClick) {
        button.addEventListener('click', (e) => {
          const result = buttonConfig.onClick(e, overlay.id);
          if (result !== false) {
            hideModal(overlay.id);
          }
        });
      }

      footer.appendChild(button);
    });

    modal.appendChild(footer);
  }

  overlay.appendChild(modal);

  // Close on backdrop click
  if (closeOnBackdrop) {
    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) {
        hideModal(overlay.id);
        if (onClose) onClose(null);
      }
    });
  }

  // Close on Escape key
  if (closeOnEscape) {
    const escapeHandler = (e) => {
      if (e.key === 'Escape') {
        hideModal(overlay.id);
        if (onClose) onClose(null);
        document.removeEventListener('keydown', escapeHandler);
      }
    };
    document.addEventListener('keydown', escapeHandler);
  }

  // Add to DOM
  document.body.appendChild(overlay);

  // Focus first button or close button
  setTimeout(() => {
    const firstButton = modal.querySelector('.modal-footer button') || closeButton;
    firstButton?.focus();
  }, 100);

  return overlay;
}

/**
 * Hide modal dialog
 *
 * @param {string} modalId - Modal ID to hide
 */
export function hideModal(modalId) {
  const modal = document.getElementById(modalId);

  if (modal) {
    modal.style.opacity = '0';
    setTimeout(() => {
      modal.remove();
    }, 200);
  }
}

/**
 * Hide all modals
 */
export function hideAllModals() {
  const modals = document.querySelectorAll('.modal-overlay');
  modals.forEach(modal => {
    modal.remove();
  });
}

/**
 * Confirmation dialog
 *
 * @param {string} message - Confirmation message
 * @param {string} title - Dialog title (default: "Confirm")
 * @param {Object} options - Additional options
 * @returns {Promise<boolean>} True if confirmed, false if cancelled
 */
export function confirmDialog(message, title = 'Confirm', options = {}) {
  return new Promise((resolve) => {
    showModal({
      title,
      content: `<p>${message}</p>`,
      buttons: [
        {
          text: options.cancelText || 'Cancel',
          className: 'btn-secondary',
          onClick: () => {
            resolve(false);
          }
        },
        {
          text: options.confirmText || 'Confirm',
          className: options.confirmClass || 'btn-primary',
          onClick: () => {
            resolve(true);
          }
        }
      ],
      onClose: () => resolve(false),
      ...options
    });
  });
}

/**
 * Alert dialog
 *
 * @param {string} message - Alert message
 * @param {string} title - Dialog title (default: "Alert")
 * @returns {Promise<void>}
 */
export function alertDialog(message, title = 'Alert') {
  return new Promise((resolve) => {
    showModal({
      title,
      content: `<p>${message}</p>`,
      buttons: [
        {
          text: 'OK',
          className: 'btn-primary',
          onClick: () => {
            resolve();
          }
        }
      ],
      onClose: () => resolve()
    });
  });
}

/**
 * Prompt dialog
 *
 * @param {string} message - Prompt message
 * @param {string} defaultValue - Default input value
 * @param {string} title - Dialog title (default: "Input")
 * @returns {Promise<string|null>} Input value or null if cancelled
 */
export function promptDialog(message, defaultValue = '', title = 'Input') {
  return new Promise((resolve) => {
    const inputId = `prompt-input-${Date.now()}`;
    const content = `
      <p>${message}</p>
      <input type="text" id="${inputId}" class="form-input" value="${defaultValue}" />
    `;

    const modal = showModal({
      title,
      content,
      buttons: [
        {
          text: 'Cancel',
          className: 'btn-secondary',
          onClick: () => {
            resolve(null);
          }
        },
        {
          text: 'OK',
          className: 'btn-primary',
          onClick: () => {
            const input = document.getElementById(inputId);
            resolve(input ? input.value : null);
          }
        }
      ],
      onClose: () => resolve(null)
    });

    // Focus input
    setTimeout(() => {
      const input = document.getElementById(inputId);
      if (input) {
        input.focus();
        input.select();

        // Submit on Enter key
        input.addEventListener('keydown', (e) => {
          if (e.key === 'Enter') {
            e.preventDefault();
            resolve(input.value);
            hideModal(modal.id);
          }
        });
      }
    }, 100);
  });
}
