/*
 * MapOpen.java
 *
 * Created on December 8, 2006, 11:59 AM
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2006
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 *
 * @author akumar03
 */

package edu.tufts.vue;


import junit.framework.TestCase;
import java.net.*;
import tufts.vue.*;
import tufts.vue.action.*;
import java.io.*;
import java.util.*;

public class MapOpen  extends TestCase {
    public static final int ACTUAL_FILE_COUNT = 0;
    public static final String FILE_COUNT_ERROR = "Error: Number of files in start up map don't match the default value";
    public void testManager() {
        try{
            URL  startUp = VueResources.getURL("resource.startmap");
            LWMap map = OpenAction.loadMap(startUp);
            // check whether the url of resouces is in correct format
            int fileCount = 0;
            for(Iterator i =  map.getChildIterator(); i.hasNext(); ) {
                LWComponent component = (LWComponent) i.next();
                if(component.hasResource()){
                    Resource resource = component.getResource();
                    File file = new File(resource.getSpec());
                    if(file.isFile()) {
                        fileCount++;
                    }
                }
            }
            if(fileCount != ACTUAL_FILE_COUNT) {
                System.out.println(FILE_COUNT_ERROR+" FileCount: "+fileCount);
            }
            
        } catch(Exception ex) {
            System.out.println("MapOpen.testManager: "+ex);
        }
    }
}
