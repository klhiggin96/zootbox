/**
 * Formatting Utilities - Date/time formatters and display helpers
 *
 * Usage:
 *   import { formatDateTime, formatTimeAgo, formatStaleness } from './utils/formatting.js';
 *   const formatted = formatTimeAgo('2025-12-28T14:00:00Z');
 */

// ========== Date/Time Formatting ==========

/**
 * Format ISO 8601 timestamp to human-readable format
 *
 * @param {string} isoString - ISO 8601 timestamp (e.g., "2025-12-28T14:30:00Z")
 * @param {Object} options - Formatting options
 * @returns {string} Formatted date/time (e.g., "Dec 28, 2025 2:30 PM")
 */
export function formatDateTime(isoString, options = {}) {
  if (!isoString) {
    return 'Never';
  }

  try {
    const date = new Date(isoString);

    if (isNaN(date.getTime())) {
      return 'Invalid date';
    }

    const defaults = {
      dateStyle: 'medium',
      timeStyle: 'short',
      ...options
    };

    return new Intl.DateTimeFormat('en-US', defaults).format(date);

  } catch (error) {
    console.error('Date formatting error:', error);
    return 'Invalid date';
  }
}

/**
 * Format date only (no time)
 *
 * @param {string} isoString - ISO 8601 timestamp
 * @returns {string} Formatted date (e.g., "Dec 28, 2025")
 */
export function formatDate(isoString) {
  return formatDateTime(isoString, { timeStyle: undefined });
}

/**
 * Format time only (no date)
 *
 * @param {string} isoString - ISO 8601 timestamp
 * @returns {string} Formatted time (e.g., "2:30 PM")
 */
export function formatTime(isoString) {
  return formatDateTime(isoString, { dateStyle: undefined });
}

// ========== Relative Time Formatting (Time Ago) ==========

/**
 * Format timestamp as relative time (time ago)
 *
 * @param {string|number} timestamp - ISO 8601 string or Unix timestamp
 * @returns {string} Relative time (e.g., "5 minutes ago", "just now")
 */
export function formatTimeAgo(timestamp) {
  if (!timestamp) {
    return 'Never';
  }

  try {
    const date = typeof timestamp === 'string' ? new Date(timestamp) : new Date(timestamp);
    const now = Date.now();
    const diffMs = now - date.getTime();

    if (isNaN(diffMs)) {
      return 'Invalid date';
    }

    if (diffMs < 0) {
      return 'In the future';
    }

    const seconds = Math.floor(diffMs / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);
    const weeks = Math.floor(days / 7);
    const months = Math.floor(days / 30);
    const years = Math.floor(days / 365);

    if (seconds < 10) {
      return 'Just now';
    }

    if (seconds < 60) {
      return `${seconds} second${seconds !== 1 ? 's' : ''} ago`;
    }

    if (minutes < 60) {
      return `${minutes} minute${minutes !== 1 ? 's' : ''} ago`;
    }

    if (hours < 24) {
      return `${hours} hour${hours !== 1 ? 's' : ''} ago`;
    }

    if (days < 7) {
      return `${days} day${days !== 1 ? 's' : ''} ago`;
    }

    if (weeks < 4) {
      return `${weeks} week${weeks !== 1 ? 's' : ''} ago`;
    }

    if (months < 12) {
      return `${months} month${months !== 1 ? 's' : ''} ago`;
    }

    return `${years} year${years !== 1 ? 's' : ''} ago`;

  } catch (error) {
    console.error('Time ago formatting error:', error);
    return 'Invalid date';
  }
}

/**
 * Format timestamp as short relative time
 *
 * @param {string|number} timestamp - ISO 8601 string or Unix timestamp
 * @returns {string} Short relative time (e.g., "5m ago", "2h ago", "3d ago")
 */
