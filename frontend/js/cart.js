let cartState = null;
let profileState = null;
let restaurantState = null;
let paymentConfig = { enabled: false };
let selectedAddressId = null;
let temporaryAddress = null;
let couponState = null;
let deliveryMap = null;
let deliveryMarkers = [];
let deliveryRoute = null;

document.addEventListener("DOMContentLoaded", () => {
  if (!requireCustomer()) return;
  document.getElementById("useLocationBtn")?.addEventListener("click", useCurrentLocation);
  document.getElementById("placeOrderBtn")?.addEventListener("click", placeOrder);
  document.getElementById("applyCouponBtn")?.addEventListener("click", applyCoupon);
  document.getElementById("removeCouponBtn")?.addEventListener("click", removeCoupon);
  loadCheckout();
});

async function loadCheckout() {
  try {
    const [cart, profile, restaurant, config] = await Promise.all([
      apiFetch(API.GET_CART),
      apiFetch(API.PROFILE),
      apiFetch(API.RESTAURANT),
      apiFetch(API.PAYMENT_CONFIG),
    ]);

    cartState = cart;
    profileState = profile;
    restaurantState = restaurant;
    paymentConfig = config || { enabled: false };

    selectedAddressId =
      profileState?.addresses?.find((address) => address.defaultAddress)?.id ||
      profileState?.addresses?.[0]?.id ||
      null;

    renderCart();
    renderAddresses();
    renderPaymentOptions();
    renderDeliveryMap();
  } catch (error) {
    console.error(error);
  }
}

function renderCart() {
  const cartItems = document.getElementById("cartItems");
  const cartFooter = document.getElementById("cartFooter");
  const emptyCart = document.getElementById("emptyCart");
  cartItems.innerHTML = "";

  if (!cartState?.items?.length) {
    emptyCart.style.display = "grid";
    cartFooter.style.display = "none";
    return;
  }

  emptyCart.style.display = "none";
  cartFooter.style.display = "block";

  cartState.items.forEach((item) => {
    const div = document.createElement("article");
    div.className = "card cart-item";
    div.innerHTML = `
      <img src="${imageOrFallback(item.imageUrl)}" alt="${escapeHtml(item.name)}" onerror="this.src='${FALLBACK_IMAGE}'" />
      <div>
        <h3>${escapeHtml(item.name)}</h3>
        <p class="muted">${money(item.price)} each</p>
        <strong>${money(item.price * item.quantity)}</strong>
      </div>
      <div class="cart-actions">
        <div class="qty-control" aria-label="Quantity">
          <button type="button" onclick="changeQuantity('${item.productId}', ${item.quantity - 1})">-</button>
          <strong>${item.quantity}</strong>
          <button type="button" onclick="changeQuantity('${item.productId}', ${item.quantity + 1})">+</button>
        </div>
        <button class="icon-button" type="button" onclick="removeItem('${item.productId}')" aria-label="Remove item">
          <i data-lucide="trash-2"></i>
        </button>
      </div>
    `;
    cartItems.appendChild(div);
  });

  renderTotals();
  refreshIcons();
}

function renderTotals() {
  const subtotal = cartTotal();
  const discount = couponState?.valid ? couponState.discountAmount : 0;
  document.getElementById("subtotalAmount").textContent = money(subtotal);
  document.getElementById("discountAmount").textContent = `- ${money(discount)}`;
  document.getElementById("totalAmount").textContent = money(payableTotal());
}

function renderAddresses() {
  const list = document.getElementById("addressList");
  list.innerHTML = "";

  if (temporaryAddress) {
    list.appendChild(addressCard(temporaryAddress, "", true));
  }

  const addresses = profileState?.addresses || [];
  if (!addresses.length && !temporaryAddress) {
    list.innerHTML = `
      <div class="empty">
        <div>
          <i data-lucide="map-pin"></i>
          <h3>No saved address</h3>
          <p>Add one from your profile or use your current location.</p>
          <a class="btn btn-outline" href="profile.html"><i data-lucide="user-round"></i>Open profile</a>
        </div>
      </div>
    `;
    refreshIcons();
    return;
  }

  addresses.forEach((address) => {
    list.appendChild(addressCard(address, address.id, selectedAddressId === address.id));
  });

  refreshIcons();
}

