package com.hp.sddg.rest.openstack.entities;

import com.hp.sddg.rest.common.entities.Column;
import com.hp.sddg.rest.common.entities.Entity;
import com.hp.sddg.rest.openstack.OpenStack;

/**
 * Created by panuska on 14.10.14.
 */
public class ImageHandler extends OpenStackEntityHandler {

    public ImageHandler() {
        super();
        this.context = "image";

        columns.add(new Column("name"));
        columns.add(new Column("status"));
        columns.add(new Column("OS-EXT-IMG-SIZE:size"));

//        changeableProperties.add("name");  // it's not possible to change name of an image

        endpoint = OpenStack.STRATUS_COMPUTE_ENDPOINT;    // todo should not overwrite a value

    }
    @Override
    protected Entity newEntity(Object object) {
        return new Image(object);
    }
}
