package com.hp.sddg.rest.openstack.entities;

import com.hp.sddg.rest.common.entities.Column;
import com.hp.sddg.rest.common.entities.Entity;
import com.hp.sddg.rest.openstack.OpenStack;

/**
 * Created by panuska on 14.10.14.
 */
public class ServerHandler extends OpenStackEntityHandler {

    public ServerHandler() {
        super();
        this.context = "server";

        columns.add(new Column("name"));
        columns.add(new Column("status"));
        columns.add(new Column("address_0"));
        columns.add(new Column("address_1"));

//        changeableProperties.add("name");  // it's not possible to change name of a server

        endpoint = OpenStack.STRATUS_COMPUTE_ENDPOINT;    // todo should not overwrite a value

    }
    @Override
    protected Entity newEntity(Object object) {
        return new Server(object);
    }
}
