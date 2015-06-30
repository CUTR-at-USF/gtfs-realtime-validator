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


//Retrive the validated urls (saved to the local storage in the loading.js file) from the localStorage
var urls = localStorage.getItem("gtfsRtFeeds");
var gtfsFeeds = JSON.parse(urls);

//POST request sent to TriggerBackgroundServlet to start monitoring the feeds
$.post("http://localhost:8080/startBackground", {gtfsRtFeeds: urls})
    .done(function (data) {
        alert(JSON.stringify(data));

        //TODO: Loop through the data received and draw the appropriate UI elements

        //Start calling for updates every 10 seconds
        for (var gtfsFeed in data) {
            var currentUrl = gtfsFeeds[gtfsFeed]["url"];
            setInterval(function(){getFeedDetails(currentUrl);},10000);
        }
    });

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

//Call time elapsed evey 1 second
setInterval(getTimeElapsed,1000);


//Get feed details
function getFeedDetails(url) {

    //Ajax call to the servlet to get the json with the feed details
    $.get("http://localhost:8080/feedInfo", {gtfsurl: url}).done(function (data) {
        console.log(JSON.stringify(data));
    });
}