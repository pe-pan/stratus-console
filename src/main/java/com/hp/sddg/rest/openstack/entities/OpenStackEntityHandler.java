package com.hp.sddg.rest.openstack.entities;

import com.hp.sddg.rest.AuthenticatedClient;
import com.hp.sddg.rest.ContentType;
import com.hp.sddg.rest.HttpResponse;
import com.hp.sddg.rest.common.entities.Column;
import com.hp.sddg.rest.common.entities.Entity;
import com.hp.sddg.rest.common.entities.EntityHandler;
import com.hp.sddg.rest.openstack.OpenStack;
import com.hp.sddg.xml.XmlFile;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
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

    public List<Entity> list(boolean enforce) {
        if (lastEntities != null && !enforce) {
            resetFilteredEntities();
            return lastEntities;
        }
        HttpResponse response = client.doGet(endpoint+"/"+this.context+"s/detail");
        XmlFile xml = new XmlFile(response.getResponse());
        NodeList list = xml.getElementNodes("/"+context+"s/"+context);
        List<Entity> returnValue = new ArrayList<>(list.getLength());

        for (int i = 0; i < list.getLength(); i++) {
            Entity entity = create(list.item(i));
            returnValue.add(entity);
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
        return create(node);
    }

    public void delete(Entity entity) {
        client.doDelete(endpoint+ "/" + this.context + "s/" + entity.getId());
    }

    public void clearList() {
        lastEntities = null;
        filteredEntities = null;
    }


}