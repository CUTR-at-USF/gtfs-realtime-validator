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
var serverUpdateInterval = localStorage.getItem("updateInterval");
var updateInterval = (serverUpdateInterval / 2) * 1000;

var hideErrors = [];
var paginationLog = [];
var paginationSummary = [];

var toggleDataOn = '<input type="checkbox" checked data-toggle="toggle" data-onstyle="success"/>';
var toggleDataOff = '<input type="checkbox" data-toggle="toggle" data-onstyle="success"/>';

//PUT request to start monitoring of the given gtfsRtFeed ID /api/gtfs-rt-feed/{id}/monitor
for (var gtfsRtFeed in gtfsRtFeeds) {
    if (gtfsRtFeeds.hasOwnProperty(gtfsRtFeed)) {
        $.ajax({
            url: "http://localhost:8080/api/gtfs-rt-feed/" + gtfsRtFeeds[gtfsRtFeed]["feedId"] + "/" + serverUpdateInterval + "/monitor",
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

    $.get("http://localhost:8080/api/gtfs-rt-feed/" + id + "/errorCount").done(function (errorData) {
        updatePaginationSummaryData(id, errorData);
        updatePaginationLogData(id, errorData);

        $.get("http://localhost:8080/api/gtfs-rt-feed/" + id + "/summary/pagination/" + paginationSummary[id]["currentPage"] + "/" + paginationSummary[id]["rowsPerPage"]).done(function (summaryData) {
            updateSummaryTables(id, summaryData);
        });

        $.get("http://localhost:8080/api/gtfs-rt-feed/" + id + "/log/" + hideErrors[id] + "/pagination/" + paginationLog[id]["currentPage"] + "/" + paginationLog[id]["rowsPerPage"]).done(function (logData) {
            updateLogTables(id, logData);
        });
    });
}

function initializeInterface(gtfsRtFeeds) {
    var id = gtfsRtFeeds["gtfsRtId"];
    hideErrors[id] = [];
    // Initializing pagination variables.
    paginationLog[id] = [];
    paginationLog[id]["currentPage"] = 1;
    // Default number of rows per page.
    paginationLog[id]["rowsPerPage"] = 10;
    paginationSummary[id] = [];
    paginationSummary[id]["currentPage"] = 1;
    paginationSummary[id]["rowsPerPage"] = 10;

    var monitorTemplateScript = $("#feed-monitor-template").html();
    var monitorTemplate = Handlebars.compile(monitorTemplateScript);
    var compiledHtml = monitorTemplate(gtfsRtFeeds);
    $('.monitor-placeholder').append(compiledHtml);
}

function updateSummaryTables(index, data) {

    var monitorTemplateScript = $("#feed-monitor-summary-row-template").html();
    var monitorTemplate = Handlebars.compile(monitorTemplateScript);
    var compiledHtml = monitorTemplate(data);

    if (paginationSummary[index]["totalRows"] > 0) {
        $("#summary-pagination-" + index).bs_pagination({
            onChangePage: function (event, page) {
                paginationSummary[index]["userSelPage"] = page["currentPage"];
                paginationSummary[index]["currentPage"] = page["currentPage"];
                paginationSummary[index]["rowsPerPage"] = page["rowsPerPage"];
                paginationSummary[index]["totalPages"] = Math.ceil(paginationSummary[index]["totalRows"] / paginationSummary[index]["rowsPerPage"]);
                $.get("http://localhost:8080/api/gtfs-rt-feed/" + index + "/summary/pagination/" + paginationSummary[index]["currentPage"] + "/" + paginationSummary[index]["rowsPerPage"]).done(function (data) {
                    updateSummaryTables(index, data);
                });
            },
            rowsPerPage: paginationSummary[index]["rowsPerPage"],
            totalPages: paginationSummary[index]["totalPages"],
            totalRows: paginationSummary[index]["totalRows"],
            currentPage: paginationSummary[index]["currentPage"],
            // Maximum number of rows we can view in a page.
            maxRowsPerPage: 10
        });
    } else {
        $("#summary-pagination-" + index).empty();
    }

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

    if (paginationLog[index]["totalRows"] > 0) {
        $("#log-pagination-" + index).bs_pagination({
            onChangePage: function (event, page) {
                paginationLog[index]["userSelPage"] = page["currentPage"];
                paginationLog[index]["currentPage"] = page["currentPage"];
                paginationLog[index]["rowsPerPage"] = page["rowsPerPage"];
                paginationLog[index]["totalPages"] = Math.ceil(paginationLog[index]["totalRows"] / paginationLog[index]["rowsPerPage"]);
                $.get("http://localhost:8080/api/gtfs-rt-feed/" + index + "/log/" + hideErrors[index] + "/pagination/" + paginationLog[index]["currentPage"] + "/" + paginationLog[index]["rowsPerPage"]).done(function (data) {
                    updateLogTables(index, data);
                });
            },
            rowsPerPage: paginationLog[index]["rowsPerPage"],
            totalPages: paginationLog[index]["totalPages"],
            totalRows: paginationLog[index]["totalRows"],
            currentPage: paginationLog[index]["currentPage"],
            // Maximum number of rows we can view in a page.
            maxRowsPerPage: 100
        });
    } else {
        $("#log-pagination-" + index).empty();
    }

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

    $.get("http://localhost:8080/api/gtfs-rt-feed/" + gtfsRtId + "/errorCount").done(function (errorData) {
        updatePaginationLogData(gtfsRtId, errorData);
        // Request data based on 'hideErrors' and 'paginationLog' data
        $.get("http://localhost:8080/api/gtfs-rt-feed/" + gtfsRtId + "/log/" + hideErrors[gtfsRtId] + "/pagination/" + paginationLog[gtfsRtId]["currentPage"] + "/" + paginationLog[gtfsRtId]["rowsPerPage"]).done(function (logData) {

            // Store current position for later use
            var scrollPosition = document.body.scrollTop;

            updateLogTables(gtfsRtId, logData);

            $('html, body').animate({scrollTop: scrollPosition}, 1);
        });
    });
}

function updatePaginationSummaryData(index, data) {
    paginationSummary[index]["totalRows"] = 0;
    for (var element in data) {
        paginationSummary[index]["totalRows"] += 1;
    }
    updatePaginationInfo("summary", index, paginationSummary);
}

function updatePaginationLogData(index, data) {
    paginationLog[index]["totalRows"] = 0;
    for (var element in data) {
        // Does not include the count of errorId's whose toggledData is 'off'.
        if(hideErrors[index].indexOf(data[element]["id"]) == -1) {
            paginationLog[index]["totalRows"] += data[element]["count"];
        }
    }
    updatePaginationInfo("log", index, paginationLog);
}

function updatePaginationInfo(logOrSumary, index, paginationInfo) {
    var pagination = paginationInfo;
    pagination[index]["totalPages"] = Math.ceil(pagination[index]["totalRows"] / pagination[index]["rowsPerPage"]);
    // If user selected a page and is less than totalPages, currentPage is user selected page.
    if (pagination[index]["userSelPage"] > 0) {
        pagination[index]["currentPage"] = pagination[index]["userSelPage"];
    }
    if (logOrSumary == "summary") {
        paginationSummary = pagination;
    } else {
        paginationLog = pagination;
    }
}

function showFeedMessage(iterationId, timestamp, occurrence) {
    $.get("http://localhost:8080/api/gtfs-rt-feed/" + iterationId + "/feedMessage").done(function (data) {
        var jsonFeed = JSON.stringify(data, undefined, 2);
        var feedTemplateScript = $("#gtfs-rt-feed-message").html();
        $(".feedMessageDialog").html(feedTemplateScript);
        document.getElementById("feedMessage").innerHTML = jsonFeed;
        $("#title-text").text("Iteration " + iterationId + " - " + timestamp + " (" + occurrence + ")");
        $("#feed-message-modal").modal();
    });

    var clipboard = new Clipboard("#clipboard");

    clipboard.on('success', function(e) {
        e.clearSelection();
        var btn = $(e.trigger);
        setTooltip(btn,'Copied!');
        hideTooltip(btn);
    });

    clipboard.on('error', function(e) {
        var btn = $(e.trigger);
        setTooltip('Failed!');
        hideTooltip(btn);
    });
}

function setTooltip(btn,message) {
    btn.attr('data-original-title', message)
        .tooltip('show');
}

function hideTooltip(btn) {
    setTimeout(function() {
        btn.tooltip('hide');
    }, 1000);
}