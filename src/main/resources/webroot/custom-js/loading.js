//Generic function that given a name, retrieves get parameters from the URL
function getUrlParameter(sParam) {
    var sPageURL = window.location.search.substring(1);
    var sURLVariables = sPageURL.split('&');
    for (var i = 0; i < sURLVariables.length; i++) {
        var sParameterName = sURLVariables[i].split('=');
        if (sParameterName[0] == sParam) {
            return sParameterName[1];
        }
    }
}

//Fetches the list of get parameter which contain 'gtfs-realtime' feed URLs
function getRtUrlList() {
    var sPageURL = window.location.search.substring(1);
    var sURLVariables = sPageURL.split('&');
    var parameterList = [];
    var urlList = new Object();
    urlList.gtfsFeeds = [];

    //Fetches all the get parameters into parameterList array
    for (var i = 0; i < sURLVariables.length; i++) {
        var getParameter = sURLVariables[i].split('=');
        parameterList.push(getParameter);
    }

    //Takes all the get parameters and selects those with 'gtfsrt' in them
    for (var i = 0; i < parameterList.length; i++) {
        if (parameterList[i][0].indexOf("gtfsrt") != -1 && parameterList[i][1] != "") {
            var gtfsFeed = new Object();

            //Extract the number associated with the rt feed and save it in rtParam objects index field
            //ex - gtfsrt-feed-1 would return the index of 1
            gtfsFeed.index = parseInt(parameterList[i][0].substr(parameterList[i][0].length - 1));
            gtfsFeed.url = parameterList[i][1];

            urlList.gtfsFeeds.push(gtfsFeed);
        }
    }

    return urlList;
}

function generateRealtimeProgressBar(urlList) {
    var progressTemplateScript = $("#gtfsrt-progress-template").html();
    var progressTemplate = Handlebars.compile(progressTemplateScript);
    var compiledHtml = progressTemplate(urlList);
    $('.progress-placeholder').html(compiledHtml);
}

//Check if the urls provided are valid gtfs-rt feeds
function checkUrlParamter(gtfsRtParameter) {
    var paramVal = getUrlParameter(paramName);
    var progressID = "#" + paramName + "-progress";
    var requestFeedType;

    if (paramName === "gtfsrt-vehicles") requestFeedType = UPDATE_FEED;
    else if (paramName === "gtfsrt-alerts") requestFeedType = ALERT_FEED;
    else if (paramName === "gtfsrt-updates") requestFeedType = TRIP_FEED;

    if (paramVal === null) {
        alert("parameter is null/ undefined?")
        //TODO: no URL for the given feed entered. Show status
        //paramName + "-progress" to white.
    } else if (paramVal === "") {
        alert("empty string")
        //TODO: no URL for the given feed entered. Show status
        // paramName + "-progress" to white.
    } else {
        $.get("http://localhost:8080/validate", {gtfsrturl: paramVal})
            .done(function (data) {
                if (data["feedStatus"] === requestFeedType) {
                    $(progressID).removeClass("progress-striped active");
                    $(progressID + " .progress-bar").addClass("progress-bar-success");

                    $(progressID).prev().find(".status").text("(Feed Valid)");

                } else if (data["feedStatus"] !== INVALID_FEED) {
                    //Incorrect feed type given
                    $(progressID).removeClass("progress-striped active");
                    $(progressID + " .progress-bar").addClass("progress-bar-warning");

                    $(progressID).prev().find(".status").text("(Invalid feed type)");
                } else {
                    //the URL provided was invalid for the feed.
                    //A warning will be displayed for the given feed
                    $(progressID).removeClass("progress-striped active");
                    $(progressID + " .progress-bar").addClass("progress-bar-danger");

                    $(progressID).prev().find(".status").text("(Invalid URL)");
                }
            });
    }
}

function checkGtfsRtFeeds(gtfsrtUrlList) {
    //alert(JSON.stringify(gtfsrtUrlList));
    for (var i = 0; i < gtfsrtUrlList.gtfsFeeds.length; i++){
        var currentURL = gtfsrtUrlList.gtfsFeeds[i].url;

        //TODO: async call to check the url change status when the result is returned.
        $.get("http://localhost:8080/validate", {gtfsrturl: paramVal})
            .done(function (data) {

                //data should be only pass or failed


                if (data["feedStatus"] === requestFeedType) {
                    $(progressID).removeClass("progress-striped active");
                    $(progressID + " .progress-bar").addClass("progress-bar-success");

                    $(progressID).prev().find(".status").text("(Feed Valid)");

                } else if (data["feedStatus"] !== INVALID_FEED) {
                    //Incorrect feed type given
                    $(progressID).removeClass("progress-striped active");
                    $(progressID + " .progress-bar").addClass("progress-bar-warning");

                    $(progressID).prev().find(".status").text("(Invalid feed type)");
                } else {
                    //the URL provided was invalid for the feed.
                    //A warning will be displayed for the given feed
                    $(progressID).removeClass("progress-striped active");
                    $(progressID + " .progress-bar").addClass("progress-bar-danger");

                    $(progressID).prev().find(".status").text("(Invalid URL)");
                }
            });
    }
}

//Download the provided GTFS feed
function downloadGTFSFeed() {
    var paramVal = getUrlParameter("gtfs");
    var progressID = "#gtfs-progress";

    if (paramVal === null) {
        alert("parameter is null/ undefined?")
        //TODO: no URL for the given feed entered. Show status
        //paramName + "-progress" to white.
    } else if (paramVal === "") {
        alert("empty string")
        //TODO: no URL for the given feed entered. Show status
        // paramName + "-progress" to white.
    } else {
        $.get("http://localhost:8080/downloadgtfs", {gtfsurl: paramVal})
            .done(function (data) {
                if (data["feedStatus"] === 1) {
                    $(progressID).removeClass("progress-striped active");
                    $(progressID + " .progress-bar").addClass("progress-bar-success");

                    $(progressID).prev().find(".status").text("(Download Successful)");
                }
            });
    }
}

var gtfsrtUrlList = getRtUrlList();

generateRealtimeProgressBar(gtfsrtUrlList);
checkGtfsRtFeeds(gtfsrtUrlList);

for (var i = 0; i < RTUrlList.length; i++) {
    checkUrlParamter(RTUrlList[i]);
}

downloadGTFSFeed();

//TODO: check if any feed has failed
$("#btn-continue").removeAttr('disabled');

function startMonitoring() {
    var path = "monitoring.html";
    var parameters = {alert: 'http://test', vehicle: 'http://test2'};

    var form = $('<form></form>');

    form.attr("method", "post");
    form.attr("action", path);

    $.each(parameters, function (key, value) {
        var field = $('<input></input>');

        field.attr("type", "hidden");
        field.attr("name", key);
        field.attr("value", value);

        form.append(field);
    });

    // The form needs to be a part of the document in
    // order for us to be able to submit it.
    $(document.body).append(form);
    form.submit();
}