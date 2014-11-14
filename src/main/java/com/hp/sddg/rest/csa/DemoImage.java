package com.hp.sddg.rest.csa;

import com.hp.sddg.main.Ansi;
import com.hp.sddg.main.Offering;
import com.hp.sddg.rest.openstack.OpenStack;
import org.apache.log4j.Logger;

/**
 * Created by panuska on 23.9.14.
 */
public class DemoImage extends DemoDetail {
    private static Logger log = Logger.getLogger(DemoImage.class.getName());
    private final String imageId;
    private final String imageName;

    DemoImage(String name, String serverId, String imageId, String imageName, String size, OpenStack openStack) {
        super(name, serverId, size, openStack);
        this.imageId = imageId;
        this.imageName = imageName;
    }

    public String getImageId() {
        return imageId;
    }

    @Override
    public String toString() {
        return "Image "+name+" ("+size+")";
    }

    @Override
    public void save(Offering offering) {
//        String newImageName = openStack.getUniqueName(true, offering.getNewName(), name);
        String newImageName = newImageVolumeName;                                                //todo
        log.debug(name + ": New image name" + newImageName + "; Saving image: " + imageId);
        setState(SaveState.Creating_Image);
        newImageSnapshotId = openStack.createImage(serverId, newImageName);
        setState(SaveState.Image_Saved);

//        offering.replace(imageId, newImageSnapshotId);
        log.debug(name + ": Image saved: " + newImageName);
    }

    @Override
    public String getStateString() {
        String value = super.getStateString();
        if (newImageSnapshotId != null) value = "saved image ID: "+ Ansi.BOLD + Ansi.CYAN + newImageSnapshotId + Ansi.RESET + "; "+value;
        return value;
    }

    @Override
    public String toConsoleString() {
        return "Demo "+ Ansi.BOLD + Ansi.CYAN +name+ Ansi.RESET +" (server: "+ Ansi.BOLD + Ansi.CYAN +serverId+ Ansi.RESET +"; image: "+ Ansi.BOLD + Ansi.CYAN +imageName+ Ansi.RESET +(newImageVolumeName == null ? ") will not be saved" : (") will be saved as image "+ Ansi.BOLD + Ansi.CYAN +newImageVolumeName))+ Ansi.RESET;
    }

    @Override
    public String getType() {
        return "image";
    }
}
