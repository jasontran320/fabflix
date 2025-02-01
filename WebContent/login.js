let login_form = $("#login_form");

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

function handleLoginResult(resultData) {
    $("#login_error_message").text(""); // Clear previous messages

    if (resultData.status === "success") {
        window.location.replace("index.html");
    } else {
        $("#login_error_message").text(resultData.message);
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
            let errorMessage = "Server error occurred";
            if (xhr.responseJSON) {
                errorMessage = xhr.responseJSON.message || errorMessage;
            }
            $("#login_error_message").text(errorMessage);
        }
    });
}

// Bind the submit action of the form to a handler function
login_form.submit(submitLoginForm);