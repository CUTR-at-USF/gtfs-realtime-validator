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

// Get data from URL.
var rowId = getUrlParameter("sessionIteration");
var iterationId = getUrlParameter("iteration");
var gtfsRtId = getUrlParameter("gtfsRtId");

// These variables store data that is needed when showing more and less errors for each error/warning.
var showMoreErrorList;
var MAX_ERRORS_TO_DISPLAY = 3;
var showLessErrorList = {};

var exportDataHeader;
var allTablesData = '';

var server = window.location.protocol + "//" + window.location.host;
$(".feed-page-loader").show();
if (iterationId != -1) {
    $(".issues-page-loader").show();
} else {
    $(".issues-page-loader").hide();
}
$.get(server + "/api/gtfs-rt-feed/" + iterationId + "/" + gtfsRtId + "/iterationDetails").done(function (data) {
    var timestamp = data["feedTimestamp"];
    var dateFormat = data["dateFormat"];
    var gtfsRtUrl = data["gtfsRtFeedModel"]["gtfsRtUrl"];

    $("#title-text").text("Iteration " + rowId + " - " + dateFormat + " (" + timestamp + ") - " + gtfsRtUrl);
});

// Get the feedMessage from server for a particular 'iterationId' and display in plain text format to user.
$.get(server + "/api/gtfs-rt-feed/" + iterationId + "/" + gtfsRtId + "/feedMessage").done(function (data) {
    var jsonFeed = JSON.stringify(data, undefined, 2);
    document.getElementById("feedMessage").innerHTML = jsonFeed;

    // Hide spinning after 'jsonFeed' is loaded into 'feedMessage' element.
    $(".feed-page-loader").hide();
    // Set 'feed-display' position to 'static' after hiding 'showSpin' element. This make sure 'copy-clipboard' is in fixed position relative to parent.
    document.getElementById('feed-display').style.position = "static";
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

if (iterationId != -1) {
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

            /*
             * 'setupExportData' method sets all the rows of each table to be exported as csv file.
             * Creates a click event for each download button.
             */
            setupExportData(errorListIndex, cardBodyTemplate);

            // Show less number of rows in each table.
            var bodyCompiledHtml = cardBodyTemplate(showLessErrorList[errorListIndex]);
            $("#error-card-body-" + errorListIndex).html(bodyCompiledHtml);
        }

        $(".issues-page-loader").hide();

        $("#error-count").text(errorCount);
        $("#warning-count").text(warningCount);

        // Add a click event listener for downloading all the tables data saved in 'allTablesData'
        allTablesData = exportDataHeader + allTablesData;
        var instance = new TableExport($('<div>')); // Instantiate TableExport
        $('#download-all-button').on('click', function(e) {
                                 // File data,   mimeType,  filename,                   fileExtension
            instance.export2file(allTablesData, 'text/csv', 'iteration_' + iterationId, '.csv');
        });
    });
}
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

function setupExportData(index, cardBodyTemplate) {

    // Get all the rows into the table
    var dataCompiledHtml = cardBodyTemplate(showMoreErrorList[index]["viewIterationErrorsModelList"]);
    $("#error-card-body-" + index).html(dataCompiledHtml);

    // Setting up export data properties and handling click events for each download button
    (function (){
        var exportTable = document.getElementById('export-table-' + index);
        var instance = new TableExport(exportTable, {
            formats: ['csv'], // The format we want to export
            exportButtons: false, // Such that we can create customized download button
            filename: 'iteration_' + iterationId + '_' + showMoreErrorList[index]["errorId"]
        });

        var exportData = instance.getExportData()['export-table-' + index]['csv'];

        // Form the table header i.e., 'exportDataHeader' and 'allTablesData' so that we can download all tables data at once
        if (!exportDataHeader) {
            exportDataHeader = exportData.data.substring(0, exportData.data.indexOf("\n"));
        }
        allTablesData = allTablesData.concat(exportData.data.substring(exportData.data.indexOf("\n")));

        var downloadButton = document.getElementById('download-button-' + index);

        $(downloadButton).on('click', function(e) {
            instance.export2file(exportData.data, exportData.mimeType, exportData.filename, exportData.fileExtension);
        });
    }());
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

function getUrlParameter(param) {
    var pageURL = window.location.search.substring(1); // Remove '?' from search string
    var pageURLVariables = pageURL.split('&');
    for (var i = 0; i < pageURLVariables.length; i++) {
        var parameterName = pageURLVariables[i].split('=');
        if (parameterName[0] == param) {
            return parameterName[1];
        }
    }
    return '';
}
