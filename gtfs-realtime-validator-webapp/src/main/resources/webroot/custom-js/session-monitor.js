/*
 * Copyright (C) 2017 University of South Florida.
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

// Get URL parameters
var gtfsRtId = getUrlParameter("gtfsRtId");
var gtfsRtUrl = {gtfsRtUrl: getUrlParameter("gtfsRtUrl")};
var sessionStartTime = getUrlParameter("sessionStartTime");
var sessionEndTime = getUrlParameter("sessionEndTime");

var hideErrors = [];
var paginationLog = [];
var paginationSummary = [];

var server = window.location.protocol + "//" + window.location.host;

var toggleDataOn = '<input type="checkbox" checked data-toggle="toggle" data-onstyle="success"/>';
var toggleDataOff = '<input type="checkbox" data-toggle="toggle" data-onstyle="success"/>';

initializeInterface();
refresh();

function refresh() {

    $.get(server + "/api/gtfs-rt-feed/monitor-data/" + gtfsRtId +
            "?summaryCurPage=" + paginationSummary["currentPage"] +
            "&summaryRowsPerPage=" + paginationSummary["rowsPerPage"] +
            "&toggledData=" + hideErrors +
            "&logCurPage=" + paginationLog["currentPage"] +
            "&logRowsPerPage=" + paginationLog["rowsPerPage"] +
            "&startTime=" + sessionStartTime +
            "&endTime=" + sessionEndTime).done(function (data) {
        updateMonitorData(data);
    });
}

function initializeInterface() {
    // Initializing pagination variables.
    paginationLog["currentPage"] = 1;
    // Default number of rows per page.
    paginationLog["rowsPerPage"] = 10;

    paginationSummary["currentPage"] = 1;
    paginationSummary["rowsPerPage"] = 10;

    var monitorTemplateScript = $("#feed-monitor-template").html();
    var monitorTemplate = Handlebars.compile(monitorTemplateScript);
    var compiledHtml = monitorTemplate(gtfsRtUrl);
    $('.monitor-placeholder').append(compiledHtml);
}

// Update Monitor data from 'MergeMonitorData.java'
function updateMonitorData(data) {
    $("#requests").text(data["iterationCount"]);
    $("#responses").text(data["uniqueFeedCount"]);
    updatePaginationSummaryData(data["viewGtfsRtFeedErrorCountModelList"]);
    updatePaginationLogData(data["viewGtfsRtFeedErrorCountModelList"]);
    updateSummaryTables(data["viewErrorSummaryModelList"]);
    updateLogTables(data["viewErrorLogModelList"]);
}

function updateSummaryTables(data) {

    var monitorTemplateScript = $("#feed-monitor-summary-row-template").html();
    var monitorTemplate = Handlebars.compile(monitorTemplateScript);
    var compiledHtml = monitorTemplate(data);

    if (paginationSummary["totalRows"] > 0) {
        $("#summary-pagination").bs_pagination({
            onChangePage: function (event, page) {
                paginationSummary["userSelPage"] = page["currentPage"];
                paginationSummary["currentPage"] = page["currentPage"];
                paginationSummary["rowsPerPage"] = page["rowsPerPage"];
                paginationSummary["totalPages"] = Math.ceil(paginationSummary["totalRows"] / paginationSummary["rowsPerPage"]);

                refresh();
            },
            rowsPerPage: paginationSummary["rowsPerPage"],
            totalPages: paginationSummary["totalPages"],
            totalRows: paginationSummary["totalRows"],
            currentPage: paginationSummary["currentPage"],
            // Maximum number of rows we can view in a page.
            maxRowsPerPage: 10
        });
    } else {
        $("#summary-pagination").empty();
    }

    var summaryTable = $("#monitor-table-summary").html(compiledHtml);

    handleToggledData(data, summaryTable);
}

function handleToggledData(data, summaryTable) {
    var i = 0, j = 0;
    // Add a toggle for each row and initialize based on toggle click history maintained in hideErrors
    summaryTable.find("span[class=toggleHolder" + "]").each(function(){
            var onOrOff = (hideErrors.indexOf(data[i]["id"]) == -1) ? 'on' : 'off'
            if (onOrOff == 'off') {
                $("#toggleId" + data[i++]["id"] + "").html(toggleDataOff);
            } else {
                $("#toggleId" + data[i++]["id"] + "").html(toggleDataOn);
            }
    });

    summaryTable.find('input[type=checkbox][data-toggle=toggle]').each(function(){
        var errorId = data[j++]["id"];
        $(this).bootstrapToggle(); // Initializing toggle
        $(this).change(function() {
            showOrHideError(errorId); // Toggle change event handler
        });
    });
}

function updateLogTables(data) {

    var monitorTemplateScript = $("#feed-monitor-log-row-template").html();
    var monitorTemplate = Handlebars.compile(monitorTemplateScript);
    var compiledHtml = monitorTemplate(data);

    if (paginationLog["totalRows"] > 0) {
        $("#log-pagination").bs_pagination({
            onChangePage: function (event, page) {
                paginationLog["userSelPage"] = page["currentPage"];
                paginationLog["currentPage"] = page["currentPage"];
                paginationLog["rowsPerPage"] = page["rowsPerPage"];
                paginationLog["totalPages"] = Math.ceil(paginationLog["totalRows"] / paginationLog["rowsPerPage"]);

                refresh();
            },
            rowsPerPage: paginationLog["rowsPerPage"],
            totalPages: paginationLog["totalPages"],
            totalRows: paginationLog["totalRows"],
            currentPage: paginationLog["currentPage"],
            // Maximum number of rows we can view in a page.
            maxRowsPerPage: 100
        });
    } else {
        $("#log-pagination").empty();
    }

    $("#monitor-table-log").html(compiledHtml);
}

function showOrHideError(errorId) {
    if (hideErrors.indexOf(errorId) == -1) {
        hideErrors.push(errorId);
    } else {
        hideErrors.splice(hideErrors.indexOf(errorId), 1);
    }
    // If toggled data is changed, update Log pagination currentPage and userSelPage to default values.
    paginationLog["currentPage"] = 1;
    paginationLog["userSelPage"] = 1;
    // Store current position for later use
    var scrollPosition = document.body.scrollTop;

    refresh(gtfsRtId);

    $('html, body').animate({scrollTop: scrollPosition}, 1);
}

function updatePaginationSummaryData(data) {
    paginationSummary["totalRows"] = 0;
    for (var element in data) {
        paginationSummary["totalRows"] += 1;
    }
    updatePaginationInfo("summary", paginationSummary);
}

function updatePaginationLogData(data) {
    paginationLog["totalRows"] = 0;
    for (var element in data) {
        // Does not include the count of errorId's whose toggledData is 'off'.
        if (hideErrors.indexOf(data[element]["id"]) == -1) {
            paginationLog["totalRows"] += data[element]["count"];
        }
    }
    updatePaginationInfo("log", paginationLog);
}

function updatePaginationInfo(logOrSumary, paginationInfo) {
    var pagination = paginationInfo;
    pagination["totalPages"] = Math.ceil(pagination["totalRows"] / pagination["rowsPerPage"]);
    // If there are 'userSelPage' and 'currentPage' values, give priority to 'userSelPage'.
    if (pagination["userSelPage"] > 0) {
        pagination["currentPage"] = pagination["userSelPage"];
    }
    if (logOrSumary == "summary") {
        paginationSummary = pagination;
    } else {
        paginationLog = pagination;
    }
}

$(document).ready(function(){
    $("body").tooltip({ selector: '[data-toggle=tooltip]' });
});

function getUrlParameter(param) {
    var pageURL = window.location.search.substring(1); // Remove '?' from search string
    var pageURLVariables = pageURL.split('&');
    for (var i = 0; i < pageURLVariables.length; i++) {
        var parameterName = pageURLVariables[i].split('=');
        if (parameterName[0] == param) {
            return parameterName[1];
        }
    }
}