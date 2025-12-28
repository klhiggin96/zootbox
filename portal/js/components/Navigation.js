/**
 * Navigation Component - Loads navigation template
 *
 * Usage:
 *   import { loadNavigation } from './components/Navigation.js';
 *   await loadNavigation();
 */

/**
 * Load navigation from nav.html
 */
export async function loadNavigation() {
  const navContainer = document.getElementById('nav-container');

  if (!navContainer) {
    console.error('Nav container not found');
    return;
  }

  try {
    const response = await fetch('/nav.html');
    const html = await response.text();
    navContainer.innerHTML = html;

    // Highlight active page
    highlightActivePage();

  } catch (error) {
    console.error('Failed to load navigation:', error);
    navContainer.innerHTML = '<div style="padding: 20px; background: #fee2e2; color: #991b1b;">Failed to load navigation</div>';
  }
}

/**
 * Highlight active page link
 */
function highlightActivePage() {
  const currentPage = getCurrentPage();
  const links = document.querySelectorAll('.nav-link');

  links.forEach(link => {
    const page = link.getAttribute('data-page');
    if (page === currentPage) {
      link.classList.add('active');
    } else {
      link.classList.remove('active');
    }
  });
}

/**
 * Get current page from URL
 */
function getCurrentPage() {
  const path = window.location.pathname;
  const filename = path.split('/').pop();

  if (!filename || filename === 'index.html') {
    return 'inventory';
  }

  if (filename === 'jam-management.html') {
    return 'jams';
  }

  if (filename === 'product-links.html') {
    return 'products';
  }

  if (filename === 'machine-settings.html') {
    return 'settings';
  }

  return 'inventory';
}
