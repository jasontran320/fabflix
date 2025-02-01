// file sourced from:
// Repository: https://github.com/UCI-Chenli-teaching/cs122b-project2-login-cart-example/tree/main
// File: WebContent/login.js

let login_form = $("#login_form");

function validateLoginForm() {
    $("#login_error_message").text("");
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

function handleLoginResult(resultData, textStatus, xhr) {
    $("#login_error_message").text("");
    try {
        let resultDataJson;
        if (typeof resultData === 'string') {
            resultDataJson = JSON.parse(resultData);
        } else {
            resultDataJson = resultData;
        }

        if (resultDataJson["status"] === "success") {
            window.location.replace("index.html");
        } else {
            $("#login_error_message").text(resultDataJson["message"]);
        }
    } catch (error) {
        console.error("Response parsing error:", error);
        $("#login_error_message").text(
            "An unexpected error occurred. Please try again later."
        );
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
        dataType: "json",
        success: handleLoginResult,
        error: function(xhr, status, error) {
            console.error("Login error:", status, error);
            try {
                if (xhr.responseJSON) {
                    $("#login_error_message").text(
                        xhr.responseJSON.message || "Server error occurred"
                    );
                } else if (xhr.responseText) {
                    const errorJson = JSON.parse(xhr.responseText);
                    $("#login_error_message").text(
                        errorJson.message || "Server error occurred"
                    );
                } else {
                    $("#login_error_message").text(
                        "Unable to connect to the server. Please try again later."
                    );
                }
            } catch (e) {
                $("#login_error_message").text(
                    "An unexpected error occurred. Please try again later."
                );
            }
        }
    });
}

login_form.submit(submitLoginForm);