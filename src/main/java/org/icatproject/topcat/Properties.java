package org.icatproject.topcat;

import java.io.InputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Properties extends java.util.Properties {

    private static Properties instance = new Properties();

    private Logger logger = LoggerFactory.getLogger(Properties.class);

    public static Properties getInstance() {
       return instance;
    }

    protected Properties() {
        super();

        logger.debug("loading run.properties...");

        try (InputStream istream = this.getClass().getClassLoader().getResourceAsStream("run.properties")) {

            if (istream == null) {
                throw new RuntimeException("Could not find run.properties");
            }

            this.load(istream);

            istream.close();
        } catch(IOException e){
            throw new RuntimeException("Error reading from run.properties", e);
        }

        logger.debug("run.properties loaded");
    }
}
