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
var gtfsFeeds = JSON.parse(urls);

//Retrive the update interval value
var updateInterval = localStorage.getItem("updateInterval");

//This object will hold the urls returned after starting the background tasks.
var monitoredFeeds = {};



//TODO: PUT request to start monitoring of the given gtfsRtFeed ID /api/gtfs-rt-feed/{id}/start
for (var gtfsFeed in gtfsFeeds) {
    if(gtfsFeeds.hasOwnProperty(gtfsFeed)){
        alert(JSON.stringify(gtfsFeeds[gtfsFeed]));
        $.ajax({
            url: "http://localhost:8080/api/gtfs-rt-feed/"+ gtfsFeeds[gtfsFeed]["feedId"] +"/monitor",
            type: 'PUT',
            success: function(data) {
                alert(data);
            }
        });
    }
}


//TODO: Remove method once REST implementation is done
//POST request sent to TriggerBackgroundServlet to start monitoring the feeds
//$.post("http://localhost:8080/startBackground", {gtfsRtFeeds: urls, updateInterval:updateInterval})
//    .done(function (data) {
//        monitoredFeeds = data;
//
//        //Use the data received and draw the appropriate UI elements
//        initializeInterface(data);
//
//        //Start calling for updates every 10 seconds
//        for (var gtfsFeed in data) {
//            //alert(JSON.stringify(data));
//            var currentUrl = gtfsFeeds[gtfsFeed]["url"];
//            var currentIndex = gtfsFeeds[gtfsFeed]["index"];
//
//            getFeedUpdates(currentUrl, currentIndex);
//
//            //Uses an anonymous wrapper to copy the references
//            (function(url, index){
//                setInterval(function(){getFeedUpdates(url, index);},10000);
//            }(currentUrl, currentIndex));
//
//        }
//    });

function initializeInterface(gtfsFeeds){
    var wrapper  = {gtfsFeeds: gtfsFeeds};
    var monitorTemplateScript = $("#feed-monitor-template").html();
    var monitorTemplate = Handlebars.compile(monitorTemplateScript);
    var compiledHtml = monitorTemplate(wrapper);
    $('.monitor-placeholder').html(compiledHtml);
}

//Calculate time for display
var start = new Date();

function getTimeElapsed(){
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

    $("#time-elapsed").text(hours + "h "+ min + "m " + sec + "s");
}

//Call time elapsed to update the clock evey second
setInterval(getTimeElapsed,1000);

//TODO: get feed details from GET request to /api/gtfs-rt-feed/{id}
//Get feed details
function getFeedUpdates(url, index) {
    //Ajax call to the servlet to get the json with the feed details
    $.get("http://localhost:8080/feedInfo", {gtfsurl: url}).done(function (data) {
        updateTables(data, index);
    });

    //TODO: Replace feedInfo with {id}


}

function updateTables(data, index) {

    var monitorTemplateScript = $("#feed-monitor-row-template").html();
    var monitorTemplate = Handlebars.compile(monitorTemplateScript);
    var compiledHtml = monitorTemplate(data);

    $("#monitor-table-"+ index +"").html(compiledHtml);
}