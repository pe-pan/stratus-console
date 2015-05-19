package com.hp.sddg.rest.openstack.entities;

import com.hp.sddg.rest.common.entities.Column;

/**
 * Created by panuska on 29.9.14.
 */
public class SnapshotHandler extends OpenStackEntityHandler {

    public SnapshotHandler() {
        super();
        this.context = "snapshot";

        columns.add(new Column("display_name"));
        columns.add(new Column("status"));
        columns.add(new Column("size"));
        columns.add(new Column("volume_id"));
        columns.add(new Column("display_description"));

        changeableProperties.add("display_name");
        changeableProperties.add("display_description");

    }

    public Snapshot newEntity(Object node) {
        return new Snapshot(node);
    }
}
