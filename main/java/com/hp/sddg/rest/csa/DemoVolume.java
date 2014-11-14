package com.hp.sddg.rest.csa;

import com.hp.sddg.main.Ansi;
import com.hp.sddg.main.Offering;
import com.hp.sddg.rest.openstack.NotCreatedException;
import com.hp.sddg.rest.openstack.OpenStack;
import com.hp.sddg.rest.openstack.VolumeDetails;
import org.apache.log4j.Logger;

/**
 * Created by panuska on 23.9.14.
 */
public class DemoVolume extends DemoDetail {
    private static Logger log = Logger.getLogger(DemoVolume.class.getName());
    private final String instanceVolumeId;
    private final String volumeSnapshotId;
    private final String volumeSnapshotName;

    DemoVolume(String name, String serverId, String instanceVolumeId, String volumeSnapshotId, String volumeSnapshotName, String size, OpenStack openStack) {
        super(name, serverId, size, openStack);
        this.instanceVolumeId = instanceVolumeId;
        this.volumeSnapshotId = volumeSnapshotId;
        this.volumeSnapshotName = volumeSnapshotName;
    }

    public String getInstanceVolumeId() {
        return instanceVolumeId;
    }

    public String getVolumeSnapshotId() {
        return volumeSnapshotId;
    }

    @Override
    public String toString() {
        return "Volume "+name+" ("+size+")";
    }

    @Override
    public void save(Offering offering) {
        log.debug(name + ": Detaching volume " + getInstanceVolumeId());
        VolumeDetails details = openStack.getVolumeDetails(getInstanceVolumeId());
        setState(SaveState.Detaching_Volume);
        openStack.detachVolume(serverId, getInstanceVolumeId());
        log.debug(name + ": Volume detached " + getInstanceVolumeId());
//        String newVolumeName = openStack.getUniqueName(false, offering.getNewName(), name);
        String newVolumeName = newImageVolumeName;                                                //todo

        log.debug(name + ": New volume name " + newVolumeName + "; Saving volume: " + getInstanceVolumeId());
        String newVolumeId;
        try {
            setState(SaveState.Cloning_Volume);
            newVolumeId = openStack.cloneVolume(details, newVolumeName);
            log.debug(name + ": Volume cloned " + newVolumeId + "; going to take a snapshot");
        } catch (NotCreatedException e) {
            newVolumeId = e.getId();
            log.debug(name + ": Error when cloning the volume; new volume in error " + newVolumeId + "; trying to clone from backup/restore");

            newVolumeId = failOver(openStack, details, newVolumeId, newVolumeName);
        }

        String snapshotId;
        try {
            setState(SaveState.Snapshoting_Volume);
            snapshotId = openStack.takeVolumeSnapshot(newVolumeId, newVolumeName);
            newImageSnapshotId = snapshotId;
            log.debug(name+": Snapshot taken "+snapshotId+"; going to attach volume "+getInstanceVolumeId());
        } catch (NotCreatedException e) {
            snapshotId = e.getId();
            log.debug(name + ": Error when taking the volume snapshot; the snapshot in error " + snapshotId + "; trying to clone from backup/restore");
            setState(SaveState.Deleting_Snapshot);
            openStack.deleteVolumeSnapshot(snapshotId);
            log.debug(name + ": Snapshot deleted " + snapshotId);

            newVolumeId = failOver(openStack, details, newVolumeId, newVolumeName);

            setState(SaveState.Snapshoting_Volume);
            snapshotId = openStack.takeVolumeSnapshot(newVolumeId, newVolumeName);
            newImageSnapshotId = snapshotId;
            log.debug(name+": Snapshot taken "+snapshotId);
        }
        setState(SaveState.Attaching_Volume);
        openStack.attachVolume(serverId, getInstanceVolumeId());
        log.debug(name + ": Volume attached; going to power on the machine");
        setState(SaveState.Volume_Saved);
//        log.info(name+": Replacing "+getVolumeSnapshotId()+" with "+snapshotId);
//        offering.replace(getVolumeSnapshotId(), snapshotId);
        log.debug(name + ": Volume cloned: " + newVolumeName);
    }

    private String failOver(OpenStack openStack, VolumeDetails details, String newVolumeId, String newVolumeName) {
        setState(SaveState.Deleting_Volume);
        openStack.deleteVolume(newVolumeId);
        log.debug(name+": Volume deleted "+newVolumeId+"; going to back the volume up");
        setState(SaveState.Backing_Volume_Up);
        String backupId = openStack.backupVolume(getInstanceVolumeId(), "temp-backup-"+newVolumeName, newVolumeName);
        log.debug(name+": Volume backed up "+backupId+"; going to create an empty volume");
        setState(SaveState.Creating_Volume);
        newVolumeId = openStack.createVolume(details, newVolumeName);
        log.debug(name+": Volume created "+newVolumeId+"; going to restore the backup into the new volume");
        setState(SaveState.Restoring_Volume);
        openStack.restoreVolume(newVolumeId, backupId);
        log.debug(name+": Volume restored "+newVolumeId+"; going to delete the backup");
        setState(SaveState.Deleting_Backup);
        openStack.deleteBackup(backupId);
        log.debug(name+": Backup deleted "+backupId);
        return newVolumeId;
    }

    @Override
    public String getStateString() {
        String value = super.getStateString();
        if (newImageSnapshotId != null) value = "saved volume snapshot ID: "+ Ansi.BOLD + Ansi.CYAN + newImageSnapshotId + Ansi.RESET + "; "+value;
        return value;
    }

    @Override
    public String toConsoleString() {
//        return "Name:"+name+" Server: "+serverId+" Volume: "+volumeSnapshotName+(newImageVolumeName == null ? " will not be saved" : (" will be saved as "+newImageVolumeName));
        return "Demo "+ Ansi.BOLD + Ansi.CYAN +name+ Ansi.RESET +" (server: "+ Ansi.BOLD + Ansi.CYAN +serverId+ Ansi.RESET +(newImageVolumeName == null ? ") will not be saved" : (") will be saved as volume (and snapshot) "+ Ansi.BOLD + Ansi.CYAN +newImageVolumeName))+ Ansi.RESET;
    }

    @Override
    public String getType() {
        return "volume";
    }
}
