package com.hp.sddg.rest.openstack.entities;

import com.hp.sddg.rest.common.entities.Column;

/**
 * Created by panuska on 29.9.14.
 */
public class VolumeHandler extends OpenStackEntityHandler {

    public VolumeHandler() {
        super();
        this.context = "volume";

        columns.add(new Column("name"));
        columns.add(new Column("status"));
        columns.add(new Column("size"));
        columns.add(new Column("description"));

        changeableProperties.add("name");
        changeableProperties.add("description");

    }

    public Volume newEntity(Object node) {
        return new Volume(node);
    }
}
