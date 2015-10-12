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
var iterations = 0;
var errorCount = 0;

var setIntervalGetFeeds;
var setIntervalClock;

//Retrieve the update interval value
var updateInterval = localStorage.getItem("updateInterval");
updateInterval = updateInterval * 1000;

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

                //Gather the GTFS feed id from the gtfs-rt-feed
                loadGtfsErrors(data["gtfsId"]);
            }
        });
    }
}

function loadGtfsErrors(gtfsFeedId) {
    $.get("http://localhost:8080/api/gtfs-feed/" + gtfsFeedId + "/errors", function (data) {
        console.log("Errors in GTFS: " + data);
        for(var errorItem in data) {
            errorItem = data[errorItem]
            alert (errorItem);

            var errorRow="";
            errorRow += "<tr>";
            errorRow += "<td>"+ errorItem.errorId +"<\/td>";
            errorRow += "<td>"+ errorItem.errorCount +"<\/td>";
            errorRow += "<td>"+ errorItem.errorDesc +"<\/td>";
            errorRow += "<\/tr>";

            $("#gtfs-feed-tbody").append(errorRow);
        }
    });
}

function refresh(id) {
    $.get("http://localhost:8080/api/gtfs-rt-feed/" + id).done(function (data) {
        $("#iterations").text(++iterations);
        updateTables(id, data);
        errorCount = errorCount + data[0]["errorCount"];
        $("#gtfs-rt-error").text(errorCount);
    });
}

function initializeInterface(gtfsRtFeeds) {
    //var wrapper  = {gtfsRtFeeds: gtfsRtFeeds};
    var monitorTemplateScript = $("#feed-monitor-template").html();
    var monitorTemplate = Handlebars.compile(monitorTemplateScript);
    var compiledHtml = monitorTemplate(gtfsRtFeeds);
    $('.monitor-placeholder').append(compiledHtml);
}

function updateTables(index, data) {

    var monitorTemplateScript = $("#feed-monitor-row-template").html();
    var monitorTemplate = Handlebars.compile(monitorTemplateScript);
    var compiledHtml = monitorTemplate(data);

    $("#monitor-table-" + index + "").html(compiledHtml);
}

//Calculate time for display
var start = new Date();

function getTimeElapsed() {
    var elapsed = new Date() - start;

    var seconds = Math.round(elapsed / 1000);
    var minutes = Math.round(seconds / 60);
    var hours = Math.round(minutes / 60);

    var sec = TrimSecondsMinutes(seconds);
    var min = TrimSecondsMinutes(minutes);

    function TrimSecondsMinutes(elapsed) {
        if (elapsed >= 60)
            return TrimSecondsMinutes(elapsed - 60);
        return elapsed;
    }

    $("#time-elapsed").text(hours + "h " + min + "m " + sec + "s");
}

//Call time elapsed to update the clock evey second
setIntervalClock = setInterval(getTimeElapsed, 1000);

function stopMonitor() {
    clearInterval(setIntervalClock);
    clearInterval(setIntervalGetFeeds);
}