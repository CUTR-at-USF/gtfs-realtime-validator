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

var validGtfs = false;
var validGtfsRT = false;
var validUrlList = {};
validUrlList.gtfsFeeds = [];
localStorage.setItem("reportURL", "http://localhost:8080/gtfs-rt-validator-webapp/gtfs-validator-master/gtfs-validator-webapp/index.html?report=http://localhost:8080/");

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
    for (var j = 0; j < sURLVariables.length; j++) {
        var getParameter = sURLVariables[j].split('=');
        parameterList.push(getParameter);
    }

    //Takes all the get parameters and selects those with 'gtfsrt' in them
    for (var i = 0; i < parameterList.length; i++) {
        if (parameterList[i][0].indexOf("gtfsrt") != -1 && parameterList[i][1] != "") {
            var gtfsFeed = {};

            //Extract the number associated with the rt feed and save it in rtParam objects index field
            //ex - gtfsrt-feed-1 would return the index of 1
            gtfsFeed.index = parseInt(parameterList[i][0].substr(parameterList[i][0].length - 1));

            var paramUrl = parameterList[i][1];
            paramUrl = decodeURIComponent(decodeURIComponent((paramUrl)));

            gtfsFeed.url = paramUrl;

            urlList.gtfsFeeds.push(gtfsFeed);
        }
    }
    return urlList;
}

//Check if the urls provided are valid gtfs-rt feeds
function monitorGtfsRtFeeds(gtfsrtUrlList, gtfsFeedId) {
    for (var i = 0; i < gtfsrtUrlList.gtfsFeeds.length; i++) {
        var currentURL = gtfsrtUrlList.gtfsFeeds[i].url;
        var currentIndex = gtfsrtUrlList.gtfsFeeds[i].index;

        (function (url, index) {

            var progressID = "#gtfsrt-progress-" + index;

            //POST request to api/gtfs-rt to add
            function success(data){
                if (data["gtfsUrl"] != null) {

                    $(progressID).removeClass("progress-striped active");
                    $(progressID + " .progress-bar").addClass("progress-bar-success");

                    $(progressID).prev().find(".status").text("(Download Successful)");

                    //data["gtfsRtId"]

                    validGtfsRT = true;

                    var gtfsFeed = {};

                    gtfsFeed.url = url;
                    gtfsFeed.feedId = data["gtfsRtId"];

                    validUrlList.gtfsFeeds.push(gtfsFeed);

                    console.log(JSON.stringify(validUrlList.gtfsFeeds));
                    localStorage.setItem("gtfsRtFeeds", JSON.stringify(validUrlList.gtfsFeeds));

                    //gtfsRtfeeds can only be started with a valid id
                    checkStatus();
                }
            }

            function feedError(data) {
                alert(JSON.stringify(data));

                var responseJson = data["responseJSON"];
                var message = responseJson["message"];

                $(progressID).removeClass("progress-striped active");
                $(progressID + " .progress-bar").addClass("progress-bar-warning");

                $(progressID).prev().find(".status").text("("+ message +")");
            }

            var jsonData = {"gtfsUrl":url, "gtfsFeedModel":{"feedId": gtfsFeedId}};

            $.ajax({
                type: "POST",
                url: "http://localhost:8080/api/gtfs-rt-feed",
                headers: {
                    'Accept': '*/*',
                    'Content-Type': 'application/json'
                },
                data: JSON.stringify(jsonData),
                success: success,
                error: feedError,
                dataType: 'json'
            });

        }(currentURL, currentIndex));
    }
}

//Download the provided GTFS feed
function downloadGTFSFeed() {
    var paramVal = getUrlParameter("gtfs");
    paramVal = decodeURIComponent(paramVal);
    localStorage.setItem("gtfsFileName", paramVal.split('/').pop().split('.')[0]);

    var progressID = "#gtfs-progress";

    function feedErrorDisplay(message) {
        $(progressID).removeClass("progress-striped active");
        $(progressID + " .progress-bar").addClass("progress-bar-danger");
        $(progressID).prev().find(".status").text("("+ message +")");
        validGtfs = false;
    }

    if (paramVal === null) {
        //No URL for the given feed entered. Show status
        feedErrorDisplay("No GTFS URL Given");

    } else if (paramVal === "") {
        //No URL for the given feed entered. Show status
        feedErrorDisplay("No GTFS URL Given");
    } else {
        function feedSuccess(data){
            if (data["feedId"] != null) {
                $(progressID).removeClass("progress-striped active");
                $(progressID + " .progress-bar").addClass("progress-bar-success");
                $(progressID).prev().find(".status").text("(Download Successful)");
                validGtfs = true;

                //gtfsRtfeeds can only be started with a valid id
                monitorGtfsRtFeeds(gtfsrtUrlList, data["feedId"]);
                checkStatus();
                loadGTFSfeedReport();
            }else{
                feedErrorDisplay(data["title"]);
            }
        }

        function feedError(xhr,status,error){
            feedErrorDisplay(status);
        }

        $.ajax({
            type: "POST",
            url: "http://localhost:8080/api/gtfs-feed",
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            data: {gtfsurl: paramVal},
            success: feedSuccess,
            error: feedError,
            dataType: 'json'
        });
    }
}

//Generates the progress bars for the list of RealTime Feeds provided
function generateRealtimeProgressBar(urlList) {
    var progressTemplateScript = $("#gtfsrt-progress-template").html();
    var progressTemplate = Handlebars.compile(progressTemplateScript);
    var compiledHtml = progressTemplate(urlList);
    $('.progress-placeholder').html(compiledHtml);
}

function loadGTFSfeedReport() {
    
	$('.modalDialog').html('<div class="modal-content">\n\
    <a href="#close" title="Close" class="close">X</a></br>\n\
    <p>GTFS data has been validated. Follow the below link to view validation report.</p>\n\
    <a href = '+localStorage.getItem("reportURL")+localStorage.getItem("gtfsFileName")+'_out.json target="_blank">GTFS Validation Report</a> <br/><br/>\n\
    <p>You can always view the validation report by clicking <b>GTFS Validation Report</b> button </p></br>\n\
    <button type="button" class="button btn-primary">OK</button></br>\n\
    </div>');
    var modal = document.getElementById('modal');
    var span = document.getElementsByClassName("close")[0];
    span = document.getElementsByClassName("button")[0];
    modal.style.display = "block";
    span.onclick = function() {
    modal.style.display = "none";
    };
    window.onclick = function(event) {
        if (event.target === modal) {
            modal.style.display = "none";
        }
    };
}

function validationReport() {
    newWindow = window.open(localStorage.getItem("reportURL")+localStorage.getItem("gtfsFileName")+'_out.json', 'GTFS Validation Report');
    if(window.focus()) newWindow.focus();
    return false;
}


function checkStatus(){
    if(validGtfs && validGtfsRT) {
        $("#btn-continue").removeAttr('disabled');
        $("#btn-GTFSReport").removeAttr('disabled');
    }
}

var gtfsrtUrlList = getRtUrlList();
generateRealtimeProgressBar(gtfsrtUrlList);
downloadGTFSFeed();

//Set the update interval that will be used to update the GTFS-rt feed
localStorage.setItem("updateInterval", getUrlParameter("updateInterval"));

//Start monitoring gtfs feeds starts on click
function startMonitoring() {
    var path = "http://localhost:8080/monitoring.html";

    var parameters = {};

    //Generate parameters from a for loop
    for (var i = 0; i < validUrlList.gtfsFeeds.length; i++) {
        var currentURL = validUrlList.gtfsFeeds[i].url;
        var currentIndex = validUrlList.gtfsFeeds[i].feedId;
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