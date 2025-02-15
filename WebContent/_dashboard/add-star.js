$(document).ready(function() {
    $("#add_star_form").submit(function(e) {
        e.preventDefault();

        const messageDiv = $("#star_message");
        messageDiv.hide();

        let formData = {
            starName: $("#starName").val().trim(),
            birthYear: $("#birthYear").val() || null
        };

        $.ajax("../api/dashboard/add-star", {
            method: "POST",
            data: formData,
            dataType: "json",
            success: function(response) {
                messageDiv.removeClass("alert-danger alert-success");
                if (response.status === "success") {
                    messageDiv.addClass("alert-success")
                        .text(`Successfully added star: ${response.starName} (ID: ${response.starId})`);
                    $("#add_star_form")[0].reset();
                } else {
                    messageDiv.addClass("alert-danger")
                        .text(response.message || "Error adding star");
                }
                messageDiv.show();
            },
            error: function(xhr, status, error) {
                messageDiv.removeClass("alert-danger alert-success")
                    .addClass("alert-danger")
                    .text(xhr.responseJSON?.message || "Error adding star")
                    .show();
            }
        });
    });
});