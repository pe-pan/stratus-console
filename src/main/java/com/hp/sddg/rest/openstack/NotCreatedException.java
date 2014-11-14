package com.hp.sddg.rest.openstack;

/**
 * Created by panuska on 17.9.14.
 */
public class NotCreatedException extends RuntimeException {
    private String id;

    public NotCreatedException(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
