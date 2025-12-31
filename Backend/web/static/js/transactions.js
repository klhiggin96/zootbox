// Transactions/Payment Log page functionality

let allTransactions = [];
let filteredTransactions = [];
let currentPage = 1;
const itemsPerPage = 50;

// Load transactions on page load
document.addEventListener('DOMContentLoaded', () => {
    loadTransactions();

    // Set default date range (last 7 days)
    const endDate = new Date();
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - 7);

    document.getElementById('filter-end-date').valueAsDate = endDate;
    document.getElementById('filter-start-date').valueAsDate = startDate;
});

// Load all transactions
async function loadTransactions() {
    try {
        showLoading('transactions-table');

        // Get filter values
        const startDate = document.getElementById('filter-start-date').value;
        const endDate = document.getElementById('filter-end-date').value;

        let endpoint = '/portal/analytics/transactions?limit=1000';
        if (startDate) endpoint += `&start=${startDate}`;
        if (endDate) endpoint += `&end=${endDate}`;

        const data = await apiGet(endpoint);
        allTransactions = data || [];
        filterTransactions();
        updateStats();
    } catch (error) {
        console.error('Failed to load transactions:', error);
        showError('transactions-table', 'Failed to load transactions');
    }
}

// Filter transactions based on criteria
function filterTransactions() {
    const statusFilter = document.getElementById('filter-status').value;
    const paymentMethodFilter = document.getElementById('filter-payment-method').value;

    filteredTransactions = allTransactions.filter(txn => {
        if (statusFilter && txn.payment_status.toLowerCase() !== statusFilter.toLowerCase()) {
            return false;
        }
        if (paymentMethodFilter && txn.payment_method.toLowerCase() !== paymentMethodFilter.toLowerCase()) {
            return false;
        }
        return true;
    });

    currentPage = 1;
    renderTransactions();
    updatePagination();
}

// Render transactions table
function renderTransactions() {
    const tbody = document.getElementById('transactions-table');

    if (!filteredTransactions || filteredTransactions.length === 0) {
        showEmpty('transactions-table', 'No transactions found for the selected filters');
        return;
    }

    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = startIndex + itemsPerPage;
    const pageTransactions = filteredTransactions.slice(startIndex, endIndex);

    tbody.innerHTML = pageTransactions.map((txn) => `
        <tr onclick="showTransactionDetail('${txn.id}')" style="cursor: pointer;">
            <td><code>${txn.id.substring(0, 12)}...</code></td>
            <td>${formatDateTime(txn.timestamp)}</td>
            <td>${txn.product_name || 'Unknown'}</td>
            <td><strong>${txn.coil_id}</strong></td>
            <td>${formatCurrency(txn.amount)}</td>
            <td>${getPaymentMethodIcon(txn.payment_method)} ${capitalizeFirst(txn.payment_method)}</td>
            <td>${getStatusBadge(txn.payment_status)}</td>
            <td>${txn.nayax_transaction_id ? `<code>${txn.nayax_transaction_id}</code>` : '—'}</td>
            <td>
                <button class="btn btn-secondary" style="padding: 4px 10px; font-size: 12px;" onclick="event.stopPropagation(); showTransactionDetail('${txn.id}')">View</button>
            </td>
        </tr>
    `).join('');
}

// Update stats
function updateStats() {
    const totalCount = filteredTransactions.length;
    const successCount = filteredTransactions.filter(t => t.payment_status === 'approved').length;
    const refundCount = filteredTransactions.filter(t => t.payment_status === 'refunded').length;
    const totalRevenue = filteredTransactions
        .filter(t => t.payment_status === 'approved')
        .reduce((sum, t) => sum + (t.amount || 0), 0);

    document.getElementById('total-count').textContent = totalCount;
    document.getElementById('total-revenue').textContent = formatCurrency(totalRevenue);
    document.getElementById('success-count').textContent = successCount;
    document.getElementById('refund-count').textContent = refundCount;
}

// Update pagination
function updatePagination() {
    const totalPages = Math.ceil(filteredTransactions.length / itemsPerPage);

    document.getElementById('page-info').textContent = `Page ${currentPage} of ${totalPages || 1}`;
    document.getElementById('prev-page').disabled = currentPage === 1;
    document.getElementById('next-page').disabled = currentPage >= totalPages;
}

// Previous page
function prevPage() {
    if (currentPage > 1) {
        currentPage--;
        renderTransactions();
        updatePagination();
    }
}

// Next page
function nextPage() {
    const totalPages = Math.ceil(filteredTransactions.length / itemsPerPage);
    if (currentPage < totalPages) {
        currentPage++;
        renderTransactions();
        updatePagination();
    }
}

// Show transaction detail modal
function showTransactionDetail(transactionId) {
    const txn = allTransactions.find(t => t.id === transactionId);
    if (!txn) return;

    document.getElementById('detail-id').textContent = txn.id;
    document.getElementById('detail-timestamp').textContent = formatDateTime(txn.timestamp);
    document.getElementById('detail-product').textContent = txn.product_name || 'Unknown';
    document.getElementById('detail-coil').textContent = txn.coil_id;
    document.getElementById('detail-amount').textContent = formatCurrency(txn.amount);
    document.getElementById('detail-payment-method').textContent = capitalizeFirst(txn.payment_method);
    document.getElementById('detail-payment-status').innerHTML = getStatusBadge(txn.payment_status);
    document.getElementById('detail-nayax-id').textContent = txn.nayax_transaction_id || 'N/A';
    document.getElementById('detail-currency').textContent = txn.currency || 'USD';

    document.getElementById('transaction-detail-modal').style.display = 'flex';
}

// Close transaction detail modal
function closeTransactionDetailModal() {
    document.getElementById('transaction-detail-modal').style.display = 'none';
}

// Export transactions to CSV
function exportTransactions() {
    if (!filteredTransactions || filteredTransactions.length === 0) {
        alert('No transactions to export');
        return;
    }

    const headers = ['Transaction ID', 'Timestamp', 'Product', 'Coil', 'Amount', 'Payment Method', 'Status', 'Nayax ID', 'Currency'];
    const rows = filteredTransactions.map(txn => [
        txn.id,
        new Date(txn.timestamp).toISOString(),
        txn.product_name || '',
        txn.coil_id,
        txn.amount || 0,
        txn.payment_method,
        txn.payment_status,
        txn.nayax_transaction_id || '',
        txn.currency || 'USD',
    ]);

    const csvContent = [
        headers.join(','),
        ...rows.map(row => row.map(cell => `"${cell}"`).join(','))
    ].join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `transactions_${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
}

// Helper: Capitalize first letter
function capitalizeFirst(str) {
    if (!str) return '';
    return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
}

// Close modal when clicking outside
window.onclick = function(event) {
    const modal = document.getElementById('transaction-detail-modal');
    if (event.target === modal) {
        closeTransactionDetailModal();
    }
}
