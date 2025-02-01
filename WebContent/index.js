function loadGenres() {
    $.get('api/genres', (data) => {
        let genresHTML = '';
        data.forEach(genre => {
            genresHTML += `<a href="movie-list.html?genre=${genre.id}">${genre.name}</a>`;
        });
        $('#browse_genres').html(genresHTML);
    });
}

function initTitleBrowse() {
    const chars = '*0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('');
    let titlesHTML = '';
    chars.forEach(char => {
        titlesHTML += `<a href="movie-list.html?startsWith=${char}">${char}</a>`;
    });
    $('#browse_titles').html(titlesHTML);
}

$('#search_form').submit((event) => {
    event.preventDefault();
    const formData = $('#search_form').serializeArray()
        .filter(item => item.value.trim() !== '');
    window.location.href = 'movie-list.html?' + $.param(formData);
});

$(document).ready(() => {
    loadGenres();
    initTitleBrowse();
});