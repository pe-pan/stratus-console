package com.hp.sddg.rest.openstack.entities;

import com.hp.sddg.rest.common.entities.Column;

/**
 * Created by panuska on 29.9.14.
 */
public class BackupHandler extends OpenStackEntityHandler {

    public BackupHandler() {
        super();
        this.context = "backup";

        columns.add(new Column("name"));
        columns.add(new Column("status"));
        columns.add(new Column("size"));
        columns.add(new Column("volume_id"));
        columns.add(new Column("description"));
    }

    public Backup newEntity(Object node) {
        return new Backup(node);
    }
}
