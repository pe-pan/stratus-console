package com.hp.sddg.rest.csa.entities;

import com.hp.sddg.rest.ContentType;
import com.hp.sddg.rest.common.entities.Entity;
import com.hp.sddg.rest.common.entities.EntityHandler;
import com.hp.sddg.rest.csa.Csa;
import com.hp.sddg.rest.common.entities.Column;

import java.util.List;

/**
 * Created by panuska on 2.10.14.
 */
public class PersonHandler extends CsaEntityHandler {

    public PersonHandler() {
        super();
        this.context = "person";

        columns.add(new Column("userName"));
        columns.add(new Column("ext.csa_active_subscription_count"));
    }

    private String organizationId;

    private Entity getOrganization(String name) {
        List<Entity> organizations = EntityHandler.getHandler("organizations").list(false);
        for (Entity o : organizations) {
            if (o.getProperty("name").equals(name)) return o;
        }
        return null;
    }

    private String getOrganizationId() {
        if (organizationId != null) {
            return organizationId;
        }
        Entity o = getOrganization("CSADemo");  //todo should not be built-in
        if (o == null) {
            throw new IllegalStateException("There is no organization called CSADemo"); //todo hack
        }
        organizationId = o.getId();
        return organizationId;
    }

    protected String getListJson() {
        return client.doGet(Csa.REST_API+"/person/organization/"+getOrganizationId(), ContentType.JSON_JSON).getResponse();
    }

    @Override
    protected Entity create(Object object) {
        return new Organization(object);
    }

    @Override
    public Entity update(Entity entity) {
        throw new IllegalStateException("Not implemented!");
    }

    @Override
    public void delete(Entity entity) {
        throw new IllegalStateException("Not implemented!");
    }
}
