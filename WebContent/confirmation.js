function displayOrderDetails() {
    try {
        let lastOrderString = sessionStorage.getItem('lastOrder');
        let lastOrder = JSON.parse(lastOrderString);

        if (!lastOrder || !lastOrder.items || lastOrder.items.length === 0) {
            $("#order_details").html("<p>No order details available.</p>");
            return;
        }

        let html = `<table class="table">
            <thead>
                <tr>
                    <th>Sale ID</th>
                    <th>Movie</th>
                    <th>Quantity</th>
                    <th>Unit Price</th>
                    <th>Total</th>
                </tr>
            </thead>
            <tbody>`;

        lastOrder.items.forEach(item => {
            html += `
                <tr>
                    <td>#${item.saleId}</td>
                    <td>${item.title}</td>
                    <td>${item.quantity}</td>
                    <td>$${item.price.toFixed(2)}</td>
                    <td>$${(item.price * item.quantity).toFixed(2)}</td>
                </tr>`;
        });

        html += '</tbody></table>';

        $("#order_details").html(html);
        $("#total_amount").text(`Total Amount: $${lastOrder.totalPrice.toFixed(2)}`);

    } catch (error) {
        console.error("Error processing order details:", error);
        $("#order_details").html("<p>Error displaying order details</p>");
    } finally {
        // Clean up the sessionStorage after displaying
        sessionStorage.removeItem('lastOrder');
    }
}

$(document).ready(displayOrderDetails);