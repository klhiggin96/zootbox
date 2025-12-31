// Base portal functionality

// API helpers
async function apiGet(endpoint) {
    try {
        const response = await fetch(`/api/v1${endpoint}`);
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        return await response.json();
    } catch (error) {
        console.error('API GET error:', error);
        throw error;
    }
}

async function apiPost(endpoint, data) {
    try {
        const response = await fetch(`/api/v1${endpoint}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data),
        });
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        return await response.json();
    } catch (error) {
        console.error('API POST error:', error);
        throw error;
    }
}

async function apiPut(endpoint, data) {
    try {
        const response = await fetch(`/api/v1${endpoint}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data),
        });
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        return await response.json();
    } catch (error) {
        console.error('API PUT error:', error);
        throw error;
    }
}

async function apiDelete(endpoint) {
    try {
        const response = await fetch(`/api/v1${endpoint}`, {
            method: 'DELETE',
        });
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        return await response.json();
    } catch (error) {
        console.error('API DELETE error:', error);
        throw error;
    }
}

// Formatting helpers
function formatCurrency(amount) {
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD',
    }).format(amount || 0);
}

function formatDateTime(timestamp) {
    const date = new Date(timestamp);
    return new Intl.DateTimeFormat('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    }).format(date);
}

function formatDate(timestamp) {
    const date = new Date(timestamp);
    return new Intl.DateTimeFormat('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
    }).format(date);
}

// Status badge helper
function getStatusBadge(status) {
    const classes = {
        'success': 'success',
        'approved': 'success',
        'refunded': 'refunded',
        'failed': 'failed',
        'declined': 'failed',
        'pending': 'pending',
    };
    const className = classes[status.toLowerCase()] || 'pending';
    return `<span class="status-badge ${className}">${status}</span>`;
}

// Payment method icons
function getPaymentMethodIcon(method) {
    const icons = {
        'card': '💳',
        'nfc': '📱',
        'cash': '💵',
        'free': '🎁',
    };
    return icons[method.toLowerCase()] || '❓';
}

// Show loading state
function showLoading(tableId) {
    const tbody = document.getElementById(tableId);
    if (tbody) {
        tbody.innerHTML = '<tr><td colspan="100" class="loading">Loading...</td></tr>';
    }
}

// Show error state
function showError(tableId, message) {
    const tbody = document.getElementById(tableId);
    if (tbody) {
        tbody.innerHTML = `<tr><td colspan="100" class="loading" style="color: #e53e3e;">${message}</td></tr>`;
    }
}

// Show empty state
function showEmpty(tableId, message) {
    const tbody = document.getElementById(tableId);
    if (tbody) {
        tbody.innerHTML = `<tr><td colspan="100" class="loading" style="color: #a0aec0;">${message}</td></tr>`;
    }
}
