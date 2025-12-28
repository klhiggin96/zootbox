/**
 * Products API - Endpoints for product link management
 *
 * Usage:
 *   import { getProductLinks, createProductLink, deleteProductLink } from './api/products.js';
 *   const links = await getProductLinks();
 */

import { apiClient } from './client.js';

/**
 * GET /api/v1/admin/product-links
 * Retrieve all product link configurations
 *
 * @returns {Promise<Array>} Array of product link objects
 *
 * Response format:
 * [
 *   {
 *     link_group_id: "c47ac10b-58cc-4372-a567-0e02b2c3d481",
 *     product_sku: "COKE-001",
 *     linked_coil_ids: "[\"A1\",\"A2\",\"A3\"]",  // JSON string
 *     selection_strategy: "first_available",
 *     created_at: "2025-12-27T10:00:00Z"
 *   },
 *   ...
 * ]
 */
export async function getProductLinks() {
  return apiClient.get('/api/v1/admin/product-links');
}

/**
 * POST /api/v1/admin/product-links
 * Create a new product link group
 *
 * @param {string} productSku - Product SKU (1-100 characters)
 * @param {Array<string>} linkedCoilIds - Array of coil IDs (1-10 coils, A1-J10)
 * @returns {Promise<Object>} Created product link object
 *
 * Request format:
 * {
 *   product_sku: "SPRITE-003",
 *   linked_coil_ids: ["C1", "C2", "C3"]
 * }
 *
 * Response format:
 * {
 *   link_group_id: "a69ce32e-70ee-6605-d890-3h35e5f6g702",
 *   product_sku: "SPRITE-003",
 *   linked_coil_ids: "[\"C1\",\"C2\",\"C3\"]",
 *   selection_strategy: "first_available",
 *   created_at: "2025-12-28T14:40:00Z"
 * }
 *
 * Validation rules:
 * - product_sku: Non-empty string (1-100 characters)
 * - linked_coil_ids: Array of 1-10 valid coil IDs (A1-J10)
 * - Coils cannot belong to multiple link groups (unique constraint)
 *
 * @throws {APIError} 400 if validation fails or coil already linked
 */
export async function createProductLink(productSku, linkedCoilIds) {
  if (!productSku || typeof productSku !== 'string') {
    throw new Error('Product SKU is required');
  }

  if (!Array.isArray(linkedCoilIds) || linkedCoilIds.length === 0) {
    throw new Error('At least one coil ID is required');
  }

  if (linkedCoilIds.length > 10) {
    throw new Error('Maximum 10 coils can be linked to a product');
  }

  return apiClient.post('/api/v1/admin/product-links', {
    product_sku: productSku,
    linked_coil_ids: linkedCoilIds
  });
}

/**
 * DELETE /api/v1/admin/product-links/{linkGroupId}
 * Delete a product link group
 *
 * @param {string} linkGroupId - Product link group UUID
 * @returns {Promise<null>} No content (204)
 *
 * Side effects (backend):
 * - Deletes product_links row
 * - Sets coils.link_group_id = NULL for all linked coils
 *
 * @throws {APIError} 404 if product link not found
 */
export async function deleteProductLink(linkGroupId) {
  if (!linkGroupId) {
    throw new Error('Link group ID is required');
  }

  return apiClient.delete(`/api/v1/admin/product-links/${linkGroupId}`);
}

/**
 * GET /api/v1/product-links/{sku}/resolve
 * Resolve product SKU to available coil (used by Android app during purchase)
 * NOTE: This endpoint is NOT used by the portal (Android app only)
 *
 * @param {string} sku - Product SKU
 * @returns {Promise<Object>} Resolution result
 *
 * Response format:
 * {
 *   product_sku: "COKE-001",
 *   selected_coil_id: "A2",
 *   link_group_id: "c47ac10b-58cc-4372-a567-0e02b2c3d481",
 *   strategy: "first_available"
 * }
 *
 * @throws {APIError} 404 if product SKU not found or no coils available
 */
export async function resolveProductSku(sku) {
  if (!sku) {
    throw new Error('Product SKU is required');
  }

  return apiClient.get(`/api/v1/product-links/${sku}/resolve`);
}

/**
 * Helper: Parse linked_coil_ids JSON string to array
 *
 * @param {string} linkedCoilIdsJson - JSON string (e.g., "[\"A1\",\"A2\",\"A3\"]")
 * @returns {Array<string>} Array of coil IDs
 */
export function parseLinkedCoilIds(linkedCoilIdsJson) {
  if (!linkedCoilIdsJson) {
    return [];
  }

  try {
    const parsed = JSON.parse(linkedCoilIdsJson);
    return Array.isArray(parsed) ? parsed : [];
  } catch (error) {
    console.error('Failed to parse linked_coil_ids:', error);
    return [];
  }
}

/**
 * Helper: Format linked coil IDs for display
 *
 * @param {string|Array} linkedCoilIds - JSON string or array of coil IDs
 * @returns {string} Human-readable string (e.g., "A1, A2, A3")
 */
export function formatLinkedCoilIds(linkedCoilIds) {
  const coilArray = typeof linkedCoilIds === 'string'
    ? parseLinkedCoilIds(linkedCoilIds)
    : linkedCoilIds;

  return coilArray.join(', ');
}

/**
 * Helper: Check if coil is linked to a product
 *
 * @param {string} coilId - Coil ID to check
 * @param {Array} productLinks - Array of product link objects
 * @returns {Object|null} Product link object if linked, null otherwise
 */
export function findProductLinkByCoil(coilId, productLinks) {
  if (!coilId || !Array.isArray(productLinks)) {
    return null;
  }

  return productLinks.find(link => {
    const linkedCoils = parseLinkedCoilIds(link.linked_coil_ids);
    return linkedCoils.includes(coilId);
  }) || null;
}

/**
 * Helper: Validate coil IDs before creating product link
 *
 * @param {Array<string>} coilIds - Array of coil IDs to validate
 * @param {Array} existingLinks - Array of existing product link objects
 * @returns {Object} { valid: boolean, errors: Array<string> }
 */
export function validateCoilIds(coilIds, existingLinks = []) {
  const errors = [];

  if (!Array.isArray(coilIds) || coilIds.length === 0) {
    errors.push('At least one coil must be selected');
    return { valid: false, errors };
  }

  if (coilIds.length > 10) {
    errors.push('Maximum 10 coils can be linked to a product');
  }

  // Check for invalid coil ID patterns
  const validPattern = /^[A-J]([1-9]|10)$/;
  const invalidCoils = coilIds.filter(id => !validPattern.test(id));
  if (invalidCoils.length > 0) {
    errors.push(`Invalid coil IDs: ${invalidCoils.join(', ')}`);
  }

  // Check for duplicates within the selection
  const duplicates = coilIds.filter((id, index) => coilIds.indexOf(id) !== index);
  if (duplicates.length > 0) {
    errors.push(`Duplicate coil IDs: ${duplicates.join(', ')}`);
  }

  // Check if coils are already linked
  const alreadyLinked = [];
  coilIds.forEach(coilId => {
    const existingLink = findProductLinkByCoil(coilId, existingLinks);
    if (existingLink) {
      alreadyLinked.push(`${coilId} (linked to ${existingLink.product_sku})`);
    }
  });

  if (alreadyLinked.length > 0) {
    errors.push(`Already linked: ${alreadyLinked.join(', ')}`);
  }

  return {
    valid: errors.length === 0,
    errors
  };
}
