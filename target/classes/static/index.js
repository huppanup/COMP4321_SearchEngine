
const HistoryQueue = (function(){
    const max = 20;

    const addHistory = function(query){
        const history = $('#history-list');
        history.prepend(`
            <div class="history-item" value="${query}">${query}</div>
            <div class="history-separator" />
        `);
        if (history.children().last().hasClass('history-separator')) history.children().last().remove();

        if (history.children().length > max-1) {
            history.children().last().remove();
            history.children().last().remove();
        }
    }

    return { addHistory }
})();

async function search(query){
    HistoryQueue.addHistory(query);
    const result = await fetch('/search', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(query)
    }).then(response => { return response.json();})
    .then(data => { return data })
    .catch(error => { console.error(error) });

    return result
}

function parsePageInfo(pageInfo){
    let parents = pageInfo.parent.split(", ");
    let parentHTML = ""
    for (var parent of parents){
        parentHTML += `<a style="display:block" href=${parent}>${parent}</a>`;
    }

    let childs = pageInfo.child.split(", ");
    let childHTML = ""
    for (var child of childs){
        childHTML += `<a style="display:block" href=${child}>${child}</a>`;
    }

    let pageString = `
    <a class="page-title" href=${pageInfo.url}>${pageInfo.title}</a>
    <a class="page-url" href=${pageInfo.url}>${pageInfo.url}</a>
    <div class="page-meta">
        <span>Last Modified : ${pageInfo.mod_date.substring(0, 10)}, </span>
        <span>Page Size : ${pageInfo.size}</span>
    </div>
    <div class="page-keywords">${pageInfo.keywords}</div>
    <div class="page-parent">
        <div class="page-title-sub">Parent Links</div>
        <div class="page-links">${parentHTML}</div>
    </div>
    <div class="page-child">
        <div class="page-title-sub">Child Links</div>
        <div class="page-links">${childHTML}</div>
    </div>
    `;
    return pageString;
}

$(document).ready(function() {
    $('input[type="text"]').on('focus', function() {
        $(this).select();
    });
});

$("#history-list").on("click", ".history-item", (e) => {
    const query = $(e.target).attr("value").trim();
    console.log("Searching from history");
    $('#search-query-header').val(query);
    search(query).then(result => {
        if ("success" in result){console.log("Error while searching."); return;}
        $("tbody").html("");
        for (rank in result){
            if (rank == 50) break;
            $("tbody").append(`
            <tr key=${rank}>
            <td class="rank">${parseInt(rank)+1}</td>
            <td class="score">${result[rank].score}</td>
            <td class="page">${parsePageInfo(result[rank])}</td>
            </tr>
        `   );
        }
    }).catch(error => { console.error(error) });
})

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

    search(query).then(result =>{
        if ("success" in result){console.log("Error while searching."); return;}
        $("tbody").html("");
        for (rank in result){
            if (rank == 50) break;
            $("tbody").append(`
            <tr key=${rank}>
            <td class="rank">${parseInt(rank)+1}</td>
            <td class="score">${result[rank].score}</td>
            <td class="page">${parsePageInfo(result[rank])}</td>
            </tr>
        `   );
        }
    });
    console.log(query);
});

$("#search-bar-header").on("submit", (e) => {
    e.preventDefault();
    const query = $("#search-query-header").val().trim();
    search(query).then(result => {
        if ("success" in result){console.log("Error while searching."); return;}
        $("tbody").html("");
        for (rank in result){
            if (rank == 50) break;
            $("tbody").append(`
            <tr key=${rank}>
            <td class="rank">${parseInt(rank)+1}</td>
            <td class="score">${result[rank].score}</td>
            <td class="page">${parsePageInfo(result[rank])}</td>
            </tr>
        `   );
        }
    }).catch(error => { console.error(error) });
});

