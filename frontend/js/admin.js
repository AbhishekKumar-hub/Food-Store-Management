let adminProducts = [];
let adminOrders = [];
let adminCoupons = [];
let adminUsers = [];
let restaurantState = null;
let restaurantMap = null;
let restaurantMarker = null;
let adminOrderMap = null;
let adminOrderMarkers = [];
let adminOrderRoute = null;
let charts = {};

document.addEventListener("DOMContentLoaded", () => {
  if (!requireAdmin()) return;

  document.getElementById("productForm")?.addEventListener("submit", saveProduct);
  document.getElementById("resetProductBtn")?.addEventListener("click", resetForm);
  document.getElementById("seedProductsBtn")?.addEventListener("click", seedProducts);
  document.getElementById("couponForm")?.addEventListener("submit", saveCoupon);
  document.getElementById("resetCouponBtn")?.addEventListener("click", resetCouponForm);
  document.getElementById("restaurantForm")?.addEventListener("submit", saveRestaurant);
  document.getElementById("restaurantLocationBtn")?.addEventListener("click", useRestaurantLocation);

  initRestaurantMap();
  loadAdminData();
});

async function loadAdminData() {
  await Promise.all([loadDashboard(), loadProducts(), loadAdminOrders(), loadRestaurant(), loadCoupons(), loadUsers()]);
}

async function loadDashboard() {
  const dashboard = await apiFetch(API.ADMIN_DASHBOARD);
  const analytics = dashboard?.analytics || {};

  document.getElementById("statRevenue").textContent = money(dashboard.totalRevenue || 0);
  document.getElementById("statOrders").textContent = dashboard.totalOrders || 0;
  document.getElementById("statProducts").textContent = dashboard.totalProducts || 0;
  document.getElementById("statPending").textContent = analytics.pendingOrders || 0;
  document.getElementById("statLow").textContent = analytics.lowStockProducts || 0;

  renderCharts(analytics);
  renderTopProducts(analytics.topProducts || []);
}

function renderCharts(analytics) {
  renderChart("revenueChart", "line", {
    labels: Object.keys(analytics.revenueByDay || {}),
    datasets: [
      {
        label: "Revenue",
        data: Object.values(analytics.revenueByDay || {}),
        borderColor: "#0f8b8d",
        backgroundColor: "rgba(15, 139, 141, 0.12)",
        tension: 0.35,
        fill: true,
      },
    ],
  });

  renderChart("categoryChart", "doughnut", {
    labels: Object.keys(analytics.categoryRevenue || {}),
    datasets: [
      {
        data: Object.values(analytics.categoryRevenue || {}),
        backgroundColor: ["#e84c3d", "#0f8b8d", "#f5a623", "#146c94", "#1f9d55"],
      },
    ],
  });

  renderChart("statusChart", "bar", {
    labels: Object.keys(analytics.statusCounts || {}),
    datasets: [
      {
        label: "Orders",
        data: Object.values(analytics.statusCounts || {}),
        backgroundColor: "#e84c3d",
        borderRadius: 8,
      },
    ],
  });
}

function renderChart(id, type, data) {
  const canvas = document.getElementById(id);
  if (!canvas || !window.Chart) return;

  if (charts[id]) charts[id].destroy();
  charts[id] = new Chart(canvas.getContext("2d"), {
    type,
    data,
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: "bottom" },
      },
      scales: type === "doughnut" ? {} : { y: { beginAtZero: true } },
    },
  });
}

function renderTopProducts(products) {
  const table = document.getElementById("topProductsTable");
  if (!table) return;

  table.innerHTML = products.length
    ? products
        .map(
          (product) => `
            <tr>
              <td>${escapeHtml(product.name)}</td>
              <td>${product.quantity}</td>
              <td>${money(product.revenue)}</td>
            </tr>
          `
        )
        .join("")
    : `<tr><td colspan="3">No order data yet.</td></tr>`;
}

