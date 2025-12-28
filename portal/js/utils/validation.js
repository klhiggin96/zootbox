/**
 * Validation Utilities - Input validators for portal forms
 *
 * Usage:
 *   import { validateCoilId, validateInventory, validateUrl } from './utils/validation.js';
 *   const result = validateInventory(15);
 */

// ========== Coil ID Validation ==========

/**
 * Validate coil ID pattern (A1-J10)
 *
 * @param {string} coilId - Coil ID to validate
 * @returns {Object} { valid: boolean, error: string|null }
 */
export function validateCoilId(coilId) {
  if (!coilId || typeof coilId !== 'string') {
    return {
      valid: false,
      error: 'Coil ID is required'
    };
  }

  const pattern = /^[A-J]([1-9]|10)$/;

  if (!pattern.test(coilId)) {
    return {
      valid: false,
      error: 'Coil ID must be A1-J10 (e.g., A5, J10)'
    };
  }

  return { valid: true, error: null };
}

/**
 * Check if coil ID is valid (boolean)
 *
 * @param {string} coilId - Coil ID to check
 * @returns {boolean} True if valid
 */
export function isValidCoilId(coilId) {
  return validateCoilId(coilId).valid;
}

// ========== Inventory Validation ==========

/**
 * Validate inventory value (0-10)
 *
 * @param {number|string} inventory - Inventory value to validate
 * @returns {Object} { valid: boolean, error: string|null, value: number }
 */
export function validateInventory(inventory) {
  // Convert to number if string
  const value = typeof inventory === 'string' ? parseInt(inventory, 10) : inventory;

  if (isNaN(value)) {
    return {
      valid: false,
      error: 'Inventory must be a number',
      value: null
    };
  }

  if (!Number.isInteger(value)) {
    return {
      valid: false,
      error: 'Inventory must be a whole number',
      value: null
    };
  }

  if (value < 0 || value > 10) {
    return {
      valid: false,
      error: 'Inventory must be between 0 and 10',
      value: null
    };
  }

  return { valid: true, error: null, value };
}

/**
 * Check if inventory value is valid (boolean)
 *
 * @param {number} inventory - Inventory value to check
 * @returns {boolean} True if valid
 */
export function isValidInventory(inventory) {
  return validateInventory(inventory).valid;
}

// ========== URL Validation ==========

/**
 * Validate endpoint URL (http/https)
 *
 * @param {string} url - URL to validate
 * @returns {Object} { valid: boolean, error: string|null }
 */
export function validateUrl(url) {
  if (!url || typeof url !== 'string') {
    return {
      valid: false,
      error: 'URL is required'
    };
  }

  const trimmed = url.trim();

  if (trimmed.length === 0) {
    return {
      valid: false,
      error: 'URL cannot be empty'
    };
  }

  try {
    const parsed = new URL(trimmed);

    if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') {
      return {
        valid: false,
        error: 'URL must start with http:// or https://'
      };
    }

    return { valid: true, error: null };

  } catch (error) {
    return {
      valid: false,
      error: 'Invalid URL format'
    };
  }
}

/**
 * Check if URL is valid (boolean)
 *
 * @param {string} url - URL to check
 * @returns {boolean} True if valid
 */
export function isValidUrl(url) {
  return validateUrl(url).valid;
}

// ========== Product SKU Validation ==========

/**
 * Validate product SKU (1-100 characters, non-empty)
 *
 * @param {string} sku - Product SKU to validate
 * @returns {Object} { valid: boolean, error: string|null }
 */
export function validateProductSku(sku) {
  if (!sku || typeof sku !== 'string') {
    return {
      valid: false,
      error: 'Product SKU is required'
    };
  }

  const trimmed = sku.trim();

  if (trimmed.length === 0) {
    return {
      valid: false,
      error: 'Product SKU cannot be empty'
    };
  }

  if (trimmed.length > 100) {
    return {
      valid: false,
      error: 'Product SKU must be 100 characters or less'
    };
  }

  return { valid: true, error: null };
}

/**
 * Check if product SKU is valid (boolean)
 *
 * @param {string} sku - Product SKU to check
 * @returns {boolean} True if valid
 */
export function isValidProductSku(sku) {
  return validateProductSku(sku).valid;
}

// ========== Machine Name Validation ==========

/**
 * Validate machine name (1-50 characters)
 *
 * @param {string} name - Machine name to validate
 * @returns {Object} { valid: boolean, error: string|null }
 */
