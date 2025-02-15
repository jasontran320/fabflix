$(document).ready(function() {
    $("#add_movie_form").submit(function(e) {
        e.preventDefault();

        const messageDiv = $("#movie_message");
        messageDiv.hide();

        let formData = {
            title: $("#title").val().trim(),
            year: $("#year").val(),
            director: $("#director").val().trim(),
            star: $("#star").val().trim(),
            genre: $("#genre").val().trim()
        };

        $.ajax("../api/dashboard/add-movie", {
            method: "POST",
            data: formData,
            dataType: "json",
            success: function(response) {
                messageDiv.removeClass("alert-danger alert-success alert-warning");

                // Handle different statuses
                if (response.status === "success") {
                    messageDiv.addClass("alert-success");
                    let messages = response.messages.join('<br>');
                    messageDiv.html(messages);
                    $("#add_movie_form")[0].reset();
                } else {
                    messageDiv.addClass("alert-danger")
                        .html(response.message + "<br>" +
                            (response.messages ? response.messages.join('<br>') : ""));
                }
                messageDiv.show();

                // Scroll to bottom of page

                $('html, body').animate({scrollTop: $(document).height()}, 'slow');
            },
            error: function(xhr, status, error) {
                messageDiv.removeClass("alert-danger alert-success alert-warning")
                    .addClass("alert-danger");

                if (xhr.status === 409) {
                    // Movie already exists
                    messageDiv.text("Movie already exists");
                } else {
                    messageDiv.text(xhr.responseJSON?.message || "Error adding movie");
                }
                messageDiv.show();

                // Scroll to bottom of page
                $('html, body').animate({scrollTop: $(document).height()}, 'slow');
            }
        });
    });
});