async function loadProducts() {
  const body = document.getElementById("productTable");
  adminProducts = (await apiFetch(API.PRODUCTS)) || [];

  body.innerHTML = adminProducts.length
    ? adminProducts
        .map((product) => {
          const stock = stockLabel(product);
          return `
            <tr>
              <td><img class="mini-image" src="${imageOrFallback(product.imageUrl)}" alt="${escapeHtml(product.name)}" onerror="this.src='${FALLBACK_IMAGE}'" /></td>
              <td><strong>${escapeHtml(product.name)}</strong><br/><small class="muted">${escapeHtml(product.category || "Meals")}</small></td>
              <td>${money(product.price)}</td>
              <td><span class="pill pill-${stock.tone}">${stock.text}</span></td>
              <td>${product.vegetarian ? "Veg" : "Regular"}</td>
              <td>
                <div class="admin-actions">
                  <button class="icon-button" type="button" onclick="editProduct('${product.id}')" aria-label="Edit product"><i data-lucide="pencil"></i></button>
                  <button class="icon-button" type="button" onclick="deleteProduct('${product.id}')" aria-label="Delete product"><i data-lucide="trash-2"></i></button>
                </div>
              </td>
            </tr>
          `;
        })
        .join("")
    : `<tr><td colspan="6">No products yet.</td></tr>`;

  refreshIcons();
}

async function saveProduct(event) {
  event.preventDefault();
  const id = document.getElementById("productId").value;
  const payload = {
    name: document.getElementById("name").value.trim(),
    category: document.getElementById("category").value.trim(),
    price: Number(document.getElementById("price").value),
    imageUrl: document.getElementById("imageUrl").value.trim(),
    stock: Number(document.getElementById("stock").value),
    preparationTimeMinutes: Number(document.getElementById("prepTime").value || 25),
    vegetarian: document.getElementById("vegetarian").checked,
    description: document.getElementById("description").value.trim(),
  };

  if (!payload.name || payload.price < 0 || payload.stock < 0) {
    showToast("Enter valid product details.", "error");
    return;
  }

  await apiFetch(id ? API.UPDATE_PRODUCT(id) : API.ADD_PRODUCT, {
    method: id ? "PUT" : "POST",
    body: JSON.stringify(payload),
  });

  showToast(id ? "Product updated." : "Product added.");
  resetForm();
  await Promise.all([loadProducts(), loadDashboard()]);
}

async function editProduct(id) {
  const product = adminProducts.find((item) => item.id === id) || (await apiFetch(API.PRODUCT_BY_ID(id)));
  document.getElementById("formTitle").textContent = "Update Product";
  document.getElementById("productId").value = product.id;
  document.getElementById("name").value = product.name || "";
  document.getElementById("category").value = product.category || "";
  document.getElementById("price").value = product.price || 0;
  document.getElementById("imageUrl").value = product.imageUrl || "";
  document.getElementById("stock").value = product.stock || 0;
  document.getElementById("prepTime").value = product.preparationTimeMinutes || 25;
  document.getElementById("vegetarian").checked = !!product.vegetarian;
  document.getElementById("description").value = product.description || "";
  document.getElementById("productForm").scrollIntoView({ behavior: "smooth" });
}

async function deleteProduct(id) {
  if (!confirm("Delete this product?")) return;
  await apiFetch(API.DELETE_PRODUCT(id), { method: "DELETE" });
  showToast("Product deleted.");
  await Promise.all([loadProducts(), loadDashboard()]);
}

function resetForm() {
  document.getElementById("formTitle").textContent = "Add Product";
  document.getElementById("productForm").reset();
  document.getElementById("productId").value = "";
  document.getElementById("prepTime").value = 25;
}

async function loadAdminOrders() {
  const body = document.getElementById("orderTable");
  adminOrders = (await apiFetch(API.ADMIN_ORDERS)) || [];

  body.innerHTML = adminOrders.length
    ? adminOrders
        .map(
          (order) => `
            <tr>
              <td><strong>${escapeHtml(order.id?.slice(-8) || "")}</strong><br/><small>${formatDateTime(order.orderTime)}</small></td>
              <td>${escapeHtml(order.customerName || order.userEmail)}<br/><small class="muted">${escapeHtml(order.userEmail)}</small></td>
              <td>${(order.items || []).map((item) => `${escapeHtml(item.name)} x ${item.quantity}`).join("<br/>")}</td>
              <td>${money(order.totalAmount)}<br/><small>${escapeHtml(order.paymentMode || "COD")}</small></td>
              <td>
                <select class="select" onchange="updateOrderStatus('${order.id}', this.value)">
                  ${["PLACED", "CONFIRMED", "PREPARING", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED"]
                    .map((status) => `<option value="${status}" ${order.status === status ? "selected" : ""}>${statusText(status)}</option>`)
                    .join("")}
                </select>
              </td>
              <td>
                <div class="admin-actions">
                  <button class="icon-button" type="button" onclick="openOrderModal('${order.id}')" aria-label="View order"><i data-lucide="eye"></i></button>
                  <button class="icon-button" type="button" onclick="printAdminInvoice('${order.id}')" aria-label="Invoice"><i data-lucide="receipt-text"></i></button>
                  ${order.status !== "DELIVERED" && order.status !== "CANCELLED" ? `<button class="icon-button" type="button" onclick="cancelAdminOrder('${order.id}')" aria-label="Cancel"><i data-lucide="x"></i></button>` : ""}
                </div>
              </td>
            </tr>
          `
        )
        .join("")
    : `<tr><td colspan="6">No orders yet.</td></tr>`;
  refreshIcons();
}

