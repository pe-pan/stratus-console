package com.hp.sddg.rest;

import java.net.HttpURLConnection;

/**
 * Created by panuska on 14.10.14.
 */
public abstract class AuthenticatedClient  {
    protected RestClient client;

    public abstract void authenticate();

    public abstract String getLoggedUserName();

    protected AuthenticatedClient() {
        this.client = new RestClient();
    }

    public HttpResponse doGet(String url) {
        return doGet(url, ContentType.XML_XML);
    }

    public HttpResponse doGet(String url, ContentType type) {
        try {
            return client.doGet(url, type);
        } catch (IllegalRestStateException e) {
            if (e.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                authenticate();
                return client.doGet(url, type);
            }
            throw e;
        }
    }

    public HttpResponse doPut(String url, String data, ContentType type) {
        try {
            return client.doPut(url, data, type);
        } catch (IllegalRestStateException e) {
            if (e.getResponseCode() == HttpURLConnection .HTTP_UNAUTHORIZED) {
                authenticate();
                return client.doPut(url, data, type);
            }
            throw e;
        }
    }

    public HttpResponse doPut(String url, String data) {
        return doPut(url, data, ContentType.JSON_JSON);
    }

    public HttpResponse doDelete(String url) {
        try {
            return client.doDelete(url);
        } catch (IllegalRestStateException e) {
            if (e.getResponseCode() == HttpURLConnection .HTTP_UNAUTHORIZED) {
                authenticate();
                return client.doDelete(url);
            }
            throw e;
        }
    }

    public HttpResponse doPost(String url, String data) {
        try {
            return client.doPost(url, data);
        } catch (IllegalRestStateException e) {
            if (e.getResponseCode() == HttpURLConnection .HTTP_UNAUTHORIZED) {
                authenticate();
                return client.doPost(url, data);
            }
            throw e;
        }
    }
}
