package org.nolanlab.CODEX.utils;

import spark.utils.IOUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

public class RequestHandler {

    private static final String USER_AGENT = "Mozilla/5.0";

    // HTTP GET request
    public static byte[] get(String url) throws IOException {
        System.out.println("Trying to connect: "+url);
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", USER_AGENT);

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        byte[] bytes = IOUtils.toByteArray(con.getInputStream());

        con.disconnect();

        return bytes;
    }

    // HTTP POST request
    public static byte[] post(String url, String urlParameters) throws IOException {

        System.out.println("Trying to connect: "+url);
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        //add request header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");


        // Send post request
        con.setDoOutput(true);

        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(urlParameters);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();

        System.out.println("\nSending 'POST' request to URL : " + url);
        System.out.println("Post parameters : " + urlParameters);
        System.out.println("Response Code : " + responseCode);

        byte[] bytes = IOUtils.toByteArray(con.getInputStream());

        con.disconnect();

        return bytes;
    }
}