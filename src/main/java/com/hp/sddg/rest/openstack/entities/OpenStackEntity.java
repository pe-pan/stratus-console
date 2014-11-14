package com.hp.sddg.rest.openstack.entities;

import com.hp.sddg.rest.common.entities.Entity;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by panuska on 2.10.14.
 */
public class OpenStackEntity extends Entity {
    protected Map<String, String> properties;
    private Set<String> dirtyProperties;

    protected OpenStackEntity() {
        dirtyProperties = new HashSet<>();
    }

    protected void init(Object o) {
        Node node = (Node)o;
        NamedNodeMap attributes = node.getAttributes();
        properties = new LinkedHashMap<>(attributes.getLength()*2);
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            properties.put(attribute.getNodeName(), attribute.getNodeValue());
        }
    }

    public String getId() {
        return properties.get("id");
    }

    public void clearDirty() {
        dirtyProperties.clear();
    }

    public String getProperty(String key) {
        String value = properties.get(key);
        if (value != null) value = value.replace('\n', ' ');    // remove new lines
        return value;
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
        dirtyProperties.add(key);
    }

    public boolean isDirty(String key) {
        return dirtyProperties.contains(key);
    }

    public boolean isDirty() {
        for (String key : properties.keySet()) {
            if (isDirty(key)) return true;
        }
        return false;
    }

    public String toJson() {
        return "{ \""+context+"\" : {"
                +propertiesToJson()
                + " } }";
    }

    /**
     * Serialized dirty properties to XML
     * @return
     */
    public String toXml() {
        return "<"+context
                +propertiesToXml()
                + "/>";
    }
    protected String propertiesToJson() {
        StringBuilder b = new StringBuilder();
        for (String key : dirtyProperties) {
            String value = properties.get(key);
            b.append("\"").append(key).append("\" : \"").append(value).append("\"");
        }
        return b.toString();
    }

    protected String propertiesToXml() {
        StringBuilder b = new StringBuilder();
        for (String key : dirtyProperties) {
            String value = properties.get(key);
            b.append(" ").append(key).append("=\"").append(value).append("\"");
        }
        return b.toString();
    }


}
