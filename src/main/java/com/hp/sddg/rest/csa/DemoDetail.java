package com.hp.sddg.rest.csa;

import com.hp.sddg.main.Ansi;
import com.hp.sddg.rest.openstack.OpenStack;
import com.hp.sddg.utils.TimeUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by panuska on 12.9.14.
 */
public abstract class DemoDetail {
    protected final String name;
    protected final String serverId;
    protected final String size;
    protected final OpenStack openStack;
    protected List<SaveState> state;
    protected long stateSince;
    protected long taskStarted;

    protected String newImageVolumeName;
    protected String newImageSnapshotId;

    public enum SaveState {
        Not_Started, Powering_Off, Creating_Image, Detaching_Volume, Cloning_Volume, Snapshoting_Volume, Attaching_Volume,
        Deleting_Snapshot, Deleting_Volume, Backing_Volume_Up, Creating_Volume, Restoring_Volume, Deleting_Backup, Image_Saved,
        Volume_Saved, Powering_On, Task_Finished
    }

    public static DemoDetail getDemoDetail(String instanceVolumeId, String name, String serverId, String volumeSnapshotId, String size, OpenStack openStack) {
        if ("NOVOLUME".equals(instanceVolumeId) || "VOLNOTSET".equals(instanceVolumeId) || "NONE".equals(instanceVolumeId)) {
            String imageId = openStack.getImageId(serverId);
            String imageName = openStack.getImageName(imageId);
            return new DemoImage(name, serverId, imageId, imageName, size, openStack);
        } else {
            String volumeSnapshotName = openStack.getVolumeSnapshotName(volumeSnapshotId);
            return new DemoVolume(name, serverId, instanceVolumeId, volumeSnapshotId, volumeSnapshotName, size, openStack);
        }
    }

    protected DemoDetail(String name, String serverId, String size, OpenStack openStack) {
        this.name = name;
        this.serverId = serverId;
        this.size = size;
        this.openStack = openStack;
        this.state = new LinkedList<>();
        setState(SaveState.Not_Started);
    }

    public String getName() {
        return name;
    }

    public String getSize() {
        return size;
    }

    public String getServerId() {
        return serverId;
    }

    public List<SaveState> getStates() {
        return state;
    }

    public String getStateString() {
        String value = "";
        int i = 0;
        for (SaveState oneState : state) {
            if (i == state.size()-1) {        // the very last state
                value = value + Ansi.BOLD + Ansi.CYAN +oneState.toString()+ Ansi.RESET;
            } else {
                value = value + oneState.toString() + ", ";
            }
            i++;
        }
        if (state.get(state.size()-1) == SaveState.Task_Finished) {
            return value + " ("+ TimeUtils.getTimeDifference(taskStarted, stateSince)+")";
        } else {
            return value + " ("+ TimeUtils.getTimeDifference(stateSince)+")";
        }
    }

    public void setState(SaveState state) {
        this.state.add(state);
        this.stateSince = System.currentTimeMillis();
        if (state == SaveState.Powering_Off) {              // todo this is not safe; the condition should not depend on a concrete state
            this.taskStarted = System.currentTimeMillis();
        }
    }

    @Override
    public String toString() {
        return "DemoDetail{" +
                "name='" + name + '\'' +
                ", serverId='" + serverId + '\'' +
                '}';
    }

    public String getNewImageVolumeName() {
        return newImageVolumeName;
    }

    public void setNewImageVolumeName(String newImageVolumeName) {
        this.newImageVolumeName = newImageVolumeName;
    }

    public abstract void save();

    public abstract String toConsoleString();

    public abstract String getType();
}