export function validateMachineName(name) {
  if (!name || typeof name !== 'string') {
    return {
      valid: false,
      error: 'Machine name is required'
    };
  }

  const trimmed = name.trim();

  if (trimmed.length === 0) {
    return {
      valid: false,
      error: 'Machine name cannot be empty'
    };
  }

  if (trimmed.length > 50) {
    return {
      valid: false,
      error: 'Machine name must be 50 characters or less'
    };
  }

  return { valid: true, error: null };
}

/**
 * Check if machine name is valid (boolean)
 *
 * @param {string} name - Machine name to check
 * @returns {boolean} True if valid
 */
export function isValidMachineName(name) {
  return validateMachineName(name).valid;
}

// ========== Form Validation ==========

/**
 * Validate entire form data
 * Returns all errors for all fields
 *
 * @param {Object} formData - Form data to validate
 * @param {Object} schema - Validation schema (field -> validator function)
 * @returns {Object} { valid: boolean, errors: Object }
 *
 * Example:
 *   const schema = {
 *     coilId: validateCoilId,
 *     inventory: validateInventory
 *   };
 *   const result = validateForm({ coilId: 'A5', inventory: 15 }, schema);
 */
export function validateForm(formData, schema) {
  const errors = {};
  let hasErrors = false;

  Object.keys(schema).forEach(field => {
    const validator = schema[field];
    const value = formData[field];
    const result = validator(value);

    if (!result.valid) {
      errors[field] = result.error;
      hasErrors = true;
    }
  });

  return {
    valid: !hasErrors,
    errors
  };
}

/**
 * Show inline validation error on form field
 *
 * @param {HTMLElement} inputElement - Input element
 * @param {string} errorMessage - Error message to display
 */
export function showFieldError(inputElement, errorMessage) {
  if (!inputElement) return;

  inputElement.classList.add('error');

  // Remove existing error message
  const existingError = inputElement.parentElement?.querySelector('.form-error');
  if (existingError) {
    existingError.remove();
  }

  // Add new error message
  if (errorMessage) {
    const errorElement = document.createElement('span');
    errorElement.className = 'form-error';
    errorElement.textContent = errorMessage;
    inputElement.parentElement?.appendChild(errorElement);
  }
}

/**
 * Clear inline validation error on form field
 *
 * @param {HTMLElement} inputElement - Input element
 */
export function clearFieldError(inputElement) {
  if (!inputElement) return;

  inputElement.classList.remove('error');

  const errorElement = inputElement.parentElement?.querySelector('.form-error');
  if (errorElement) {
    errorElement.remove();
  }
}

/**
 * Validate form on submit
 * Displays inline errors and prevents submission if invalid
 *
 * @param {HTMLFormElement} formElement - Form element
 * @param {Object} schema - Validation schema
 * @param {Function} onValid - Callback when form is valid
 */
export function validateFormOnSubmit(formElement, schema, onValid) {
  if (!formElement) return;

  formElement.addEventListener('submit', (e) => {
    e.preventDefault();

    // Get form data
    const formData = new FormData(formElement);
    const data = Object.fromEntries(formData.entries());

    // Validate
    const result = validateForm(data, schema);

    // Clear all previous errors
    formElement.querySelectorAll('input, select, textarea').forEach(input => {
      clearFieldError(input);
    });

    if (!result.valid) {
      // Show errors
      Object.keys(result.errors).forEach(field => {
        const input = formElement.querySelector(`[name="${field}"]`);
        if (input) {
          showFieldError(input, result.errors[field]);
        }
      });

      // Focus first error
      const firstErrorField = Object.keys(result.errors)[0];
      const firstInput = formElement.querySelector(`[name="${firstErrorField}"]`);
      firstInput?.focus();

      return;
    }

    // Form is valid, call callback
    if (typeof onValid === 'function') {
      onValid(data);
    }
  });
}

// ========== Real-time Validation ==========

/**
 * Add real-time validation to input field
 *
 * @param {HTMLElement} inputElement - Input element
 * @param {Function} validator - Validator function
 * @param {number} debounceMs - Debounce delay (default: 500ms)
 */
export function addRealTimeValidation(inputElement, validator, debounceMs = 500) {
  if (!inputElement || typeof validator !== 'function') {
    return;
  }

  let timeout;

  inputElement.addEventListener('input', () => {
    clearTimeout(timeout);

    timeout = setTimeout(() => {
      const result = validator(inputElement.value);

      if (!result.valid) {
        showFieldError(inputElement, result.error);
      } else {
        clearFieldError(inputElement);
      }
    }, debounceMs);
  });

  // Clear error on focus
  inputElement.addEventListener('focus', () => {
    clearFieldError(inputElement);
  });
}