async function updateOrderStatus(orderId, status) {
  await apiFetch(API.ADMIN_UPDATE_ORDER(orderId), {
    method: "PUT",
    body: JSON.stringify({ status }),
  });
  showToast("Order status updated.");
  await Promise.all([loadAdminOrders(), loadDashboard()]);
}

async function cancelAdminOrder(orderId) {
  if (!confirm("Cancel this order and restore stock?")) return;
  await apiFetch(API.ADMIN_CANCEL_ORDER(orderId), { method: "PUT" });
  showToast("Order cancelled.");
  await Promise.all([loadAdminOrders(), loadDashboard(), loadProducts()]);
}

async function printAdminInvoice(orderId) {
  const invoice = await apiFetch(API.ADMIN_ORDER_INVOICE(orderId));
  const win = window.open("", "_blank", "width=720,height=800");
  win.document.write(`<pre style="font:16px/1.5 monospace;white-space:pre-wrap;">${escapeHtml(invoice)}</pre>`);
  win.document.close();
  win.print();
}

function openOrderModal(orderId) {
  const order = adminOrders.find((item) => item.id === orderId);
  if (!order) return;

  document.getElementById("orderModalTitle").textContent = `Order ${order.id.slice(-8)}`;
  document.getElementById("orderModalBody").innerHTML = `
    <div class="split-grid">
      <div>
        <h3>${escapeHtml(order.customerName || order.userEmail)}</h3>
        <p class="muted">${escapeHtml(order.userEmail)} ${order.customerPhone ? `- ${escapeHtml(order.customerPhone)}` : ""}</p>
        <p><strong>Status:</strong> ${statusText(order.status)}</p>
        <p><strong>Payment:</strong> ${escapeHtml(order.paymentMode || "")} / ${escapeHtml(order.paymentStatus || "")}</p>
        <p><strong>Address:</strong> ${escapeHtml(order.deliveryAddress?.line1 || "")}, ${escapeHtml(order.deliveryAddress?.city || "")}</p>
      </div>
      <div>
        <h3>Bill</h3>
        <p>Subtotal: ${money(order.subtotalAmount || order.totalAmount + (order.discountAmount || 0))}</p>
        <p>Discount: ${money(order.discountAmount || 0)} ${order.couponCode ? `(${escapeHtml(order.couponCode)})` : ""}</p>
        <h3>Payable: ${money(order.totalAmount)}</h3>
      </div>
    </div>
    <div class="table-wrap" style="margin-top:14px;">
      <table>
        <thead><tr><th>Item</th><th>Qty</th><th>Total</th></tr></thead>
        <tbody>
          ${(order.items || []).map((item) => `<tr><td>${escapeHtml(item.name)}</td><td>${item.quantity}</td><td>${money(item.price * item.quantity)}</td></tr>`).join("")}
        </tbody>
      </table>
    </div>
  `;
  document.getElementById("orderModal").classList.add("open");
  renderAdminOrderMap(order);
  refreshIcons();
}

function closeOrderModal() {
  document.getElementById("orderModal").classList.remove("open");
}

function renderAdminOrderMap(order) {
  const restaurantCoords = [order.restaurant?.latitude || 28.6139, order.restaurant?.longitude || 77.209];
  const address = order.deliveryAddress;
  const addressCoords = address?.latitude && address?.longitude ? [address.latitude, address.longitude] : null;

  if (!adminOrderMap) {
    adminOrderMap = L.map("adminOrderMap", { scrollWheelZoom: false }).setView(restaurantCoords, 12);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: "&copy; OpenStreetMap",
    }).addTo(adminOrderMap);
  }

  adminOrderMarkers.forEach((marker) => marker.remove());
  if (adminOrderRoute) adminOrderRoute.remove();
  adminOrderMarkers = [L.marker(restaurantCoords).addTo(adminOrderMap).bindPopup("Restaurant")];

  if (addressCoords) {
    adminOrderMarkers.push(L.marker(addressCoords).addTo(adminOrderMap).bindPopup("Customer"));
    adminOrderRoute = L.polyline([restaurantCoords, addressCoords], {
      color: "#1f9d55",
      weight: 5,
      opacity: 0.9,
    }).addTo(adminOrderMap);
    adminOrderMap.fitBounds([restaurantCoords, addressCoords], { padding: [30, 30] });
  } else {
    adminOrderMap.setView(restaurantCoords, 12);
  }

  setTimeout(() => adminOrderMap.invalidateSize(), 120);
}

