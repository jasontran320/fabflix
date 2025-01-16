// Function to extract a query parameter from the URL
function getParameterByName(target) {
        let url = window.location.href;
        target = target.replace(/[\[\]]/g, "\\$&");

        let regex = new RegExp("[?&]" + target + "(=([^&#]*)|&|#|$)"),
            results = regex.exec(url);
        if (!results) return null;
        if (!results[2]) return '';

        return decodeURIComponent(results[2].replace(/\+/g, " "));
}

// Function to handle the result data and populate the HTML
function handleResult(resultData) {
        console.log("handleResult: populating star info from resultData");

        // Populate the star info section
        let starInfoElement = jQuery("#star_info");
        starInfoElement.append(
            "<p>Star Name: " + resultData[0]["star_name"] + "</p>" +
            "<p>Date Of Birth: " + (resultData[0]["star_dob"] || "N/A") + "</p>"
        );

        console.log("handleResult: populating movie table from resultData");

        // Populate the movie table
        let movieTableBodyElement = jQuery("#movie_table_body");

        for (let i = 0; i < resultData.length; i++) {
                let rowHTML = "";
                rowHTML += "<tr>";
                rowHTML += "<td><a href='single-movie.html?id=" + resultData[i]["movie_id"] + "'>" +
                    resultData[i]["movie_title"] + "</a></td>";
                rowHTML += "<td>" + resultData[i]["movie_year"] + "</td>";
                rowHTML += "<td>" + resultData[i]["movie_director"] + "</td>";
                rowHTML += "</tr>";

                movieTableBodyElement.append(rowHTML);
        }
}

// Get the star ID from the URL
let starId = getParameterByName('id');

// Send an AJAX request to fetch data for the star
jQuery.ajax({
        dataType: "json",
        method: "GET",
        url: "api/single-star?id=" + starId,
        success: (resultData) => handleResult(resultData)
});