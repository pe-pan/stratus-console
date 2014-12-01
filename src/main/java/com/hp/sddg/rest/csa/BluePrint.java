package com.hp.sddg.rest.csa;

/**
 * Created by panuska on 23.9.14.
 */
public class BluePrint {
    private final String id;
    private final String name;

    public BluePrint(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}