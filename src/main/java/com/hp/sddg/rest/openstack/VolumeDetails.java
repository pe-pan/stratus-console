package com.hp.sddg.rest.openstack;

import com.jayway.jsonpath.JsonPath;

/**
 * Created by panuska on 17.9.14.
 */
public class VolumeDetails {

    private final String id;
    private final String name;
    private final String az;
    private final String size;
    private final String metadata;
    private final String snapshotId;

    public VolumeDetails(String id, String json) {
        this.id = id;
        az = JsonPath.read(json, "$.volume.availability_zone");
        size = JsonPath.read(json, "$.volume.size").toString();
        name = JsonPath.read(json, "$.volume.name");
        metadata = JsonPath.read(json, "$.volume.metadata").toString();
        snapshotId = JsonPath.read(json, "$.volume.snapshot_id");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAz() {
        return az;
    }

    public String getSize() {
        return size;
    }

    public String getMetadata() {
        return metadata;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    @Override
    public String toString() {
        return "VolumeDetails{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", az='" + az + '\'' +
                ", size='" + size + '\'' +
                ", metadata='" + metadata + '\'' +
                ", snapshotId='" + snapshotId + '\'' +
                '}';
    }
}
