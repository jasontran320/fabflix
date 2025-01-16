function handleMovieListResult(resultData) {
    console.log("handleMovieListResult: populating movie table from resultData");

    let movieTableBodyElement = jQuery("#movie_table_body");

    for (let i = 0; i < resultData.length; i++) {
        let movieData = resultData[i];
        let rowHTML = "";
        rowHTML += "<tr>";

        // Movie title with link
        rowHTML += "<td><a href='single-movie.html?id=" + movieData["movie_id"] + "'>" +
            movieData["movie_title"] + "</a></td>";

        // Year
        rowHTML += "<td>" + movieData["movie_year"] + "</td>";

        // Director
        rowHTML += "<td>" + movieData["movie_director"] + "</td>";

        // Genres (up to 3)
        rowHTML += "<td>";
        let genres = movieData["genres"];
        genres.forEach((genre, index) => {
            rowHTML += genre["genre_name"];
            if (index < genres.length - 1) rowHTML += ", ";
        });
        rowHTML += "</td>";

        // Stars (up to 3)
        rowHTML += "<td>";
        let stars = movieData["stars"];
        stars.forEach((star, index) => {
            rowHTML += "<a href='single-star.html?id=" + star["star_id"] + "'>" +
                star["star_name"] + "</a>";
            if (index < stars.length - 1) rowHTML += ", ";
        });
        rowHTML += "</td>";

        // Rating
        rowHTML += "<td>" + movieData["movie_rating"] + "</td>";

        rowHTML += "</tr>";

        movieTableBodyElement.append(rowHTML);
    }
}

// Makes the HTTP GET request and registers on success callback function handleMovieListResult
jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "api/movies",
    success: (resultData) => handleMovieListResult(resultData)
});