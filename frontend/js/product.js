let productId = null;
let currentProduct = null;
let wishlistIds = [];

document.addEventListener("DOMContentLoaded", async () => {
  productId = new URLSearchParams(window.location.search).get("id");
  if (!productId) {
    showToast("Invalid product.", "error");
    window.location.href = "index.html";
    return;
  }

  await loadWishlistForProduct();
  loadProduct();
  loadReviews();
});

async function loadWishlistForProduct() {
  const user = getUser();
  if (user?.role !== "ROLE_CUSTOMER") return;
  try {
    wishlistIds = (await apiFetch(API.WISHLIST)) || [];
  } catch (error) {
    wishlistIds = [];
  }
}

async function loadProduct() {
  const container = document.getElementById("productContainer");

  try {
    currentProduct = await apiFetch(API.PRODUCT_BY_ID(productId));
    const user = getUser();
    const isAdmin = user?.role === "ROLE_ADMIN";
    const stock = stockLabel(currentProduct);
    const out = !currentProduct.available || currentProduct.stock <= 0;

    container.innerHTML = `
      <div class="detail-grid">
        <div class="detail-image">
          <img src="${imageOrFallback(currentProduct.imageUrl)}" alt="${escapeHtml(currentProduct.name)}" onerror="this.src='${FALLBACK_IMAGE}'" />
        </div>
        <section class="panel">
          <p class="eyebrow">${escapeHtml(currentProduct.category || "Meals")}</p>
          <h1>${escapeHtml(currentProduct.name)}</h1>
          <div class="rating-line">
            <strong>${currentProduct.averageRating ? currentProduct.averageRating.toFixed(1) : "New"}</strong>
            <span>${currentProduct.reviewCount ? `${currentProduct.reviewCount} review${currentProduct.reviewCount === 1 ? "" : "s"}` : "No reviews yet"}</span>
          </div>
          <p class="muted">${escapeHtml(currentProduct.description || "Freshly prepared and delivered warm.")}</p>
          <div class="metric-strip">
            <div class="metric"><strong>${money(currentProduct.price)}</strong><span>Price</span></div>
            <div class="metric"><strong>${currentProduct.preparationTimeMinutes || 25} min</strong><span>Prep time</span></div>
            <div class="metric"><strong>${currentProduct.stock}</strong><span>Stock</span></div>
          </div>
          <div class="hero-actions">
            <span class="pill pill-${stock.tone}">${stock.text}</span>
            ${
              isAdmin
                ? `<a class="btn btn-outline" href="admin.html"><i data-lucide="settings"></i>Manage</a>`
                : `<button class="btn btn-outline" type="button" onclick="toggleWishlist('${currentProduct.id}')">
                    <i data-lucide="heart"></i>${wishlistIds.includes(currentProduct.id) ? "Wishlisted" : "Wishlist"}
                  </button>
                  <button class="btn btn-primary" type="button" ${out ? "disabled" : ""} onclick="addToCart('${currentProduct.id}')">
                    <i data-lucide="shopping-cart"></i>${out ? "Unavailable" : "Add to cart"}
                  </button>`
            }
          </div>
          ${ratingBars(currentProduct)}
        </section>
      </div>
    `;

    const reviewForm = document.getElementById("reviewForm");
    if (reviewForm && user?.role === "ROLE_CUSTOMER") {
      reviewForm.style.display = "grid";
    }
    refreshIcons();
  } catch (error) {
    container.innerHTML = `<div class="empty"><div><i data-lucide="circle-alert"></i><h2>Product not found</h2></div></div>`;
    refreshIcons();
  }
}

async function loadReviews() {
  const list = document.getElementById("reviewsList");
  if (!list) return;

  try {
    const reviews = await apiFetch(API.GET_REVIEWS(productId));
    list.innerHTML = "";

    if (!reviews || reviews.length === 0) {
      list.innerHTML = `<div class="empty"><div><i data-lucide="message-square"></i><h3>No reviews yet</h3><p>Delivered customers can leave the first one.</p></div></div>`;
      refreshIcons();
      return;
    }

    reviews.forEach((review) => {
      const div = document.createElement("article");
      div.className = "review-card";
      div.innerHTML = `
        <strong>Rating ${review.rating}/5</strong>
        <p>${escapeHtml(review.comment)}</p>
        <small class="muted">${formatDateTime(review.createdAt)}</small>
      `;
      list.appendChild(div);
    });
  } catch (error) {
    list.innerHTML = "";
  }
}

async function submitReview() {
  const rating = Number(document.getElementById("rating").value);
  const comment = document.getElementById("comment").value.trim();

  if (!rating || rating < 1 || rating > 5) {
    showToast("Choose a rating between 1 and 5.", "error");
    return;
  }

  if (!comment) {
    showToast("Write a short review.", "error");
    return;
  }

  await apiFetch(API.ADD_REVIEW(productId), {
    method: "POST",
    body: JSON.stringify({ rating, comment }),
  });

  document.getElementById("rating").value = "";
  document.getElementById("comment").value = "";
  showToast("Review submitted.");
  await loadProduct();
  loadReviews();
}

async function addToCart(id) {
  const user = getUser();
  if (!user) {
    showToast("Please login first.", "error");
    window.location.href = "login.html";
    return;
  }

  await apiFetch(API.ADD_TO_CART(id, 1), { method: "POST" });
  showToast("Added to cart.");
  updateCartCount();
}

async function toggleWishlist(id) {
  const user = getUser();
  if (!user) {
    showToast("Please login first.", "error");
    window.location.href = "login.html";
    return;
  }

  if (wishlistIds.includes(id)) {
    wishlistIds = await apiFetch(API.WISHLIST_ITEM(id), { method: "DELETE" });
    showToast("Removed from wishlist.");
  } else {
    wishlistIds = await apiFetch(API.WISHLIST_ITEM(id), { method: "POST" });
    showToast("Saved to wishlist.");
  }

  loadProduct();
}

function ratingBars(product) {
  const count = product.reviewCount || 0;
  const breakdown = product.ratingBreakdown || {};
  return `
    <div class="panel" style="box-shadow:none;margin-top:16px;">
      <h3>Rating summary</h3>
      <div class="rating-bars">
        ${[5, 4, 3, 2, 1]
          .map((rating) => {
            const value = breakdown[rating] || breakdown[String(rating)] || 0;
            const percent = count ? Math.round((value / count) * 100) : 0;
            return `
              <div class="rating-bar">
                <span>${rating} star</span>
                <span class="rating-bar-track"><span class="rating-bar-fill" style="width:${percent}%"></span></span>
                <span>${value}</span>
              </div>
            `;
          })
          .join("")}
      </div>
    </div>
  `;
}
