/**
 * Status Indicator Component - Online/offline connection badge
 *
 * Usage:
 *   import { initStatusIndicator, updateConnectionStatus } from './components/StatusIndicator.js';
 *   initStatusIndicator();
 *   updateConnectionStatus('online');
 */

import { getCurrentMachine } from '../state/machines.js';
import { isCacheStale, getCacheStaleness } from '../state/inventory.js';
import { formatTimeAgo } from '../utils/formatting.js';

/**
 * Initialize status indicator
 */
export function initStatusIndicator() {
  // Update status every 5 seconds
  setInterval(updateStatusDisplay, 5000);

  // Initial update
  updateStatusDisplay();
}

/**
 * Update connection status badge
 *
 * @param {string} status - Connection status ('online', 'offline', 'checking')
 */
export function updateConnectionStatus(status) {
  const badge = document.getElementById('connection-status');
  const textElement = document.getElementById('connection-status-text');

  if (!badge && !textElement) {
    return;
  }

  // Update badge in navigation
  if (badge) {
    badge.className = `status-indicator ${status}`;
  }

  // Update text in status bar
  if (textElement) {
    let text = 'Unknown';
    let color = 'inherit';

    switch (status) {
      case 'online':
        text = 'Online';
        color = '#10b981';
        break;
      case 'offline':
        text = 'Offline';
        color = '#ef4444';
        break;
      case 'checking':
        text = 'Checking...';
        color = '#f59e0b';
        break;
    }

    textElement.textContent = text;
    textElement.style.color = color;
    textElement.style.fontWeight = '600';
  }

  // Update machine status in localStorage
  const machine = getCurrentMachine();
  if (machine) {
    const { updateMachine } = require('../state/machines.js');
    updateMachine(machine.id, { status });
  }
}

/**
 * Update status display (called periodically)
 */
function updateStatusDisplay() {
  const machine = getCurrentMachine();

  if (!machine) {
    updateConnectionStatus('offline');
    return;
  }

  // Check if cache is stale
  const isStale = isCacheStale(machine.id);

  if (isStale) {
    const staleness = getCacheStaleness(machine.id);
    showStaleDataBanner(staleness);
  } else {
    hideStaleDataBanner();
  }

  // Update connection status based on machine status
  updateConnectionStatus(machine.status || 'offline');
}

/**
 * Show stale data banner
 */
function showStaleDataBanner(staleness) {
  const banner = document.getElementById('stale-data-banner');
  const timeElement = document.getElementById('stale-data-time');

  if (!banner) {
    return;
  }

  banner.classList.remove('hidden');

  if (timeElement && staleness) {
    const lastSync = staleness.lastSuccessfulFetch
      ? formatTimeAgo(staleness.lastSuccessfulFetch)
      : 'unknown time';

    timeElement.textContent = lastSync;
  }

  // Add dismiss button handler (only once)
  const dismissBtn = document.getElementById('dismiss-stale-banner');
  if (dismissBtn && !dismissBtn.hasAttribute('data-handler-attached')) {
    dismissBtn.setAttribute('data-handler-attached', 'true');
    dismissBtn.addEventListener('click', () => {
      banner.classList.add('hidden');
    });
  }
}

/**
 * Hide stale data banner
 */
function hideStaleDataBanner() {
  const banner = document.getElementById('stale-data-banner');

  if (banner) {
    banner.classList.add('hidden');
  }
}

/**
 * Show checking status
 */
export function showChecking() {
  updateConnectionStatus('checking');
}

/**
 * Show online status
 */
export function showOnline() {
  updateConnectionStatus('online');
}

/**
 * Show offline status
 */
export function showOffline() {
  updateConnectionStatus('offline');
}
