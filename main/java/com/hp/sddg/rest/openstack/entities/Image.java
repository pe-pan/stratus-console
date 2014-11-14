package com.hp.sddg.rest.openstack.entities;

/**
 * Created by panuska on 26.9.14.
 */
public class Image extends OpenStackEntity {

    public Image(Object node) {
        init(node);
        context = "image";    //todo merge with handler context
    }

}
