/**
 * Machines State Management - LocalStorage wrapper for machine configurations
 *
 * Usage:
 *   import { getMachines, addMachine, updateMachine, deleteMachine, getCurrentMachine } from './state/machines.js';
 *   const machines = getMachines();
 */

import { log, error } from '../utils/logger.js';

const STORAGE_KEYS = {
  MACHINES: 'zootbox.machines',
  UI_STATE: 'zootbox.uiState',
  METADATA: 'zootbox.metadata'
};

const SCHEMA_VERSION = '1.0.0';

/**
 * Initialize LocalStorage schema (run on first load)
 */
export async function initializeStorage() {
  // Check if metadata exists
  const metadata = getMetadata();

  if (!metadata) {
    // First-time setup
    setMetadata({
      schemaVersion: SCHEMA_VERSION,
      lastBackup: new Date().toISOString()
    });

    // Initialize machines array
    if (!localStorage.getItem(STORAGE_KEYS.MACHINES)) {
      setMachines([]);
    }

    // Initialize UI state
    if (!localStorage.getItem(STORAGE_KEYS.UI_STATE)) {
      setUIState(getDefaultUIState());
    }
  } else {
    // Check for schema migrations
    migrateSchema(metadata.schemaVersion);
  }

  // Update API client with current machine's endpoint
  await initializeAPIClient();
}

/**
 * Initialize API client with current machine's endpoint URL
 */
async function initializeAPIClient() {
  const currentMachine = getCurrentMachine();
  if (currentMachine && currentMachine.endpointUrl) {
    const { apiClient } = await import('../api/client.js');
    apiClient.setBaseURL(currentMachine.endpointUrl);
    log(`API client initialized with ${currentMachine.endpointUrl}`);
  }
}

/**
 * Get default UI state
 */
function getDefaultUIState() {
  return {
    currentMachineId: null,
    activePage: 'inventory',
    gridFilter: {
      showLowStock: false,
      showJammed: false,
      showEmpty: false
    },
    refreshInterval: 5,
    lastError: null
  };
}

/**
 * Migrate schema if version changed
 */
function migrateSchema(currentVersion) {
  if (currentVersion === SCHEMA_VERSION) {
    return; // No migration needed
  }

  log(`Migrating schema from ${currentVersion} to ${SCHEMA_VERSION}`);

  // Example migration: v1.0.0 → v1.1.0
  // if (currentVersion === '1.0.0') {
  //   const machines = getMachines();
  //   machines.forEach(m => m.timezone = 'UTC'); // Add new field
  //   setMachines(machines);
  //   const metadata = getMetadata();
  //   metadata.schemaVersion = '1.1.0';
  //   setMetadata(metadata);
  // }

  // Update metadata version
  const metadata = getMetadata();
  metadata.schemaVersion = SCHEMA_VERSION;
  setMetadata(metadata);
}

// ========== Metadata Operations ==========

/**
 * Get metadata
 */
function getMetadata() {
  try {
    const data = localStorage.getItem(STORAGE_KEYS.METADATA);
    return data ? JSON.parse(data) : null;
  } catch (err) {
    error('Failed to parse metadata:', err);
    return null;
  }
}

/**
 * Set metadata
 */
function setMetadata(metadata) {
  localStorage.setItem(STORAGE_KEYS.METADATA, JSON.stringify(metadata));
}

// ========== Machine CRUD Operations ==========

/**
 * Get all machines
 *
 * @returns {Array} Array of machine objects
 */
export function getMachines() {
  try {
    const data = localStorage.getItem(STORAGE_KEYS.MACHINES);
    return data ? JSON.parse(data) : [];
  } catch (err) {
    error('Failed to parse machines:', err);
    return [];
  }
}

/**
 * Set machines array (internal use)
 */
function setMachines(machines) {
  localStorage.setItem(STORAGE_KEYS.MACHINES, JSON.stringify(machines));
}

/**
 * Get machine by ID
 *
 * @param {string} machineId - Machine UUID
 * @returns {Object|null} Machine object or null if not found
 */
export function getMachine(machineId) {
  const machines = getMachines();
  return machines.find(m => m.id === machineId) || null;
}

/**
 * Add a new machine
 *
 * @param {Object} machineData - Machine data (name, endpointUrl)
 * @returns {Object} Created machine object
 */
export function addMachine(machineData) {
  const { name, endpointUrl } = machineData;

  // Validate required fields
  if (!name || typeof name !== 'string' || name.trim().length === 0) {
    throw new Error('Machine name is required');
  }

  if (!endpointUrl || !isValidUrl(endpointUrl)) {
    throw new Error('Valid endpoint URL is required');
  }

  // Create machine object
  const machine = {
    id: crypto.randomUUID(),
    name: name.trim(),
    endpointUrl: endpointUrl.trim(),
    status: 'offline', // Will be updated by health check
    lastSyncTime: null,
    createdAt: new Date().toISOString()
  };

  // Add to machines array
  const machines = getMachines();
  machines.push(machine);
  setMachines(machines);

  return machine;
}

/**
 * Update an existing machine
 *
 * @param {string} machineId - Machine UUID
 * @param {Object} updates - Fields to update (name, endpointUrl, status, lastSyncTime)
 * @returns {Object} Updated machine object
 */
