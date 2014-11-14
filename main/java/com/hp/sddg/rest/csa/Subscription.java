package com.hp.sddg.rest.csa;

import com.hp.sddg.rest.AuthenticatedClient;
import com.hp.sddg.rest.ContentType;
import com.hp.sddg.rest.HttpResponse;
import com.hp.sddg.rest.RestClient;
import com.hp.sddg.rest.openstack.OpenStack;
import com.hp.sddg.xml.XmlFile;
import com.jayway.jsonpath.JsonPath;
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
    private final String description;

    private final String instanceId;
    private final String catalogId;
    private final String userId;

    private final ServiceOffering serviceOffering;
    private final BluePrint bluePrint;
    private final ResourceProvider provider;

    private final AuthenticatedClient client;

    public static Subscription getSubscription(Csa csa, String subscriptionId) {
        HttpResponse response = csa.doGet(Csa.REST_API + "/service/subscription/" + subscriptionId, ContentType.JSON_JSON);
        String json = response.getResponse();

        String name = JsonPath.read(json, "$.name");
        String description = JsonPath.read(json, "$.description");
        String userId = JsonPath.read(json, "$.ext.csa_requested_by_person_id");
        String instanceId = JsonPath.read(json, "$.ext.csa_service_instance_id");

        String bluePrintId = JsonPath.read(json, "$.ext.csa_service_blueprint_id");
        String bluePrintName = JsonPath.read(json, "$.ext.csa_service_blueprint_name");
        BluePrint bluePrint = new BluePrint(bluePrintId, bluePrintName);

        String serviceOfferingId = JsonPath.read(json, "$.ext.csa_service_offering_id");
        String serviceOfferingName = JsonPath.read(json, "$.ext.csa_service_offering_name");
        ServiceOffering serviceOffering = new ServiceOffering(serviceOfferingId, serviceOfferingName);

        response = csa.doGet(Csa.REST_API +"/service/instance/"+instanceId+"/providers", ContentType.JSON_JSON);
        json = response.getResponse();
        String providerId = JsonPath.read(json, "$.providers[0].id");
        String providerName = JsonPath.read(json, "$.providers[0].name");

        String adminId = csa.getUserId("admin");  // todo built-in name

        response = csa.doGet(Csa.REST_URI +"/artifact/"+subscriptionId+"?userIdentifier="+adminId);
        XmlFile xmlFile = new XmlFile(response.getResponse());
        String computeEndpoint = xmlFile.getElementValue("/ServiceSubscription/optionModel/optionSets/options/property[name='COMPUTEENDPOINT']/values/value");
        String tenantId = xmlFile.getElementValue("/ServiceSubscription/optionModel/optionSets/options/property[name='TENANTID']/values/value");
        String catalogId = xmlFile.getElementValue("/ServiceSubscription/catalogItem/catalog/id");

        ResourceProvider provider = ResourceProvider.getResourceProvider(csa, providerId, providerName, computeEndpoint, tenantId, adminId);  //todo passing providerName, ..., adminId parameters is kind of a hack

        return new Subscription(csa, provider, bluePrint, serviceOffering, userId, catalogId, instanceId, description, name, subscriptionId);
    }

    private Subscription(AuthenticatedClient client, ResourceProvider provider, BluePrint bluePrint, ServiceOffering serviceOffering, String userId, String catalogId, String instanceId, String description, String name, String id) {
        this.client = client;
        this.provider = provider;
        this.bluePrint = bluePrint;
        this.serviceOffering = serviceOffering;
        this.userId = userId;
        this.catalogId = catalogId;
        this.instanceId = instanceId;
        this.description = description;
        this.name = name;
        this.id = id;
        log.debug("Created new "+this);
    }

    public String getCatalogId() {
        return catalogId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public String getUserId() {
        return userId;
    }

    public String getId() {
        return id;
    }

    public ServiceOffering getServiceOffering() {
        return serviceOffering;
    }

    public BluePrint getBluePrint() {
        return bluePrint;
    }

    public ResourceProvider getProvider() {
        return provider;
    }

    public OpenStack getCloudClient() {
        return OpenStack.getCloudClient(provider);
    }

    @Override
    public String toString() {
        return "Subscription{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", instanceId='" + instanceId + '\'' +
                ", catalogId='" + catalogId + '\'' +
                ", userId='" + userId + '\'' +
                ", serviceOffering=" + serviceOffering +
                ", bluePrint=" + bluePrint +
                ", provider=" + provider +
                '}';
    }

    public List<DemoDetail> getDemoDetails(OpenStack openStack) {
        HttpResponse response = client.doGet(Csa.REST_URI +"/catalog/"+catalogId+"/instance/"+instanceId+"?userIdentifier="+userId+"&resolveProperties&scope=subtree");

        XmlFile xml = new XmlFile(response.getResponse());

        List<String> instanceVolumeIds = xml.getElementValues("/ServiceInstance/componentRoot//componentChild[property[name='ATTACHEDVOLUMEID']]/property[name='ATTACHEDVOLUMEID']/values/value");
        List<String> demoNames         = xml.getElementValues("/ServiceInstance/componentRoot//componentChild[property[name='ATTACHEDVOLUMEID']]/property[name='DEMONAME']/values/value");
        List<String> serverIds         = xml.getElementValues("/ServiceInstance/componentRoot//componentChild[property[name='ATTACHEDVOLUMEID']]/property[name='Server ID']/values/value");
//        List<String> volumeSnapshotIds = xml.getElementValues("/ServiceInstance/componentRoot//componentChild[property[name='ATTACHEDVOLUMEID']]/property[name='VOLUMEREF']/values/value");
        List<String> sizes             = xml.getElementValues("/ServiceInstance/componentRoot//componentChild[property[name='ATTACHEDVOLUMEID']]/property[name='SIZEREF']/values/value");

        assert(instanceVolumeIds.size() == demoNames.size());
        assert(instanceVolumeIds.size() == serverIds.size());
//        assert(instanceVolumeIds.size() == volumeSnapshotIds.size());

        List<DemoDetail> returnValue = new LinkedList<>();

        for (int i = 0; i < instanceVolumeIds.size(); i++) {
//            DemoDetail detail = DemoDetail.getDemoDetail(instanceVolumeIds.get(i), demoNames.get(i), serverIds.get(i), volumeSnapshotIds.get(i), sizes.get(i), openStack);
            DemoDetail detail = DemoDetail.getDemoDetail(instanceVolumeIds.get(i), demoNames.get(i), serverIds.get(i), null, sizes.get(i), openStack);
            log.debug(detail);
            if (detail instanceof DemoVolume) { // move images to the end of the list
                returnValue.add(0, detail);
            } else {
                returnValue.add(detail);
            }
        }

        return returnValue;
    }


}
