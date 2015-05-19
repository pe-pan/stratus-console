package com.hp.sddg.rest.common.entities;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by panuska on 24.9.14.
 */
public abstract class Entity {
    protected String context;

    protected Entity() {
    }

    protected abstract void init(Object o);  //todo this is kind of hack as OpenStack and CSA entities are being initialized differently

    public abstract String getId();
    public abstract void clearDirty();
    public abstract String getProperty(String key);
    public String removeProperty(String key) {
        throw new IllegalStateException("Not implemented");
    }
    public abstract void setProperty(String key, String value);
    public abstract boolean isDirty(String key);
    public abstract boolean isDirty();
    public abstract String toJson();
    public abstract String toXml();

}
