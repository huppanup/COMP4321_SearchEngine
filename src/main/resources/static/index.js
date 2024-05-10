$("#search-bar").on("submit", (e) => {
    // Do not submit the form
    e.preventDefault();
    $(".landing").fadeOut(300);
    setTimeout(() => {
        $(".header").fadeIn(300);
        $("#results").fadeIn(300);
    }, 300);

    // Get the input fields
    const query = $("#search-query").val().trim();
    fetch('/search', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ query })
    })
        .then(response => {
            console.log(response);
            return response.text();
        })
        .then(data => {
        // Handle the response data
        console.log(data);
        $("#results").text(data);
    })
        .catch(error => {
        // Handle any error that occurred during the request
        console.error(error);
    });
    console.log(query);
});

$("#search-bar-header").on("submit", (e) => {});