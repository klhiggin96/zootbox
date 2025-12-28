/**
 * Coils API - Endpoints for inventory management
 *
 * Usage:
 *   import { getCoils, getCoil, getLowStockCoils } from './api/coils.js';
 *   const coils = await getCoils();
 */

import { apiClient } from './client.js';

/**
 * GET /api/v1/coils
 * Retrieve all 100 coils for the selected machine
 *
 * @returns {Promise<Array>} Array of coil objects
 *
 * Response format:
 * [
 *   {
 *     id: "A1",
 *     inventory: 10,
 *     status: "available",
 *     version: 1,
 *     link_group_id: null,
 *     updated_at: "2025-12-28T14:00:00Z"
 *   },
 *   ...
 * ]
 */
export async function getCoils() {
  return apiClient.get('/api/v1/coils');
}

/**
 * GET /api/v1/coils/{coilId}
 * Retrieve a single coil by ID
 *
 * @param {string} coilId - Coil ID (e.g., "A5", "J10")
 * @returns {Promise<Object>} Coil object
 *
 * Response format:
 * {
 *   id: "A5",
 *   inventory: 7,
 *   status: "available",
 *   version: 12,
 *   link_group_id: null,
 *   updated_at: "2025-12-28T14:30:00Z"
 * }
 *
 * @throws {APIError} 404 if coil not found
 */
export async function getCoil(coilId) {
  if (!coilId) {
    throw new Error('Coil ID is required');
  }
  return apiClient.get(`/api/v1/coils/${coilId}`);
}

/**
 * GET /api/v1/coils/low-stock
 * Retrieve coils with inventory <= 2 (low stock threshold)
 *
 * @returns {Promise<Array>} Array of low-stock coil objects
 *
 * Response format:
 * [
 *   {
 *     id: "B3",
 *     inventory: 2,
 *     status: "available",
 *     version: 7,
 *     link_group_id: null,
 *     updated_at: "2025-12-28T13:00:00Z"
 *   },
 *   ...
 * ]
 */
export async function getLowStockCoils() {
  return apiClient.get('/api/v1/coils/low-stock');
}

/**
 * Helper: Get coil grid position (row, column)
 *
 * @param {string} coilId - Coil ID (e.g., "A5")
 * @returns {Object} { row: 0, col: 4 } (0-indexed)
 */
export function getCoilPosition(coilId) {
  if (!coilId || coilId.length < 2) {
    return { row: -1, col: -1 };
  }

  const row = coilId.charCodeAt(0) - 65; // 'A' = 0, 'B' = 1, ..., 'J' = 9
  const col = parseInt(coilId.substring(1), 10) - 1; // "1" = 0, "2" = 1, ..., "10" = 9

  return { row, col };
}

/**
 * Helper: Get coil ID from grid position
 *
 * @param {number} row - Row index (0-9)
 * @param {number} col - Column index (0-9)
 * @returns {string} Coil ID (e.g., "A5")
 */
export function getCoilId(row, col) {
  if (row < 0 || row > 9 || col < 0 || col > 9) {
    return null;
  }

  const rowLetter = String.fromCharCode(65 + row); // 0 = 'A', 1 = 'B', ..., 9 = 'J'
  const colNumber = col + 1; // 0 = "1", 1 = "2", ..., 9 = "10"

  return `${rowLetter}${colNumber}`;
}

/**
 * Helper: Get coil status class for CSS styling
 *
 * @param {Object} coil - Coil object
 * @returns {string} CSS class name ('available', 'low-stock', 'empty', 'jammed')
 */
export function getCoilStatusClass(coil) {
  if (!coil) return 'empty';

  if (coil.status === 'jammed') {
    return 'jammed';
  }

  if (coil.inventory === 0) {
    return 'empty';
  }

  if (coil.inventory <= 2) {
    return 'low-stock';
  }

  return 'available';
}

/**
 * Helper: Check if coil is linked to a product
 *
 * @param {Object} coil - Coil object
 * @returns {boolean} True if linked
 */
export function isCoilLinked(coil) {
  return coil && coil.link_group_id !== null;
}
