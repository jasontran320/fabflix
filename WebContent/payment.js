// Initialize form element
let payment_form = $("#payment_form");

// Format credit card number with spaces
$('input[name="creditCard"]').on('input', function(e) {
    let value = $(this).val().replace(/\s+/g, '');
    let formatted = value.replace(/(\d{4})(?=\d)/g, '$1 ');
    $(this).val(formatted);
});

function handlePayment(resultDataString) {
    let resultDataJson = JSON.parse(resultDataString);
    let messageDiv = $("#payment_message");

    if (resultDataJson.status === "success") {
        // Store the complete order data including sale IDs
        sessionStorage.setItem('lastOrder', JSON.stringify(resultDataJson.orderData));
        window.location.href = "confirmation.html";
    } else {
        messageDiv.text(resultDataJson.message);
        messageDiv.show();
    }
}

// Handle form submission
payment_form.submit(function(formEvent) {
    formEvent.preventDefault();
    $("#payment_message").hide();

    let formData = {
        firstName: $('input[name="firstName"]').val(),
        lastName: $('input[name="lastName"]').val(),
        creditCard: $('input[name="creditCard"]').val(),
        expiration: $('input[name="expiration"]').val()
    };

    $.ajax("api/order", {
        method: "POST",
        data: formData,
        success: function(resultDataString) {
            let resultDataJson = JSON.parse(resultDataString);
            let messageDiv = $("#payment_message");

            if (resultDataJson.status === "success") {
                // Store the complete order data including sale IDs
                sessionStorage.setItem('lastOrder', JSON.stringify(resultDataJson.orderData));
                window.location.href = "confirmation.html";
            } else {
                messageDiv.text(resultDataJson.message);
                messageDiv.show();
            }
        },
        error: function(xhr, status, error) {
            console.log("Error:", status, error);
            $("#payment_message")
                .text("Error processing payment. Please try again.")
                .show();
        }
    });
});

// Load total price with error handling
$(document).ready(function() {
    $.ajax("api/cart", {
        method: "GET",
        dataType: "json",
        success: function(result) {
            // Try to access the data regardless of nesting
            const price = result && result.totalPrice ? result.totalPrice :
                result && result.data && result.data.totalPrice ? result.data.totalPrice :
                    0;
            if (price != null) {
                $("#total_price").text(`Total to pay: $${Number(price).toFixed(2)}`);
            } else {
                $("#total_price").text("Total to pay: $0.00");
            }
        },
        error: function(xhr, status, error) {
            console.log("Ajax error:", status, error);
            $("#total_price").text("Total to pay: $0.00");
        }
    });
});