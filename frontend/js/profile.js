let profileMap = null;
let profileMarker = null;
let profileState = null;
let profileProducts = [];

document.addEventListener("DOMContentLoaded", () => {
  if (!requireCustomer()) return;
  document.getElementById("profileForm")?.addEventListener("submit", saveProfile);
  document.getElementById("addressForm")?.addEventListener("submit", saveAddress);
  document.getElementById("addressLocationBtn")?.addEventListener("click", useAddressLocation);
  document.getElementById("clearAddressBtn")?.addEventListener("click", resetAddressForm);
  loadProfile();
  initProfileMap();
});

async function loadProfile() {
  [profileState, profileProducts] = await Promise.all([
    apiFetch(API.PROFILE),
    apiFetch(API.PRODUCTS),
  ]);
  document.getElementById("profileName").value = profileState.name || "";
  document.getElementById("profileEmail").value = profileState.email || "";
  document.getElementById("profilePhone").value = profileState.phone || "";
  renderProfileAddresses();
  renderWishlist();
}

function renderProfileAddresses() {
  const list = document.getElementById("profileAddressList");
  const addresses = profileState?.addresses || [];

  if (!addresses.length) {
    list.innerHTML = `
      <div class="empty">
        <div>
          <i data-lucide="map-pin"></i>
          <h3>No addresses saved</h3>
          <p>Add home, hostel, or office delivery addresses here.</p>
        </div>
      </div>
    `;
    refreshIcons();
    return;
  }

  list.innerHTML = addresses
    .map(
      (address) => `
        <article class="card">
          <div class="card-row">
            <div>
              <span class="pill ${address.defaultAddress ? "pill-success" : ""}">${address.defaultAddress ? "Default" : "Saved"}</span>
              <h3>${escapeHtml(address.label || "Delivery")}</h3>
              <p class="muted">${escapeHtml(address.line1)}, ${escapeHtml(address.landmark || "")}</p>
              <p class="muted">${escapeHtml(address.city)}, ${escapeHtml(address.state)} - ${escapeHtml(address.pincode)}</p>
            </div>
            <div class="hero-actions">
              <button class="icon-button" type="button" onclick="editAddress('${address.id}')" aria-label="Edit address"><i data-lucide="pencil"></i></button>
              <button class="icon-button" type="button" onclick="makeDefaultAddress('${address.id}')" aria-label="Make default"><i data-lucide="check"></i></button>
              <button class="icon-button" type="button" onclick="deleteAddress('${address.id}')" aria-label="Delete address"><i data-lucide="trash-2"></i></button>
            </div>
          </div>
        </article>
      `
    )
    .join("");
  refreshIcons();
}

