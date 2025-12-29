/**
 * ThemeToggle Component - Dark/Light mode toggle
 *
 * Usage:
 *   import { initThemeToggle, getCurrentTheme } from './components/ThemeToggle.js';
 *   initThemeToggle();
 */

const THEME_KEY = 'zootbox-theme';
const THEME_DARK = 'dark';
const THEME_LIGHT = 'light';

/**
 * Initialize theme toggle functionality
 * - Loads saved theme from localStorage
 * - Applies theme to document
 * - Creates toggle button if needed
 */
export function initThemeToggle() {
  // Load saved theme or default to light
  const savedTheme = localStorage.getItem(THEME_KEY) || THEME_LIGHT;
  applyTheme(savedTheme);

  // Listen for theme toggle events
  document.addEventListener('theme-toggle', handleThemeToggle);

  // Check if system preference changed
  if (window.matchMedia) {
    const darkModeQuery = window.matchMedia('(prefers-color-scheme: dark)');
    darkModeQuery.addEventListener('change', (e) => {
      // Only apply system preference if user hasn't set a preference
      if (!localStorage.getItem(THEME_KEY)) {
        applyTheme(e.matches ? THEME_DARK : THEME_LIGHT);
      }
    });
  }
}

/**
 * Apply theme to document
 * @param {string} theme - 'dark' or 'light'
 */
function applyTheme(theme) {
  const html = document.documentElement;

  if (theme === THEME_DARK) {
    html.classList.add('dark');
  } else {
    html.classList.remove('dark');
  }

  // Save to localStorage
  localStorage.setItem(THEME_KEY, theme);

  // Update toggle buttons if they exist
  updateToggleButtons(theme);

  // Dispatch event for other components
  document.dispatchEvent(new CustomEvent('theme-changed', { detail: { theme } }));
}

/**
 * Toggle between dark and light themes
 */
export function toggleTheme() {
  const currentTheme = getCurrentTheme();
  const newTheme = currentTheme === THEME_DARK ? THEME_LIGHT : THEME_DARK;
  applyTheme(newTheme);
}

/**
 * Get current theme
 * @returns {string} 'dark' or 'light'
 */
export function getCurrentTheme() {
  return document.documentElement.classList.contains('dark') ? THEME_DARK : THEME_LIGHT;
}

/**
 * Handle theme toggle event
 */
function handleThemeToggle() {
  toggleTheme();
}

/**
 * Update all theme toggle buttons to reflect current theme
 * @param {string} theme - 'dark' or 'light'
 */
function updateToggleButtons(theme) {
  const buttons = document.querySelectorAll('[data-theme-toggle]');

  buttons.forEach(button => {
    // Update aria-label
    button.setAttribute('aria-label', `Switch to ${theme === THEME_DARK ? 'light' : 'dark'} mode`);

    // Update icon if using Material Symbols
    const icon = button.querySelector('.material-symbols-outlined');
    if (icon) {
      icon.textContent = theme === THEME_DARK ? 'light_mode' : 'dark_mode';
    }

    // Update text if present
    const text = button.querySelector('[data-theme-text]');
    if (text) {
      text.textContent = theme === THEME_DARK ? 'Light' : 'Dark';
    }
  });
}

/**
 * Create a theme toggle button
 * @param {object} options - Button configuration
 * @returns {HTMLElement} Theme toggle button
 */
export function createThemeToggleButton(options = {}) {
  const {
    showText = false,
    className = '',
    iconOnly = true
  } = options;

  const button = document.createElement('button');
  button.setAttribute('data-theme-toggle', 'true');
  button.setAttribute('aria-label', `Switch to ${getCurrentTheme() === THEME_DARK ? 'light' : 'dark'} mode`);
  button.className = `p-2 text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-white transition-colors rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 ${className}`;

  // Add icon
  const icon = document.createElement('span');
  icon.className = 'material-symbols-outlined text-[20px]';
  icon.textContent = getCurrentTheme() === THEME_DARK ? 'light_mode' : 'dark_mode';
  button.appendChild(icon);

  // Add text if requested
  if (showText) {
    const text = document.createElement('span');
    text.setAttribute('data-theme-text', 'true');
    text.className = 'ml-2 text-sm font-medium';
    text.textContent = getCurrentTheme() === THEME_DARK ? 'Light' : 'Dark';
    button.appendChild(text);
  }

  // Add click handler
  button.addEventListener('click', toggleTheme);

  return button;
}

/**
 * Set theme programmatically
 * @param {string} theme - 'dark' or 'light'
 */
export function setTheme(theme) {
  if (theme === THEME_DARK || theme === THEME_LIGHT) {
    applyTheme(theme);
  } else {
    console.warn(`Invalid theme: ${theme}. Use '${THEME_DARK}' or '${THEME_LIGHT}'`);
  }
}

/**
 * Reset theme to system preference
 */
export function resetToSystemTheme() {
  localStorage.removeItem(THEME_KEY);

  if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
    applyTheme(THEME_DARK);
  } else {
    applyTheme(THEME_LIGHT);
  }
}
