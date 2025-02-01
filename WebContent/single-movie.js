function getParameterByName(target) {
    let url = window.location.href;
    target = target.replace(/[\[\]]/g, "\\$&");
    let regex = new RegExp("[?&]" + target + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

function handleResult(resultData) {
    console.log("handleResult: populating movie info from resultData");

    // Populate movie info
    let movieInfoElement = jQuery("#movie_info");
    movieInfoElement.append("<h2>" + resultData["movie_title"] + " (" + resultData["movie_year"] + ")</h2>");
    movieInfoElement.append("<p><strong>Director:</strong> " + resultData["movie_director"] + "</p>");
    movieInfoElement.append("<p><strong>Rating:</strong> " +
        (resultData["movie_rating"] || "N/A") +
        " (" + (resultData["movie_votes"] || "0") + " votes)</p>");

    // Sort and populate genres
    let genresElement = jQuery("#movie_genres");
    let genres = resultData["genres"];
    genres.sort((a, b) => a.genre_name.localeCompare(b.genre_name));
    let genreHtml = "<p><strong>Genres:</strong> ";
    genres.forEach((genre, index) => {
        genreHtml += `<a href="movie-list.html?genre=${genre["genre_id"]}">${genre["genre_name"]}</a>`;
        if (index < genres.length - 1) genreHtml += ", ";
    });
    genreHtml += "</p>";
    genresElement.append(genreHtml);

    genresElement.append(`
        <button class="btn btn-primary add-to-cart mb-3" data-movie-id="${resultData["movie_id"]}">
            Add to Cart
        </button>
    `);

    // Populate stars table (already sorted by movie count from backend)
    let starsTableBodyElement = jQuery("#movie_stars_body");
    let stars = resultData["stars"];
    stars.forEach((star) => {
        let rowHTML = "<tr>";
        rowHTML += "<td><a href='single-star.html?id=" + star["star_id"] + "'>" +
            star["star_name"] + "</a></td>";
        rowHTML += "</tr>";
        starsTableBodyElement.append(rowHTML);
    });

    // Add cart button handler
    $('.add-to-cart').click(function(e) {
        e.preventDefault();
        const movieId = $(this).data('movie-id');
        console.log("Attempting to add movie");

        $.ajax('api/cart', {
            method: 'POST',
            data: {
                movieId: movieId,
                action: 'add',
                quantity: 1
            },
            success: function(response) {
                console.log("Successful Response:");
                alert('Added to cart successfully!');
            },
            error: function(xhr, status, error) {
                console.log("Error details:", {
                    status: status,
                    error: error,
                    response: xhr.responseText
                });
                alert('Error adding to cart');
            }
        });
    });
}

// Get movie ID from URL and make the AJAX call
let movieId = getParameterByName('id');
jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "api/single-movie?id=" + movieId,
    success: (resultData) => handleResult(resultData)
});
