let currentParams = new URLSearchParams(window.location.search);

function fetchMovies() {
    const params = new URLSearchParams(currentParams);
    params.set('limit', $('#recordsPerPage').val());


    params.set('sort', $('#sortOrder').val());

    const filterParams = ['title', 'year', 'director', 'star', 'genre', 'startsWith'];
    filterParams.forEach(param => {
        const value = params.get(param);
        if (value) params.set(param, value);
    });

    currentParams = params;

    $.get(`api/movies?${params.toString()}`, (data) => {
        displayMovies(data);
        savePage();
    })
        .fail(error => console.error('Error:', error));
}

function displayMovies(data) {
    let html = `<table class="table">
        <thead>
            <tr>
                <th>Title</th>
                <th>Year</th>
                <th>Director</th>
                <th>Genres</th>
                <th>Stars</th>
                <th>Rating</th>
                <th>Action</th> 
            </tr>
        </thead>
        <tbody>`;

    data.movies.forEach(movie => {
        const genres = movie.genres.map(g =>
            `<a href="movie-list.html?genre=${g.genre_id}">${g.genre_name}</a>`
        ).join(', ');

        const stars = movie.stars.map(s =>
            `<a href="single-star.html?id=${s.star_id}">${s.star_name}</a>`
        ).join(', ');

        html += `<tr>
            <td><a href="single-movie.html?id=${movie.movie_id}">${movie.movie_title}</a></td>
            <td>${movie.movie_year}</td>
            <td>${movie.movie_director}</td>
            <td>${genres}</td>
            <td>${stars}</td>
            <td>${isNaN(parseFloat(movie.movie_rating)) ? "N/A" : parseFloat(movie.movie_rating).toFixed(1)}</td>
            <td>
                <button class="btn btn-primary add-to-cart" data-movie-id="${movie.movie_id}">
                    Add to Cart
                </button>
            </td>
        </tr>`;
    });
    html += '</tbody></table>';

    $('#movie_table').html(html);
    displayPagination(data.totalRecords);

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

    window.history.replaceState(
        {},
        '',
        `${window.location.pathname}?${currentParams.toString()}`
    );
}

function displayPagination(total) {
    const limit = parseInt($('#recordsPerPage').val());
    const totalPages = Math.ceil(total / limit);
    const currentPage = parseInt(currentParams.get('page')) || 1;

    let html = '<ul class="pagination justify-content-center">';

    if (currentPage > 1) {
        html += `<li class="page-item">
            <a class="page-link" data-page="${currentPage-1}">Previous</a>
        </li>`;
    }

    for (let i = Math.max(1, currentPage-2); i <= Math.min(totalPages, currentPage+2); i++) {
        html += `<li class="page-item ${i === currentPage ? 'active' : ''}">
            <a class="page-link" data-page="${i}">${i}</a>
        </li>`;
    }

    if (currentPage < totalPages) {
        html += `<li class="page-item">
            <a class="page-link" data-page="${currentPage+1}">Next</a>
        </li>`;
    }

    $('#pagination').html(html);
}

$(document).ready(() => {
    const backToTop = $('#backToTop');

    $(window).scroll(function() {
        if ($(window).scrollTop() > 200) {
            backToTop.addClass('visible');
        } else {
            backToTop.removeClass('visible');
        }
    });

    backToTop.click(function(e) {
        e.preventDefault();
        $('html, body').animate({scrollTop: 0}, 300);
    });
    $('#recordsPerPage').val(currentParams.get('limit') || '25');
    $('#sortOrder').val(currentParams.get('sort') || 'title,ASC,rating,ASC');

    if (!currentParams.has('page')) {
        currentParams.set('page', '1');
    }

    $('#recordsPerPage, #sortOrder').change(() => {
        currentParams.set('page', '1');
        fetchMovies();
    });

    $('#pagination').on('click', '.page-link', function(e) {
        e.preventDefault();
        currentParams.set('page', $(this).data('page'));
        fetchMovies();
    });

    fetchMovies();
});