async function seedProducts() {
  await apiFetch(API.ADMIN_SEED_PRODUCTS, { method: "POST" });
  showToast("Sample products are ready.");
  await Promise.all([loadProducts(), loadDashboard()]);
}

async function loadCoupons() {
  const body = document.getElementById("couponTable");
  if (!body) return;
  adminCoupons = (await apiFetch(API.ADMIN_COUPONS)) || [];
  body.innerHTML = adminCoupons.length
    ? adminCoupons
        .map(
          (coupon) => `
            <tr>
              <td><strong>${escapeHtml(coupon.code)}</strong></td>
              <td>${escapeHtml(coupon.discountType)}</td>
              <td>${coupon.discountType === "PERCENT" ? `${coupon.discountValue}%` : money(coupon.discountValue)}</td>
              <td>Min ${money(coupon.minOrderAmount)}<br/>Max ${coupon.maxDiscountAmount ? money(coupon.maxDiscountAmount) : "No cap"}<br/>${coupon.expiryDate || "No expiry"}</td>
              <td><span class="pill pill-${coupon.active ? "success" : "danger"}">${coupon.active ? "Active" : "Inactive"}</span></td>
              <td>
                <div class="admin-actions">
                  <button class="icon-button" type="button" onclick="editCoupon('${coupon.id}')" aria-label="Edit coupon"><i data-lucide="pencil"></i></button>
                  <button class="icon-button" type="button" onclick="deleteCoupon('${coupon.id}')" aria-label="Delete coupon"><i data-lucide="trash-2"></i></button>
                </div>
              </td>
            </tr>
          `
        )
        .join("")
    : `<tr><td colspan="6">No coupons yet.</td></tr>`;
  refreshIcons();
}

async function saveCoupon(event) {
  event.preventDefault();
  const id = document.getElementById("couponId").value;
  const payload = {
    code: document.getElementById("couponCodeInput").value.trim(),
    discountType: document.getElementById("couponType").value,
    discountValue: Number(document.getElementById("couponValue").value),
    minOrderAmount: Number(document.getElementById("couponMin").value || 0),
    maxDiscountAmount: Number(document.getElementById("couponMax").value || 0),
    expiryDate: document.getElementById("couponExpiry").value || null,
    active: document.getElementById("couponActive").checked,
  };

  await apiFetch(id ? API.ADMIN_COUPON_BY_ID(id) : API.ADMIN_COUPONS, {
    method: id ? "PUT" : "POST",
    body: JSON.stringify(payload),
  });
  showToast(id ? "Coupon updated." : "Coupon added.");
  resetCouponForm();
  loadCoupons();
}

function editCoupon(id) {
  const coupon = adminCoupons.find((item) => item.id === id);
  if (!coupon) return;
  document.getElementById("couponFormTitle").textContent = "Update Coupon";
  document.getElementById("couponId").value = coupon.id;
  document.getElementById("couponCodeInput").value = coupon.code || "";
  document.getElementById("couponType").value = coupon.discountType || "PERCENT";
  document.getElementById("couponValue").value = coupon.discountValue || "";
  document.getElementById("couponMin").value = coupon.minOrderAmount || "";
  document.getElementById("couponMax").value = coupon.maxDiscountAmount || "";
  document.getElementById("couponExpiry").value = coupon.expiryDate || "";
  document.getElementById("couponActive").checked = !!coupon.active;
  document.getElementById("couponForm").scrollIntoView({ behavior: "smooth" });
}

async function deleteCoupon(id) {
  if (!confirm("Delete this coupon?")) return;
  await apiFetch(API.ADMIN_COUPON_BY_ID(id), { method: "DELETE" });
  showToast("Coupon deleted.");
  loadCoupons();
}

function resetCouponForm() {
  document.getElementById("couponFormTitle").textContent = "Add Coupon";
  document.getElementById("couponForm").reset();
  document.getElementById("couponId").value = "";
  document.getElementById("couponType").value = "PERCENT";
  document.getElementById("couponActive").checked = true;
}