async function saveProfile(event) {
  event.preventDefault();
  const payload = {
    name: document.getElementById("profileName").value.trim(),
    phone: document.getElementById("profilePhone").value.trim(),
  };

  profileState = await apiFetch(API.PROFILE, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
  showToast("Profile updated.");
}

async function saveAddress(event) {
  event.preventDefault();
  const id = document.getElementById("addressId").value;
  const payload = {
    label: document.getElementById("addressLabel").value.trim(),
    line1: document.getElementById("addressLine1").value.trim(),
    landmark: document.getElementById("addressLandmark").value.trim(),
    city: document.getElementById("addressCity").value.trim(),
    state: document.getElementById("addressState").value.trim(),
    pincode: document.getElementById("addressPincode").value.trim(),
    latitude: Number(document.getElementById("addressLat").value || 0),
    longitude: Number(document.getElementById("addressLng").value || 0),
    defaultAddress: document.getElementById("addressDefault").checked,
  };

  profileState = await apiFetch(id ? API.ADDRESS_BY_ID(id) : API.ADDRESS, {
    method: id ? "PUT" : "POST",
    body: JSON.stringify(payload),
  });

  resetAddressForm();
  renderProfileAddresses();
  showToast(id ? "Address updated." : "Address added.");
}

function editAddress(id) {
  const address = profileState.addresses.find((item) => item.id === id);
  if (!address) return;

  document.getElementById("addressId").value = address.id;
  document.getElementById("addressLabel").value = address.label || "";
  document.getElementById("addressLine1").value = address.line1 || "";
  document.getElementById("addressLandmark").value = address.landmark || "";
  document.getElementById("addressCity").value = address.city || "";
  document.getElementById("addressState").value = address.state || "";
  document.getElementById("addressPincode").value = address.pincode || "";
  document.getElementById("addressDefault").checked = !!address.defaultAddress;
  setAddressCoords(address.latitude || 0, address.longitude || 0);
  document.getElementById("addressFormTitle").textContent = "Edit Address";
  document.getElementById("addressForm").scrollIntoView({ behavior: "smooth" });
}

async function makeDefaultAddress(id) {
  profileState = await apiFetch(API.ADDRESS_DEFAULT(id), { method: "PUT" });
  renderProfileAddresses();
  showToast("Default address changed.");
}

async function deleteAddress(id) {
  if (!confirm("Delete this address?")) return;
  profileState = await apiFetch(API.ADDRESS_BY_ID(id), { method: "DELETE" });
  renderProfileAddresses();
  showToast("Address deleted.");
}

function renderWishlist() {
  const list = document.getElementById("profileWishlist");
  if (!list) return;

  const ids = profileState?.wishlistProductIds || [];
  const products = profileProducts.filter((product) => ids.includes(product.id));

  if (!products.length) {
    list.innerHTML = `
      <div class="empty">
        <div>
          <i data-lucide="heart"></i>
          <h3>No favourites yet</h3>
          <p>Tap the heart on any food card to save it here.</p>
        </div>
      </div>
    `;
    refreshIcons();
    return;
  }

  list.innerHTML = products
    .map(
      (product) => `
        <article class="card">
          <div class="cart-item">
            <img src="${imageOrFallback(product.imageUrl)}" alt="${escapeHtml(product.name)}" onerror="this.src='${FALLBACK_IMAGE}'" />
            <div>
              <h3>${escapeHtml(product.name)}</h3>
              <p class="muted">${escapeHtml(product.category || "Meals")} - ${money(product.price)}</p>
            </div>
            <div class="cart-actions">
              <button class="btn btn-outline" type="button" onclick="location.href='product.html?id=${product.id}'"><i data-lucide="eye"></i>View</button>
              <button class="icon-button" type="button" onclick="removeWishlist('${product.id}')" aria-label="Remove"><i data-lucide="trash-2"></i></button>
            </div>
          </div>
        </article>
      `
    )
    .join("");
  refreshIcons();
}

async function removeWishlist(productId) {
  profileState.wishlistProductIds = await apiFetch(API.WISHLIST_ITEM(productId), { method: "DELETE" });
  renderWishlist();
  showToast("Removed from wishlist.");
}

function resetAddressForm() {
  document.getElementById("addressFormTitle").textContent = "Add Address";
  document.getElementById("addressForm").reset();
  document.getElementById("addressId").value = "";
  setAddressCoords(0, 0, false);
}

function initProfileMap() {
  const mapEl = document.getElementById("profileAddressMap");
  if (!mapEl || !window.L) return;

  profileMap = L.map("profileAddressMap", { scrollWheelZoom: false }).setView([28.6139, 77.209], 11);
  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    attribution: "&copy; OpenStreetMap",
  }).addTo(profileMap);

  profileMap.on("click", (event) => {
    setAddressCoords(event.latlng.lat, event.latlng.lng);
  });

  setTimeout(() => profileMap.invalidateSize(), 100);
}

function useAddressLocation() {
  if (!navigator.geolocation) {
    showToast("Location is not supported by this browser.", "error");
    return;
  }

  navigator.geolocation.getCurrentPosition(
    (position) => {
      setAddressCoords(position.coords.latitude, position.coords.longitude);
      showToast("Location pinned.");
    },
    () => showToast("Please allow location access from the browser.", "error"),
    { enableHighAccuracy: true, timeout: 10000 }
  );
}

function setAddressCoords(latitude, longitude, moveMap = true) {
  document.getElementById("addressLat").value = latitude ? Number(latitude).toFixed(6) : "";
  document.getElementById("addressLng").value = longitude ? Number(longitude).toFixed(6) : "";

  if (!profileMap || !latitude || !longitude) {
    if (profileMarker) {
      profileMarker.remove();
      profileMarker = null;
    }
    return;
  }

  const latLng = [latitude, longitude];
  if (!profileMarker) {
    profileMarker = L.marker(latLng, { draggable: true }).addTo(profileMap);
    profileMarker.on("dragend", () => {
      const next = profileMarker.getLatLng();
      setAddressCoords(next.lat, next.lng, false);
    });
  } else {
    profileMarker.setLatLng(latLng);
  }

  if (moveMap) {
    profileMap.setView(latLng, 15);
  }
}
