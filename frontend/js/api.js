const BASE_URL = "http://localhost:8080";

const API = {
  REGISTER: `${BASE_URL}/api/auth/register`,
  LOGIN: `${BASE_URL}/api/auth/login`,
  PROFILE: `${BASE_URL}/api/users/me`,
  ADDRESS: `${BASE_URL}/api/users/me/addresses`,
  ADDRESS_BY_ID: (id) => `${BASE_URL}/api/users/me/addresses/${id}`,
  ADDRESS_DEFAULT: (id) => `${BASE_URL}/api/users/me/addresses/${id}/default`,
  PRODUCTS: `${BASE_URL}/api/products`,
  PRODUCT_BY_ID: (id) => `${BASE_URL}/api/products/${id}`,
  PRODUCT_SEARCH: `${BASE_URL}/api/products/search`,
  PRODUCT_CATEGORIES: `${BASE_URL}/api/products/categories`,
  FEATURED_PRODUCTS: `${BASE_URL}/api/products/featured`,
  WISHLIST: `${BASE_URL}/api/users/me/wishlist`,
  WISHLIST_ITEM: (productId) => `${BASE_URL}/api/users/me/wishlist/${productId}`,
  ADD_TO_CART: (productId, quantity = 1) =>
    `${BASE_URL}/api/cart/add/${productId}?quantity=${quantity}`,
  UPDATE_CART_ITEM: (productId, quantity) =>
    `${BASE_URL}/api/cart/item/${productId}?quantity=${quantity}`,
  REMOVE_CART_ITEM: (productId) => `${BASE_URL}/api/cart/remove/${productId}`,
  GET_CART: `${BASE_URL}/api/cart`,
  CLEAR_CART: `${BASE_URL}/api/cart/clear`,
  PLACE_ORDER: `${BASE_URL}/api/orders/place`,
  GET_ORDERS: `${BASE_URL}/api/orders`,
  CANCEL_ORDER: (id) => `${BASE_URL}/api/orders/${id}/cancel`,
  ORDER_INVOICE: (id) => `${BASE_URL}/api/orders/${id}/invoice`,
  ADMIN_ORDERS: `${BASE_URL}/api/orders/admin/all`,
  ADMIN_UPDATE_ORDER: (id) => `${BASE_URL}/api/orders/admin/${id}/status`,
  ADMIN_CANCEL_ORDER: (id) => `${BASE_URL}/api/orders/admin/${id}/cancel`,
  ADMIN_ORDER_INVOICE: (id) => `${BASE_URL}/api/orders/admin/${id}/invoice`,
  ADD_REVIEW: (productId) => `${BASE_URL}/api/reviews/${productId}`,
  GET_REVIEWS: (productId) => `${BASE_URL}/api/reviews/${productId}`,
  ADD_PRODUCT: `${BASE_URL}/api/products`,
  UPDATE_PRODUCT: (id) => `${BASE_URL}/api/products/${id}`,
  DELETE_PRODUCT: (id) => `${BASE_URL}/api/products/${id}`,
  ADMIN_DASHBOARD: `${BASE_URL}/api/admin/dashboard`,
  ADMIN_ANALYTICS: `${BASE_URL}/api/orders/admin/analytics`,
  ADMIN_SEED_PRODUCTS: `${BASE_URL}/api/admin/seed-products`,
  ADMIN_USERS: `${BASE_URL}/api/admin/users`,
  ADMIN_BLOCK_USER: (id, blocked) => `${BASE_URL}/api/admin/users/${id}/block?blocked=${blocked}`,
  COUPON_VALIDATE: (code, subtotal) =>
    `${BASE_URL}/api/coupons/validate?code=${encodeURIComponent(code)}&subtotal=${subtotal}`,
  ADMIN_COUPONS: `${BASE_URL}/api/admin/coupons`,
  ADMIN_COUPON_BY_ID: (id) => `${BASE_URL}/api/admin/coupons/${id}`,
  PAYMENT_CONFIG: `${BASE_URL}/api/payments/config`,
  CREATE_PAYMENT_ORDER: `${BASE_URL}/api/payments/create-order`,
  RESTAURANT: `${BASE_URL}/api/restaurant`,
  CHATBOT: `${BASE_URL}/api/chatbot/message`,
};

