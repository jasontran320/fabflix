function handleCartData(resultData) {
    console.log("Handling cart data...");

    let cartBody = $("#cart_list");
    let paymentButton = $("#proceed-payment");

    if (typeof resultData === 'string') {
        resultData = JSON.parse(resultData);
    }

    cartBody.empty();

    // Handle empty cart
    if (!resultData.items || resultData.items.length === 0) {
        cartBody.html('<div class="alert alert-info">Your cart is empty</div>');
        $("#total_price").html("");

        paymentButton
            .addClass('disabled')
            .attr('disabled', true)
            .attr('title', 'Add items to cart first')
            .css('cursor', 'not-allowed')
            .attr('href', '#')
            .on('click', function(e) {
                e.preventDefault();
                return false;
            });

        return;
    }

    // Enable payment button if cart has items
    paymentButton
        .removeClass('disabled')
        .removeAttr('disabled')
        .removeAttr('title')
        .attr('href', 'payment.html')
        .css('cursor', 'pointer')
        .off('click');

    let html = `
        <table class="table">
            <thead>
                <tr>
                    <th>Title</th>
                    <th>Price</th>
                    <th>Quantity</th>
                    <th>Total</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
    `;

    resultData.items.forEach(item => {
        html += `
            <tr>
                <td>${item.title}</td>
                <td>$${item.price.toFixed(2)}</td>
                <td>
                    <div class="btn-group">
                        <button class="btn btn-sm btn-outline-secondary" onclick="updateQuantity('${item.movieId}', ${item.quantity-1})">-</button>
                        <span class="px-2">${item.quantity}</span>
                        <button class="btn btn-sm btn-outline-secondary" onclick="updateQuantity('${item.movieId}', ${item.quantity+1})">+</button>
                    </div>
                </td>
                <td>$${(item.price * item.quantity).toFixed(2)}</td>
                <td>
                    <button class="btn btn-sm btn-danger" onclick="removeItem('${item.movieId}')">Remove</button>
                </td>
            </tr>
        `;
    });

    html += '</tbody></table>';
    cartBody.html(html);
    $("#total_price").html(`<h3>Total: $${resultData.totalPrice.toFixed(2)}</h3>`);
}
function loadCart() {
    console.log("Loading cart...");
    $.ajax("api/cart", {
        method: "GET",
        success: function(result) {
            console.log("Cart data received:");
            handleCartData(result);
        },
        error: function(xhr, status, error) {
            console.log("Error loading cart:", error);
        }
    });
}

function updateQuantity(movieId, quantity) {
    console.log("Updating quantity:", quantity);
    $.ajax("api/cart", {
        method: "POST",
        data: {
            movieId: movieId,
            action: "update",
            quantity: quantity
        },
        success: function(response) {
            console.log("Update response:", response);
            loadCart();
        },
        error: function(xhr, status, error) {
            console.log("Error updating quantity:", error);
        }
    });
}

function removeItem(movieId) {
    console.log("Removing item");
    $.ajax("api/cart", {
        method: "POST",
        data: {
            movieId: movieId,
            action: "remove"
        },
        success: function(response) {
            console.log("Successfully Removed:");
            loadCart();
        },
        error: function(xhr, status, error) {
            console.log("Error removing item:", error);
        }
    });
}

// Make sure to load cart when page is ready
$(document).ready(function() {
    console.log("Document ready");
    loadCart();

    // Initialize tooltips
    $('[data-toggle="tooltip"]').tooltip();
});