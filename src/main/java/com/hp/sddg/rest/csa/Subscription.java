package com.hp.sddg.rest.csa;

import com.hp.sddg.rest.AuthenticatedClient;
import com.hp.sddg.rest.ContentType;
import com.hp.sddg.rest.HttpResponse;
import com.hp.sddg.rest.openstack.OpenStack;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by panuska on 11.9.14.
 */
public class Subscription {
    private static Logger log = Logger.getLogger(Subscription.class.getName());
    private final String id;
    private final String name;
    private final String instanceId;

    private final AuthenticatedClient client;

    public static Subscription getSubscription(Csa csa, String subscriptionId) {
        HttpResponse response = csa.doGet(Csa.REST_API + "/service/subscription/" + subscriptionId, ContentType.JSON_JSON);
        String json = response.getResponse();

        String name = JsonPath.read(json, "$.name");
        String instanceId = JsonPath.read(json, "$.ext.csa_service_instance_id");

        return new Subscription(csa, instanceId, name, subscriptionId);
    }

    private Subscription(AuthenticatedClient client, String instanceId, String name, String id) {
        this.client = client;
        this.instanceId = instanceId;
        this.name = name;
        this.id = id;
        log.debug("Created new "+this);
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Subscription{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", instanceId='" + instanceId + '\'' +
                '}';
    }

    private String getServerProperty(String json, String propertyName) {
        try {
            JSONArray property = JsonPath.read(json, "$.properties[?(@key=='"+propertyName+"')].values");
            if (property.size() == 0) {
                return null;
            }
            return (String)((JSONArray)property.get(0)).get(0);
        } catch (PathNotFoundException e) {
            log.debug("Exception thrown when looking for property "+propertyName+" at "+json, e);
            return null;
        }
    }

    public List<DemoDetail> getDemoDetails(OpenStack openStack) {
        HttpResponse response = client.doGet(Csa.REST_API+"/service/instance/"+instanceId+"/topology", ContentType.JSON_JSON);
        String json = response.getResponse();
        JSONArray nodes = JsonPath.read(json, "$.nodes[?(@type.key=='HPCSSERVER')]");

        List<DemoDetail> returnValue = new LinkedList<>();
        for (Object node : nodes) {
            JSONObject nodeJson = (JSONObject) node;
            String nodeId = (String) nodeJson.get("id");
            response = client.doGet(Csa.REST_API+"/service/instance/"+instanceId+"/topology/"+nodeId+"/properties", ContentType.JSON_JSON);
            json = response.getResponse();
            boolean activated = "true".equals(getServerProperty(json, "ACTIVATED"));
            if (activated) {
                String instanceVolumeId = getServerProperty(json, "ATTACHEDVOLUMEID");
                String demoName = getServerProperty(json, "DEMONAME");
                String serverId = getServerProperty(json, "Server ID");
                String volumeSnapshotId = getServerProperty(json, "VOLUMEREF");
                String size = getServerProperty(json, "SIZEREF");
                DemoDetail detail = DemoDetail.getDemoDetail(instanceVolumeId, demoName, serverId, volumeSnapshotId, size, openStack);
                if (detail instanceof DemoVolume) { // move images to the end of the list
                    returnValue.add(0, detail);
                } else {
                    returnValue.add(detail);
                }
            }
        }
        return returnValue;
    }
}
