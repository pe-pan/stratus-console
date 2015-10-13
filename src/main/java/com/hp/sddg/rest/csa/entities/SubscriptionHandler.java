package com.hp.sddg.rest.csa.entities;

import com.hp.sddg.main.Console;
import com.hp.sddg.rest.ContentType;
import com.hp.sddg.rest.common.entities.EntityHandler;
import com.hp.sddg.rest.csa.Csa;
import com.hp.sddg.rest.common.entities.Entity;
import com.hp.sddg.rest.common.entities.Column;
import com.hp.sddg.rest.csa.DemoDetail;
import com.hp.sddg.rest.csa.DemoImage;
import com.hp.sddg.rest.csa.DemoVolume;
import org.apache.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by panuska on 2.10.14.
 */
public class SubscriptionHandler extends CsaEntityHandler {
    private static Logger log = Logger.getLogger(SubscriptionHandler.class.getName());

    private String loggedUserId;

    public SubscriptionHandler() {
        super();
        this.context = "subscription";

        columns.add(new Column("name"));
        columns.add(new Column("@self"));
        columns.add(new Column("description"));
        columns.add(new Column("ext.csa_subscription_status"));
        columns.add(new Column("ext.csa_service_offering_name"));
        columns.add(new Column("ext.csa_service_offering_id"));

    }
    private Entity getPerson(String userName) {
        List<Entity> persons = EntityHandler.getHandler("persons").list(false);
        for (Entity person : persons) {
            if (person.getProperty("userName").equals(userName)) return person;
        }
        return null;
    }

    private String interpersonatedPerson;

    public String getInterpersonatedPerson() {
        if (interpersonatedPerson == null) {
            interpersonatedPerson = client.getLoggedUserName();
        }
        return interpersonatedPerson;
    }

    public boolean setInterpersonatedPerson(String interpersonatedPerson) {
        Entity person = getPerson(interpersonatedPerson);

        if (person == null) {
            return false;
        }

        this.interpersonatedPerson = interpersonatedPerson;
        this.loggedUserId = person.getId();
        return true;
    }

    private String getLoggedUserId() {
        if (loggedUserId != null) {
            return loggedUserId;
        }
        Entity person = getPerson(getInterpersonatedPerson());
//        Entity person = getPerson(client.getLoggedUserName());

        if (person == null) {
//            throw new IllegalStateException("There is no user called "+client.getLoggedUserName()); //todo hack
            throw new IllegalStateException("There is no user called "+getInterpersonatedPerson()); //todo hack
        }
        loggedUserId = person.getId();
        return loggedUserId;
    }

    protected String getListJson() {
        return client.doGet(Csa.REST_API+"/service/subscription/person/"+getLoggedUserId(), ContentType.JSON_JSON).getResponse();
    }

    public Entity newEntity(Object o) {
        return new Subscription(o);
    }

    @Override
    public Entity update(Entity entity) {
        throw new IllegalStateException("Not implemented!");
    }

    @Override
    public void delete(Entity entity) {
        throw new IllegalStateException("Not implemented!");
    }

    public List<Entity> goTo(String token) {
        switch (token) {
            case "offerings" : return goToOfferings(token);
            case "servers" : return goToServers(token);
            case "snapshots" : return goToSnapshots(token);
            case "images" : return goToImages(token);
            default: return null;
        }
    }

    private List<Entity> goToOfferings(String token) {
        List<Entity> returnValue = new LinkedList<>();
        EntityHandler offeringHandler = EntityHandler.getHandler(token);
        for (Entity subscription : getFilteredEntities()) {
            String offeringId = subscription.getProperty("ext.csa_service_offering_id");
            returnValue.add(offeringHandler.get(offeringId));
        }
        return returnValue;
    }

    private List<Entity> goToServers(String token) {
        List<Entity> returnValue = new LinkedList<>();
        EntityHandler serverHandler = EntityHandler.getHandler(token);
        System.out.println("Collecting subscription details; please wait...");
        for (Entity subscription : getFilteredEntities()) {
            com.hp.sddg.rest.csa.Subscription sub = com.hp.sddg.rest.csa.Subscription.getSubscription(Console.csa, subscription.getId());
            List<DemoDetail> details = sub.getDemoDetails(Console.os);
            for (DemoDetail detail : details) {
                Entity server = serverHandler.get(detail.getServerId());
                log.debug("Listing server "+detail.getServerId()+"; details: "+server);
                if (server != null) {
                    returnValue.add(server);
                }
            }
        }
        return returnValue;
    }

    private List<Entity> goToSnapshots(String token) {
        List<Entity> returnValue = new LinkedList<>();
        EntityHandler snapshotHandler = EntityHandler.getHandler(token);
        System.out.println("Collecting subscription details; please wait...");
        for (Entity subscription : getFilteredEntities()) {
            com.hp.sddg.rest.csa.Subscription sub = com.hp.sddg.rest.csa.Subscription.getSubscription(Console.csa, subscription.getId());
            List<DemoDetail> details = sub.getDemoDetails(Console.os);
            for (DemoDetail detail : details) {
                if (detail instanceof DemoVolume) {
                    DemoVolume volumeDetail = (DemoVolume) detail;
                    Entity snapshot = snapshotHandler.get(volumeDetail.getVolumeSnapshotId());
                    returnValue.add(snapshot);
                }
            }
        }
        return returnValue;
    }

    private List<Entity> goToImages(String token) {
        List<Entity> returnValue = new LinkedList<>();
        EntityHandler imageHandler = EntityHandler.getHandler(token);
        System.out.println("Collecting subscription details; please wait...");
        for (Entity subscription : getFilteredEntities()) {
            com.hp.sddg.rest.csa.Subscription sub = com.hp.sddg.rest.csa.Subscription.getSubscription(Console.csa, subscription.getId());
            List<DemoDetail> details = sub.getDemoDetails(Console.os);
            for (DemoDetail detail : details) {
                if (detail instanceof DemoImage) {
                    DemoImage imageDetail = (DemoImage) detail;
                    Entity snapshot = imageHandler.get(imageDetail.getImageId());
                    returnValue.add(snapshot);
                }
            }
        }
        return returnValue;
    }

}
