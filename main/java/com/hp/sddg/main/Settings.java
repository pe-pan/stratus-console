package com.hp.sddg.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * Created by panuska on 13.11.14.
 */
public class Settings {
    public static final String FILE_NAME = "conf/settings.txt";
    private static Settings settings;

    private Properties properties;

    private Settings() {
        properties = new Properties();
    }

    public void load() throws IOException {
        properties.load(new FileInputStream(new File(FILE_NAME)));
    }

    public void save() throws IOException {
        File file = new File(FILE_NAME);
        file.getParentFile().mkdirs();
        properties.store(new FileOutputStream(file), "Stratus Console Properties");
    }

    public String getProperty(String key) {
        return (String)properties.get(key);
    }

    public void setProperty(String key, String value) {
        if (value == null) {
            properties.remove(key);
        } else {
            properties.setProperty(key, value);
        }
    }

    public static Settings getSettings() {
        if (settings == null) {
            settings = new Settings();
        }
        return settings;
    }

    public void enterProperty(String key, String defaultValue, String displayName, boolean secret) throws IOException {
        if (defaultValue == null) defaultValue = "";

        if (!secret) {
            System.out.print(displayName+" [" + Ansi.BOLD + defaultValue + Ansi.RESET + "]: ");
            System.out.flush();  // to support Ansi Console
            String line = new BufferedReader(new InputStreamReader(System.in)).readLine();
            if (line.length() > 0) defaultValue = line;
        } else {
            System.out.print(displayName+": ");
            System.out.flush();  // to support Ansi Console
            if (System.console() != null) {
                char[] chars = System.console().readPassword();
                defaultValue = new String(chars);
            } else {
                defaultValue = new BufferedReader(new InputStreamReader(System.in)).readLine();
            }
        }

        if (defaultValue.length() == 0) defaultValue = null;
        setProperty(key, defaultValue);
    }
}
