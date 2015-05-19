package com.hp.sddg.rest.openstack;

import com.hp.sddg.main.Ansi;
import com.hp.sddg.rest.AuthenticatedClient;
import com.hp.sddg.rest.ContentType;
import com.hp.sddg.rest.HttpResponse;
import com.hp.sddg.rest.IllegalRestStateException;
import com.hp.sddg.rest.csa.DemoDetail;
import com.hp.sddg.rest.csa.DemoImage;
import com.hp.sddg.rest.csa.ResourceProvider;
import com.hp.sddg.xml.XmlFile;
import com.jayway.jsonpath.JsonPath;
import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by panuska on 11.9.14.
 */
public class OpenStack extends AuthenticatedClient {
    private static Logger log = Logger.getLogger(OpenStack.class.getName());

    public static final String STRATUS_STORAGE_ENDPOINT = "https://region-b.geo-1.block.hpcloudsvc.com/v1/63404451948086";
    public static final String STRATUS_COMPUTE_ENDPOINT = "https://region-b.geo-1.compute.hpcloudsvc.com/v2/63404451948086";

    private final String url;
    private String username;
    private String password;
    private final String tenantId;
    private final String computeEndpoint;

    public OpenStack(String url, String username, String password, String tenantId, String computeEndpoint) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.tenantId = tenantId;
        this.computeEndpoint = computeEndpoint;
        openStack = this;
    }

    private static OpenStack openStack;

    public static OpenStack getCloudClient(ResourceProvider provider) {
        return new OpenStack(provider.getUrl(), provider.getUsername(), provider.getPassword(), provider.getTenantId(), provider.getComputeEndpoint());
    }

    public static OpenStack getOpenStack() {
        return openStack;
    }

    public void authenticate() {
        client.clearCustomHeader();
        String data = "{\"auth\": {\"tenantId\": \""+tenantId+"\", \"passwordCredentials\": {\"username\": \""+username+"\", \"password\": \""+password+"\"}}}";
        HttpResponse response = client.doPost(url+"/v2.0/tokens", data);
        String token = JsonPath.read(response.getResponse(), "$.access.token.id");
        client.setCustomHeader("X-Auth-Token", token);
        log.debug("OpenStack user " + username + " authenticated");
        System.out.println("User " + Ansi.BOLD+Ansi.CYAN + username + Ansi.RESET+ " authenticated to OpenStack");
    }

    public String getLoggedUserName() {
        return username;   //todo this should return null if not authenticated yet
    }

    public AuthenticatedClient getClient() {
        return this;
    }

    public synchronized String getImageId(String serverId) {
        return getValueTemp("/servers/" + serverId, "/server/image/@id");
    }

    public synchronized String createImage(String serverId, String imageName) {

//        return doBlockingOperation(
//                new Status() {
//                    @Override
//                    public String getStatus(String... inputs) {
//                        return getImageStatus(inputs[0]);
//                    }
//                }, "ACTIVE", false, new Action() {
//                    @Override
//                    public String doOp(String... inputs) {
//                        final String imageName = inputs[1];
//                        final String serverId = inputs[0];
//                        final String data = "{\n" +
//                                "    \"createImage\" : {\n" +
//                                "        \"name\" : \"" + imageName + "\",\n" +
//                                "        \"metadata\": {\n" +
//                                "        }\n" +
//                                "    }\n" +
//                                "}";
//                        client.setCustomHeader("X-Auth-Token", token);
//                        HttpResponse response = client.doPost(computeEndpoint + "/servers/" + serverId + "/action", data);
//                        int index = response.getLocation().lastIndexOf('/');
//                        return response.getLocation().substring(index + 1);
//                    }
//                }, 30, 60, new IllegalStateException("Image not saved " + serverId + "; " + imageName), serverId, imageName
//        );

        final String data = "{\n" +
                "    \"createImage\" : {\n" +
                "        \"name\" : \""+imageName+"\",\n" +
                "        \"metadata\": {\n" +
                "        }\n" +
                "    }\n" +
                "}";
        HttpResponse response = doPost(computeEndpoint+"/servers/"+serverId+"/action", data);
        int index = response.getLocation().lastIndexOf('/');
        String imageId = response.getLocation().substring(index + 1);
//        for(int i = 0; i < 30; i++) {
        for(;;) {
            String status = getImageStatus(imageId);
            if (status.equals("ACTIVE")) {
                break;
            }
            try {
                wait(60 * 1000);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
        for(;;) {    // wait for server to be back in valid state
            String status = openStack.getServerStatus(serverId);
            if (status.equals("SHUTOFF")) {
                return imageId;
            }
            try {
                wait(10 * 1000);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
//        throw new IllegalStateException("Image not saved "+serverId+"; "+imageName);
    }


    /**
     * Power machines on. Return false if already in ACTIVE state; true otherwise.
     * @param serverId the machine to switch off.
     * @return false if no action taken.
     */
    public synchronized boolean powerMachineOn(String serverId) {
        String status = getServerStatus(serverId);
        if (status.equals("ACTIVE")) {
            return false;
        }
        doPost(computeEndpoint+"/servers/"+serverId+"/action", "{\"os-start\": null}");
        return true;
    }

    /**
     * Powers machine off. Returns false if already in SHUTOFF state; true otherwise.
     * @param serverId the machine to switch off.
     * @return false if no action taken.
     */
    public synchronized boolean powerMachineOff(String serverId) {
        String status = getServerStatus(serverId);
        if (status.equals("SHUTOFF")) {
            return false;
        }
        doPost(computeEndpoint+"/servers/"+serverId+"/action", "{\"os-stop\": null}");
        return true;
    }

    public synchronized void powerMachineOffSync(String serverId) {
        if (!powerMachineOff(serverId)) {
            return;
        }

//        for(int i = 0; i < 30; i++) {
        for(;;) {
            String status = getServerStatus(serverId);
            if (status.equals("SHUTOFF")) {
                return;
            }
            try {
                wait(15 * 1000);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
//        throw new IllegalStateException("Machine not switched off "+serverId);
    }

    public synchronized void detachVolume(String serverId, String volumeId) {
        String status = getVolumeStatus(volumeId);
        if (status.equals("available")) {
            return;
        }
        doDelete(computeEndpoint + "/servers/" + serverId + "/os-volume_attachments/" + volumeId);
//        for(int i = 0; i < 30; i++) {
        for(;;) {
            status = getVolumeStatus(volumeId);
            if (status.equals("available")) {
                return;
            }
            try {
                wait(5 * 1000);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
//        throw new IllegalStateException("Volume not detached "+serverId+"; "+volumeId);
    }

    public synchronized void attachVolume(String serverId, String volumeId) {
        String status = getVolumeStatus(volumeId);
        if (status.equals("in-use")) {
            return;
        }
        final String data = "{\n" +
                "    \"volumeAttachment\": {\n" +
                "        \"volumeId\": \""+volumeId+"\"\n" +
                "    }\n" +
                "}";
        doPost(computeEndpoint + "/servers/" + serverId + "/os-volume_attachments", data);
        for(;;) {
//        for(int i = 0; i < 30; i++) {
            status = getVolumeStatus(volumeId);
            if (status.equals("in-use")) {
                return;
            }
            try {
                wait(5 * 1000);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
//        throw new IllegalStateException("Volume not attached "+serverId+"; "+volumeId);
    }

    public synchronized VolumeDetails getVolumeDetails(String volumeId) {
        HttpResponse response = doGet(STRATUS_STORAGE_ENDPOINT + "/volumes/" + volumeId, ContentType.JSON_JSON);
        String json = response.getResponse();
        VolumeDetails details = new VolumeDetails(volumeId, json);
        log.debug(details);
        return details;
    }

    public synchronized String cloneVolume (VolumeDetails details, String newVolumeName) throws NotCreatedException {
        final String data = "{\n" +
                "    \"volume\":{\n" +
                "        \"availability_zone\": \""+details.getAz()+"\",\n" +
                "        \"size\":\""+details.getSize()+"\", \n" +
                "        \"display_description\": \""+"Clone of "+details.getName()+"\", \n" +
                "        \"display_name\": \""+newVolumeName+"\",\n" +
                "        \"source_volid\": \""+details.getId()+"\",\n" +
                "        \"bootable\": \"true\",\n" +
                "        \"metadata\":"+details.getMetadata()+"\n" +
                "    }\n" +
                "}";
        HttpResponse response = doPost(STRATUS_STORAGE_ENDPOINT + "/volumes", data);
        String newVolumeId = JsonPath.read(response.getResponse(),"$.volume.id");
        for(;;) {
//        for(int i = 0; i < 30; i++) {
            String status = getVolumeStatus(newVolumeId);
            if (status.equals("error")) {
                throw new NotCreatedException(newVolumeId);
            }
            if (status.equals("available")) {
                return newVolumeId;
            }
            try {
                wait(60 * 1000);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
//        throw new IllegalStateException("Volume not cloned "+details);
    }

    public synchronized String takeVolumeSnapshot (String volumeId, String name) throws NotCreatedException {
        final String data = "{\n" +
                "    \"snapshot\": {\n" +
                "        \"display_name\": \""+name+"\",\n" +
                "        \"display_description\": \""+name+"\",\n" +
                "        \"volume_id\": \""+volumeId+"\"\n" +
                "    }\n" +
                "}";
        HttpResponse response = doPost(STRATUS_STORAGE_ENDPOINT + "/snapshots", data);
        String snapshotId = JsonPath.read(response.getResponse(),"$.snapshot.id");
        for(;;) {
//        for(int i = 0; i < 30; i++) {
            String status = getSnapshotStatus(snapshotId);
            if (status.equals("error")) {
                throw new NotCreatedException(snapshotId);
            }
            if (status.equals("available")) {
                return snapshotId;
            }
            try {
                wait(30 * 1000);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
//        throw new IllegalStateException("Snapshot not taken "+volumeId+"; "+name);
    }

    public synchronized String backupVolume (String volumeId, String name, String description) {
        final String data = "{\n" +
                "    \"backup\": {\n" +
                "        \"description\": \""+description+"\", \n" +
                "        \"name\": \""+name+"\", \n" +
                "        \"volume_id\": \""+volumeId+"\"\n" +
                "    }\n" +
                "}";
        HttpResponse response = doPost(STRATUS_STORAGE_ENDPOINT + "/backups", data);
        String backupId = JsonPath.read(response.getResponse(),"$.backup.id");
        for(;;) {
//        for(int i = 0; i < 60; i++) {
            String status = getBackupStatus(backupId);
            if (status.equals("error")) {
                throw new NotCreatedException(backupId);
            }
            if (status.equals("available")) {
                return backupId;
            }
            try {
                wait(600 * 1000);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
//        throw new IllegalStateException("Backup not created "+volumeId+"; "+name);
    }

    public synchronized void deleteVolume(String volumeId) {
        doDelete(STRATUS_STORAGE_ENDPOINT + "/volumes/" + volumeId);
    }

    public synchronized void deleteBackup(String backupId) {
        doDelete(STRATUS_STORAGE_ENDPOINT + "/backups/" + backupId);
    }

    public synchronized void deleteVolumeSnapshot (String snapshotId) {
        doDelete(STRATUS_STORAGE_ENDPOINT + "/snapshots/"+snapshotId);
        for(;;) {
//        for(int i = 0; i < 30; i++) {
            try {
                String status = getSnapshotStatus(snapshotId);
            } catch (IllegalRestStateException e) {
                // snapshot deleted
                return;
            }
            try {
                wait(10 * 1000);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
//        throw new IllegalStateException("Snapshot not deleted "+snapshotId);
    }

    public synchronized String createVolume (VolumeDetails details, String name) {
        final String data = "{\n" +
                "    \"volume\":{\n" +
                "        \"availability_zone\": \""+details.getAz()+"\",\n" +
                "        \"size\":\""+details.getSize()+"\", \n" +
                "        \"display_description\": \"Via restore/backup - "+details.getName()+"\", \n" +
                "        \"display_name\": \""+name+"\",\n" +
                "        \"bootable\": \"true\",\n" +
                "        \"metadata\":"+details.getMetadata()+"\n" +
                "    }\n" +
                "}";
        HttpResponse response = doPost(STRATUS_STORAGE_ENDPOINT + "/volumes", data);
        String volumeId = JsonPath.read(response.getResponse(),"$.volume.id");
        for(;;) {
//        for(int i = 0; i < 30; i++) {
            String status = getVolumeStatus(volumeId);
            if (status.equals("error")) {
                throw new NotCreatedException(volumeId);
            }
            if (status.equals("available")) {
                return volumeId;
            }
            try {
                wait(60 * 1000);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
//        throw new IllegalStateException("Volume not created "+details);
    }

    public synchronized String restoreVolume (String volumeId, String backupId) {
        final String data = "{\n" +
                "    \"restore\": {\n" +
                "         \"volume_id\": \""+volumeId+"\"\n" +
                "     }\n" +
                "}";
        HttpResponse response = doPost(STRATUS_STORAGE_ENDPOINT + "/backups/"+backupId+"/restore", data);
        String newVolumeId = JsonPath.read(response.getResponse(),"$.restore.volume_id");
        assert volumeId.equals(newVolumeId);

        for(;;) {
//        for(int i = 0; i < 60; i++) {
            String status = getVolumeStatus(volumeId);
            if (status.equals("error")) {
                throw new NotCreatedException(newVolumeId);
            }
            if (status.equals("available")) {
                return volumeId;
            }
            try {
                wait(600 * 1000);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
//        throw new IllegalStateException("Volume not restored "+volumeId+"; "+backupId);
    }


    private static interface Status {
        String getStatus(String... inputs);
    }

    private static interface Action {
        String doOp(String... inputs);
    }

    private String doBlockingOperation(Status statusMethod, String wantedValue, boolean checkFirst, Action actionMethod, int counter, int sleepTime, RuntimeException e, String... inputs) {
        if (checkFirst) {
            String status = statusMethod.getStatus(inputs);
            if (status.equals(wantedValue)) {
                return null;
            }
        }
        String returnValue = actionMethod.doOp(inputs);
        for (int i = 0; i < counter; i++) {
            String status = statusMethod.getStatus(inputs);
            if (status.equals(wantedValue)) {
                return returnValue;
            }
            try {
                wait(sleepTime * 1000);
            } catch (InterruptedException ie) {
                throw new IllegalStateException(ie);
            }
        }
        throw e;
    }

    public synchronized String getSnapshotId(String volumeId) {
        return getValueBlockTemp("/volumes/"+volumeId, "/volume/@snapshot_id");
    }

    public synchronized String getImageStatus(String imageId) {
        return getValueTemp("/images/" + imageId, "/image/@status");
    }

    public synchronized  String getImageName(String imageId) {
        return getValueTemp("/images/" + imageId, "/image/@name");
    }

    public synchronized String getSnapshotStatus(String snapshotId) {
        return getValueBlockTemp("/snapshots/" + snapshotId, "/snapshot/@status");
    }

    public synchronized String getBackupStatus(String backupId) {
        return getValueBlockTemp("/backups/" + backupId, "/backup/@status");
    }

    public  synchronized String getVolumeStatus(String volumeId) {
        return getValueBlockTemp("/volumes/" + volumeId, "/volume/@status");
    }

    public synchronized String getVolumeSnapshotName(String volumeSnapshotId) {
        return getValueBlockTemp("/snapshots/" + volumeSnapshotId, "/snapshot/@display_name");
    }

    public synchronized String getServerStatus(String serverId) {
        return getValueTemp("/servers/"+serverId, "/server/@status");
    }

    private String _getValueTemp(String uri, String path) {
        HttpResponse response = doGet(uri);
        XmlFile xml = new XmlFile(response.getResponse());
        return xml.getElementValue(path);
    }

    private String getValueTemp(String uri, String path) {
        return _getValueTemp(computeEndpoint+uri, path);
    }

    private String getValueBlockTemp(String uri, String path) {
        return _getValueTemp(STRATUS_STORAGE_ENDPOINT+uri, path);
    }

    public synchronized String getUniqueName(DemoDetail detail, String newOfferingName) {
        return getUniqueName(detail instanceof DemoImage, newOfferingName, detail.getName());
    }

    public synchronized String getUniqueName(boolean image, String newOfferingName, String demoName) {
        String endpoint;
        String uriPrefix;
        String bootImageOrVolume;
        String zoneSuffix;
        if (image) {
            endpoint = computeEndpoint;
            uriPrefix = "/images.xml?name=";
            bootImageOrVolume = "BootImage";
            zoneSuffix = "";
        } else {
            endpoint = STRATUS_STORAGE_ENDPOINT;
            uriPrefix = "/volumes.xml?display_name=";
            bootImageOrVolume = "BootVolume";
            zoneSuffix = "-AZ1";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMMyy", Locale.ENGLISH);
        String date = sdf.format(new Date());
        //                                          Boot     Volume  -   2   0      S    e    p    t  2 0 1  4  .v  1   .  0  -AZ1
        String newShortName = demoName.replaceAll("-?"+bootImageOrVolume+"-?\\d?\\d[A-Za-z][a-z][a-z][a-z]?2?0?1\\d\\.v\\d(\\.\\d)?-AZ1$", "");
        String newName = null;
        for (int i = 1; i < 1000; i++ )  {
            newName = newOfferingName+"-"+newShortName+"-"+bootImageOrVolume+"-"+date+".v"+i+zoneSuffix;
            newName = newName.replaceAll("\\s+","");
            HttpResponse response = doGet(endpoint+uriPrefix+newName);

            XmlFile xml = new XmlFile(response.getResponse());
            if (xml.getNumber("count(/*/*/@id)") == 0) {
                return newName;
            }
        }
        throw new IllegalStateException("Cannot calculate the new demo name: "+newName);
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }
}
