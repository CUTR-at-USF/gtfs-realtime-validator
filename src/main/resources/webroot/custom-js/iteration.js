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

// Get all the local data required.
var urls = localStorage.getItem("gtfsRtFeeds");
var gtfsRtFeeds = JSON.parse(urls);
var gtfsRtId = sessionStorage.getItem("gtfsRtId");
var rowId = sessionStorage.getItem("rowId");
var iterationId = sessionStorage.getItem("iterationId");
var timestamp = sessionStorage.getItem("timestamp");
var occurrence = sessionStorage.getItem("occurrence");

// These variables store data that is needed when showing more and less errors for each error/warning.
var showMoreErrorList;
var MAX_ERRORS_TO_DISPLAY = 3;
var showLessErrorList = {};

var server = window.location.protocol + "//" + window.location.host;

// Get the feedMessage from server for a particular 'iterationId' and display in plain text format to user.
$.get(server + "/api/gtfs-rt-feed/" + iterationId + "/feedMessage").done(function (data) {
    var gtfsRtUrl = "";
    for (gtfsRtFeed in gtfsRtFeeds) {
        if (gtfsRtFeeds[gtfsRtFeed]["feedId"] == gtfsRtId) {
            gtfsRtUrl = gtfsRtFeeds[gtfsRtFeed]["url"];
        }
    }
    var jsonFeed = JSON.stringify(data, undefined, 2);
    document.getElementById("feedMessage").innerHTML = jsonFeed;
    $("#title-text").text("Iteration " + rowId + " - " + timestamp + " (" + occurrence + ") - " + gtfsRtUrl);
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

// Get the list of errors/warnings and list of their occurrences from server for a particular 'iterationId'.
$.get(server + "/api/gtfs-rt-feed/" + iterationId + "/iterationErrors").done(function (data) {

    showMoreErrorList = data;
    var errorCount = 0;
    var warningCount = 0;

    // Get the correct count of error occurrences to show in text '...and xx more'
    for (errorListIndex in data) {
        data[errorListIndex]["errorOccurrences"] = data[errorListIndex]["errorOccurrences"] - MAX_ERRORS_TO_DISPLAY;
    }

    /*
     * Form the required number of error/warning cards table structure.
     * The size of 'data' is the number of error/warning cards required.
     */
    var eachCardTemplateScript = $("#error-card-table-template").html();
    var eachCardTemplate = Handlebars.compile(eachCardTemplateScript);
    var cardsCompiledHtml = eachCardTemplate(data);
    $("#error-cards").html(cardsCompiledHtml);

    for (errorListIndex in data) {
        // Hide '...and xx more' message if occurrences <= 0
        if (data[errorListIndex]["errorOccurrences"] <= 0) {
            $(".show-more-" + errorListIndex).hide();
        }
        // Calculate the count of errors and warnings.
        if (data[errorListIndex]["errorId"].startsWith("E")) {
            errorCount++;
        } else if (data[errorListIndex]["errorId"].startsWith("W")) {
            warningCount++;
        }

        // Fill each error/warning card table data
        var cardBodyTemplateScript = $("#error-card-body-template").html();
        var cardBodyTemplate = Handlebars.compile(cardBodyTemplateScript);

        showLessErrorList[errorListIndex] = {};

        /*
         * At first, we will display less number of rows in each error/warning table.
         * 'MAX_ERRORS_TO_DISPLAY' is the number of error/warning occurrences to display.
         * 'showLessErrorList' will store each error occurrences data size to <= 'MAX_ERRORS_TO_DISPLAY'
         */
        for (numErrorsIndex in data[errorListIndex]["viewIterationErrorsModelList"]) {
            if (numErrorsIndex < MAX_ERRORS_TO_DISPLAY) {
                showLessErrorList[errorListIndex][numErrorsIndex] = data[errorListIndex]["viewIterationErrorsModelList"][numErrorsIndex];
            }
        }
        var bodyCompiledHtml = cardBodyTemplate(showLessErrorList[errorListIndex]);
        $("#error-card-body-" + errorListIndex).html(bodyCompiledHtml);
    }

    $("#error-count").text(errorCount);
    $("#warning-count").text(warningCount);
});

// On clicking '...and xx more' text, this method will show all the occurrences of that error/warning
function showEntireList(errorIndex) {
    var cardDataTemplateScript = $("#error-card-body-template").html();
    var cardDataTemplate = Handlebars.compile(cardDataTemplateScript);
    var dataCompiledHtml = cardDataTemplate(showMoreErrorList[errorIndex]["viewIterationErrorsModelList"]);

    $("#error-card-body-" + errorIndex).html(dataCompiledHtml);

    $(".show-more-" + errorIndex).hide();
    $(".show-less-" + errorIndex).show();
}

// On clicking 'show less' text, this method will show 'MAX_ERRORS_TO_DISPLAY' occurrences of that error/warning
function showLessErrors(errorIndex) {
    var cardBodyTemplateScript = $("#error-card-body-template").html();
    var cardBodyTemplate = Handlebars.compile(cardBodyTemplateScript);
    var bodyCompiledHtml = cardBodyTemplate(showLessErrorList[errorIndex]);

    $("#error-card-body-" + errorIndex).html(bodyCompiledHtml);

    $(".show-less-" + errorIndex).hide();
    $(".show-more-" + errorIndex).show();
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