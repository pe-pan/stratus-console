package com.hp.sddg.rest.csa.entities;

import com.hp.sddg.rest.common.entities.Entity;
import com.jayway.jsonpath.JsonPath;

/**
 * Created by panuska on 2.10.14.
 */
public class CsaEntity extends Entity {
    protected String json;

    public CsaEntity(Object json) {
        this.json = (String)json;
    }

    @Override
    protected void init(Object o) {
        this.json = (String) o;
    }

    public String getId() {
        String self = getProperty("@self");
        int index = self.lastIndexOf('/');
        return self.substring(index+1);
    }

    @Override
    public void clearDirty() {
        throw new IllegalStateException("Not implemented!");
    }

    @Override
    public String getProperty(String key) {
        if (key.equals("id")) {
            return getId();
        }
        Object value = JsonPath.read(json, "$."+key);
        return value == null ? "null" : value.toString().replace('\n', ' '); //todo remove new lines
    }

    @Override
    public void setProperty(String key, String value) {
        throw new IllegalStateException("Not implemented!");
    }

    @Override
    public boolean isDirty(String key) {
        //todo not properly implemented
        return false;
    }

    @Override
    public boolean isDirty() {
        //todo not properly implemented
        return false;
    }

    @Override
    public String toJson() {
        return json;
    }

    @Override
    public String toXml() {
        throw new IllegalStateException("Not implemented!");
    }
}