export function formatTimeAgoShort(timestamp) {
  if (!timestamp) {
    return 'Never';
  }

  try {
    const date = typeof timestamp === 'string' ? new Date(timestamp) : new Date(timestamp);
    const now = Date.now();
    const diffMs = now - date.getTime();

    if (isNaN(diffMs)) {
      return 'Invalid';
    }

    if (diffMs < 0) {
      return 'Future';
    }

    const seconds = Math.floor(diffMs / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    if (seconds < 60) {
      return `${seconds}s ago`;
    }

    if (minutes < 60) {
      return `${minutes}m ago`;
    }

    if (hours < 24) {
      return `${hours}h ago`;
    }

    return `${days}d ago`;

  } catch (error) {
    return 'Invalid';
  }
}

// ========== Duration Formatting ==========

/**
 * Format duration in milliseconds to human-readable
 *
 * @param {number} durationMs - Duration in milliseconds
 * @returns {string} Formatted duration (e.g., "5 minutes", "2 hours 30 minutes")
 */
export function formatDuration(durationMs) {
  if (typeof durationMs !== 'number' || durationMs < 0) {
    return '0 seconds';
  }

  const seconds = Math.floor(durationMs / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);

  if (seconds < 60) {
    return `${seconds} second${seconds !== 1 ? 's' : ''}`;
  }

  if (minutes < 60) {
    const remainingSeconds = seconds % 60;
    if (remainingSeconds === 0) {
      return `${minutes} minute${minutes !== 1 ? 's' : ''}`;
    }
    return `${minutes} minute${minutes !== 1 ? 's' : ''} ${remainingSeconds} second${remainingSeconds !== 1 ? 's' : ''}`;
  }

  if (hours < 24) {
    const remainingMinutes = minutes % 60;
    if (remainingMinutes === 0) {
      return `${hours} hour${hours !== 1 ? 's' : ''}`;
    }
    return `${hours} hour${hours !== 1 ? 's' : ''} ${remainingMinutes} minute${remainingMinutes !== 1 ? 's' : ''}`;
  }

  const remainingHours = hours % 24;
  if (remainingHours === 0) {
    return `${days} day${days !== 1 ? 's' : ''}`;
  }
  return `${days} day${days !== 1 ? 's' : ''} ${remainingHours} hour${remainingHours !== 1 ? 's' : ''}`;
}

// ========== Cache Staleness Formatting ==========

/**
 * Format cache staleness indicator
 *
 * @param {number} ageMinutes - Age in minutes
 * @returns {Object} { text: string, className: string }
 *
 * Example: { text: "Data is 5 minutes old", className: "stale-warning" }
 */
export function formatStaleness(ageMinutes) {
  if (typeof ageMinutes !== 'number' || ageMinutes < 0) {
    return {
      text: 'Current data',
      className: 'stale-fresh'
    };
  }

  if (ageMinutes < 1) {
    return {
      text: 'Just updated',
      className: 'stale-fresh'
    };
  }

  if (ageMinutes < 5) {
    return {
      text: `Data is ${ageMinutes} minute${ageMinutes !== 1 ? 's' : ''} old`,
      className: 'stale-fresh'
    };
  }

  if (ageMinutes < 30) {
    return {
      text: `Data is ${ageMinutes} minutes old`,
      className: 'stale-warning'
    };
  }

  const hours = Math.floor(ageMinutes / 60);
  return {
    text: `Data is ${hours} hour${hours !== 1 ? 's' : ''} old`,
    className: 'stale-critical'
  };
}

// ========== Number Formatting ==========

/**
 * Format number with thousands separator
 *
 * @param {number} num - Number to format
 * @returns {string} Formatted number (e.g., "1,234")
 */
export function formatNumber(num) {
  if (typeof num !== 'number') {
    return '0';
  }

  return new Intl.NumberFormat('en-US').format(num);
}

/**
 * Format percentage
 *
 * @param {number} value - Value (0-1 or 0-100)
 * @param {boolean} isDecimal - If true, value is 0-1; if false, value is 0-100
 * @returns {string} Formatted percentage (e.g., "75.5%")
 */
export function formatPercentage(value, isDecimal = true) {
  if (typeof value !== 'number') {
    return '0%';
  }

  const percentage = isDecimal ? value * 100 : value;
  return `${percentage.toFixed(1)}%`;
}

// ========== Inventory Formatting ==========

/**
 * Format inventory status
 *
 * @param {number} inventory - Inventory count (0-10)
 * @returns {Object} { text: string, className: string }
 */
export function formatInventoryStatus(inventory) {
  if (typeof inventory !== 'number') {
    return {
      text: 'Unknown',
      className: 'inventory-unknown'
    };
  }

  if (inventory === 0) {
    return {
      text: 'Empty',
      className: 'inventory-empty'
    };
  }

  if (inventory <= 2) {
    return {
      text: 'Low Stock',
      className: 'inventory-low'
    };
  }

  return {
    text: 'Available',
    className: 'inventory-available'
  };
}

/**
 * Format coil status for display
 *
 * @param {string} status - Coil status ("available", "jammed")
 * @returns {Object} { text: string, className: string }
 */
export function formatCoilStatus(status) {
  const statusMap = {
    available: {
      text: 'Available',
      className: 'status-available'
    },
    jammed: {
      text: 'Jammed',
      className: 'status-jammed'
    }
  };

  return statusMap[status] || {
    text: 'Unknown',
    className: 'status-unknown'
  };
}

// ========== Connection Status Formatting ==========

/**
 * Format connection status
 *
 * @param {string} status - Connection status ("online", "offline")
 * @returns {Object} { text: string, className: string }
 */
export function formatConnectionStatus(status) {
  const statusMap = {
    online: {
      text: 'Online',
      className: 'status-online'
    },
    offline: {
      text: 'Offline',
      className: 'status-offline'
    }
  };

  return statusMap[status] || {
    text: 'Unknown',
    className: 'status-unknown'
  };
}

// ========== Uptime Formatting ==========

/**
 * Format uptime in seconds to human-readable
 *
 * @param {number} uptimeSeconds - Uptime in seconds
 * @returns {string} Formatted uptime (e.g., "2 hours 30 minutes")
 */
export function formatUptime(uptimeSeconds) {
  if (typeof uptimeSeconds !== 'number' || uptimeSeconds < 0) {
    return '0 seconds';
  }

  return formatDuration(uptimeSeconds * 1000);
}

// ========== Truncation ==========

/**
 * Truncate text to maximum length
 *
 * @param {string} text - Text to truncate
 * @param {number} maxLength - Maximum length
 * @param {string} suffix - Suffix to add (default: "...")
 * @returns {string} Truncated text
 */
export function truncateText(text, maxLength, suffix = '...') {
  if (!text || typeof text !== 'string') {
    return '';
  }

  if (text.length <= maxLength) {
    return text;
  }

  return text.substring(0, maxLength - suffix.length) + suffix;
}

// ========== ISO 8601 Conversion ==========

/**
 * Convert Date object to ISO 8601 string
 *
 * @param {Date} date - Date object
 * @returns {string} ISO 8601 string (e.g., "2025-12-28T14:30:00Z")
 */
export function toISOString(date) {
  if (!(date instanceof Date) || isNaN(date.getTime())) {
    return new Date().toISOString();
  }

  return date.toISOString();
}

/**
 * Get current timestamp as ISO 8601 string
 *
 * @returns {string} Current timestamp
 */
export function getCurrentTimestamp() {
  return new Date().toISOString();
}
