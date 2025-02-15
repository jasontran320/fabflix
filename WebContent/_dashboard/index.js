$(document).ready(function() {
    const backToTop = $('#backToTop');

    // Back to top button visibility
    $(window).scroll(function() {
        if ($(window).scrollTop() > 200) {
            backToTop.addClass('visible');
        } else {
            backToTop.removeClass('visible');
        }
    });

    // Back to top button click handler
    backToTop.click(function(e) {
        e.preventDefault();
        $('html, body').animate({scrollTop: 0}, 300);
    });

    // Fetch metadata
    $.ajax("../api/dashboard/metadata", {
        method: "GET",
        success: function(response) {
            let html = "";
            response.tables.forEach(table => {
                // In dashboard.js, modify the table-card-body section:
                html += `
                    <div class="table-card">
                        <div class="table-card-header" style="background-color: #007bff; color: white;">
                            ${table.name}
                        </div>
                        <div class="table-card-body">
                            <ul class="column-list">
                                <li class="column-item" style="font-weight: bold;">
                                    <span class="column-name">Attribute</span>
                                    <span class="column-type">Type</span>
                                </li>
                                ${table.columns.map(column => `
                                    <li class="column-item">
                                        <span class="column-name">${column.name}</span>
                                        <span class="column-type">${column.type}</span>
                                    </li>
                                `).join('')}
                            </ul>
                        </div>
                    </div>
                `;
            });
            $("#metadata_content").html(html);
        },
        error: function(xhr) {
            $("#metadata_content").html(`
                <div class="alert alert-danger" role="alert">
                    Error loading metadata: ${xhr.responseJSON?.message || "Unknown error occurred"}
                </div>
            `);
        }
    });
});