const FALLBACK_IMAGE =
  "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=900&q=80";

const ORDER_STATUSES = [
  "PLACED",
  "CONFIRMED",
  "PREPARING",
  "OUT_FOR_DELIVERY",
  "DELIVERED",
];

function getUser() {
  try {
    const user = localStorage.getItem("user");
    return user ? JSON.parse(user) : null;
  } catch (error) {
    localStorage.removeItem("user");
    return null;
  }
}

function getToken() {
  return getUser()?.token || null;
}

function isLoggedIn() {
  return !!getUser();
}

function logout() {
  localStorage.removeItem("user");
  window.location.href = "login.html";
}

async function apiFetch(url, options = {}) {
  const token = getToken();
  const headers = {
    "Content-Type": "application/json",
    ...(token && { Authorization: `Bearer ${token}` }),
    ...(options.headers || {}),
  };

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    showToast("Your session expired. Please login again.", "error");
    if (isLoggedIn()) logout();
    return null;
  }

  const text = await response.text();
  let data = null;

  if (text) {
    try {
      data = JSON.parse(text);
    } catch (error) {
      data = text;
    }
  }

  if (!response.ok) {
    const message = typeof data === "object" && data?.message ? data.message : "Request failed";
    showToast(message, "error");
    throw new Error(message);
  }

  return data;
}

function renderNav(activePage = document.body.dataset.page || "home") {
  const nav = document.getElementById("navLinks");
  if (!nav) return;

  const user = getUser();
  const isAdmin = user?.role === "ROLE_ADMIN";
  const isCustomer = user?.role === "ROLE_CUSTOMER";

  const link = (href, label, icon, key, extra = "") => `
    <li>
      <a class="${activePage === key ? "active" : ""}" href="${href}">
        <i data-lucide="${icon}"></i>
        <span>${label}</span>${extra}
      </a>
    </li>`;

  let html = link("index.html", "Home", "utensils", "home");

  if (!user) {
    html += link("login.html", "Login", "log-in", "login");
    html += link("register.html", "Register", "user-plus", "register");
  } else if (isAdmin) {
    html += link("admin.html", "Admin", "layout-dashboard", "admin");
    html += `<li><button class="nav-button" type="button" onclick="logout()"><i data-lucide="log-out"></i><span>Logout</span></button></li>`;
  } else if (isCustomer) {
    html += link(
      "cart.html",
      "Cart",
      "shopping-cart",
      "cart",
      `<span class="badge" id="cartCount">0</span>`
    );
    html += link("orders.html", "Orders", "package-check", "orders");
    html += link("profile.html", "Profile", "user-round", "profile");
    html += `<li><button class="nav-button" type="button" onclick="logout()"><i data-lucide="log-out"></i><span>Logout</span></button></li>`;
  }

  nav.innerHTML = html;
  refreshIcons();
  updateCartCount();
}

async function updateCartCount() {
  const badge = document.getElementById("cartCount");
  const user = getUser();
  if (!badge || user?.role !== "ROLE_CUSTOMER") return;

  try {
    const cart = await apiFetch(API.GET_CART);
    const count = cart?.items?.reduce((sum, item) => sum + item.quantity, 0) || 0;
    badge.textContent = count;
  } catch (error) {
    badge.textContent = "0";
  }
}

function requireCustomer() {
  const user = getUser();
  if (!user || user.role !== "ROLE_CUSTOMER") {
    showToast("Please login as a customer first.", "error");
    window.location.href = "login.html";
    return false;
  }
  return true;
}

function requireAdmin() {
  const user = getUser();
  if (!user || user.role !== "ROLE_ADMIN") {
    showToast("Admin access only.", "error");
    window.location.href = "index.html";
    return false;
  }
  return true;
}

