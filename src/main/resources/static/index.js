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
        body: JSON.stringify(query)
    })
        .then(response => {
            return response.json();
        })
        .then(data => {
        // Handle the response data
        if ("success" in data){console.log("Error while searching."); return;}
        $("tbody").html("");
        for (rank in data){
            if (rank == 50) break;
            $("tbody").append(`
            <tr key=${rank}>
            <td class="rank">${parseInt(rank)+1}</td>
            <td class="score">${data[rank].score}</td>
            <td class="page">${data[rank].title}</td>
            </tr>
        `);
        }
    })
        .catch(error => {
        // Handle any error that occurred during the request
        console.error(error);
    });
    console.log(query);
});

$("#search-bar-header").on("submit", (e) => {
    e.preventDefault();
    const query = $("#search-query-header").val().trim();
    fetch('/search', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(query)
    })
        .then(response => {
        console.log(response);
        return response.json();
    })
        .then(data => {
        // Handle the response data
        if ("success" in data){console.log("Error while searching."); return;}
        $("tbody").html("");
        for (rank in data){
            if (rank == 50) break;
            $("tbody").append(`
            <tr key=${rank}>
            <td class="rank">${parseInt(rank)+1}</td>
            <td class="score">${data[rank].score}</td>
            <td class="page">${data[rank].title}</td>
            </tr>
        `);
        }
    })
        .catch(error => {
        // Handle any error that occurred during the request
        console.error(error);
    });
});