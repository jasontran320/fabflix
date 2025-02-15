$(document).ready(function() {
    $("#star_search_form").submit(function(e) {
        e.preventDefault();

        const messageDiv = $("#search_message");
        messageDiv.hide();

        let formData = {
            name: $("#starName").val().trim(),
            birthYear: $("#birthYear").val() || null
        };

        if (!formData.name) {
            messageDiv.removeClass("alert-success alert-danger")
                .addClass("alert-danger")
                .text("Star name is required")
                .show();
            return;
        }

        $.ajax("api/search-star", {
            method: "GET",
            data: formData,
            dataType: "json",
            success: function(response) {
                if (response.found) {
                    // Redirect to single-star page
                    window.location.href = "single-star.html?id=" + response.starId;
                } else {
                    messageDiv.removeClass("alert-success alert-danger")
                        .addClass("alert-danger")
                        .text("No star found with the given criteria")
                        .show();
                }
            },
            error: function(xhr, status, error) {
                messageDiv.removeClass("alert-success alert-danger")
                    .addClass("alert-danger")
                    .text(xhr.responseJSON?.message || "Error searching for star")
                    .show();
            }
        });
    });
});