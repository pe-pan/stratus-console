package com.hp.sddg.main;

import com.hp.sddg.rest.IllegalRestStateException;
import com.hp.sddg.rest.common.entities.EntityHandler;
import com.hp.sddg.rest.common.entities.Entity;
import com.hp.sddg.rest.csa.Csa;
import com.hp.sddg.rest.csa.DemoDetail;
import com.hp.sddg.rest.csa.Subscription;
import com.hp.sddg.rest.csa.entities.CsaEntityHandler;
import com.hp.sddg.rest.csa.entities.SubscriptionHandler;
import com.hp.sddg.rest.openstack.entities.OpenStackEntityHandler;
import com.hp.sddg.rest.openstack.OpenStack;
import com.hp.sddg.utils.TimeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.fusesource.jansi.AnsiConsole;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by panuska on 24.9.14.
 */
public class Console {
    private static Logger log = Logger.getLogger(Console.class.getName());

    private String context = "";

    private Console() {
        EntityHandler.initHandlers();
    }

    public static void main(String[] args) throws IOException, InvocationTargetException, IllegalAccessException {
        AnsiConsole.systemInstall();
        System.out.println("Stratus console "+ Ansi.BOLD +Console.class.getPackage().getImplementationVersion()+ Ansi.RESET +" (build time: "+ Ansi.BOLD +getBuildTime()+ Ansi.RESET +")");
        if (args.length != 4) {
            System.out.println(Ansi.BOLD+"Usage:"+Ansi.RESET+" java -jar stratus-console.jar open-stack-user-name open-stack-user-password csa-user-name csa-user-password");
            System.exit(-1);
        }
        System.out.println("For help, type "+ Ansi.BOLD + Ansi.CYAN+"help"+ Ansi.RESET +" once you get authenticated to OpenStack and CSA");
        String username = "";
        String password = "";
//        if (args.length > 0) {
           username =  args[0];
//        }

//        if (args.length > 1) {
            password = args[1];
//        } else {
//            System.out.print("Username [" + Ansi.BOLD + username + Ansi.RESET + "]: ");
//            System.out.flush();  // to support Ansi Console
//            String line = new BufferedReader(new InputStreamReader(System.in)).readLine();
//            if (line.length() > 0) username = line;
//
//            System.out.print("Password: ");
//            System.out.flush();  // to support Ansi Console
//            if (System.console() != null) {
//                char[] chars = System.console().readPassword();
//                password = new String(chars);
//            } else {
//                password = new BufferedReader(new InputStreamReader(System.in)).readLine();
//            }
//        }

        os = new OpenStack("https://region-a.geo-1.identity.hpcloudsvc.com:35357", username, password, "63404451948086", OpenStack.STRATUS_COMPUTE_ENDPOINT);
        try {
//            System.setProperty("java.net.useSystemProxies", "false");
//            System.setProperty("https.proxyHost", "");
//            System.setProperty("https.proxyPort", "");
//            System.setProperty("http.proxyHost", "");
//            System.setProperty("http.proxyPort", "");
            os.authenticate();         //todo hack, this should not be necessary
        } catch (IllegalRestStateException e) {
            System.err.println(Ansi.BOLD + Ansi.RED + "Exception thrown when authenticating to OpenStack!"+ Ansi.CYAN);
            if (e.getErrorStream() != null) {
                System.err.println(e.getErrorStream());
            } else if (e.getMessage() != null) {
                System.err.println(e.getMessage());
            } else {
                e.printStackTrace();
            }
            System.out.println(Ansi.RESET);
            return;
        }

//        System.setProperty("java.net.useSystemProxies", "false");
//        System.setProperty("https.proxyHost", "proxy.bbn.hp.com");
//        System.setProperty("https.proxyPort", "8080");
//        System.setProperty("http.proxyHost", "proxy.bbn.hp.com");
//        System.setProperty("http.proxyPort", "8080");

        OpenStackEntityHandler.setClient(os);

        csa = new Csa(args[2], args[3]);
        csa.authenticate();  //todo a hack; this should not be necessary

        CsaEntityHandler.setClient(csa);
        Console console = new Console();

        console.readInput();
    }

    private static OpenStack os;
    private static Csa csa;


    public static String getBuildTime() {
        try {
            JarFile jarFile = new JarFile(Console.class.getProtectionDomain().getCodeSource().getLocation().getFile());
            Manifest manifest = jarFile.getManifest();
            Attributes attr = manifest.getMainAttributes();
            return attr.getValue("Build-Time");
        } catch (IOException e) {
            log.debug("Exception when reading build time from manifest file!", e);
            return null;
        }
    }

