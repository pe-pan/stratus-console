package com.hp.sddg.main;

import com.hp.sddg.rest.IllegalRestStateException;
import com.hp.sddg.rest.csa.Csa;
import com.hp.sddg.rest.csa.DemoDetail;
import com.hp.sddg.rest.csa.DemoImage;
import com.hp.sddg.rest.csa.Subscription;
import com.hp.sddg.rest.openstack.OpenStack;
import org.apache.log4j.Logger;
import org.fusesource.jansi.AnsiConsole;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by panuska on 11.9.14.
 */
public class Main {
    private static Logger log = Logger.getLogger(Main.class.getName());
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println(
                    "Usage: saveSubscriptionAsServiceOffering id name\n" +
                            "       id   - ID of subscription (taken from CSA)\n" +
                            "       name - name of the newly created service offering\n"

            );
            System.exit(-1);
        }
        try {
            System.setProperty("java.net.useSystemProxies", "false");
            System.setProperty("https.proxyHost", "proxy.bbn.hp.com");
            System.setProperty("https.proxyPort", "8080");
            System.setProperty("http.proxyHost", "proxy.bbn.hp.com");
            System.setProperty("http.proxyPort", "8080");


            Csa csa = new Csa("ooInboundUser", "go.CSA.admin");
            csa.authenticate();
//            String personId = csa.getNormalUserId("petr.panuska@hp.com");

            String newOfferingName = args[1];
            Subscription subscription = Subscription.getSubscription(csa, args[0]);
            log.info("Subscription '"+ subscription.getName()+"' based on '"+subscription.getServiceOffering().getName()+"' is going to be saved as '"+newOfferingName+"'");

            System.setProperty("java.net.useSystemProxies", "false");
            System.clearProperty("https.proxyHost");
            System.clearProperty("https.proxyPort");
            System.clearProperty("http.proxyHost");
            System.clearProperty("http.proxyPort");
            OpenStack openStack = subscription.getCloudClient();
            openStack.authenticate();

            System.setProperty("java.net.useSystemProxies", "false");
            System.setProperty("https.proxyHost", "proxy.bbn.hp.com");
            System.setProperty("https.proxyPort", "8080");
            System.setProperty("http.proxyHost", "proxy.bbn.hp.com");
            System.setProperty("http.proxyPort", "8080");

            List<DemoDetail> demoDetails = subscription.getDemoDetails(openStack);
            int demoImages = 0;
            for (DemoDetail detail : demoDetails) {
                if (detail instanceof DemoImage) demoImages++;
            }
            log.info("Subscription contains "+demoDetails.size()+" servers ("+(demoDetails.size()-demoImages)+" volumes/"+demoImages+" images)");
            for (DemoDetail detail : demoDetails) {
                log.info(detail);
            }
//            log.info(demoDetails);

            String offeringJson = csa.getServiceOffering(subscription.getServiceOffering().getId());
            log.debug("Offering: "+offeringJson);
            Offering offering = new Offering(offeringJson, newOfferingName);
            offering.transform();

            List<Thread> cloneThreads  = new LinkedList<>();

            for (DemoDetail demo : demoDetails) {
                Thread cloneThread = new Thread(new CloneDemo(demo, openStack, offering));
                cloneThread.start();

                cloneThreads.add(cloneThread);
            }

            // wait till all the threads finish their jobs
            for (Thread thread : cloneThreads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }

            csa.login("ooInboundUser", "go.CSA.admin");
            String newOfferingId = csa.createNewServiceOffering(offering.getJson());
            log.info("New service offering "+newOfferingName+" created: "+newOfferingId);

        } catch (IllegalRestStateException e) {
            System.out.println(e.getErrorStream());
            System.out.println(e.getResponseCode());
            throw e;
        }
    }

}