function addressCard(address, id, active) {
  const label = address.label || "Delivery";
  const card = document.createElement("button");
  card.type = "button";
  card.className = `address-option ${active ? "active" : ""}`;
  card.onclick = () => {
    temporaryAddress = id ? null : temporaryAddress;
    selectedAddressId = id || null;
    renderAddresses();
    renderDeliveryMap();
  };
  card.innerHTML = `
    <i data-lucide="${id ? "home" : "crosshair"}"></i>
    <span>
      <strong>${escapeHtml(label)}</strong><br/>
      <small>${escapeHtml(address.line1 || "")}, ${escapeHtml(address.city || "")}, ${escapeHtml(address.pincode || "")}</small>
    </span>
  `;
  return card;
}

function renderPaymentOptions() {
  const razorpayOption = document.getElementById("razorpayOption");
  const razorpayRadio = document.getElementById("payRazorpay");
  if (!razorpayOption || !razorpayRadio) return;

  if (!paymentConfig.enabled) {
    razorpayRadio.disabled = true;
    razorpayOption.classList.add("muted");
    document.getElementById("payCod").checked = true;
  } else {
    razorpayRadio.disabled = false;
    razorpayOption.classList.remove("muted");
  }
}

function renderDeliveryMap() {
  const mapEl = document.getElementById("deliveryMap");
  if (!mapEl || !window.L || !restaurantState) return;

  const restaurantCoords = [restaurantState.latitude || 28.6139, restaurantState.longitude || 77.209];
  const address = selectedAddress() || temporaryAddress;
  const addressCoords = address?.latitude && address?.longitude ? [address.latitude, address.longitude] : null;

  if (!deliveryMap) {
    deliveryMap = L.map("deliveryMap", { scrollWheelZoom: false }).setView(restaurantCoords, 12);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: "&copy; OpenStreetMap",
    }).addTo(deliveryMap);
  }

  deliveryMarkers.forEach((marker) => marker.remove());
  if (deliveryRoute) {
    deliveryRoute.remove();
    deliveryRoute = null;
  }
  deliveryMarkers = [
    L.marker(restaurantCoords).addTo(deliveryMap).bindPopup("Restaurant"),
  ];

  if (addressCoords) {
    deliveryMarkers.push(L.marker(addressCoords).addTo(deliveryMap).bindPopup("Delivery"));
    deliveryRoute = L.polyline([restaurantCoords, addressCoords], {
      color: "#1f9d55",
      weight: 5,
      opacity: 0.9,
      dashArray: "8 8",
    }).addTo(deliveryMap);
    deliveryMap.fitBounds([restaurantCoords, addressCoords], { padding: [30, 30] });
    const eta = document.getElementById("deliveryEta");
    eta.style.display = "block";
    eta.textContent = `Estimated rider route: ${routeDistanceKm(restaurantCoords, addressCoords).toFixed(1)} km, around ${etaMinutes(restaurantCoords, addressCoords)} min`;
  } else {
    deliveryMap.setView(restaurantCoords, 12);
    const eta = document.getElementById("deliveryEta");
    eta.style.display = "none";
  }

  setTimeout(() => deliveryMap.invalidateSize(), 100);
}

async function changeQuantity(productId, quantity) {
  await apiFetch(API.UPDATE_CART_ITEM(productId, quantity), { method: "PUT" });
  const cart = await apiFetch(API.GET_CART);
  cartState = cart;
  couponState = null;
  resetCouponMessage();
  renderCart();
  updateCartCount();
}

async function removeItem(productId) {
  await apiFetch(API.REMOVE_CART_ITEM(productId), { method: "DELETE" });
  const cart = await apiFetch(API.GET_CART);
  cartState = cart;
  couponState = null;
  resetCouponMessage();
  renderCart();
  updateCartCount();
}

