package com.hp.sddg.rest.openstack.entities;

import com.hp.sddg.rest.common.entities.Entity;
import org.w3c.dom.Node;

/**
 * Created by panuska on 26.9.14.
 */
public class Volume extends OpenStackEntity {

    public Volume(Object node) {
        init(node);
        context = "volume";  //todo merge with handler context
    }

}