export function updateMachine(machineId, updates) {
  const machines = getMachines();
  const index = machines.findIndex(m => m.id === machineId);

  if (index === -1) {
    throw new Error(`Machine ${machineId} not found`);
  }

  // Validate updates
  if (updates.name !== undefined) {
    if (typeof updates.name !== 'string' || updates.name.trim().length === 0) {
      throw new Error('Machine name cannot be empty');
    }
    updates.name = updates.name.trim();
  }

  if (updates.endpointUrl !== undefined) {
    if (!isValidUrl(updates.endpointUrl)) {
      throw new Error('Invalid endpoint URL');
    }
    updates.endpointUrl = updates.endpointUrl.trim();
  }

  // Update machine
  machines[index] = {
    ...machines[index],
    ...updates
  };

  setMachines(machines);
  return machines[index];
}

/**
 * Delete a machine
 *
 * @param {string} machineId - Machine UUID
 * @returns {boolean} True if deleted
 */
export function deleteMachine(machineId) {
  const machines = getMachines();
  const index = machines.findIndex(m => m.id === machineId);

  if (index === -1) {
    return false;
  }

  machines.splice(index, 1);
  setMachines(machines);

  // If this was the current machine, clear selection
  const uiState = getUIState();
  if (uiState.currentMachineId === machineId) {
    setCurrentMachine(null);
  }

  return true;
}

// ========== UI State Operations ==========

/**
 * Get UI state
 *
 * @returns {Object} UI state object
 */
export function getUIState() {
  try {
    const data = localStorage.getItem(STORAGE_KEYS.UI_STATE);
    return data ? JSON.parse(data) : getDefaultUIState();
  } catch (err) {
    error('Failed to parse UI state:', err);
    return getDefaultUIState();
  }
}

/**
 * Set UI state (internal use)
 */
function setUIState(state) {
  localStorage.setItem(STORAGE_KEYS.UI_STATE, JSON.stringify(state));
}

/**
 * Update UI state fields
 *
 * @param {Object} updates - Fields to update
 */
export function updateUIState(updates) {
  const state = getUIState();
  const newState = { ...state, ...updates };
  setUIState(newState);
}

/**
 * Get current machine ID
 *
 * @returns {string|null} Current machine UUID or null
 */
export function getCurrentMachineId() {
  const state = getUIState();
  return state.currentMachineId;
}

/**
 * Get current machine object
 *
 * @returns {Object|null} Current machine or null
 */
export function getCurrentMachine() {
  const machineId = getCurrentMachineId();
  return machineId ? getMachine(machineId) : null;
}

/**
 * Set current machine
 *
 * @param {string|null} machineId - Machine UUID or null to clear
 */
export function setCurrentMachine(machineId) {
  if (machineId !== null && !getMachine(machineId)) {
    throw new Error(`Machine ${machineId} not found`);
  }

  updateUIState({ currentMachineId: machineId });

  // Update API client baseURL when machine changes
  if (machineId) {
    const machine = getMachine(machineId);
    if (machine && machine.endpointUrl) {
      import('../api/client.js').then(({ apiClient }) => {
        apiClient.setBaseURL(machine.endpointUrl);
        log(`API client updated to ${machine.endpointUrl}`);
      });
    }
  }
}

/**
 * Get grid filter settings
 *
 * @returns {Object} Grid filter object
 */
export function getGridFilter() {
  const state = getUIState();
  return state.gridFilter;
}

/**
 * Update grid filter settings
 *
 * @param {Object} filters - Filter updates
 */
export function setGridFilter(filters) {
  const state = getUIState();
  const newFilter = { ...state.gridFilter, ...filters };
  updateUIState({ gridFilter: newFilter });
}

// ========== Validation Helpers ==========

/**
 * Validate URL format
 *
 * @param {string} url - URL to validate
 * @returns {boolean} True if valid
 */
function isValidUrl(url) {
  try {
    const parsedUrl = new URL(url);
    return parsedUrl.protocol === 'http:' || parsedUrl.protocol === 'https:';
  } catch (error) {
    return false;
  }
}

/**
 * Validate machine name length
 *
 * @param {string} name - Machine name
 * @returns {boolean} True if valid (1-50 characters)
 */
export function isValidMachineName(name) {
  return typeof name === 'string' && name.trim().length > 0 && name.trim().length <= 50;
}

// ========== Export/Import Operations ==========

/**
 * Export all machines as JSON
 *
 * @returns {string} JSON string of all machines
 */
export function exportMachines() {
  const machines = getMachines();
  return JSON.stringify(machines, null, 2);
}

/**
 * Import machines from JSON
 *
 * @param {string} jsonString - JSON string of machines
 * @param {boolean} merge - If true, merge with existing; if false, replace
 * @returns {number} Number of machines imported
 */
export function importMachines(jsonString, merge = false) {
  try {
    const importedMachines = JSON.parse(jsonString);

    if (!Array.isArray(importedMachines)) {
      throw new Error('Invalid format: must be an array');
    }

    const existingMachines = merge ? getMachines() : [];
    const newMachines = [...existingMachines];

    // Add imported machines (avoid duplicates by ID)
    importedMachines.forEach(machine => {
      const exists = newMachines.find(m => m.id === machine.id);
      if (!exists) {
        newMachines.push(machine);
      }
    });

    setMachines(newMachines);
    return importedMachines.length;

  } catch (error) {
    throw new Error(`Import failed: ${error.message}`);
  }
}

// ========== Initialization ==========

// Auto-initialize on module load
initializeStorage();
