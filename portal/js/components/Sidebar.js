/**
 * Sidebar Component - Left navigation sidebar
 *
 * Usage:
 *   import { renderSidebar } from './components/Sidebar.js';
 *   renderSidebar(document.getElementById('sidebar'));
 */

import { getCurrentMachineId, getMachines } from '../state/machines.js';

/**
 * Render the sidebar navigation
 * @param {HTMLElement} container - Container element for sidebar
 */
export function renderSidebar(container, currentPage = 'inventory') {
  if (!container) {
    console.error('Sidebar container not found');
    return;
  }

  // Get current machine for display
  const currentMachineId = getCurrentMachineId();
  const machines = getMachines();
  const currentMachine = machines.find(m => m.id === currentMachineId);
  const machineName = currentMachine ? currentMachine.name : 'No Machine';
  const machineId = currentMachine ? `#${currentMachineId.substring(0, 8)}` : '';

  container.innerHTML = `
    <div class="flex flex-col h-full p-4 justify-between">
      <!-- Top: Logo + Nav links -->
      <div class="flex flex-col gap-4">
        <!-- Logo -->
        <div class="flex gap-3 items-center mb-6">
          <div class="bg-primary/20 flex items-center justify-center rounded-lg size-10 text-primary">
            <span class="material-symbols-outlined text-2xl">grid_view</span>
          </div>
          <div class="flex flex-col">
            <h1 class="text-base font-bold leading-tight">Vending Admin</h1>
            <p class="text-slate-500 dark:text-[#9cabba] text-xs font-normal">${machineName}</p>
          </div>
        </div>

        <!-- Nav links -->
        <nav class="flex flex-col gap-2">
          <a href="index.html" class="flex items-center gap-3 px-3 py-2 rounded-lg ${currentPage === 'inventory' ? 'bg-primary/10 text-primary' : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'} transition-colors">
            <span class="material-symbols-outlined ${currentPage === 'inventory' ? 'fill-1' : ''}">grid_on</span>
            <span class="text-sm font-medium">Inventory Map</span>
          </a>
          <a href="jam-management.html" class="flex items-center gap-3 px-3 py-2 rounded-lg ${currentPage === 'jams' ? 'bg-primary/10 text-primary' : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'} transition-colors">
            <span class="material-symbols-outlined ${currentPage === 'jams' ? 'fill-1' : ''}">warning</span>
            <span class="text-sm font-medium">Jam Management</span>
          </a>
          <a href="product-links.html" class="flex items-center gap-3 px-3 py-2 rounded-lg ${currentPage === 'products' ? 'bg-primary/10 text-primary' : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'} transition-colors">
            <span class="material-symbols-outlined ${currentPage === 'products' ? 'fill-1' : ''}">link</span>
            <span class="text-sm font-medium">Product Links</span>
          </a>
          <a href="machine-settings.html" class="flex items-center gap-3 px-3 py-2 rounded-lg ${currentPage === 'settings' ? 'bg-primary/10 text-primary' : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'} transition-colors">
            <span class="material-symbols-outlined ${currentPage === 'settings' ? 'fill-1' : ''}">settings</span>
            <span class="text-sm font-medium">Machine Settings</span>
          </a>
        </nav>
      </div>

      <!-- Bottom: User profile + Log out -->
      <div class="p-4 rounded-xl bg-slate-50 dark:bg-[#1e2732] border border-slate-100 dark:border-slate-800">
        <div class="flex items-center gap-3 mb-3">
          <div class="bg-center bg-no-repeat aspect-square bg-cover rounded-full size-10 bg-gradient-to-br from-purple-400 to-pink-600"></div>
          <div>
            <p class="text-sm font-bold">Portal User</p>
            <p class="text-xs text-slate-500">Operator</p>
          </div>
        </div>
        <button class="w-full py-2 text-xs font-medium text-slate-500 dark:text-slate-400 border border-slate-200 dark:border-slate-700 rounded hover:bg-slate-100 dark:hover:bg-slate-700 transition-colors" id="logout-btn">
          Log Out
        </button>
      </div>
    </div>
  `;

  // Add event listeners
  const logoutBtn = container.querySelector('#logout-btn');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', handleLogout);
  }
}

/**
 * Handle logout action
 */
function handleLogout() {
  // For now, just reload the page
  // In the future, this could clear session data, redirect to login, etc.
  if (confirm('Are you sure you want to log out?')) {
    // Clear session storage
    sessionStorage.clear();
    location.reload();
  }
}

/**
 * Update sidebar with current machine info
 */
export function updateSidebarMachine() {
  const sidebar = document.getElementById('sidebar');
  if (sidebar) {
    const currentPage = detectCurrentPage();
    renderSidebar(sidebar, currentPage);
  }
}

/**
 * Detect current page from URL
 * @returns {string} Page identifier
 */
function detectCurrentPage() {
  const path = window.location.pathname;

  if (path.includes('machine-settings')) return 'settings';
  if (path.includes('jam-management')) return 'jams';
  if (path.includes('product-links')) return 'products';
  if (path.includes('index')) return 'inventory';

  return 'inventory'; // default
}

/**
 * Initialize sidebar
 */
export function initSidebar() {
  const sidebar = document.getElementById('sidebar');
  if (!sidebar) return;

  const currentPage = detectCurrentPage();
  renderSidebar(sidebar, currentPage);

  // Listen for machine changes
  document.addEventListener('machine-changed', () => {
    updateSidebarMachine();
  });
}