async function loadUsers() {
  const body = document.getElementById("userTable");
  if (!body) return;
  adminUsers = (await apiFetch(API.ADMIN_USERS)) || [];
  body.innerHTML = adminUsers
    .map(
      (user) => `
        <tr>
          <td>${escapeHtml(user.name || "")}</td>
          <td>${escapeHtml(user.email || "")}</td>
          <td>${escapeHtml(user.role || "")}</td>
          <td>${user.orderCount}</td>
          <td>${money(user.totalSpent)}</td>
          <td><span class="pill pill-${user.blocked ? "danger" : "success"}">${user.blocked ? "Blocked" : "Active"}</span></td>
          <td>
            ${
              user.role === "ROLE_ADMIN"
                ? `<span class="muted">Protected</span>`
                : `<button class="btn btn-outline" type="button" onclick="setUserBlocked('${user.id}', ${!user.blocked})">${user.blocked ? "Unblock" : "Block"}</button>`
            }
          </td>
        </tr>
      `
    )
    .join("");
}

async function setUserBlocked(userId, blocked) {
  await apiFetch(API.ADMIN_BLOCK_USER(userId, blocked), { method: "PUT" });
  showToast(blocked ? "User blocked." : "User unblocked.");
  loadUsers();
}

async function loadRestaurant() {
  restaurantState = await apiFetch(API.RESTAURANT);
  document.getElementById("restaurantName").value = restaurantState.name || "";
  document.getElementById("restaurantPhone").value = restaurantState.phone || "";
  document.getElementById("restaurantHours").value = restaurantState.openingHours || "";
  document.getElementById("restaurantAddress").value = restaurantState.addressLine || "";
  document.getElementById("restaurantCity").value = restaurantState.city || "";
  document.getElementById("restaurantDescription").value = restaurantState.description || "";
  setRestaurantCoords(restaurantState.latitude || 28.6139, restaurantState.longitude || 77.209);
}

async function saveRestaurant(event) {
  event.preventDefault();
  const payload = {
    name: document.getElementById("restaurantName").value.trim(),
    phone: document.getElementById("restaurantPhone").value.trim(),
    openingHours: document.getElementById("restaurantHours").value.trim(),
    addressLine: document.getElementById("restaurantAddress").value.trim(),
    city: document.getElementById("restaurantCity").value.trim(),
    description: document.getElementById("restaurantDescription").value.trim(),
    latitude: Number(document.getElementById("restaurantLat").value || 0),
    longitude: Number(document.getElementById("restaurantLng").value || 0),
  };

  restaurantState = await apiFetch(API.RESTAURANT, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
  showToast("Restaurant details updated.");
}

function initRestaurantMap() {
  const mapEl = document.getElementById("restaurantMap");
  if (!mapEl || !window.L) return;

  restaurantMap = L.map("restaurantMap", { scrollWheelZoom: false }).setView([28.6139, 77.209], 12);
  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    attribution: "&copy; OpenStreetMap",
  }).addTo(restaurantMap);

  restaurantMap.on("click", (event) => setRestaurantCoords(event.latlng.lat, event.latlng.lng));
  setTimeout(() => restaurantMap.invalidateSize(), 100);
}

function useRestaurantLocation() {
  if (!navigator.geolocation) {
    showToast("Location is not supported by this browser.", "error");
    return;
  }

  navigator.geolocation.getCurrentPosition(
    (position) => {
      setRestaurantCoords(position.coords.latitude, position.coords.longitude);
      showToast("Restaurant location pinned.");
    },
    () => showToast("Please allow location access from the browser.", "error"),
    { enableHighAccuracy: true, timeout: 10000 }
  );
}

function setRestaurantCoords(latitude, longitude) {
  document.getElementById("restaurantLat").value = latitude ? Number(latitude).toFixed(6) : "";
  document.getElementById("restaurantLng").value = longitude ? Number(longitude).toFixed(6) : "";

  if (!restaurantMap || !latitude || !longitude) return;
  const latLng = [latitude, longitude];

  if (!restaurantMarker) {
    restaurantMarker = L.marker(latLng, { draggable: true }).addTo(restaurantMap);
    restaurantMarker.on("dragend", () => {
      const next = restaurantMarker.getLatLng();
      setRestaurantCoords(next.lat, next.lng);
    });
  } else {
    restaurantMarker.setLatLng(latLng);
  }

  restaurantMap.setView(latLng, 14);
}

function statusText(status) {
  const labels = {
    PLACED: "Placed",
    CONFIRMED: "Confirmed",
    PREPARING: "Preparing",
    OUT_FOR_DELIVERY: "Rider on the way",
    DELIVERED: "Delivered",
    CANCELLED: "Cancelled",
  };
  return labels[status] || status;
}
