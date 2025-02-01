// file sourced from:
// Repository: https://github.com/UCI-Chenli-teaching/cs122b-project2-login-cart-example/tree/main
// File: WebContent/login.js

let login_form = $("#login_form");

/**
 * Handle the data returned by LoginServlet
 * @param resultDataString jsonObject
 */
function validateLoginForm() {
    $("#login_error_message").text(""); // Clear previous messages
    const username = $("input[name=username]").val();
    const password = $("input[name=password]").val();

    if (!username.trim()) {
        $("#login_error_message").text("Username cannot be empty");
        return false;
    }
    if (!password.trim()) {
        $("#login_error_message").text("Password cannot be empty");
        return false;
    }
    if (username.length > 50) {
        $("#login_error_message").text("Username cannot exceed 50 characters");
        return false;
    }
    if (password.length > 20) {
        $("#login_error_message").text("Password cannot exceed 20 characters");
        return false;
    }
    return true;
}

function handleLoginResult(resultDataString) {
    let resultDataJson = JSON.parse(resultDataString);
    $("#login_error_message").text(""); // Clear previous messages

    if (resultDataJson["status"] === "success") {
        window.location.replace("index.html");
    } else {
        $("#login_error_message").text(resultDataJson["message"]);
    }
}

function submitLoginForm(formSubmitEvent) {
    formSubmitEvent.preventDefault();

    if (!validateLoginForm()) {
        return;
    }

    $.ajax("api/login", {
        method: "POST",
        data: login_form.serialize(),
        success: handleLoginResult,
        error: function(xhr) {
            const errorJson = JSON.parse(xhr.responseText);
            $("#login_error_message").text(
                errorJson.message || "Server error occurred"
            );
        }
    });
}

// Bind the submit action of the form to a handler function
login_form.submit(submitLoginForm);
