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

var counter = 1;
var limit = 5;
function addInput(){
    if (counter == limit)  {
        alert("You have reached the limit of adding " + counter + " inputs");
    }
    else {
        var newdiv = document.createElement('div');
        var count = (counter + 1);
        var HTMLString;

        HTMLString = "<div class=\"form-group\"> <label for=\"gtfsrt-feed-"+ count +"\">GTFS-Realtime Feed "+ count +"</label>";
        HTMLString += "<input class=\"form-control\" name=\"gtfsrt-feed-"+ count+ "\"/></div>";
        newdiv.innerHTML = HTMLString;
        $("#dynamicInput").append(newdiv);
        counter++;
    }
}

function removeInput(){
    if (counter == 1)  {
        alert("You must have at least one GTFS-Realtime feed")
    }else{
        $('#dynamicInput').children().last().remove();
        counter = counter-1;
    }
}

//Checking if there is text on both GTFS and GTFS-RT fields
var gtfsURLField = $('#gtfsrt-feed-1');
var gtfsrtURLField = $('#gtfs');
var $submit = $('input[type="submit"]');

$submit.prop('disabled', true);

gtfsURLField.on('keyup', checkStatus);
gtfsrtURLField.on('keyup', checkStatus);

function checkStatus() {
    var status = ($.trim(gtfsrtURLField.val()) === '' || $.trim(gtfsURLField.val()) === '');
    $submit.prop('disabled', status);
}