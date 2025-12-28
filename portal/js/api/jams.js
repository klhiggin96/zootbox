/**
 * Jams API - Endpoints for jam event management
 *
 * Usage:
 *   import { getJamEvents, resolveJamEvent } from './api/jams.js';
 *   const openJams = await getJamEvents('open');
 */

import { apiClient } from './client.js';

/**
 * GET /api/v1/jam-events
 * Retrieve jam events with optional status filter
 *
 * @param {string} status - Filter by status: "open", "resolved", "all" (default: all)
 * @returns {Promise<Array>} Array of jam event objects
 *
 * Response format:
 * [
 *   {
 *     id: "e47ac10b-58cc-4372-a567-0e02b2c3d480",
 *     coil_id: "B3",
 *     timestamp: "2025-12-28T13:15:00Z",
 *     status: "open",
 *     resolved_at: null
 *   },
 *   {
 *     id: "f58bd21d-69dd-5594-c789-2g24d4e5f691",
 *     coil_id: "G8",
 *     timestamp: "2025-12-28T11:00:00Z",
 *     status: "resolved",
 *     resolved_at: "2025-12-28T11:30:00Z"
 *   }
 * ]
 */
export async function getJamEvents(status = 'all') {
  const validStatuses = ['open', 'resolved', 'all'];

  if (!validStatuses.includes(status)) {
    throw new Error(`Invalid status filter. Must be one of: ${validStatuses.join(', ')}`);
  }

  // If status is 'all', don't include query parameter
  if (status === 'all') {
    return apiClient.get('/api/v1/jam-events');
  }

  return apiClient.get(`/api/v1/jam-events?status=${status}`);
}

/**
 * GET /api/v1/jam-events (open only)
 * Convenience method to get only open jam events
 *
 * @returns {Promise<Array>} Array of open jam event objects
 */
export async function getOpenJams() {
  return getJamEvents('open');
}

/**
 * GET /api/v1/jam-events (resolved only)
 * Convenience method to get only resolved jam events
 *
 * @returns {Promise<Array>} Array of resolved jam event objects
 */
export async function getResolvedJams() {
  return getJamEvents('resolved');
}

/**
 * POST /api/v1/jam-events/{eventId}/resolve
 * Resolve an open jam event
 *
 * @param {string} eventId - Jam event UUID
 * @returns {Promise<Object>} Resolution result
 *
 * Response format:
 * {
 *   success: true,
 *   jam_event_id: "e47ac10b-58cc-4372-a567-0e02b2c3d480",
 *   coil_id: "B3",
 *   resolved_at: "2025-12-28T14:35:00Z"
 * }
 *
 * Side effects (backend):
 * - Sets jam_events.status = 'resolved'
 * - Sets jam_events.resolved_at = NOW()
 * - Updates coils.status = 'available' (if inventory > 0)
 *
 * @throws {APIError} 404 if jam event not found
 * @throws {APIError} 400 if jam event already resolved
 */
export async function resolveJamEvent(eventId) {
  if (!eventId) {
    throw new Error('Event ID is required');
  }

  return apiClient.post(`/api/v1/jam-events/${eventId}/resolve`);
}

/**
 * Helper: Check if jam event is open
 *
 * @param {Object} jamEvent - Jam event object
 * @returns {boolean} True if jam is open
 */
export function isJamOpen(jamEvent) {
  return jamEvent && jamEvent.status === 'open';
}

/**
 * Helper: Check if jam event is resolved
 *
 * @param {Object} jamEvent - Jam event object
 * @returns {boolean} True if jam is resolved
 */
export function isJamResolved(jamEvent) {
  return jamEvent && jamEvent.status === 'resolved';
}

/**
 * Helper: Get jam duration in milliseconds
 *
 * @param {Object} jamEvent - Jam event object
 * @returns {number} Duration in milliseconds (or current time if not resolved)
 */
export function getJamDuration(jamEvent) {
  if (!jamEvent || !jamEvent.timestamp) {
    return 0;
  }

  const startTime = new Date(jamEvent.timestamp).getTime();
  const endTime = jamEvent.resolved_at
    ? new Date(jamEvent.resolved_at).getTime()
    : Date.now();

  return endTime - startTime;
}

/**
 * Helper: Sort jam events by timestamp (newest first)
 *
 * @param {Array} jamEvents - Array of jam event objects
 * @returns {Array} Sorted array
 */
export function sortJamsByTimestamp(jamEvents) {
  if (!Array.isArray(jamEvents)) {
    return [];
  }

  return [...jamEvents].sort((a, b) => {
    const timeA = new Date(a.timestamp).getTime();
    const timeB = new Date(b.timestamp).getTime();
    return timeB - timeA; // Newest first
  });
}

/**
 * Helper: Group jams by coil ID
 *
 * @param {Array} jamEvents - Array of jam event objects
 * @returns {Object} Map of coilId -> jam events array
 */
export function groupJamsByCoil(jamEvents) {
  if (!Array.isArray(jamEvents)) {
    return {};
  }

  return jamEvents.reduce((groups, jam) => {
    const coilId = jam.coil_id;
    if (!groups[coilId]) {
      groups[coilId] = [];
    }
    groups[coilId].push(jam);
    return groups;
  }, {});
}
