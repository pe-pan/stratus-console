package com.hp.sddg.rest.csa;

import com.hp.sddg.rest.AuthenticatedClient;
import com.hp.sddg.rest.HttpResponse;
import com.hp.sddg.rest.RestClient;
import com.hp.sddg.xml.XmlFile;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by panuska on 11.9.14.
 */
public class ResourceProvider {
    private final String id;
    private final String name;

    private final String url;
    private final String username;
    private final String password;
    private final String computeEndpoint;
    private final String tenantId;

    public static ResourceProvider getResourceProvider(Csa csa, String providerId, String providerName, String computeEndpoint, String tenantId, String adminId) {
        HttpResponse response = csa.doGet(Csa.REST_URI +"/artifact/fastview/"+providerId+"?userIdentifier="+adminId+"&view=accesspoint");

        XmlFile xmlFile = new XmlFile(response.getResponse());
        String url;
        try {
            URI uri = new URI(xmlFile.getElementValue("/resultView/resultMap/entry[key='accessPoint.uri']/value"));
            url = uri.getScheme()+"://"+uri.getHost()+":"+uri.getPort();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        String username = xmlFile.getElementValue("/resultView/resultMap/entry[key='accessPoint.username']/value");
        String password = xmlFile.getElementValue("/resultView/resultMap/entry[key='accessPoint.password']/value");

        return new ResourceProvider(providerId, providerName, url, username, password, computeEndpoint, tenantId);
    }

    private ResourceProvider(String id, String name, String url, String username, String password, String computeEndpoint, String tenantId) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.username = username;
        this.password = password;
        this.computeEndpoint = computeEndpoint;
        this.tenantId = tenantId;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getComputeEndpoint() {
        return computeEndpoint;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "ResourceProvider{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", username='" + username + '\'' +
                ", computeEndpoint='" + computeEndpoint + '\'' +
                ", tenantId='" + tenantId + '\'' +
                '}';
    }
}
