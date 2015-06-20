/*
 ***********************************************************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 ***********************************************************************************************************************
*/

package edu.usf.cutr.gtfsrtvalidator.servlets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;

@SuppressWarnings("serial")
public class GTFSDownloaderServlet extends HttpServlet {

    private static final int BUFFER_SIZE = 4096;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String feedURL = getParamter(request, "gtfsurl");

        downloadFeed(feedURL);

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);

        //Creates simple json object giving the feed type
        //Should be changed to a java object if more complexities occur.
        //TODO: Change according to the status
        response.getWriter().println("{\"feedStatus\" :1}");
    }

    private void downloadFeed(String fileURL) throws ServletException, IOException{

        String path = GTFSDownloaderServlet.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedPath = URLDecoder.decode(path, "UTF-8");
        System.out.println(decodedPath);

        //String basePath = new File("").getAbsolutePath();
        //System.out.println(basePath);

        URL url = new URL(fileURL);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        connection.connect();
        System.out.println(connection.getResponseCode());
        // Check if the request is handled successfully
        if(connection.getResponseCode() / 100 == 2)
        {
            // This should get you the size of the file to download (in bytes)
            System.out.println(connection.getContentLength());
            String fileName = "";
            String disposition = connection.getHeaderField("Content-Disposition");
            String contentType = connection.getContentType();
            int contentLength = connection.getContentLength();

            if (disposition != null) {
                // extracts file name from header field
                int index = disposition.indexOf("filename=");
                if (index > 0) {
                    fileName = disposition.substring(index + 10,
                            disposition.length() - 1);
                }
            } else {
                // extracts file name from URL
                fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1,
                        fileURL.length());
            }

            System.out.println("Content-Type = " + contentType);
            System.out.println("Content-Disposition = " + disposition);
            System.out.println("Content-Length = " + contentLength);
            System.out.println("fileName = " + fileName);

            // opens input stream from the HTTP connection
            InputStream inputStream = connection.getInputStream();

            String saveDir = ".";
            String saveFilePath = saveDir + File.separator + fileName;

            // opens an output stream to save into file
            FileOutputStream outputStream = new FileOutputStream(saveFilePath);

            int bytesRead = -1;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            System.out.println("File downloaded");
        }
    }

    private String getParamter(HttpServletRequest request, String paramName){

        String parameter = "";
        String value = request.getParameter(paramName);

        if (!(value == null || "".equals(value))) {
            parameter = value;

            try {
                parameter = java.net.URLDecoder.decode(parameter, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        System.out.println(parameter);
        return parameter;
    }


}

