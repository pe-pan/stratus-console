package com.hp.sddg.rest.openstack.entities;

import com.hp.sddg.main.Ansi;
import com.hp.sddg.rest.AuthenticatedClient;
import com.hp.sddg.rest.ContentType;
import com.hp.sddg.rest.HttpResponse;
import com.hp.sddg.rest.IllegalRestStateException;
import com.hp.sddg.rest.common.entities.Column;
import com.hp.sddg.rest.common.entities.Entity;
import com.hp.sddg.rest.common.entities.EntityHandler;
import com.hp.sddg.rest.openstack.OpenStack;
import com.hp.sddg.xml.XmlFile;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sun.net.www.protocol.http.HttpURLConnection;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by panuska on 26.9.14.
 */
public abstract class OpenStackEntityHandler extends EntityHandler {

    protected static AuthenticatedClient client;
    protected String endpoint;

    public static void setClient(AuthenticatedClient client) {
        OpenStackEntityHandler.client = client;
    }

    protected OpenStackEntityHandler() {
        super();
        columns.add(new Column("id"));
        endpoint = OpenStack.STRATUS_STORAGE_ENDPOINT;
    }

    @Override
    public Entity get(String id) {
        if (id == null) throw new NullPointerException("ID of "+context+" must not be null");
        HttpResponse response;
        try {
            response = client.doGet(endpoint+"/"+this.context+"s/"+id);
        } catch (IllegalRestStateException e) {
            if (e.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) { // if uri not found, the entity might have been deleted
                lastRefresh = System.currentTimeMillis();  //todo hack: last refresh should not be set here
                return null;
            }
            throw e;
        }
        XmlFile xml = new XmlFile(response.getResponse());
        Node node = xml.getElementNode(context);
        lastRefresh = System.currentTimeMillis();
        return newEntity(node);
    }

    public List<Entity> list(boolean enforce) {
        if (lastEntities != null && !enforce) {
            resetFilteredEntities();
            return lastEntities;
        }
        String marker = "";
        List<Entity> returnValue = new LinkedList<>();
        NodeList list;
        for (;;){
            HttpResponse response = client.doGet(endpoint+"/"+this.context+"s/detail"+marker);
            XmlFile xml = new XmlFile(response.getResponse());
            list = xml.getElementNodes("/"+context+"s/"+context);
            if (list.getLength() <= 0) {
                break;
            }

            for (int i = 0; i < list.getLength(); i++) {
                Entity entity = newEntity(list.item(i));
                returnValue.add(entity);
            }
            if ("snapshot".equals(context)) break; //todo bug in HPCS REST API; marker parameter does not work for snapshots
            marker = "?marker="+returnValue.get(returnValue.size()-1).getId();
            if (list.getLength() == 1000) {
                System.out.println("At least "+Ansi.BOLD+returnValue.size()+Ansi.RESET+" "+context+"s");
            }
        }

        lastRefresh = System.currentTimeMillis();
        lastEntities = returnValue;
        resetFilteredEntities();// every list resets also the filter
        return returnValue;
    }

    public Entity update(Entity entity) {
        HttpResponse response = client.doPut(endpoint+"/"+this.context+"s/"+entity.getId(), entity.toXml(), ContentType.XML_XML);
        XmlFile xml = new XmlFile(response.getResponse());
        Node node = xml.getElementNode(context);
        entity.clearDirty();
        return newEntity(node);
    }

    public void delete(Entity entity) {
        client.doDelete(endpoint+ "/" + this.context + "s/" + entity.getId());
    }

    public void clearList() {
        lastEntities = null;
        filteredEntities = null;
    }


}
