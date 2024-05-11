
let keywords;

$(document).ready(function() {

    $('input[type="text"]').on('focus', function() {
        $(this).select();
    });

    getKeywords().then(() => {
        setKeywordsHeader();
    });
});

function setKeywordsHeader(){
    for(let startChar in keywords){
        $("#keywords-header").append(`<button id="${startChar}">${startChar}</button>`);
        $(`#${startChar}`).on('click', function() {
            $("#keywords-header button").removeClass("active");
            $(`#${startChar}`).addClass("active");
            showKeywords(startChar);
        });
    }
}

function showKeywords(startChar){
    $('#keywords').html("");
    for(const keyword of keywords[startChar]){
        $('#keywords').append(`<button class="keyword-item">${keyword}</button>`);
    }
    $(`.keyword-item`).on('click', function() {
        addIndex($(this).html());
    });

}

function addIndex(index){
    if ($("#index").children().length == 5){
        removeIndex($("#index").children().first());
    }
    if ($(`#index-${index}`).length) return;
    $('#index').append(`
    <button id="index-${index}" class="index-item"><span>${index}</span><img height="17px" src="cross.svg"></button>
    `);
    $(`#index-${index}`).on('click', e => removeIndex($(`#index-${index}`)));
}

function removeIndex(target){
    target.off('click');
    target.remove();
}

async function getKeywords(){
    keywords = await fetch('/keywords')
        .then(response => { return response.json();})
        .then(data => { return data })
        .catch(error => { console.error(error) });
}

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
    if (query.length === 0 && $("#index").children().length == 0) return;
    if (query.length !== 0) HistoryQueue.addHistory(query);
    if ($("#index").children().length > 0){
        var values = $('#index button.index-item').map(function() {
            return $(this).find('span').text();
        }).get();
        query += " " + values.join(' ');
    }
    query.trim();
    $("#results-list").show();
    $("#keywords-list").hide();
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
    <div class="page-keywords">Keywords : ${pageInfo.keywords}</div>
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



$(".title-header img").on("click", (e) => {
    $(".header").fadeOut(300);
    $("#results").fadeOut(300);
    setTimeout(() => {
        $(".landing").fadeIn(300);
    }, 300);

});

$("#history-list").on("click", ".history-item", (e) => {
    const query = $(e.target).attr("value").trim();
    $('#search-query-header').val(query);
    search(query).then(result => {
        if (!result) return;
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

$("#viewIndex").on("click", (e) => {
    $("#viewIndex").hide();
    $("#results-list").hide();
    $("#keywords-list").show();
    $("#hideIndex").show();
});

$("#hideIndex").on("click", (e) => {
    $("#hideIndex").hide()
    $("#results-list").show();
    $("#keywords-list").hide();
    $("#viewIndex").show();

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
        if (!result) return;
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
});

$("#search-bar-header").on("submit", (e) => {
    e.preventDefault();
    const query = $("#search-query-header").val().trim();
    search(query).then(result => {
        if (!result) return;
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

