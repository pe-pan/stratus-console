package com.hp.sddg.rest.csa.entities;

import com.hp.sddg.rest.AuthenticatedClient;
import com.hp.sddg.rest.common.entities.Entity;
import com.hp.sddg.rest.common.entities.EntityHandler;
import com.hp.sddg.rest.common.entities.Column;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by panuska on 2.10.14.
 */
public abstract class CsaEntityHandler extends EntityHandler {
    protected static AuthenticatedClient client;

    public static void setClient(AuthenticatedClient client) {
        CsaEntityHandler.client = client;
    }

    protected CsaEntityHandler() {
        super();
        columns.add(new Column("id"));

    }
    protected abstract String getListJson();

    public List<Entity> list(boolean enforce) {
        if (lastEntities != null && !enforce) {
            resetFilteredEntities();
            return lastEntities;
        }

        String json = getListJson();
        JSONArray array = JsonPath.read(json, "$.members");
        List<Entity> returnValue = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            JSONObject o = (JSONObject) array.get(i);
            returnValue.add(newEntity(o.toJSONString()));
        }

        lastRefresh = System.currentTimeMillis();
        lastEntities = returnValue;
        resetFilteredEntities();// every list resets also the filter
        return returnValue;
    }

    public void clearList() {
        lastEntities = null;
        filteredEntities = null;
    }
}
