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
  console.log(`showToast called: message="${message}", type="${type}", duration=${duration}`);

  const container = getToastContainer();
  console.log('Toast container:', container);
  console.log('Container children before:', container.children.length);

  // Create toast with Tailwind styles
  const toast = document.createElement('div');

  // Base styles (removed animate-in classes that might not work with CDN Tailwind)
  let toastClass = 'flex items-center gap-3 min-w-[320px] max-w-md p-4 rounded-lg shadow-lg backdrop-blur-sm transition-all duration-300';

  // Type-specific styles and icons
  let icon = '';
  let iconClass = '';

  switch (type) {
    case 'success':
      toastClass += ' bg-emerald-500/90 text-white';
      icon = 'check_circle';
      iconClass = 'text-white';
      break;
    case 'error':
      toastClass += ' bg-red-500/90 text-white';
      icon = 'error';
      iconClass = 'text-white';
      break;
    case 'warning':
      toastClass += ' bg-orange-500/90 text-white';
      icon = 'warning';
      iconClass = 'text-white';
      break;
    case 'info':
    default:
      toastClass += ' bg-primary/90 text-white';
      icon = 'info';
      iconClass = 'text-white';
      break;
  }

  toast.className = toastClass;

  // Set initial styles for visibility
  toast.style.opacity = '1';
  toast.style.transform = 'translateX(0)';
  toast.style.pointerEvents = 'auto';

  console.log('Toast className:', toast.className);

  // Icon
  const iconElement = document.createElement('span');
  iconElement.className = `material-symbols-outlined ${iconClass}`;
  iconElement.textContent = icon;
  toast.appendChild(iconElement);

  // Message
  const messageElement = document.createElement('span');
  messageElement.className = 'flex-1 font-medium';
  messageElement.textContent = message;
  toast.appendChild(messageElement);

  // Close button
  const closeButton = document.createElement('button');
  closeButton.className = 'p-1 hover:bg-white/20 rounded transition-colors';
  closeButton.innerHTML = '<span class="material-symbols-outlined text-[18px]">close</span>';
  closeButton.addEventListener('click', () => {
    removeToast(toast);
  });
  toast.appendChild(closeButton);

  container.appendChild(toast);
  console.log('Container children after:', container.children.length);
  console.log('Toast appended to container:', toast);
  console.log('Toast computed style:', window.getComputedStyle(toast).display);

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
      z-index: 9999;
      display: flex;
      flex-direction: column;
      gap: 12px;
      pointer-events: none;
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
