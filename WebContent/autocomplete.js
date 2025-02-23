// Cache object to store previous queries
const searchCache = new Map();

function handleLookup(query, doneCallback) {
    // Only trigger search for 3 or more characters
    if (query.length < 3) {
        doneCallback([]);
        return;
    }

    console.log("Autocomplete search initiated for query:", query);

    // Check cache first
    if (searchCache.has(query)) {
        console.log("Using cached results for query:", query);
        console.log("Cached suggestions:", searchCache.get(query));
        doneCallback(searchCache.get(query));
        return;
    }

    console.log("Sending AJAX request to server for query:", query);

    // If not in cache, make AJAX request
    jQuery.ajax({
        method: "GET",
        url: "api/autocomplete?query=" + encodeURIComponent(query),
        success: function(data) {
            console.log("Server returned suggestions:", data);
            // Transform data for jQuery UI autocomplete
            const suggestions = data.map(item => ({
                label: item.value,
                value: item.value,
                movieId: item.data.movieId
            }));
            // Cache the results
            searchCache.set(query, suggestions);
            doneCallback(suggestions);
        },
        error: function(errorData) {
            console.log("Lookup ajax error:", errorData);
            doneCallback([]);
        }
    });
}

// Initialize autocomplete on the title input
$(document).ready(function() {
    $('input[name="title"]').autocomplete({
        minLength: 3,
        delay: 300,
        source: function(request, response) {
            handleLookup(request.term, response);
        },
        select: function(event, ui) {
            window.location.href = `single-movie.html?id=${ui.item.movieId}`;
            return false;
        }
    }).on('focus', function() {
        if (this.value.length >= 3) {
            $(this).autocomplete('search');
        }
    });

    // Remove the separate keypress handler and handle everything in the form submit
    $('#search_form').submit((event) => {
        event.preventDefault();

        // Check if autocomplete dropdown is visible
        if ($('.ui-autocomplete').is(':visible')) {
            console.log("No selection with enter. Closing dropdown options and returning to main search method");
            $('input[name="title"]').autocomplete("close");
            return;
        }
        else {
            // If dropdown isn't visible, proceed with normal search
            const formData = $('#search_form').serializeArray()
                .filter(item => item.value.trim() !== '');
            window.location.href = 'movie-list.html?' + $.param(formData);
        }
    });
});