/*
 * Copyright (C) 2015 Nipuna Gunathilake.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    var urlList = {};
    urlList.gtfsFeeds = [];

    //Fetches all the get parameters into parameterList array
    for (var i = 0; i < sURLVariables.length; i++) {
        var getParameter = sURLVariables[i].split('=');
        parameterList.push(getParameter);
    }

    //Takes all the get parameters and selects those with 'gtfsrt' in them
    for (var i = 0; i < parameterList.length; i++) {
        if (parameterList[i][0].indexOf("gtfsrt") != -1 && parameterList[i][1] != "") {
            var gtfsFeed = {};

            //Extract the number associated with the rt feed and save it in rtParam objects index field
            //ex - gtfsrt-feed-1 would return the index of 1
            gtfsFeed.index = parseInt(parameterList[i][0].substr(parameterList[i][0].length - 1));

            var paramUrl = parameterList[i][1]
            paramUrl = decodeURIComponent(decodeURIComponent((paramUrl)));

            gtfsFeed.url = paramUrl;

            urlList.gtfsFeeds.push(gtfsFeed);
        }
    }

    return urlList;
}

//Generates the progress bars for the list of RealTime Feeds provided
function generateRealtimeProgressBar(urlList) {
    var progressTemplateScript = $("#gtfsrt-progress-template").html();
    var progressTemplate = Handlebars.compile(progressTemplateScript);
    var compiledHtml = progressTemplate(urlList);
    $('.progress-placeholder').html(compiledHtml);
}

var validUrlList = {};
validUrlList.gtfsFeeds = [];

//Check if the urls provided are valid gtfs-rt feeds
function checkGtfsRtFeeds(gtfsrtUrlList) {
    for (var i = 0; i < gtfsrtUrlList.gtfsFeeds.length; i++) {
        var currentURL = gtfsrtUrlList.gtfsFeeds[i].url;
        var currentIndex = gtfsrtUrlList.gtfsFeeds[i].index;

        (function (url, index) {
            $.get("http://localhost:8080/validate", {gtfsrturl: currentURL})
                .done(function (data) {
                    var progressID = "#gtfsrt-progress-" + index;
                    if (data["feedStatus"] === 1) {
                        $(progressID).removeClass("progress-striped active");
                        $(progressID + " .progress-bar").addClass("progress-bar-success");

                        $(progressID).prev().find(".status").text("(Feed Valid)");

                        var gtfsFeed = {index: index, url: url};
                        validUrlList.gtfsFeeds.push(gtfsFeed);

                        validGtfsRT = true;
                        checkStatus();
                    } else if (data["feedStatus"] === 0) {
                        //Incorrect feed type given
                        $(progressID).removeClass("progress-striped active");
                        $(progressID + " .progress-bar").addClass("progress-bar-danger");

                        $(progressID).prev().find(".status").text("(Invalid type)");
                    }
                });
        }(currentURL, currentIndex));
    }
}

var validGtfs = false;
var validGtfsRT = false;

//Download the provided GTFS feed
function downloadGTFSFeed() {
    var paramVal = getUrlParameter("gtfs");
    var progressID = "#gtfs-progress";

    if (paramVal === null) {
        alert("parameter is null/ undefined?");
        //TODO: no URL for the given feed entered. Show status
        //paramName + "-progress" to white.
    } else if (paramVal === "") {
        alert("empty string");
        //TODO: no URL for the given feed entered. Show status
        // paramName + "-progress" to white.
    } else {
        $.get("http://localhost:8080/downloadgtfs", {gtfsurl: paramVal})
            .done(function (data) {
                if (data["feedStatus"] === 1) {
                    $(progressID).removeClass("progress-striped active");
                    $(progressID + " .progress-bar").addClass("progress-bar-success");

                    $(progressID).prev().find(".status").text("(Download Successful)");

                    validGtfs = true;
                    checkStatus();
                }
            });
    }
}

function checkStatus(){
    localStorage.setItem("gtfsRtFeeds", JSON.stringify(validUrlList.gtfsFeeds));

    if(validGtfs && validGtfsRT) {
        $("#btn-continue").removeAttr('disabled');
    }
}

var gtfsrtUrlList = getRtUrlList();
generateRealtimeProgressBar(gtfsrtUrlList);
checkGtfsRtFeeds(gtfsrtUrlList);
downloadGTFSFeed();

function startMonitoring() {
    var path = "http://localhost:8080/monitoring.html";

    var parameters = {};

    //Generate parameters from a for loop
    for (var i = 0; i < validUrlList.gtfsFeeds.length; i++) {
        var currentURL = validUrlList.gtfsFeeds[i].url;
        var currentIndex = validUrlList.gtfsFeeds[i].index;
        parameters['gtfsrt' + currentIndex] = currentURL;
    }

    var form = $('<form></form>');

    form.attr("method", "post");
    form.attr("action", path);

    $.each(parameters, function (key, value) {
        var field = $('<input/>');

        field.attr("type", "hidden");
        field.attr("name", key);
        field.attr("value", value);

        form.append(field);
    });

    $(document.body).append(form);
    form.submit();
}