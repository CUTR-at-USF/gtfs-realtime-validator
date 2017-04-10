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


//Retrieve the validated urls (saved to the local storage in the loading.js file) from the localStorage
var urls = localStorage.getItem("gtfsRtFeeds");
var gtfsRtFeeds = JSON.parse(urls);
var totalRequests = 0;
var totalResponses = 0;
var requests = [];
var responses = [];

var setIntervalGetFeeds;
var setIntervalClock;

//Retrieve the update interval value
var updateInterval = localStorage.getItem("updateInterval");
updateInterval = updateInterval * 1000;

var hideErrors = [];

var toggleDataOn = '<input type="checkbox" checked data-toggle="toggle" data-onstyle="success"/>';
var toggleDataOff = '<input type="checkbox" data-toggle="toggle" data-onstyle="success"/>';

//PUT request to start monitoring of the given gtfsRtFeed ID /api/gtfs-rt-feed/{id}/monitor
for (var gtfsRtFeed in gtfsRtFeeds) {
    if (gtfsRtFeeds.hasOwnProperty(gtfsRtFeed)) {
        $.ajax({
            url: "http://localhost:8080/api/gtfs-rt-feed/" + gtfsRtFeeds[gtfsRtFeed]["feedId"] + "/monitor",
            type: 'PUT',
            success: function (data) {
                initializeInterface(data);
                refresh(data["gtfsRtId"]);

                setIntervalGetFeeds = setInterval(function () {
                    refresh(data["gtfsRtId"])
                }, updateInterval);

                // Get gtfs error count
                loadGtfsErrorCount(data["gtfsFeedModel"]["feedId"]);
            }
        });
    }
}

function loadGtfsErrorCount(gtfsFeedId) {
    $.get("http://localhost:8080/api/gtfs-feed/" + gtfsFeedId + "/errorCount").done(function (data)  {
        $("#gtfs-error").text(data["errorCount"]);

        var linkToReport = '<a href = ' + localStorage.getItem("reportURL") + localStorage.getItem("gtfsFileName") + '_out.json target="_blank">' + data["errorCount"] + ' error(s)/warning(s)</a>';
        $(".GTFS-report-link").html(linkToReport);
    });
}

function refresh(id) {

    $.get("http://localhost:8080/api/gtfs-rt-feed/" + id + "/feedIterations").done(function (data) {
        updateRequestData(id, data);
    });

    $.get("http://localhost:8080/api/gtfs-rt-feed/" + id + "/uniqueResponses").done(function (data) {
        updateUniqueFeedResponseData(id, data);
    })

    $.get("http://localhost:8080/api/gtfs-rt-feed/" + id + "/summary").done(function (data) {
        updateSummaryTables(id, data);
    });

    $.get("http://localhost:8080/api/gtfs-rt-feed/" + id + "/log/" + hideErrors[id]).done(function (data) {
        updateLogTables(id, data);
    });
}

function initializeInterface(gtfsRtFeeds) {
    //var wrapper  = {gtfsRtFeeds: gtfsRtFeeds};
    hideErrors[gtfsRtFeeds["gtfsRtId"]] = [];
    var monitorTemplateScript = $("#feed-monitor-template").html();
    var monitorTemplate = Handlebars.compile(monitorTemplateScript);
    var compiledHtml = monitorTemplate(gtfsRtFeeds);
    $('.monitor-placeholder').append(compiledHtml);
}

function updateSummaryTables(index, data) {

    var monitorTemplateScript = $("#feed-monitor-summary-row-template").html();
    var monitorTemplate = Handlebars.compile(monitorTemplateScript);
    var compiledHtml = monitorTemplate(data);
    var summaryTable = $("#monitor-table-summary-" + index + "").html(compiledHtml);

    handleToggledData(index, data, summaryTable);
}

function handleToggledData(index, data, summaryTable) {
    var i = 0, j = 0;
    // Add a toggle for each row and initialize based on toggle click history maintained in hideErrors
    summaryTable.find("span[class=toggleHolder"+index+"]").each(function(){
        if(hideErrors.hasOwnProperty(index)) {
            var onOrOff = (hideErrors[index].indexOf(data[i]["id"]) == -1) ? 'on' : 'off'
            if(onOrOff == 'off') {
                $("#toggleId"+index+data[i++]["id"]+"").html(toggleDataOff);
            }
            else{
                $("#toggleId"+index+data[i++]["id"]+"").html(toggleDataOn);
            }
        }
    });

    summaryTable.find('input[type=checkbox][data-toggle=toggle]').each(function(){
        var errorId = data[j++]["id"];
        $(this).bootstrapToggle(); // Initializing toggle
        $(this).change(function() {
            showOrHideError(index, errorId); // Toggle change event handler
        });
    });
}

function updateLogTables(index, data) {

    var monitorTemplateScript = $("#feed-monitor-log-row-template").html();
    var monitorTemplate = Handlebars.compile(monitorTemplateScript);
    var compiledHtml = monitorTemplate(data);

    $("#monitor-table-log-" + index + "").html(compiledHtml);
}

function updateRequestData(id, data) {
    requests[id] = data["iterationCount"];
    $("#requests-" + id + "").text(requests[id]);
    for(var id in requests) {
        if(requests.hasOwnProperty(id)) {
            totalRequests += requests[id];
        }
    }
    $("#totalRequests").text(totalRequests);
    totalRequests = 0; // Again the new count is calculated in the next refresh
}

function updateUniqueFeedResponseData(id, data) {
    responses[id] = data["uniqueFeedCount"];
    $("#responses-" + id + "").text(responses[id]);
    for(var id in responses) {
        if(responses.hasOwnProperty(id)) {
            totalResponses += responses[id];
        }
    }
    $("#totalResponses").text(totalResponses);
    totalResponses = 0;
}

//Calculate time for display
var start = new Date();

function getTimeElapsed() {
    var elapsed = new Date() - start;

    var seconds = Math.floor(elapsed / 1000);
    var minutes = Math.floor(seconds / 60);
    var hours = Math.floor(minutes / 60);

    var sec = TrimSecondsMinutes(seconds);

    function TrimSecondsMinutes(elapsed) {
        if (elapsed >= 60)
            return TrimSecondsMinutes(elapsed - 60);
        return elapsed;
    }

    $("#time-elapsed").text(hours + "h " + minutes + "m " + sec + "s");
}

//Call time elapsed to update the clock evey second
setIntervalClock = setInterval(getTimeElapsed, 1000);

function stopMonitor() {
    clearInterval(setIntervalClock);
    clearInterval(setIntervalGetFeeds);
}

function showOrHideError(gtfsRtId, errorId) {
    if(hideErrors[gtfsRtId].indexOf(errorId) == -1) {
        hideErrors[gtfsRtId].push(errorId);
    }
    else {
        hideErrors[gtfsRtId].splice(hideErrors[gtfsRtId].indexOf(errorId), 1);
    }

    // Request data based on 'hideErrors'
    $.get("http://localhost:8080/api/gtfs-rt-feed/" + gtfsRtId + "/log/" + hideErrors[gtfsRtId]).done(function (data) {

        // Store current position for later use
        var scrollPosition = document.body.scrollTop;

        updateLogTables(gtfsRtId, data);

        $('html, body').animate({scrollTop: scrollPosition}, 1);
    });
}