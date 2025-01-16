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

    // Populate genres
    let genresElement = jQuery("#movie_genres");
    let genres = resultData["genres"];
    let genreHtml = "<p><strong>Genres:</strong> ";
    genres.forEach((genre, index) => {
        genreHtml += genre["genre_name"];
        if (index < genres.length - 1) genreHtml += ", ";
    });
    genreHtml += "</p>";
    genresElement.append(genreHtml);

    // Populate stars table
    let starsTableBodyElement = jQuery("#movie_stars_body");
    let stars = resultData["stars"];

    stars.forEach((star) => {
        let rowHTML = "<tr>";
        rowHTML += "<td><a href='single-star.html?id=" + star["star_id"] + "'>" +
            star["star_name"] + "</a></td>";
        rowHTML += "</tr>";
        starsTableBodyElement.append(rowHTML);
    });
}

let movieId = getParameterByName('id');

jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "api/single-movie?id=" + movieId,
    success: (resultData) => handleResult(resultData)
});