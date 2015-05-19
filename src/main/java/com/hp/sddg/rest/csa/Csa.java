package com.hp.sddg.rest.csa;

import com.hp.sddg.main.Ansi;
import com.hp.sddg.rest.AuthenticatedClient;
import com.hp.sddg.rest.ContentType;
import com.hp.sddg.rest.HttpResponse;
import com.hp.sddg.xml.XmlFile;
import org.apache.log4j.Logger;

/**
 * Created by panuska on 11.9.14.
 */
public class Csa extends AuthenticatedClient {
    private static Logger log = Logger.getLogger(Csa.class.getName());

    public static final String REST_URL = "https://csa4.hpswdemoportal.com/csa";
    public static final String REST_URI = REST_URL +"/rest";
    public static final String REST_API = REST_URL +"/api";

    private String username;
    private String password;
    public Csa(String username, String password) {
        this.username = username;
        this.password = password;
        loggedUserName = null;

    }

    private String loggedUserName;      // todo this is duplicated to username

    @Override
    public void authenticate() {
        login(username, password);
        System.out.println("User " + Ansi.BOLD+Ansi.CYAN + username + Ansi.RESET + " authenticated to CSA");
    }

    public void login(String username, String password) {
        final String[][] data = {
                { "j_username", username },
                { "j_password", password }
        };
        HttpResponse response = client.doPost(REST_URL +"/j_spring_security_check", data);
        if (response.getLocation().endsWith("login_error=1")) {
            throw new IllegalStateException("Cannot login to CSA");
        }
        loggedUserName = username;
        log.debug("CSA user "+username+" logged in");
    }

    public String getLoggedUserName() {
        return loggedUserName;
    }

    public boolean isAuthenticated() {
        return loggedUserName != null;
    }

    public String getUserId(String admin) {
        HttpResponse response = doGet(REST_URI +"/login/CSA-Provider/"+admin);
        XmlFile file = new XmlFile(response.getResponse());
        String userId = file.getElementValue("/person/id");
        log.debug("User "+admin+" has ID: "+userId);
        return userId;
    }
}
