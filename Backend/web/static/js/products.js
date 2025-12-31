// Products management page functionality

let allProducts = [];
let availableCoils = [];

// Load products on page load
document.addEventListener('DOMContentLoaded', () => {
    loadProducts();
    loadAvailableCoils();
});

// Load all products
async function loadProducts() {
    try {
        showLoading('products-table');
        const data = await apiGet('/products');
        allProducts = data;
        renderProducts(allProducts);
    } catch (error) {
        console.error('Failed to load products:', error);
        showError('products-table', 'Failed to load products');
    }
}

// Render products table
function renderProducts(products) {
    const tbody = document.getElementById('products-table');

    if (!products || products.length === 0) {
        showEmpty('products-table', 'No products found. Click "Add New Product" to create one.');
        return;
    }

    tbody.innerHTML = products.map((product) => `
        <tr>
            <td><code>${product.id}</code></td>
            <td><strong>${product.name}</strong></td>
            <td>${product.category}</td>
            <td>${formatCurrency(product.price)}</td>
            <td>${product.age_restriction > 0 ? product.age_restriction + '+' : 'None'}</td>
            <td>${getAssignedCoilsBadge(product.assigned_coils)}</td>
            <td>
                <button class="btn btn-secondary" style="padding: 6px 12px; font-size: 12px;" onclick="editProduct('${product.id}')">Edit</button>
                <button class="btn btn-secondary" style="padding: 6px 12px; font-size: 12px;" onclick="showAssignCoilModal('${product.id}')">Assign Coil</button>
            </td>
        </tr>
    `).join('');
}

// Get assigned coils badge
function getAssignedCoilsBadge(coils) {
    if (!coils || coils.length === 0) {
        return '<span style="color: #a0aec0;">None</span>';
    }
    return coils.map(coil => `<span class="status-badge success">${coil}</span>`).join(' ');
}

// Filter products
function filterProducts() {
    const searchTerm = document.getElementById('search-input').value.toLowerCase();

    if (!searchTerm) {
        renderProducts(allProducts);
        return;
    }

    const filtered = allProducts.filter(product =>
        product.name.toLowerCase().includes(searchTerm) ||
        product.category.toLowerCase().includes(searchTerm) ||
        product.id.toLowerCase().includes(searchTerm)
    );

    renderProducts(filtered);
}

// Show create product modal
function showCreateProductModal() {
    document.getElementById('modal-title').textContent = 'Add New Product';
    document.getElementById('product-form').reset();
    document.getElementById('product-id').value = '';
    document.getElementById('product-modal').style.display = 'flex';
}

// Edit product
function editProduct(productId) {
    const product = allProducts.find(p => p.id === productId);
    if (!product) return;

    document.getElementById('modal-title').textContent = 'Edit Product';
    document.getElementById('product-id').value = product.id;
    document.getElementById('product-name').value = product.name;
    document.getElementById('product-category').value = product.category;
    document.getElementById('product-price').value = product.price;
    document.getElementById('product-age').value = product.age_restriction || 0;
    document.getElementById('product-modal').style.display = 'flex';
}

// Close product modal
function closeProductModal() {
    document.getElementById('product-modal').style.display = 'none';
}

// Save product (create or update)
async function saveProduct(event) {
    event.preventDefault();

    const formData = new FormData(event.target);
    const productId = formData.get('id');
    const productData = {
        name: formData.get('name'),
        category: formData.get('category'),
        price: parseFloat(formData.get('price')),
        age_restriction: parseInt(formData.get('age_restriction')) || 0,
    };

    try {
        if (productId) {
            // Update existing product
            await apiPut(`/products/${productId}`, productData);
        } else {
            // Create new product
            await apiPost('/products', productData);
        }

        closeProductModal();
        loadProducts();
    } catch (error) {
        console.error('Failed to save product:', error);
        alert('Failed to save product. Please try again.');
    }
}

// Load available coils
async function loadAvailableCoils() {
    try {
        const data = await apiGet('/coils');
        availableCoils = data;
    } catch (error) {
        console.error('Failed to load coils:', error);
    }
}

// Show assign coil modal
function showAssignCoilModal(productId) {
    const product = allProducts.find(p => p.id === productId);
    if (!product) return;

    document.getElementById('assign-product-id').value = productId;

    // Populate coil dropdown
    const select = document.getElementById('coil-select');
    select.innerHTML = availableCoils.map(coil => `
        <option value="${coil.id}">
            ${coil.id} - ${coil.product_name || 'Empty'} (${coil.current_inventory} in stock)
        </option>
    `).join('');

    document.getElementById('assign-coil-modal').style.display = 'flex';
}

// Close assign coil modal
function closeAssignCoilModal() {
    document.getElementById('assign-coil-modal').style.display = 'none';
}

// Assign product to coil
async function assignCoil(event) {
    event.preventDefault();

    const formData = new FormData(event.target);
    const productId = formData.get('product_id');
    const coilId = formData.get('coil_id');

    try {
        await apiPost(`/products/${productId}/assign-coil`, {
            coil_id: coilId,
        });

        closeAssignCoilModal();
        loadProducts();
    } catch (error) {
        console.error('Failed to assign coil:', error);
        alert('Failed to assign coil. Please try again.');
    }
}

// Close modal when clicking outside
window.onclick = function(event) {
    const productModal = document.getElementById('product-modal');
    const assignCoilModal = document.getElementById('assign-coil-modal');

    if (event.target === productModal) {
        closeProductModal();
    }
    if (event.target === assignCoilModal) {
        closeAssignCoilModal();
    }
}
