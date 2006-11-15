/*
 * JenaTest.java
 *
 * Created on November 13, 2006, 2:06 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author akumar03
 */

package edu.tufts.vue;
import tufts.vue.*;
import com.hp.hpl.jena.rdf.model.*;

import java.util.*;

public class JenaTest {
    public static final int N_OBJECTS = 1000;
    public static final int N_SETS = 100000;
    public static final String CODED="coded";
    public static final String LABEL="LABEL";
    public static ArrayList<LWNode> nodeList;
    private static final Runtime sRuntime = Runtime.getRuntime();
    /** Creates a new instance of JenaTest */
    public JenaTest() {
    }
    
    /**
     * @param args the command line arguments
     */
    
    public static void testCreateObjects() {
        System.gc();
        Runtime.getRuntime().gc();
        nodeList = new ArrayList();
        long totalMemory = usedMemory();
        long tStart = System.currentTimeMillis();
        for(int i=0;i<N_OBJECTS;i++) {
            LWNode node = new LWNode("test");
            nodeList.add(node);
        }
        System.out.println("Time taken for initiation(ms): "+(System.currentTimeMillis()-tStart));
        Runtime.getRuntime().gc();
        System.out.println("Memory Used: "+(usedMemory()-totalMemory));
        tStart = System.currentTimeMillis();
        for(int i=0;i<N_SETS;i++){
            int rand = (int)Math.random()*(N_OBJECTS-1);
            nodeList.get(rand).setLabel("Test");
        }
        int randCode = (int)(Math.random()*(N_OBJECTS-1));
        nodeList.get(randCode).setLabel(CODED);
        System.out.println("Time taken for sets(ms): "+(System.currentTimeMillis()-tStart));
        tStart = System.currentTimeMillis();
        for(int i=0;i<N_SETS;i++){
            int rand = (int)Math.random()*(N_OBJECTS-1);
            nodeList.get(rand).getLabel();
        }
        System.out.println("Time taken for gets(ms): "+(System.currentTimeMillis()-tStart));
        tStart = System.currentTimeMillis();
        int j = 0;
        while(!nodeList.get(j).getLabel().equals(CODED)) {
            j++;
        }
        System.out.println("Time taken for search: "+(System.currentTimeMillis()-tStart));
        System.out.println("Coded found in: "+j+" randCode:"+randCode);
        Runtime.getRuntime().gc();
        System.out.println("Memory used after these steps: "+(usedMemory()-totalMemory));
        nodeList = null;
        sRuntime.runFinalization();
        sRuntime.gc();
        
    }
    public static void testJena() {
        sRuntime.gc();
        nodeList = new ArrayList();
        long totalMemory = usedMemory();
        com.hp.hpl.jena.rdf.model.Model model = com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel();
        com.hp.hpl.jena.rdf.model.Property labelProperty = model.createProperty(LABEL);
        long tStart = System.currentTimeMillis();
        for(int i=0;i<N_OBJECTS;i++) {
            LWNode node = new LWNode("test");
            nodeList.add(node);
            com.hp.hpl.jena.rdf.model.Resource resource = model.createResource(node.getID());
        }
        System.out.println("Time taken for inititation(ms): "+(System.currentTimeMillis()-tStart));
        sRuntime.gc();
        System.out.println("Memory Used: "+(usedMemory()-totalMemory));
        tStart = System.currentTimeMillis();
        for(int i=0;i<N_SETS;i++) {
            int rand = (int)Math.random()*(N_OBJECTS-1);
            model.add(model.getResource(""+rand),labelProperty,"test");
        }
        int randCode = (int)(Math.random()*(N_OBJECTS-1));
        model.add(model.getResource(""+randCode),labelProperty,"test");
        System.out.println("Time taken for sets(ms): "+(System.currentTimeMillis()-tStart));
        tStart = System.currentTimeMillis();
        for(int i=0;i<N_SETS;i++) {
            int rand = (int)Math.random()*(N_OBJECTS-1);
            model.getResource(""+rand).getProperty(labelProperty);
        }
        System.out.println("Time taken for gets(ms): "+(System.currentTimeMillis()-tStart));
        tStart = System.currentTimeMillis();
        ResIterator iter = model.listSubjectsWithProperty(labelProperty);
        String j = "";
        boolean flag = true;
        if(iter.hasNext() && flag) {
           j = iter.nextResource().getURI();
           if(j.equalsIgnoreCase(CODED)){ flag = false;}
        }
        System.out.println("Time taken for search: "+(System.currentTimeMillis()-tStart));
        System.out.println("Coded found in: "+j+" randCode:"+randCode);
        
        
    }
    public static void main(String[] args) {
        System.out.println("STARTING TESTS");
        System.out.println("Creating objects: "+N_OBJECTS);
        System.out.println("Performing get/sets: "+N_SETS);
        testCreateObjects();
        System.out.println("Testing through Jena");
        testJena();
        
        // TODO code application logic here
    }
    
    public String getCODED() {
        return CODED;
    }
    
    private static long usedMemory() {
        return (sRuntime.totalMemory() - sRuntime.freeMemory());
    }
}
