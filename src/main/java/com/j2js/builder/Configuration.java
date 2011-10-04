package com.j2js.builder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "j2js-configuration")
public class Configuration {

    @XmlElement(name = "basedir")
    public File basedir = new File(".");
    
    @XmlElement(name = "uaCommand", required = true)
    public String uaCommand;
    
    @XmlElement(name = "uaArg")
    public List<String> uaArgs = new ArrayList<String>();
    
    @XmlElement(name = "devModeWebSocketURL", required = true)
    public URI devModeWebSocketURL;
    
    @XmlElement(name = "devModeHttpURL", required = true)
    public URI devModeHttpURL;

    public void write(OutputStream os) {
        try {
            JAXBContext context = JAXBContext.newInstance(Configuration.class);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.marshal(this, os);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static Configuration read() {
        File file = new File(System.getProperty("user.home") + "/.m2/j2js.xml");
        InputStream is = null;
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not read file " + file);
        }
        System.out.println("Reading j2js configuration from " + file);
        return Configuration.read(is);
    }
    
    public static Configuration read(InputStream inStream) {
        JAXBContext context;
        try {
            context = JAXBContext.newInstance(Configuration.class);
            Unmarshaller um = context.createUnmarshaller();
            return (Configuration) um.unmarshal(inStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
//    public ApplicationConfiguration getApplicationConfiguration(String applicationName) {
//        if ( applicationName == null ) {
//            throw new NullPointerException("applicationName cannot be null.");
//        }
//        
//        for (ApplicationConfiguration applConfig : applications) {
//            if ( applicationName.equals(applConfig.applicationName) ) {
//                return applConfig;
//            }
//        }
//
//        return null;
//    }
}
