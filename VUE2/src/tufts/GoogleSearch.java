/*
 * GoogleSearch.java
 *
 * Created on April 18, 2003, 11:23 AM
 */

/**
 *
 * @author  akumar03
 */

import java.io.*;
import java.net.*;

import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import java.io.OutputStreamWriter;

import org.xml.sax.InputSource;

import google.*;

import java.util.Iterator;

public class GoogleSearch {
    private static final String searchURL = "http://googlesearch.tufts.edu/search?submit.y=5&site=tufts01&submit.x=11&output=xml_no_dtd&client=tufts01&q=";
    
    private static final String  XML_MAPPING = "c:\\anoop\\demos\\google\\google.xml";
    private static String query;
    
    private static int NResults = 10;
    
    private static String result ="";
    
    private static URL url;
    /** Creates a new instance of GoogleSearch */
    public GoogleSearch() {
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        query = args[0];
        
        try {
           url = new URL(searchURL+query);
           InputStream input = url.openStream();
           int c;
           while((c=input.read())!= -1) {
               result = result + (char) c;
           }
           FileWriter fileWriter = new FileWriter("google/google_result.xml");
           fileWriter.write(result);
           fileWriter.close();
         
          GSP gsp = loadGSP("c:\\anoop\\demos\\google\\google_result.xml");
          // GSP gsp = loadGSP(url);
           Iterator i = gsp.getRES().getResultList().iterator();
           while(i.hasNext()) {
               Result r = (Result)i.next();
               System.out.println(r.getTitle()+" "+r.getUrl());
           } 
        } catch (Exception e) {}
        
           
         //  System.out.println(result);
     
        
    }
    
    private static GSP loadGSP(String filename)
    {
       
        try {
            Unmarshaller unmarshaller = getUnmarshaller();
           unmarshaller.setValidation(false);
           GSP gsp = (GSP) unmarshaller.unmarshal(new InputSource(new FileReader(filename)));
            return gsp;
        } catch (Exception e) {
            System.out.println("loadGSP[" + filename + "]: " + e);
            e.printStackTrace();
            return null;
        }
    }
    
   private static GSP loadGSP(URL url)
    {
       try {
         InputStream input = url.openStream();
         int c;
         while((c=input.read())!= -1) {
               result = result + (char) c;
         }
       
           Unmarshaller unmarshaller = getUnmarshaller();
           unmarshaller.setValidation(false);
           GSP gsp = (GSP) unmarshaller.unmarshal(new InputSource());
            return gsp;
        } catch (Exception e) {
            System.out.println("loadGSP " + e);
            e.printStackTrace();
            return null;
        }
    }
    

    private static Unmarshaller unmarshaller = null;
    private static Unmarshaller getUnmarshaller()
    {
        if (unmarshaller == null) {
            unmarshaller = new Unmarshaller();
            Mapping mapping = new Mapping();
            try {
                mapping.loadMapping(XML_MAPPING);
                unmarshaller.setMapping(mapping);
            } catch (Exception e) {
                System.err.println("getUnmarshaller: " + e);
            }
        }
        return unmarshaller;
    }
}
