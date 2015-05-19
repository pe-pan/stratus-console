package com.hp.sddg.rest.csa.entities;

import com.hp.sddg.rest.ContentType;
import com.hp.sddg.rest.HttpResponse;
import com.hp.sddg.rest.common.entities.Entity;
import com.hp.sddg.rest.csa.Csa;
import com.hp.sddg.rest.common.entities.Column;

/**
 * Created by panuska on 2.10.14.
 */
public class OfferingHandler extends CsaEntityHandler {

    public OfferingHandler() {
        super();
        this.context = "offering";

        columns.add(new Column("name"));
        columns.add(new Column("description"));
        columns.add(new Column("state"));
        columns.add(new Column("@deleted"));
    }

    protected String getListJson() {
        return client.doGet(Csa.REST_API+"/service/offering/", ContentType.JSON_JSON).getResponse();
    }

    public Entity newEntity(Object o) {
        return new Offering(o);
    }

    @Override
    public String create(Entity entity) {
        HttpResponse response = client.doPost(Csa.REST_API + "/service/offering", entity.toJson());
        int index = response.getLocation().lastIndexOf('/');
        return response.getLocation().substring(index + 1);
    }

    @Override
    public Entity update(Entity entity) {
        throw new IllegalStateException("Not implemented!");
    }

    @Override
    public void delete(Entity entity) {
        throw new IllegalStateException("Not implemented!");
    }

    @Override
    public Entity get(String id) {
        String json = client.doGet(Csa.REST_API+"/service/offering/"+id, ContentType.JSON_JSON).getResponse();
        lastRefresh = System.currentTimeMillis(); // todo kind of hack (it assumes get() method is called when refreshing the list...)
        return newEntity(json);
    }
}