function useCurrentLocation() {
  if (!navigator.geolocation) {
    showToast("Location is not supported by this browser.", "error");
    return;
  }

  navigator.geolocation.getCurrentPosition(
    (position) => {
      temporaryAddress = {
        label: "Current location",
        line1: "Current location pin",
        landmark: "Shared from browser",
        city: "Pinned",
        state: "Pinned",
        pincode: "000000",
        latitude: position.coords.latitude,
        longitude: position.coords.longitude,
      };
      selectedAddressId = null;
      renderAddresses();
      renderDeliveryMap();
      showToast("Location pinned for this order.");
    },
    () => showToast("Please allow location access from the browser.", "error"),
    { enableHighAccuracy: true, timeout: 10000 }
  );
}

async function placeOrder() {
  if (!cartState?.items?.length) {
    showToast("Your cart is empty.", "error");
    return;
  }

  if (!selectedAddressId && !temporaryAddress) {
    showToast("Choose an address or use your current location.", "error");
    return;
  }

  const mode = document.querySelector("input[name='paymentMode']:checked")?.value || "CASH_ON_DELIVERY";

  if (mode === "RAZORPAY") {
    await payWithRazorpay();
    return;
  }

  await submitOrder({ paymentMode: "CASH_ON_DELIVERY" });
}

async function payWithRazorpay() {
  if (!paymentConfig.enabled || !window.Razorpay) {
    showToast("Razorpay is not configured yet.", "error");
    return;
  }

  const paymentOrder = await apiFetch(API.CREATE_PAYMENT_ORDER, {
    method: "POST",
    body: JSON.stringify({ amount: payableTotal() }),
  });

  const options = {
    key: paymentOrder.keyId,
    amount: paymentOrder.amount,
    currency: paymentOrder.currency,
    name: "Foody",
    description: "Food order payment",
    order_id: paymentOrder.orderId,
    handler: async (response) => {
      await submitOrder({
        paymentMode: "RAZORPAY",
        razorpayOrderId: response.razorpay_order_id,
        razorpayPaymentId: response.razorpay_payment_id,
        razorpaySignature: response.razorpay_signature,
      });
    },
    theme: { color: "#e84c3d" },
  };

  new Razorpay(options).open();
}

async function submitOrder(paymentData) {
  const payload = {
    ...paymentData,
    addressId: temporaryAddress ? null : selectedAddressId,
    deliveryAddress: temporaryAddress,
    couponCode: couponState?.valid ? couponState.code : "",
    orderNote: document.getElementById("orderNote")?.value.trim() || "",
  };

  await apiFetch(API.PLACE_ORDER, {
    method: "POST",
    body: JSON.stringify(payload),
  });

  showToast("Order placed successfully.");
  window.location.href = "orders.html";
}

function selectedAddress() {
  return profileState?.addresses?.find((address) => address.id === selectedAddressId);
}

function cartTotal() {
  return cartState?.items?.reduce((sum, item) => sum + item.price * item.quantity, 0) || 0;
}

function payableTotal() {
  return Math.max(cartTotal() - (couponState?.valid ? couponState.discountAmount : 0), 0);
}

async function applyCoupon() {
  const code = document.getElementById("couponCode").value.trim();
  if (!code) {
    showToast("Enter a coupon code.", "error");
    return;
  }

  couponState = await apiFetch(API.COUPON_VALIDATE(code, cartTotal()));
  const message = document.getElementById("couponMessage");
  message.textContent = couponState.message;
  message.style.color = couponState.valid ? "var(--green)" : "var(--danger)";
  renderTotals();
}

function removeCoupon() {
  couponState = null;
  document.getElementById("couponCode").value = "";
  resetCouponMessage();
  renderTotals();
}

function resetCouponMessage() {
  const message = document.getElementById("couponMessage");
  if (!message) return;
  message.textContent = "";
  message.style.color = "";
}

function routeDistanceKm(start, end) {
  const toRad = (value) => (value * Math.PI) / 180;
  const earthRadiusKm = 6371;
  const dLat = toRad(end[0] - start[0]);
  const dLng = toRad(end[1] - start[1]);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(start[0])) * Math.cos(toRad(end[0])) *
      Math.sin(dLng / 2) * Math.sin(dLng / 2);
  return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function etaMinutes(start, end) {
  return Math.max(8, Math.round((routeDistanceKm(start, end) / 18) * 60));
}
