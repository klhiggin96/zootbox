/**
 * Logger utility for ZootBox Portal
 *
 * Provides conditional logging based on environment.
 * In production, console.log and console.warn are disabled.
 * console.error is always enabled for critical issues.
 *
 * Usage:
 *   import { log, warn, error } from './utils/logger.js';
 *   log('Debug message');
 *   warn('Warning message');
 *   error('Error message');
 */

// Detect environment based on hostname
// localhost = development, anything else = production
const isDevelopment = window.location.hostname === 'localhost' ||
                      window.location.hostname === '127.0.0.1' ||
                      window.location.hostname === '';

// Development mode flag - can be overridden by setting localStorage.DEBUG = 'true'
const DEBUG = isDevelopment || localStorage.getItem('DEBUG') === 'true';

/**
 * Log function - only outputs in development mode
 * @param {...any} args - Arguments to log
 */
export const log = DEBUG ? console.log.bind(console) : () => {};

/**
 * Warn function - only outputs in development mode
 * @param {...any} args - Arguments to warn
 */
export const warn = DEBUG ? console.warn.bind(console) : () => {};

/**
 * Error function - always outputs (even in production)
 * @param {...any} args - Arguments to error
 */
export const error = console.error.bind(console);

/**
 * Info function - only outputs in development mode
 * @param {...any} args - Arguments to info
 */
export const info = DEBUG ? console.info.bind(console) : () => {};

/**
 * Debug function - only outputs in development mode
 * @param {...any} args - Arguments to debug
 */
export const debug = DEBUG ? console.debug.bind(console) : () => {};

// Export DEBUG flag for conditional logic
export { DEBUG };
