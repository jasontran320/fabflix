// Save the current page
function savePage() {
    const currentUrl = window.location.href;
    fetch('../api/save-page?page=' + encodeURIComponent(currentUrl), {
        method: 'POST'
    }).then(() => {
        const returnLinks = document.querySelectorAll('.return-link');
        returnLinks.forEach(link => {
            link.style.display = 'inline';
            link.href = currentUrl;
        });
    });
}

// Load the navbar and manage return links
fetch('navbar.html')
    .then(response => response.text())
    .then(data => {
        document.getElementById('navbar-placeholder').innerHTML = data;
        document.body.style.paddingTop = '80px';

        // Check if user is employee
        fetch('../api/check-employee')
            .then(response => response.json())
            .then(data => {
                if (data.isEmployee) {
                    document.getElementById('metadata-link').style.display = 'inline';
                    document.getElementById('add-star-link').style.display = 'inline';
                    document.getElementById('add-movie-link').style.display = 'inline';
                }
            });



        fetch('../api/save-page')
            .then(response => response.text())
            .then(savedUrl => {
                const returnLinks = document.querySelectorAll('.return-link');
                returnLinks.forEach(link => {
                    link.style.display = savedUrl ? 'inline' : 'none';
                    if (savedUrl) link.href = savedUrl;
                });
            });
    });

