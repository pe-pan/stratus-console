package com.hp.sddg.rest;

/**
* Created by panuska on 11.9.14.
*/
public class HttpResponse {
    private final String response;
    private final int responseCode;
    private final String location;

    public HttpResponse(String response, int responseCode, String location) {
        this.response = response;
        this.responseCode = responseCode;
        this.location = location;
    }

    public String getResponse() {
        return response;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getLocation() {     //todo resolve this hack
        return location;
    }
}
