package com.hp.sddg.main;

/**
 * Created by panuska on 19.9.14.
 */
public class Offering {
    private String json;
    private String newName;

    public Offering(String json, String newName) {
        this.json = json;
        this.newName = newName;
    }

    public synchronized void replace(String oldString, String newString) {
        json = json.replace(oldString, newString);
    }

    public String getJson() {
        return json;
    }

    public String getNewName() {
        return newName;
    }

    public void transform() {
        json = json.replaceFirst("\"name\"\\s?:\\s?\"[^\"]*\"", "\"name\" : \"" + newName + "\"");
        json = json.replaceFirst("\"@self\"\\s?:\\s?\"[^\"]*\",?", "");          // remove @self
        json = json.replaceFirst("\"csa_name_key\"\\s?:\\s?\"[^\"]*\",?", "");   // remove csa_name_key
        ///
//        json = json.replaceFirst("\"@deleted\"\\s?:\\s?[^,]*,", "");          // remove @deleted
    }

}
