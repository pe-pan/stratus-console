package com.hp.sddg.rest.csa.entities;

/**
 * Created by panuska on 3.10.14.
 */
public class Offering extends CsaEntity {
    public Offering(Object json) {
        super(json);
    }

    public Offering(String json, String newName) {
        super(json);
        transform(newName);
    }

    private void transform(String newName) {
        json = json.replaceFirst("\"name\"\\s?:\\s?\"[^\"]*\"", "\"name\" : \"" + newName + "\"");
        json = json.replaceFirst("\"@self\"\\s?:\\s?\"[^\"]*\",?", "");          // remove @self
        json = json.replaceFirst("\"csa_name_key\"\\s?:\\s?\"[^\"]*\",?", "");   // remove csa_name_key
//        json = json.replaceFirst("\"@deleted\"\\s?:\\s?[^,]*,", "");             // remove @deleted
    }
}