    private void readInput() throws IOException, InvocationTargetException, IllegalAccessException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        for (;;) {
            System.out.print("Stratus/" + Ansi.BOLD +context+ Ansi.RESET +"> ");
            System.out.flush();  // to support Ansi Console
            String line = reader.readLine();
            if (line.length() == 0) continue;

            int quotes = StringUtils.countMatches(line, "\"");
            if (quotes % 2 != 0) {
                System.out.println(Ansi.BOLD + Ansi.RED + "Ending double quotes not found!!" + Ansi.RESET);
                continue;
            }

            List<String> list = new LinkedList<>();
            Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(line);
            while (m.find()) {
                list.add(m.group(1).replace("\"", ""));
            }

            String[] tokens = list.toArray(new String[list.size()]);
            String cmd = tokens[0];
            try {
                Method method = getClass().getMethod("run_"+cmd, String[].class);
                method.invoke(this, new Object[] { tokens });
            } catch (NoSuchMethodException e) {
                System.out.println(Ansi.BOLD + Ansi.RED + "Unknown command " + Ansi.CYAN + cmd + Ansi.RESET);
                System.out.println("For help, type "+ Ansi.CYAN +"help"+ Ansi.RESET);
            } catch (InvocationTargetException ie) {
                System.out.println(Ansi.BOLD + Ansi.RED + "Command did not finish correctly; an exception thrown!" + Ansi.RESET);
                System.err.print(Ansi.BOLD + Ansi.CYAN);
                if (ie.getMessage() != null) System.err.println(ie.getMessage());
                ie.printStackTrace();
                System.err.println(Ansi.RESET);
            }
        }
    }

    private boolean enforceAtLeastParameters(String[] params, int number) {
        if (params.length < number+1) {
            System.out.println(Ansi.BOLD + Ansi.RED + "At least " + Ansi.CYAN + number + Ansi.RED + " parameters expected!" + Ansi.RESET);
            return false;
        }
        return true;
    }

    private boolean enforceMaximumParameters(String[] params, int number) {
        if (params.length > number+1) {
            System.out.println(Ansi.BOLD + Ansi.RED + "At most " + Ansi.CYAN + number + Ansi.RED + " parameters expected!" + Ansi.RESET);
            return false;
        }
        return true;
    }

    private EntityHandler enforceContext() {
        EntityHandler handler = EntityHandler.getHandler(context);
        if (handler == null) {
            System.out.println(Ansi.BOLD + Ansi.RED + "Select an entity first!");
            System.out.println("Possible options are: " + Ansi.CYAN + EntityHandler.getHandlerNames() + Ansi.RESET);
        }
        return handler;
    }

    private EntityHandler enforceList() {
        EntityHandler handler = enforceContext();
        if (handler == null) return null;
        List<Entity> list = handler.getFilteredEntities();
        if (list == null || list.size() == 0) {
            System.out.println(Ansi.BOLD + Ansi.RED + "List entities first!" + Ansi.RESET);
            return null;
        }
        return handler;
    }

    private EntityHandler enforceOpenStackEntity() {
        EntityHandler handler = enforceList();
        if (!(handler instanceof OpenStackEntityHandler)) {
            System.out.println(Ansi.BOLD + Ansi.RED +"Can be performed on OpenStack entities only!"+ Ansi.RESET);
            return null;
        }
        return handler;

    }

    public void run_select(String[] tokens) {
        if (!enforceAtLeastParameters(tokens, 1)) {
            System.out.println("Possible values are "+ Ansi.BOLD + Ansi.CYAN + EntityHandler.getHandlerNames()+ Ansi.RESET);
            return;
        }
        if (!enforceMaximumParameters(tokens, 1)) return;
        String newContext = tokens[1];
        if (EntityHandler.getHandler(newContext) != null) {
            this.context = newContext;
        } else {
            System.out.println(Ansi.BOLD + Ansi.RED + "Unknown entity " + Ansi.CYAN + newContext + Ansi.RED + "!");
            System.out.println("Possible values are "+ Ansi.BOLD + Ansi.CYAN + EntityHandler.getHandlerNames()+ Ansi.RESET);
        }
    }

    public void run_refresh(String[] tokens) {
        list(tokens, true);
    }

    public void run_list(String[] tokens) {
        list(tokens, false);
    }

    private void list(String[] tokens, boolean enforce) {
        if (!enforceMaximumParameters(tokens, 0)) return;
        EntityHandler handler = enforceContext();
        if (handler == null) return;

        List<Entity> list = handler.getFilteredEntities();
        if (list == null || enforce) {
            handler.list(enforce);
        }
        printFilteredEntities(handler);
    }

    void printFilteredEntities(EntityHandler handler) {
        List<Entity> list = handler.getFilteredEntities();
        if (list == null) return;
        int i = 1;
        for (Entity e : list) {
            if ((i-1) % 40 == 0) {              // every 40th line, print the table header again
                System.out.printf(Ansi.BOLD + Ansi.CYAN +" index "+ Ansi.RESET);
                handler.printTableHeader();
            }
            System.out.printf("["+ Ansi.BOLD +"%04d"+ Ansi.RESET +"] ", i++);

            handler.printEntity(e);
        }
        long earlier = EntityHandler.getHandler(context).getLastRefresh();   // in milisecs
        System.out.println(Ansi.BOLD +list.size() + Ansi.RESET + " " + context + " altogether; " + Ansi.BOLD + TimeUtils.getTimeDifference(earlier)+ Ansi.RESET);
    }

    public void run_exit(String[] tokens) {
        System.exit(0);
    }

    public void run_filter(String[] tokens) {
        if (!enforceAtLeastParameters(tokens, 1)) return;
        EntityHandler handler = enforceContext();
        if (handler == null) return;
        if (tokens.length == 2) { // one parameter
            switch (tokens[1]) {
                case "clear":
                    handler.resetFilteredEntities();
                    printFilteredEntities(handler);
                    break;
                case "index":
                    System.out.println(Ansi.BOLD + Ansi.RED + "No index number provided!" + Ansi.RESET);
                    break;
                default:
                    System.out.println(Ansi.BOLD + Ansi.RED + "Unknown filter command " + Ansi.CYAN + tokens[1] + Ansi.RESET);
                    break;
            }
            return;
        }
        // 2 or more parameters

        handler = enforceList();
        if (handler == null) return;
        List<Entity> list = handler.getFilteredEntities();
        if (list == null) return;

        LinkedList<Entity> newFilteredEntities = new LinkedList<>();
        if (tokens[1].equals("index")) {
            int[] indices = new int[tokens.length-2];
            for (int i = 2; i < tokens.length; i++) {
                try {
                    indices[i-2] = Integer.parseInt(tokens[i]);
                } catch (NumberFormatException  e) {
                    System.out.println(Ansi.BOLD + Ansi.RED +"Provided value is not a number! "+ Ansi.CYAN +tokens[i]+ Ansi.RESET);
                    return;
                }
                if (indices[i-2] > list.size() || indices[i-2] <= 0) {
                    System.out.println(Ansi.BOLD + Ansi.RED +"Index out of list! "+ Ansi.CYAN + indices[i-2]+ Ansi.RESET);
                    return;
                }
            }
            for (int index : indices) {
                Entity toBeAdded = list.get(index - 1);
                if (!newFilteredEntities.contains(toBeAdded)) {
                    newFilteredEntities.add(toBeAdded);
                }
            }
        } else {
            Set<String> columnNames = handler.getColumnNames();
            String columnName = tokens[1];
            if (!columnNames.contains(columnName)) {
                System.out.println(Ansi.BOLD + Ansi.RED +"Unknown column "+ Ansi.CYAN +columnName+ Ansi.RED+"; cannot filter using it"+Ansi.RESET);
                return;
            }
            for (Entity entity : list) {
                for (int i = 2; i < tokens.length; i++) {
                    String property = entity.getProperty(columnName);
                    if (property != null && StringUtils.containsIgnoreCase(property, tokens[i])) {
                        newFilteredEntities.add(entity);
                        break;  // do not lookup in other provided tokens
                    }
                }
            }
        }
        handler.setFilteredEntities(newFilteredEntities);
        printFilteredEntities(handler);
    }

    public void run_set(String[] tokens) {
        if (!enforceAtLeastParameters(tokens, 2)) return;
        if (!enforceMaximumParameters(tokens, 2)) return;
        EntityHandler handler = enforceContext();
        if (handler == null) return;
        List<Entity> list = handler.getFilteredEntities();
        if (!handler.isChangeableProperty(tokens[1])) {
            System.out.println(Ansi.BOLD + Ansi.RED +"Property " + Ansi.CYAN + tokens[1].toLowerCase() + Ansi.RED +" is not allowed to be changed!");
            System.out.println("Possible values: "+ Ansi.CYAN +handler.getChangeableProperties()+ Ansi.RESET);
            return;
        }

        for (Entity ent : list) {
            ent.setProperty(tokens[1], tokens[2]);
        }
        handler.resetColumnsSize();
        printFilteredEntities(handler);
    }

    public void run_update(String[] tokens) throws IOException {
        if (!enforceAtLeastParameters(tokens, 0)) return;
        if (!enforceMaximumParameters(tokens, 0)) return;
        EntityHandler handler = enforceOpenStackEntity();
        if (handler == null) return;
        List<Entity> list = handler.getFilteredEntities();

        if (list.size() > 10) {
            System.out.println(Ansi.BOLD + Ansi.RED + "There is more than 10 entities selected; you cannot update more than 10 entities at once! (Security policy)"+Ansi.RESET);
            return;
        }

        for (Entity ent : list) {
            if (!ent.isDirty()) {
                System.out.println(Ansi.BOLD + Ansi.RED +"For "+ Ansi.CYAN +ent.getId()+ Ansi.RED +"; there is nothing to update - set a property value first!"+ Ansi.RESET);
                return;
            }
        }

        printFilteredEntities(handler);
        System.out.println(Ansi.BOLD + Ansi.GREEN +"Are you sure you want to update those " + Ansi.CYAN +list.size() +" "+handler.getContexts()+ Ansi.GREEN +" above? ["+Ansi.CYAN+"yes"+Ansi.GREEN+"]"+ Ansi.RESET);
        System.out.println(Ansi.BOLD + Ansi.YELLOW +"There is no way back!"+ Ansi.RESET);
        String line = new BufferedReader(new InputStreamReader(System.in)).readLine();
        if (line.equals("yes")) {
            System.out.printf(Ansi.BOLD + Ansi.CYAN +" index "+ Ansi.RESET);
            handler.printTableHeader();
            int i = 1;
            for (Entity ent : list) {
                Entity newEntity = handler.update(ent);
                System.out.printf("["+ Ansi.BOLD +"%04d"+ Ansi.RESET +"] ", i++);
                handler.printEntity(newEntity);
            }
        } else {
            System.out.println("Nothing updated");
        }
    }

    public void run_delete(String[] tokens) throws IOException {
        if (!enforceAtLeastParameters(tokens, 0)) return;
        if (!enforceMaximumParameters(tokens, 0)) return;
        EntityHandler handler = enforceOpenStackEntity();
        if (handler == null) return;
        List<Entity> list = handler.getFilteredEntities();

        if (list.size() > 10) {
            System.out.println(Ansi.BOLD + Ansi.RED + "There is more than 10 entities selected; you cannot delete more than 10 entities at once! (Security policy)"+Ansi.RESET);
            return;
        }

        for (Entity ent : list) {
            if (ent.isDirty()) {
                System.out.println(Ansi.BOLD + Ansi.RED +"For "+ Ansi.CYAN +ent.getId()+ Ansi.RED +"; there are pending changes; reset the values first!"+ Ansi.RESET);
                return;
            }
        }

        printFilteredEntities(handler);
        System.out.println(Ansi.BOLD + Ansi.GREEN + "Are you sure you want to delete those " + Ansi.CYAN + list.size() + " " + handler.getContexts() + Ansi.GREEN + " above? ["+Ansi.CYAN+"yes"+Ansi.GREEN+"]" + Ansi.RESET);
        System.out.println(Ansi.BOLD + Ansi.YELLOW +"There is no way back!"+ Ansi.RESET);
        String line = new BufferedReader(new InputStreamReader(System.in)).readLine();
        if (line.equals("yes")) {

            System.out.printf(Ansi.BOLD + Ansi.CYAN +"          index "+ Ansi.RESET);
            handler.printTableHeader();
            int i = 1;
            for (Entity ent : list) {
                System.out.printf(Ansi.BOLD + Ansi.GREEN +"Deleting "+ Ansi.RESET +"["+ Ansi.BOLD +"%04d"+ Ansi.RESET +"] ", i++);
                handler.printEntity(ent);
                handler.delete(ent);
            }
            System.out.println(Ansi.BOLD + Ansi.GREEN +list.size()+" "+handler.getContexts()+" deleted"+ Ansi.RESET);
            //todo clear filter list (as it's supposed to be empty now)
            handler.clearList();  //todo it would be better just to clear only those entities from "lastEntities" which got deleted
        } else {
            System.out.println("Nothing deleted");
        }
    }

    public void run_goto(String[] tokens) {
        if (!enforceAtLeastParameters(tokens, 1)) return;
        if (!enforceMaximumParameters(tokens, 2)) return;
        EntityHandler handler = enforceList();
        if (handler == null) return;
        List<Entity> list = handler.goTo(tokens[1]);
        if (list == null) {
            System.out.println(Ansi.BOLD + Ansi.RED +"Cannot go to "+ Ansi.CYAN +tokens[1]+ Ansi.RED +" in this context: "+ Ansi.CYAN +context+ Ansi.RESET);
            return;
        }
        context = tokens[1];
        handler = EntityHandler.getHandler(context);

        handler.setFilteredEntities(list);

        printFilteredEntities(handler);
    }

    private boolean enforceContext(String[] possibleValues) {
        for (String v : possibleValues) {
            if (v.equals(context)) return true;
        }
        System.out.println(Ansi.BOLD + Ansi.RED +"Select context to one of these values: "+ Ansi.CYAN + Arrays.toString(possibleValues) + Ansi.RESET);
        return false;
    }

    private Entity enforceSingleFilteredEntity() {
        EntityHandler handler = enforceList();
        if (handler == null) return null;
        List<Entity> entities = handler.getFilteredEntities();
        if (entities.size() != 1) {
            System.out.println(Ansi.BOLD + Ansi.RED +"Filter a single "+handler.getContext()+" only! Currently, there are "+ Ansi.CYAN +entities.size()+ Ansi.RED +" filtered "+handler.getContexts()+"!"+ Ansi.RESET);
            return null;
        }
        return entities.get(0);
    }

    public void run_person(String[] tokens) throws IOException {
        if (!enforceMaximumParameters(tokens, 1)) return;

        SubscriptionHandler handler = (SubscriptionHandler)EntityHandler.getHandler("subscriptions");
        if (tokens.length == 2) {
            if (handler.setInterpersonatedPerson(tokens[1])) {
                handler.clearList();
            } else {
                System.out.println(Ansi.RED+Ansi.BOLD+"There is no user called "+Ansi.CYAN+tokens[1]);
            }
        }
        System.out.println("Interpersonated as " + Ansi.BOLD + Ansi.CYAN +handler.getInterpersonatedPerson()+ Ansi.RESET);
    }

    private String askForValue(String newOfferingName) throws IOException {
        String line;
        do {
            System.out.println("Leave empty for no offering name change or type the new offering name.");
            System.out.print("New offering name ["+ Ansi.BOLD +newOfferingName+ Ansi.RESET +"]: ");
            System.out.flush();
            line = new BufferedReader(new InputStreamReader(System.in)).readLine();
            if (line.length() > 0) newOfferingName = line;
        } while(line.length() > 0);
        return newOfferingName;
    }

    private boolean askForConfirmation() throws IOException {
        System.out.println(Ansi.BOLD + Ansi.GREEN +"Start the save now?"+ Ansi.RESET);
        System.out.println("Entering "+ Ansi.BOLD + Ansi.CYAN +"yes"+ Ansi.RESET +" will start the save; "+ Ansi.BOLD + Ansi.YELLOW +"no further change will be possible!!"+ Ansi.RESET);
        System.out.println("Entering "+ Ansi.BOLD + Ansi.CYAN +"anything else"+ Ansi.RESET +" will leave this command."+ Ansi.RESET);
        String line = new BufferedReader(new InputStreamReader(System.in)).readLine();
        return "yes".equals(line);
    }
    Map<String, List<CloneDemo>> jobs = new LinkedHashMap<>();

    public void run_save(String[] tokens) throws IOException {
        if (!enforceMaximumParameters(tokens, 0)) return;
        if (!enforceContext(new String[]{"subscriptions"})) return;
        Entity entity = enforceSingleFilteredEntity();
        if (entity == null) return;

        //todo this should be done differently
        System.out.println("Collecting subscription details; please wait...");
        Subscription sub = Subscription.getSubscription(csa, entity.getId());
        String newOfferingName = sub.getName();
        List<DemoDetail> details = sub.getDemoDetails(os);

        System.out.println("New volumes/images are going to be saved from "+ Ansi.BOLD + Ansi.CYAN +details.size()+ Ansi.RESET +" running server instances.");
//        System.out.println("Do you also want to create a new offering?");
//        System.out.println("The original offering is called '"+CYAN+sub.getServiceOffering().getName()+"'.");
//        boolean repeat;
//        do {
//            System.out.println("Type "+CYAN+"yes"+CYAN+" to create a new offering, type "+CYAN+"no"+CYAN+" to save only volumes/images, type "+CYAN+"leave"+CYAN+" to leave this save command.");
//            String line = new BufferedReader(new InputStreamReader(System.in)).readLine();
//            switch (line) {
//                case "yes" :
//                    // nothing to change
//                    repeat = false;
//                    break;
//                case "no" :
//                    newOfferingName = null;
//                    repeat = false;
//                    break;
//                case "leave" :
//                    return;
//                default :
//                    repeat = true;
//            }
//        } while (repeat);
//


////        System.out.println("Do you want to also create a new offering ("+CYAN+newOfferingName+GREEN+") ["+CYAN+"new"+GREEN+"],");
////        System.out.println("replace the original offering "+CYAN+sub.getServiceOffering().getName()+GREEN+" ["+CYAN+"old"+GREEN+"],");
////        System.out.println("or create no offering (only images / volumes & snapshots will be created) ["+CYAN+"no"+GREEN+"]?");
////        System.out.println("Type "+CYAN+"new"+GREEN+", "+CYAN+"old"+GREEN+" or "+CYAN+"no"+GREEN+" to continue. Anything else to exit the save command.");

//        if (newOfferingName != null)  {
//            newOfferingName = askForValue(newOfferingName);
//        }
//
//        System.out.println("Name set to: "+newOfferingName);

        for (DemoDetail d : details) {
//            System.out.println("Calculating new unique "+d.getType()+" name for demo "+Ansi.BOLD+Ansi.CYAN+d.getName()+Ansi.RESET+"; please wait...");
//            d.setNewImageVolumeName(os.getUniqueName(d, newOfferingName));
            d.setNewImageVolumeName(d.getName()); //todo unique names take too much time to calculate and proved to be always wrong
        }

        boolean repeat;
        do {
            boolean userMadeChange = false;
            for (DemoDetail d : details) {
                System.out.println(d.toConsoleString());
                System.out.println(Ansi.BOLD + Ansi.GREEN +"Do you want to keep this "+d.getType()+" name?"+Ansi.RESET);
                System.out.println(Ansi.BOLD + Ansi.CYAN +"Leave empty"+ Ansi.RESET+" for no change; "+ Ansi.BOLD+Ansi.CYAN +"write the new "+d.getType()+" name"+ Ansi.RESET +" or enter "+ Ansi.BOLD+Ansi.CYAN +"no"+ Ansi.RESET +" to skip saving this demo."+ Ansi.RESET);
                String line = new BufferedReader(new InputStreamReader(System.in)).readLine();
                if (line.length() > 0) {
                    if (line.equals("no")) d.setNewImageVolumeName(null);
                    else d.setNewImageVolumeName(line);
                    userMadeChange = true;
                }

                // todo ask for confirmation to leave the unique name or change it
            }
            if (userMadeChange) {
                System.out.println("The image/volume names are set accordingly:");
                for (DemoDetail d : details) {
                    System.out.println(d.toConsoleString());
                }
            }
            System.out.println(Ansi.BOLD + Ansi.GREEN +"Do you want to make additional changes ["+ Ansi.CYAN +"yes, no, leave"+ Ansi.GREEN +"]?"+ Ansi.RESET);
            String line = new BufferedReader(new InputStreamReader(System.in)).readLine();
            switch (line) {
                case "yes" :
                    // nothing to change
                    repeat = true;
                    break;
                case "no" :
                    repeat = false;
                    break;
                case "leave" :
                    return;
                default :
                    repeat = true;
            }
        } while (repeat);

        if (!askForConfirmation()) return;

        System.out.println("For all machines being saved start cmd-line (as an administrator!) and then execute those 2 commands :");
        System.out.println(Ansi.BOLD + Ansi.CYAN +"Shutdown /s /t 90\n" +
                           "Ipconfig /release\n"+ Ansi.RESET);
        System.out.print(Ansi.BOLD + Ansi.YELLOW +"Press enter to start the save!!"+ Ansi.RESET);
        System.out.flush();  // to support Ansi Console
        String line = new BufferedReader(new InputStreamReader(System.in)).readLine();

//        Offering offering = new Offering(csa.getServiceOffering(sub.getServiceOffering().getId()), newOfferingName);
//        offering.transform();

        List<CloneDemo> cloneThreads  = new LinkedList<>();
        for (DemoDetail demo : details) {
            if (demo.getNewImageVolumeName() == null) continue;    // skip the ones I don't want to save
//            CloneDemo cloneThread = new CloneDemo(demo, os, offering);
            CloneDemo cloneThread = new CloneDemo(demo, os, null);
            cloneThread.start();                    // todo shall I also remove the dead threads?

            cloneThreads.add(cloneThread);
        }
        System.out.println("Save has been started. Type "+ Ansi.BOLD + Ansi.CYAN +"jobs"+ Ansi.RESET +" to list the job status.");

        // todo save offering into CSA!

        //todo there should be one more job that would clean all these jobs once finished

        jobs.put("["+(jobs.size()+1)+"] "+newOfferingName, cloneThreads);  //todo is it good to use newOfferingName? it's not creating one...

    }

    public void run_jobs(String[] tokens) {                      //todo how about dead CloneDemo threads? are they going to be visible here?
        if (!enforceMaximumParameters(tokens, 0)) return;
        if (jobs.size() == 0) {
            System.out.println("There are no jobs to list");
            return;
        }
        for (String name : jobs.keySet()) {
            List<CloneDemo> threads = jobs.get(name);
            System.out.println("Status of "+ Ansi.BOLD + Ansi.CYAN+name+ Ansi.RESET);
            for (CloneDemo thread : threads) {
                DemoDetail demo = thread.getDemo();
                System.out.println(Ansi.BOLD +demo.getName()+ Ansi.RESET +": "+demo.getStateString());
            }
        }
    }

    public void run_version(String[] tokens) {
        System.out.println("Stratus console "+ Ansi.BOLD +Console.class.getPackage().getImplementationVersion()+ Ansi.RESET +" (build time: "+ Ansi.BOLD +getBuildTime()+ Ansi.RESET +")");
    }

    public void run_clone(String[] tokens) throws IOException {
        if (!enforceMaximumParameters(tokens, 0)) return;
        if (!enforceContext(new String[]{"offerings"})) return;
        Entity entity = enforceSingleFilteredEntity();
        if (entity == null) return;

        String newOfferingName = entity.getProperty("name");

        SimpleDateFormat sdf = new SimpleDateFormat("ddMMMyy", Locale.ENGLISH);
        String date = sdf.format(new Date());
        newOfferingName = (newOfferingName.startsWith("Clone of ") ? "" : "Clone of ") + newOfferingName;
        if (newOfferingName.matches(".* - \\d\\d[A-Z][a-z][a-z]\\d\\d$")) {
            newOfferingName = newOfferingName.substring(0, newOfferingName.length()-"ddMMMyy".length());
            newOfferingName = newOfferingName +date;
        } else {
            newOfferingName = newOfferingName+" - "+date;
        }
        newOfferingName = askForValue(newOfferingName);

        if (!askForConfirmation()) return;

        Offering offering = new Offering(csa.getServiceOffering(entity.getId()), newOfferingName);
        offering.transform();
        String newOfferingId = csa.createNewServiceOffering(offering.getJson());
        System.out.println("New offering "+Ansi.BOLD+Ansi.CYAN+newOfferingName+Ansi.RESET+" created with ID "+Ansi.BOLD+Ansi.CYAN+newOfferingId+Ansi.RESET);

        //todo clear filter list (as it's supposed not to be up-to-date)
    }

    public void run_power(String[] tokens) throws IOException {
        if (!enforceMaximumParameters(tokens, 1)) return;
        if (!enforceAtLeastParameters(tokens, 1) || (!"on".equals(tokens[1]) && !"off".equals(tokens[1]))) {
            System.out.println(Ansi.BOLD+Ansi.RED+"Please provide either "+Ansi.BOLD+Ansi.CYAN+"on"+Ansi.RED+" or "+Ansi.BOLD+Ansi.CYAN+"off"+Ansi.RESET);
            return;
        }

        if (!enforceContext(new String[]{"servers"})) return;

        EntityHandler handler = enforceList();
        if (handler == null) return;
        List<Entity> list =  handler.getFilteredEntities();

        System.out.println(Ansi.GREEN+Ansi.BOLD+"Are you sure to "+Ansi.BOLD+Ansi.CYAN+"power "+list.size()+" servers "+tokens[1]+Ansi.GREEN+"?"+Ansi.RESET);
        String line = new BufferedReader(new InputStreamReader(System.in)).readLine();
        if (!"yes".equals(line)) return;

        System.out.printf(Ansi.BOLD + Ansi.CYAN +StringUtils.leftPad("", tokens[1].length())+ "          index "+ Ansi.RESET);
        handler.printTableHeader();
        int i = 1;
        for (Entity server : list) {
            boolean actionTaken;
            if ("off".equals(tokens[1])) {
                actionTaken = os.powerMachineOff(server.getId());
            } else {
                actionTaken = os.powerMachineOn(server.getId());
            }
            if (actionTaken) {
                System.out.printf(Ansi.BOLD + Ansi.GREEN +"Powered "+tokens[1]+Ansi.RESET +" ["+ Ansi.BOLD +"%04d"+ Ansi.RESET +"] ", i++);
            } else {
                System.out.printf(Ansi.BOLD              +"Skipped "+tokens[1]+Ansi.RESET +" ["+ Ansi.BOLD +"%04d"+ Ansi.RESET +"] ", i++);
            }
            handler.printEntity(server);
        }
    }

    public void run_help(String[] tokens) {
        System.out.println("Stratus console");
        System.out.println("Commands:");
        System.out.println("  * "+ Ansi.BOLD + Ansi.CYAN +"select "+ EntityHandler.getHandlerNames()+ Ansi.RESET);
        System.out.println("    - selects the set of entities to operate on");

        System.out.println("  * "+ Ansi.BOLD + Ansi.CYAN +"list"+ Ansi.RESET);
        System.out.println("    - lists all entities of the selected kind");

        System.out.println("  * "+ Ansi.BOLD + Ansi.CYAN +"refresh"+ Ansi.RESET);
        System.out.println("    - refreshes the list of entities");

        System.out.println("  * "+ Ansi.BOLD + Ansi.CYAN +"filter index number1 number2 ... numberN"+ Ansi.RESET);
        System.out.println("    - filters entities of the provided indices");

        System.out.println("  * "+ Ansi.BOLD + Ansi.CYAN +"filter column_name string1 string2 ... stringN"+ Ansi.RESET);
        System.out.println("    - filters entities containing one of the strings in the provided column");

        System.out.println("  * "+ Ansi.BOLD + Ansi.CYAN +"filter clear"+ Ansi.RESET);
        System.out.println("    - clears filter");

        System.out.println("  * "+ Ansi.BOLD + Ansi.CYAN +"set property_name \"property_value\""+ Ansi.RESET);
        System.out.println("    - sets the provided property of all filtered entities to the provided value");
        System.out.println("    - client side only; does not change anything in OpenStack yet");

        System.out.println("  * "+ Ansi.BOLD + Ansi.CYAN +"update"+ Ansi.RESET);
        System.out.println("    - commits all changes into OpenStack (HTTP PUT)");
        System.out.println("    - OpenStack entities only");

        System.out.println("  * "+ Ansi.BOLD + Ansi.CYAN +"delete"+ Ansi.RESET);
        System.out.println("    - deletes all filtered entities from OpenStack (HTTP DELETE)");
        System.out.println("    - OpenStack entities only");

        System.out.println("  * "+ Ansi.BOLD + Ansi.CYAN +"power [on, off]"+ Ansi.RESET);
        System.out.println("    - powers all filtered servers on or off ");

        System.out.println("  * "+ Ansi.BOLD + Ansi.CYAN +"save"+ Ansi.RESET);
        System.out.println("    - saves volumes/snapshots/images of an active subscription");
        System.out.println("    - interactive; asks for volume/image names");

        System.out.println("  * "+ Ansi.BOLD + Ansi.CYAN +"jobs"+ Ansi.RESET);
        System.out.println("    - list running job status (subscription saves)");

        System.out.println("  * "+ Ansi.BOLD + Ansi.CYAN +"person [userName]"+ Ansi.RESET);
        System.out.println("    - shows the active person or sets the active person");
        System.out.println("    - to list subscriptions I don't own");

        System.out.println("  * "+ Ansi.BOLD + Ansi.CYAN +"clone"+ Ansi.RESET);
        System.out.println("    - clones a service offering");
        System.out.println("      - keeps all original values (apart from the offering name)");
        System.out.println("      - does not publish the offering");
        System.out.println("    - interactive; asks for the new offering name");

        System.out.println("  * "+ Ansi.BOLD + Ansi.CYAN +"goto offerings"+ Ansi.RESET);
        System.out.println("    - switches from subscriptions to offerings");
        System.out.println("    - lists one offering per subscription");

        System.out.println("  * "+ Ansi.BOLD + Ansi.CYAN +"version"+ Ansi.RESET);
        System.out.println("    - prints version of this tool");

        System.out.println("  * "+ Ansi.BOLD + Ansi.CYAN +"exit"+ Ansi.RESET);
        System.out.println("    - exits this console");
    }
}
