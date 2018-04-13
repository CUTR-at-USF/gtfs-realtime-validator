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
var clientId;
var enableValidation;
var enableShapes;

var server = window.location.protocol + "//" + window.location.host;


function about() {

}

function addInput(){
    if (counter == limit)  {
        alert("You have reached the limit of adding " + counter + " inputs");
    }
    else {
        var newdiv = document.createElement('div');
        var count = (counter + 1);
        var HTMLString;

        HTMLString = "<div class=\"form-group has-feedback has-clear\"> <label for=\"gtfsrt-feed-"+ count +"\">GTFS-Realtime Feed "+ count +"</label>";
        HTMLString += "<input type=\"text\" class=\"form-control\" name=\"gtfsrt-feed-"+ count+ "\"/>";
        HTMLString += "<span class=\"form-control-clear glyphicon glyphicon-remove form-control-feedback hidden\"></span> </div>"
        newdiv.innerHTML = HTMLString;
        $("#dynamicInput").append(newdiv);
        removeTextInput();
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
// Adding clear text field functionality to each of input type='text'
removeTextInput();
var $submit = $('input[type="submit"]');
var reset = $('input[type="reset"]');
reset.on('click', disableSubmit);
/*
 * 'checkStatus' method call here ensures submit button is disabled/enabled based on text input values
 *   when we come back from loading.html to welcome page.
 */
checkStatus();

gtfsURLField.bind('input', checkStatus);
gtfsrtURLField.bind('input', checkStatus);

function checkStatus() {
    var status = ($.trim(gtfsrtURLField.val()) === '' || $.trim(gtfsURLField.val()) === '');
    $submit.prop('disabled', status);
}

function disableSubmit() {
    $submit.prop('disabled', true);
    $('.form-control-clear').toggleClass('hidden', true);
}

/*
 * Make visible the remove glyphicon in input text field on entering text in it
 *  and upon clicking the remove glyphicon, clears the input text.
 */
function removeTextInput() {
    $('.has-clear input[type="text"]').on('input propertychange', function() {
      var $this = $(this);
      var visible = Boolean($this.val());
      $this.siblings('.form-control-clear').toggleClass('hidden', !visible);
    }).trigger('propertychange');

    $('.form-control-clear').click(function() {
      $(this).siblings('input[type="text"]').val('')
        .trigger('propertychange').focus();
      checkStatus();
    });
}

// Trim spaces for each of input type="text", before submitting the form.
$('input[type="submit"]').click(function () {
    $('input[type="text"]').each(function(){
      this.value=$(this).val().trim();
    })
});

clientId = readCookie("clientId");
enableValidation = readCookie("enableValidation");
if (enableValidation == "checked" || enableValidation == "") {
    document.getElementById("enable-validation").checked=true;
    document.getElementById("enable-validation").value = "checked";
    enableValidation = "checked";
} else {
    document.getElementById("enable-validation").checked=false;
    document.getElementById("enable-validation").value = "unchecked";
}
sessionStorage.setItem("enablevalidation", enableValidation);

enableShapes = readCookie("enableShapes");
if (enableShapes == "true" || enableShapes == "") {
    document.getElementById("enable-shapes").checked=true;
    document.getElementById("enable-shapes").value = "true";
    enableShapes = "true";
} else {
    document.getElementById("enable-shapes").checked=false;
    document.getElementById("enable-shapes").value = "false";
}
sessionStorage.setItem("enableshapes", enableShapes);
// Get past session data for the user.
$.get(server + "/api/gtfs-rt-feed/pastSessions?clientId=" + clientId).done(function (data) {
    if (clientId == "") {
        createCookie("clientId", data["clientId"], 1000);
    } else {
        displayPastSessionData(data);
    }
    sessionStorage.setItem("clientId", readCookie("clientId"));
});

function displayPastSessionData(data) {
    var sessionTemplateScript = $("#past-session-data-template").html();
    var sessionTemplate = Handlebars.compile(sessionTemplateScript);
    var compiledHtml = sessionTemplate(data);
    $("#past-session-data").html(compiledHtml);
}

function createCookie (name, value, days) {
    var expires = "";
    if (days) {
        var date = new Date();
        date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
        expires = "expires=" + date.toUTCString();
    }
    document.cookie = name + "=" + value + ";" + expires + ";path =/";
}

function readCookie(name) {
    var nameEQ = name + "=";
    var decodedCookie = decodeURIComponent(document.cookie);
    var ca = decodedCookie.split(';');
    for (var i=0; i < ca.length; i++) {
        var c = ca[i];
        while (c.charAt(0) == ' ') {
            c = c.substring(1, c.length);
        }
        if (c.indexOf(nameEQ) == 0) {
            return c.substring(nameEQ.length, c.length);
        }
    }
    return "";
}

function toggleValidation() {
    var checkboxStatus;
    if(document.getElementById("enable-validation").checked) {
        checkboxStatus = "checked";
        document.getElementById("enable-validation").value = "checked";
    } else {
        checkboxStatus = "unchecked";
        document.getElementById("enable-validation").value = "unchecked";
    }
    createCookie("enableValidation", checkboxStatus);
    enableValidation = checkboxStatus;
    sessionStorage.setItem("enablevalidation", enableValidation);
}

function toggleEnableShapes() {
    var checkboxStatus;
    if(document.getElementById("enable-shapes").checked) {
        checkboxStatus = "true";
        document.getElementById("enable-shapes").value = "true";
    } else {
        checkboxStatus = "false";
        document.getElementById("enable-shapes").value = "false";
    }
    createCookie("enableShapes", checkboxStatus);
    enableShapes = checkboxStatus;
    sessionStorage.setItem("enableshapes", enableShapes);
}