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
        console.log("handleResult: populating star info from resultData");

        let starInfoElement = jQuery("#star_info");
        let movieTableBodyElement = jQuery("#movie_table_body");

        // Check if resultData exists and has content
        if (!resultData || !Array.isArray(resultData) || resultData.length === 0) {
                starInfoElement.html("<div class='alert alert-danger'>Star information not found</div>");
                return;
        }

        // Populate star info
        starInfoElement.append(
            "<p>Star Name: " + resultData[0]["star_name"] + "</p>" +
            "<p>Date Of Birth: " + (resultData[0]["star_dob"] || "N/A") + "</p>"
        );

        console.log("handleResult: populating movie table from resultData");

        // Check if star has any movies
        if (resultData.length > 0 && resultData[0].hasOwnProperty("movie_id")) {
                for (let i = 0; i < resultData.length; i++) {
                        let movieData = resultData[i];
                        if (movieData["movie_id"] && movieData["movie_title"]) {
                                let rowHTML = "";
                                rowHTML += "<tr>";
                                rowHTML += "<td><a href='single-movie.html?id=" + movieData["movie_id"] + "'>" +
                                    movieData["movie_title"] + "</a></td>";
                                rowHTML += "<td>" + (movieData["movie_year"] || "N/A") + "</td>";
                                rowHTML += "<td>" + (movieData["movie_director"] || "N/A") + "</td>";
                                rowHTML += "</tr>";
                                movieTableBodyElement.append(rowHTML);
                        }
                }
        } else {
                movieTableBodyElement.append("<tr><td colspan='3'>No movies found for this star</td></tr>");
        }
}

let starId = getParameterByName('id');

jQuery.ajax({
        dataType: "json",
        method: "GET",
        url: "api/single-star?id=" + starId,
        success: (resultData) => handleResult(resultData),
        error: (xhr, status, error) => {
                $("#star_info").html("<div class='alert alert-danger'>Error loading star information</div>");
                console.error(error);
        }
});