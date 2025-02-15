let login_form = $("#login_form");

function validateLoginForm() {
    $("#login_error_message").text(""); // Clear previous messages
    const email = $("input[name=email]").val();
    const password = $("input[name=password]").val();
    const recaptchaResponse = grecaptcha.getResponse();

    if (!email.trim()) {
        $("#login_error_message").text("Email cannot be empty");
        return false;
    }
    if (!password.trim()) {
        $("#login_error_message").text("Password cannot be empty");
        return false;
    }
    if (!recaptchaResponse) {
        $("#login_error_message").text("Please complete the reCAPTCHA");
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
        grecaptcha.reset(); // Reset reCAPTCHA on failed login
    }
}

function submitLoginForm(formSubmitEvent) {
    formSubmitEvent.preventDefault();

    if (!validateLoginForm()) {
        return;
    }

    const formData = login_form.serialize() + "&g-recaptcha-response=" + grecaptcha.getResponse();

    $.ajax("../api/dashboard-login", {
        method: "POST",
        data: formData,
        dataType: "json",
        success: handleLoginResult,
        error: function(xhr, status, error) {
            let errorMessage = "Server error occurred";
            if (xhr.responseJSON) {
                errorMessage = xhr.responseJSON.message || errorMessage;
            }
            $("#login_error_message").text(errorMessage);
            grecaptcha.reset(); // Reset reCAPTCHA on error
        }
    });
}

login_form.submit(submitLoginForm);