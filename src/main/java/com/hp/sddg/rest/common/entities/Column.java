package com.hp.sddg.rest.common.entities;

/**
 * Created by panuska on 29.9.14.
 */
public class Column {
    public String name;
    public int size;
    public boolean sorted;

    public Column(String name, int size, boolean sorted) {
        this.name = name;
        this.size = size;
        this.sorted = sorted;
    }

    public Column(String name) {
        this.name = name;
    }
}
