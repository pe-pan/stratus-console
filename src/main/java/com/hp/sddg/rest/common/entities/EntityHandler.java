package com.hp.sddg.rest.common.entities;

import com.hp.sddg.main.Ansi;
import com.hp.sddg.rest.csa.entities.OfferingHandler;
import com.hp.sddg.rest.csa.entities.OrganizationHandler;
import com.hp.sddg.rest.csa.entities.PersonHandler;
import com.hp.sddg.rest.csa.entities.SubscriptionHandler;
import com.hp.sddg.rest.openstack.entities.BackupHandler;
import com.hp.sddg.rest.openstack.entities.ImageHandler;
import com.hp.sddg.rest.openstack.entities.ServerHandler;
import com.hp.sddg.rest.openstack.entities.SnapshotHandler;
import com.hp.sddg.rest.openstack.entities.VolumeHandler;
import org.apache.commons.lang.StringUtils;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by panuska on 2.10.14.
 */
public abstract class EntityHandler {

    private static Map<String, EntityHandler> handlers;
    public static void initHandlers() {
        handlers = new LinkedHashMap<>();
        final Class[] entityClasses = new Class[] {
                VolumeHandler.class, BackupHandler.class, SnapshotHandler.class, ImageHandler.class, ServerHandler.class,
                SubscriptionHandler.class, OfferingHandler.class, PersonHandler.class, OrganizationHandler.class, };
        for (Class entityClass : entityClasses) {
            try {
                EntityHandler handler = (EntityHandler) entityClass.newInstance();
                handlers.put(handler.getContexts(), handler);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public static EntityHandler getHandler(String contexts) {
        return handlers.get(contexts);
    }
    public static Set<String> getHandlerNames() {
        return handlers.keySet();
    }

    protected List<Entity> lastEntities = null;
    protected List<Entity> filteredEntities = null;
    protected long lastRefresh;
    protected List<Column> columns;

    protected EntityHandler() {
        columns = new LinkedList<>();
        changeableProperties = new HashSet<>();
    }

    public void resetColumnsSize() {
        for (Column column : columns) {  // reset column size
            column.size = column.name.length();
        }
        for (Entity entity : filteredEntities) {
            for (Column column : columns) {                            // find the biggest value
                String columnValue = entity.getProperty(column.name);
                if (columnValue != null && columnValue.length() > column.size) {
                    column.size = columnValue.length();
                }
            }
        }
    }

    public long getLastRefresh() {
        return lastRefresh;
    }

    public List<Entity> getFilteredEntities() {
        return filteredEntities;
    }

    public void setFilteredEntities(List<Entity> filteredEntities) {
        this.filteredEntities = filteredEntities;
        resetColumnsSize();
    }

    public void resetFilteredEntities() {
        if (lastEntities == null) return;
        this.filteredEntities = new LinkedList<>(this.lastEntities);
        resetColumnsSize();
    }

    protected abstract Entity newEntity(Object object); // todo hack

    protected String context = "";
    public String getContext() {
        return context;
    }

    public String getContexts() {
        return context+"s";
    }

    public Set<String> getColumnNames () {
        Set<String> columnNames = new HashSet<>();
        for (Column column : columns) {
            columnNames.add(column.name);
        }
        return columnNames;
    }

    protected Set<String> changeableProperties;
    public boolean isChangeableProperty(String key) {
        return changeableProperties.contains(key);
    }

    public Set<String> getChangeableProperties() {
        return changeableProperties;
    }

    public void printTableHeader() {

        System.out.print(Ansi.BOLD + Ansi.CYAN);
        for (Column column : columns) {
            if (column.name.length() >= column.size) {
                System.out.print(column.name.substring(0, column.size)+" ");
            } else {
                int prefSize = (column.size-column.name.length()) / 2;
                System.out.print(StringUtils.rightPad(StringUtils.leftPad(column.name, prefSize + column.name.length()), column.size + 1));
            }
        }
        System.out.println(Ansi.RESET);
    }

    public void printEntity(Entity entity) {
        for (Column column : columns) {
            String value = entity.getProperty(column.name);
            if (value == null) value = "";
            if (entity.isDirty(column.name)) {
                System.out.print(Ansi.BOLD + Ansi.GREEN);
            }
            if (value.length() >= column.size) {
                System.out.print(value.substring(0, column.size)+" ");
            } else {
                System.out.print(StringUtils.rightPad(value, column.size + 1));
            }
            System.out.print(Ansi.RESET);
        }
        System.out.println(Ansi.RESET);
    }

    public String create(Entity entity) {
        throw new IllegalStateException("Not implemented");
    }

    public abstract List<Entity> list(boolean enforce);
    public abstract Entity update(Entity entity);
    public abstract void delete(Entity entity);

    public abstract void clearList();

    public Entity get(String id) {
        throw new IllegalStateException("Not implemented");
    }

    public List<Entity> goTo(String token) {
        return null;
    }
}
