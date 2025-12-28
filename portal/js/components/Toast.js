/**
 * Toast Notification Component
 *
 * Usage:
 *   import { showToast } from './components/Toast.js';
 *   showToast('Operation successful!', 'success');
 */

/**
 * Show toast notification
 *
 * @param {string} message - Notification message
 * @param {string} type - Toast type ('success', 'error', 'warning', 'info')
 * @param {number} duration - Auto-dismiss duration in ms (default: 5000)
 */
export function showToast(message, type = 'info', duration = 5000) {
  const container = getToastContainer();

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;

  const messageElement = document.createElement('span');
  messageElement.textContent = message;
  toast.appendChild(messageElement);

  // Close button
  const closeButton = document.createElement('button');
  closeButton.className = 'toast-close';
  closeButton.innerHTML = '&times;';
  closeButton.addEventListener('click', () => {
    removeToast(toast);
  });
  toast.appendChild(closeButton);

  container.appendChild(toast);

  // Auto-dismiss
  if (duration > 0) {
    setTimeout(() => {
      removeToast(toast);
    }, duration);
  }

  return toast;
}

/**
 * Get or create toast container
 */
function getToastContainer() {
  let container = document.getElementById('toast-container');

  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    container.style.cssText = `
      position: fixed;
      bottom: 24px;
      right: 24px;
      z-index: 2000;
      display: flex;
      flex-direction: column;
      gap: 12px;
    `;
    document.body.appendChild(container);
  }

  return container;
}

/**
 * Remove toast with animation
 */
function removeToast(toast) {
  toast.style.opacity = '0';
  toast.style.transform = 'translateX(400px)';

  setTimeout(() => {
    toast.remove();
  }, 300);
}

/**
 * Show success toast
 */
export function showSuccess(message, duration) {
  return showToast(message, 'success', duration);
}

/**
 * Show error toast
 */
export function showError(message, duration) {
  return showToast(message, 'error', duration);
}

/**
 * Show warning toast
 */
export function showWarning(message, duration) {
  return showToast(message, 'warning', duration);
}
