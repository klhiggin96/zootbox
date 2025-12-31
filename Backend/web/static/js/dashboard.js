// Dashboard page functionality

let revenueChart = null;
let paymentMethodChart = null;

// Load dashboard data on page load
document.addEventListener('DOMContentLoaded', () => {
    loadMetrics();
    loadRevenueChart();
    loadPaymentMethodChart();
    loadTopProducts();
    loadRecentTransactions();

    // Refresh every 30 seconds
    setInterval(() => {
        loadMetrics();
        loadRecentTransactions();
    }, 30000);
});

// Load key metrics
async function loadMetrics() {
    try {
        const data = await apiGet('/portal/analytics/metrics');

        document.getElementById('today-revenue').textContent = formatCurrency(data.today_revenue);
        document.getElementById('today-sales').textContent = data.today_sales;
        document.getElementById('avg-transaction').textContent = formatCurrency(data.avg_transaction);
        document.getElementById('refunds').textContent = data.refunds;

        // Update change indicators
        if (data.revenue_change !== undefined) {
            const changeElem = document.getElementById('today-change');
            changeElem.textContent = `${data.revenue_change >= 0 ? '+' : ''}${data.revenue_change.toFixed(1)}%`;
            changeElem.className = `metric-change ${data.revenue_change >= 0 ? 'positive' : 'negative'}`;
        }

        if (data.sales_change !== undefined) {
            const changeElem = document.getElementById('sales-change');
            changeElem.textContent = `${data.sales_change >= 0 ? '+' : ''}${data.sales_change.toFixed(1)}%`;
            changeElem.className = `metric-change ${data.sales_change >= 0 ? 'positive' : 'negative'}`;
        }

        if (data.refund_rate !== undefined) {
            document.getElementById('refund-change').textContent = `${data.refund_rate.toFixed(1)}%`;
        }
    } catch (error) {
        console.error('Failed to load metrics:', error);
    }
}

// Load revenue chart (last 7 days)
async function loadRevenueChart() {
    try {
        const data = await apiGet('/portal/analytics/revenue-by-day?days=7');

        const ctx = document.getElementById('revenue-chart');
        if (!ctx) return;

        // Destroy existing chart
        if (revenueChart) {
            revenueChart.destroy();
        }

        revenueChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: data.labels,
                datasets: [{
                    label: 'Revenue',
                    data: data.values,
                    borderColor: '#14A566',
                    backgroundColor: 'rgba(20, 165, 102, 0.1)',
                    tension: 0.3,
                    fill: true,
                }],
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                plugins: {
                    legend: {
                        display: false,
                    },
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: (value) => formatCurrency(value),
                        },
                    },
                },
            },
        });
    } catch (error) {
        console.error('Failed to load revenue chart:', error);
    }
}

// Load payment method breakdown
async function loadPaymentMethodChart() {
    try {
        const data = await apiGet('/portal/analytics/payment-methods');

        const ctx = document.getElementById('payment-method-chart');
        if (!ctx) return;

        // Destroy existing chart
        if (paymentMethodChart) {
            paymentMethodChart.destroy();
        }

        paymentMethodChart = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: data.labels,
                datasets: [{
                    data: data.values,
                    backgroundColor: [
                        '#14A566',
                        '#3182ce',
                        '#f6ad55',
                        '#fc8181',
                    ],
                }],
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                plugins: {
                    legend: {
                        position: 'bottom',
                    },
                },
            },
        });
    } catch (error) {
        console.error('Failed to load payment method chart:', error);
    }
}

// Load top products table
async function loadTopProducts() {
    try {
        const data = await apiGet('/portal/analytics/top-products?limit=10');
        const tbody = document.getElementById('top-products-table');

        if (!data || data.length === 0) {
            showEmpty('top-products-table', 'No sales data available');
            return;
        }

        tbody.innerHTML = data.map((product, index) => `
            <tr>
                <td><strong>#${index + 1}</strong></td>
                <td>${product.name}</td>
                <td>${product.category}</td>
                <td>${product.units_sold}</td>
                <td>${formatCurrency(product.revenue)}</td>
                <td>${getTrendIndicator(product.trend)}</td>
            </tr>
        `).join('');
    } catch (error) {
        console.error('Failed to load top products:', error);
        showError('top-products-table', 'Failed to load products');
    }
}

// Load recent transactions
async function loadRecentTransactions() {
    try {
        const data = await apiGet('/portal/analytics/recent-transactions?limit=5');
        const tbody = document.getElementById('recent-transactions-table');

        if (!data || data.length === 0) {
            showEmpty('recent-transactions-table', 'No recent transactions');
            return;
        }

        tbody.innerHTML = data.map((txn) => `
            <tr>
                <td>${formatDateTime(txn.timestamp)}</td>
                <td>${txn.product_name || 'Unknown'}</td>
                <td><strong>${txn.coil_id}</strong></td>
                <td>${formatCurrency(txn.amount)}</td>
                <td>${getPaymentMethodIcon(txn.payment_method)} ${capitalizeFirst(txn.payment_method)}</td>
                <td>${getStatusBadge(txn.payment_status)}</td>
            </tr>
        `).join('');
    } catch (error) {
        console.error('Failed to load recent transactions:', error);
        showError('recent-transactions-table', 'Failed to load transactions');
    }
}

// Helper: Get trend indicator
function getTrendIndicator(trend) {
    if (trend > 0) {
        return `<span style="color: #14A566;">↗ ${trend}%</span>`;
    } else if (trend < 0) {
        return `<span style="color: #e53e3e;">↘ ${Math.abs(trend)}%</span>`;
    } else {
        return '<span style="color: #a0aec0;">—</span>';
    }
}

// Helper: Capitalize first letter
function capitalizeFirst(str) {
    if (!str) return '';
    return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
}
