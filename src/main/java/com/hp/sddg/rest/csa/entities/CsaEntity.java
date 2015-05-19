package com.hp.sddg.rest.csa.entities;

import com.hp.sddg.rest.common.entities.Entity;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by panuska on 2.10.14.
 */
public class CsaEntity extends Entity {
    private static Logger log = Logger.getLogger(CsaEntity.class.getName());
    protected Map<String, String> properties;
    private boolean isDirty;
    protected String json;

    public CsaEntity(Object json) {
        this.json = (String)json;
        properties = new HashMap<>();
        isDirty = false;
    }

    @Override
    protected void init(Object o) {
        this.json = (String) o;
        isDirty = false;
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
        String value = properties.get(key);
        if (value == null) {
            try {
                Object o = JsonPath.read(json, "$."+key);
                if (o instanceof JSONArray) {
                    JSONArray a = (JSONArray) o;
                    if (a.size() != 1) return null;
                    value = a.get(0).toString();
                } else {
                    value = o == null ? null : o.toString();
                }
            } catch (InvalidPathException e) {
                log.debug("Invalid Path $." + key, e);
            }
            if (value != null) {
                properties.put(key, value);
            }
        }
        if (value != null) value = value.replace('\n', ' ');    // todo remove new lines
        return value;
    }

    public String removeProperty(String key) {
        String value = properties.remove(key);
        if (value != null) isDirty = true;
        return value;
    }

    @Override
    public void setProperty(String key, String value) {
        properties.put(key, value);
        isDirty = true;
    }

    @Override
    public boolean isDirty(String key) {
        //todo not properly implemented
        return isDirty;
    }

    @Override
    public boolean isDirty() {
        return isDirty;
    }

    @Override
    public String toJson() {
        if (isDirty) {
            json = "{ "
                    +propertiesToJson()
                    + " }";
            isDirty = false;
        }
        return json;
    }

    protected String propertiesToJson() {
        Map<String, String> fields = new HashMap<>();
        StringBuilder b = new StringBuilder();
        for (String key : properties.keySet()) {
            String value = properties.get(key);
            if (key.startsWith("field_")) {
                fields.put(key, value);
            } else {
                b.append("\"").append(key).append("\" : \"").append(value).append("\", ");
            }
        }
        b.deleteCharAt(b.length()-1);  // remove the very last comma
        b.deleteCharAt(b.length()-1);

        if (!fields.isEmpty()) {
            b.append(", \"fields\" : {");
            for (String key : fields.keySet()) {
                String value = fields.get(key);
                if (value.equals("true") || value.equals("false")) {
                    b.append("\"").append(key.substring("field_".length())).append("\" : ").append(value).append(", ");
                }
                b.append("\"").append(key.substring("field_".length())).append("\" : \"").append(value).append("\", ");
            }
            b.deleteCharAt(b.length()-1);  // remove the very last comma
            b.deleteCharAt(b.length()-1);
            b.append("}");
        }

        return b.toString();
    }

    @Override
    public String toXml() {
        throw new IllegalStateException("Not implemented!");
    }
}
