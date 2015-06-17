package com.hp.sddg.rest.openstack.entities;

import com.hp.sddg.rest.common.entities.Column;
import com.hp.sddg.rest.common.entities.Entity;
import com.hp.sddg.rest.common.entities.EntityHandler;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by panuska on 29.9.14.
 */
public class SnapshotHandler extends OpenStackEntityHandler {

    public SnapshotHandler() {
        super();
        this.context = "snapshot";

        columns.add(new Column("name"));
        columns.add(new Column("status"));
        columns.add(new Column("size"));
        columns.add(new Column("volume_id"));
        columns.add(new Column("description"));

        changeableProperties.add("name");
        changeableProperties.add("description");

    }

    public Snapshot newEntity(Object node) {
        return new Snapshot(node);
    }

    public List<Entity> goTo(String token) {
        switch (token) {
            case "volumes" : return goToVolumes(token);
            default: return null;
        }
    }

    private List<Entity> goToVolumes(String token) {
        List<Entity> returnValue = new LinkedList<>();
        EntityHandler volumeHandler = EntityHandler.getHandler(token);
        for (Entity snapshot : getFilteredEntities()) {
            String volumeId = snapshot.getProperty("volume_id");
            Entity volume = volumeHandler.get(volumeId);
            returnValue.add(volume);
        }
        return returnValue;
    }
}
