/**
 * Admin API - Endpoints for administrative operations
 *
 * Usage:
 *   import { refillAllCoils, updateCoilInventory } from './api/admin.js';
 *   await refillAllCoils();
 */

import { apiClient } from './client.js';

/**
 * POST /api/v1/admin/refill
 * Refill all 100 coils to maximum inventory (10 units)
 *
 * @returns {Promise<Object>} Refill result
 *
 * Response format:
 * {
 *   success: true,
 *   refilled_count: 100,
 *   timestamp: "2025-12-28T14:45:00Z"
 * }
 */
export async function refillAllCoils() {
  return apiClient.post('/api/v1/admin/refill');
}

/**
 * PUT /api/v1/admin/coils/{coilId}
 * Manually set inventory for a specific coil
 *
 * @param {string} coilId - Coil ID (e.g., "A5")
 * @param {number} inventory - New inventory value (0-10)
 * @returns {Promise<Object>} Updated coil object
 *
 * Request format:
 * {
 *   inventory: 5
 * }
 *
 * Response format:
 * {
 *   id: "A5",
 *   inventory: 5,
 *   status: "available",
 *   version: 13,
 *   link_group_id: null,
 *   updated_at: "2025-12-28T14:50:00Z"
 * }
 *
 * @throws {APIError} 400 if inventory out of range (0-10)
 * @throws {APIError} 404 if coil not found
 * @throws {APIError} 409 if optimistic locking conflict (coil updated by another user)
 */
export async function updateCoilInventory(coilId, inventory) {
  if (!coilId) {
    throw new Error('Coil ID is required');
  }

  if (typeof inventory !== 'number' || inventory < 0 || inventory > 10) {
    throw new Error('Inventory must be a number between 0 and 10');
  }

  return apiClient.put(`/api/v1/admin/coils/${coilId}`, {
    inventory
  });
}

/**
 * Helper: Validate refill operation
 * Checks if user wants to proceed with refilling all coils
 *
 * @returns {Promise<boolean>} True if user confirms
 */
export async function confirmRefillAll() {
  return new Promise((resolve) => {
    const confirmed = window.confirm(
      'Refill all 100 coils to inventory = 10?\n\n' +
      'This will reset all coils regardless of current inventory levels.'
    );
    resolve(confirmed);
  });
}

/**
 * Helper: Validate coil inventory update
 * Checks if the new inventory value is different from current
 *
 * @param {Object} coil - Current coil object
 * @param {number} newInventory - New inventory value
 * @returns {boolean} True if update is valid
 */
export function shouldUpdateInventory(coil, newInventory) {
  if (!coil) {
    return false;
  }

  if (coil.inventory === newInventory) {
    return false; // No change
  }

  return true;
}
