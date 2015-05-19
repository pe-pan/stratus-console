package com.hp.sddg.main;

import com.hp.sddg.rest.csa.DemoDetail;
import com.hp.sddg.rest.openstack.OpenStack;
import org.apache.log4j.Logger;

/**
 * Created by panuska on 19.9.14.
 */
public class CloneDemo extends Thread implements Runnable {
    private static Logger log = Logger.getLogger(CloneDemo.class.getName());

    private final DemoDetail demo;
    private final OpenStack openStack;

    public CloneDemo(DemoDetail demo, OpenStack openStack) {
        this.demo = demo;
        this.openStack = openStack;
    }

    public DemoDetail getDemo() {
        return demo;
    }

    @Override
    public void run() {
        log.debug(demo.getName()+" :Processing "+demo.getName());

        demo.setState(DemoDetail.SaveState.Powering_Off);
        openStack.powerMachineOff(demo.getServerId());
        log.debug(demo.getName() + " :Machine powered off " + demo.getServerId());
        demo.save();
/*
        if (demo.getInstanceVolumeId().equals("NOVOLUME") || demo.getInstanceVolumeId().equals("VOLNOTSET")) {
            String imageId = openStack.getImageId(demo.getServerId());
            String newImageName = openStack.getUniqueName(true, offering.getNewName(), demo.getName());
            log.info(demo.getName()+":New image name"+newImageName+"; Saving image: " + imageId);
            String newImageId = openStack.createImage(demo.getServerId(), newImageName);
            offering.replace(imageId, newImageId);
            log.info(demo.getName()+":Image saved: " + newImageName);
        } else {
            log.info(demo.getName()+":Detaching volume "+demo.getInstanceVolumeId());
            VolumeDetails details = openStack.getVolumeDetails(demo.getInstanceVolumeId());
            openStack.detachVolume(demo.getServerId(), demo.getInstanceVolumeId());
            log.info(demo.getName()+":Volume detached "+demo.getInstanceVolumeId());
            String newVolumeName = openStack.getUniqueName(false, offering.getNewName(), demo.getName());
            log.info(demo.getName()+":New volume name "+newVolumeName+"; Saving volume: " + demo.getInstanceVolumeId());
            String newVolumeId;
            try {
                newVolumeId = openStack.cloneVolume(details, newVolumeName);
                log.info(demo.getName()+":Volume cloned "+newVolumeId+"; going to take a snapshot");
            } catch (NotCreatedException e) {
                newVolumeId = e.getId();
                log.info(demo.getName()+":Error when cloning the volume; new volume in error "+newVolumeId+"; trying to clone from backup/restore");

                newVolumeId = failOver(openStack, demo, details, newVolumeId, newVolumeName);
            }

            String snapshotId;
            try {
                snapshotId = openStack.takeVolumeSnapshot(newVolumeId, newVolumeName);
                log.info(demo.getName()+":Snapshot taken "+snapshotId+"; going to attach volume "+demo.getInstanceVolumeId());
            } catch (NotCreatedException e) {
                snapshotId = e.getId();
                log.info(demo.getName()+":Error when taking the volume snapshot; the snapshot in error "+snapshotId+"; trying to clone from backup/restore");
                openStack.deleteVolumeSnapshot(snapshotId);
                log.info(demo.getName()+":Snapshot deleted "+snapshotId);

                newVolumeId = failOver(openStack, demo, details, newVolumeId, newVolumeName);

                snapshotId = openStack.takeVolumeSnapshot(newVolumeId, newVolumeName);
                log.info(demo.getName()+":Snapshot taken "+snapshotId);
            }

            openStack.attachVolume(demo.getServerId(), demo.getInstanceVolumeId());
            log.info(demo.getName()+":Volume attached; going to power on the machine");

            log.info(demo.getName()+":Replacing "+demo.getVolumeSnapshotId()+" for "+snapshotId);
            offering.replace(demo.getVolumeSnapshotId(), snapshotId);
            log.info(demo.getName()+":Volume cloned: "+newVolumeName);
        }
*/
        demo.setState(DemoDetail.SaveState.Powering_On);
        openStack.powerMachineOnAsync(demo.getServerId());
        log.debug(demo.getName() + " :Machine powered on");
        demo.setState(DemoDetail.SaveState.Task_Finished);
    }

/*
    private String failOver(OpenStack openStack, DemoDetail demo, VolumeDetails details, String newVolumeId, String newVolumeName) {
        openStack.deleteVolume(newVolumeId);
        log.info(demo.getName()+":Volume deleted "+newVolumeId+"; going to back the volume up");
        String backupId = openStack.backupVolume(demo.getInstanceVolumeId(), "temp-backup-"+newVolumeName, newVolumeName);
        log.info(demo.getName()+":Volume backed up "+backupId+"; going to create an empty volume");
        newVolumeId = openStack.createVolume(details, newVolumeName);
        log.info(demo.getName()+":Volume created "+newVolumeId+"; going to restore the backup into the new volume");
        openStack.restoreVolume(newVolumeId, backupId);
        log.info(demo.getName()+":Volume restored "+newVolumeId+"; going to delete the backup");
        openStack.deleteBackup(backupId);
        log.info(demo.getName()+":Backup deleted "+backupId);
        return newVolumeId;
    }
*/

}
