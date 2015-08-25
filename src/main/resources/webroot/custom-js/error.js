/*
 * Copyright (C) 2011 Nipuna Gunathilake.
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

/**
 * Created by nipuna on 8/21/15.
 */

$.get("http://localhost:8080/getFeed")
    .done(function (data) {

        var res1 = jsonPath(data, "$.entity.*");
        var res2 = jsonPath(data, "$.entity.*", {resultType: "PATH"});

        var text = "";

        for (var errorItem in res1) {
            var jsonItem = JSON.stringify(res1[errorItem], null, 2);
            $("#error-list").append("<pre class='errorItem'>" + jsonItem + "</pre>");
        }

        for (var item in res2) {
            var subitemArray = res2[item].split(";");

            subitemArray.shift();
            for (var subItem in subitemArray) {

                if (!isNaN(text)) {
                    text += "." + subitemArray[subItem];
                } else {
                    text += "[" + subitemArray[subItem] + "]";
                }
            }
            var parsedPath = "data" + text;

            var jsonObject = eval(parsedPath);


            eval(parsedPath + "['color'] = '#e4e4e4'");
            eval(parsedPath + "['error'] = 'Timestamp error occurred'");
            text = "";
        }

        Handlebars.registerPartial("marked", $("#marked-partial").html());

        var feedTemplateScript = $("#gtfsrt-feed-template").html();
        var feedTemplate = Handlebars.compile(feedTemplateScript);
        var compiledHtml = feedTemplate(data);
        //$('#googleFeed').html(compiledHtml);


        var jsonPrint = JSON.stringify(data, null, 2);
        $("#error-json").html(jsonPrint);

        var lines = jsonPrint.split('\n');

        $(".errorItem").hover(function () {
            var selectedText = $(this).text();
            console.log(selectedText);
            $("body").highlight(selectedText, true);
        }, function () {
            $("body").removeHighlight();
        });

    });