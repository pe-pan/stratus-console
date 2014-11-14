package com.hp.sddg.rest.openstack.entities;

import com.hp.sddg.rest.common.entities.Column;

/**
 * Created by panuska on 29.9.14.
 */
public class VolumeHandler extends OpenStackEntityHandler {

    public VolumeHandler() {
        super();
        this.context = "volume";

        columns.add(new Column("display_name"));
        columns.add(new Column("status"));
        columns.add(new Column("size"));
        columns.add(new Column("display_description"));

        changeableProperties.add("display_name");
        changeableProperties.add("display_description");

    }

    public Volume create(Object node) {
        return new Volume(node);
    }
}