function showToast(message, type = "success") {
  let container = document.getElementById("toastContainer");
  if (!container) {
    container = document.createElement("div");
    container.id = "toastContainer";
    container.className = "toast-container";
    document.body.appendChild(container);
  }

  const toast = document.createElement("div");
  toast.className = `toast toast-${type}`;
  toast.textContent = message;
  container.appendChild(toast);

  setTimeout(() => toast.classList.add("show"), 10);
  setTimeout(() => {
    toast.classList.remove("show");
    setTimeout(() => toast.remove(), 250);
  }, 3200);
}

function money(value) {
  return `Rs ${Number(value || 0).toFixed(0)}`;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function imageOrFallback(url) {
  return url && url.trim() ? escapeHtml(url.trim()) : FALLBACK_IMAGE;
}

function formatDateTime(value) {
  if (!value) return "Just now";
  return new Date(value).toLocaleString("en-IN", {
    dateStyle: "medium",
    timeStyle: "short",
  });
}

function stockLabel(product) {
  if (!product.available || product.stock <= 0) {
    return { text: "Out of stock", tone: "danger" };
  }
  if (product.stock <= 5) {
    return { text: `${product.stock} left`, tone: "warning" };
  }
  return { text: "Available", tone: "success" };
}

function refreshIcons() {
  if (window.lucide) {
    window.lucide.createIcons();
  }
}

function mountChatbot() {
  const root = document.getElementById("chatbotRoot");
  if (!root) return;

  root.innerHTML = `
    <button class="chat-toggle" id="chatToggle" type="button" aria-label="Open chat">
      <i data-lucide="bot"></i>
    </button>
    <section class="chat-panel" id="chatPanel" aria-label="Food assistant">
      <div class="chat-head">
        <div>
          <strong>Foody Assistant</strong>
          <span>Online</span>
        </div>
        <button class="icon-button" id="chatClose" type="button" aria-label="Close chat">
          <i data-lucide="x"></i>
        </button>
      </div>
      <div class="chat-messages" id="chatMessages">
        <div class="chat-message bot">Ask me about delivery, payments, addresses, reviews, or recommendations.</div>
      </div>
      <form class="chat-form" id="chatForm">
        <input id="chatInput" placeholder="Type your question" autocomplete="off" />
        <button class="icon-button filled" type="submit" aria-label="Send">
          <i data-lucide="send"></i>
        </button>
      </form>
    </section>
  `;

  const panel = document.getElementById("chatPanel");
  const toggle = document.getElementById("chatToggle");
  const close = document.getElementById("chatClose");
  const form = document.getElementById("chatForm");
  const input = document.getElementById("chatInput");
  const messages = document.getElementById("chatMessages");

  toggle.addEventListener("click", () => panel.classList.toggle("open"));
  close.addEventListener("click", () => panel.classList.remove("open"));
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const message = input.value.trim();
    if (!message) return;
    appendChat(messages, message, "user");
    input.value = "";

    try {
      const data = await apiFetch(API.CHATBOT, {
        method: "POST",
        body: JSON.stringify({ message }),
      });

      let reply = data?.reply || "I could not understand that.";
      const recommendations = data?.recommendations || [];
      if (recommendations.length) {
        reply += `<div class="chat-recos">${recommendations
          .map(
            (product) =>
              `<button type="button" onclick="location.href='product.html?id=${product.id}'">${escapeHtml(product.name)}</button>`
          )
          .join("")}</div>`;
      }
      appendChat(messages, reply, "bot", true);
    } catch (error) {
      appendChat(messages, "I am having trouble connecting right now.", "bot");
    }
  });

  refreshIcons();
}

function appendChat(container, message, type, isHtml = false) {
  const div = document.createElement("div");
  div.className = `chat-message ${type}`;
  if (isHtml) {
    div.innerHTML = message;
  } else {
    div.textContent = message;
  }
  container.appendChild(div);
  container.scrollTop = container.scrollHeight;
}

document.addEventListener("DOMContentLoaded", () => {
  renderNav();
  mountChatbot();
});
