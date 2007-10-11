/*
 * -----------------------------------------------------------------------------
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
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/*
 * Publish.java
 *
 * Created on December 24, 2003, 1:46 PM
 */

package tufts.vue.action;

import javax.swing.AbstractAction;
import javax.swing.*;
import java.io.*;
import tufts.vue.*;
import java.util.*;
import java.net.*;
import org.apache.commons.net.ftp.*;
import java.awt.*;

import fedora.server.management.FedoraAPIM;
import fedora.server.utilities.StreamUtility;
//import fedora.client.ingest.AutoIngestor;

/**
 *
 * @author  akumar03
 */
public class Publish extends VueAction  {
    public static final org.osid.shared.Type fedoraRepositoryType = new edu.tufts.vue.util.Type("tufts.edu","repository","fedora_2_2");
    private static JMenu menu;
    public static final String LABEL = "Publish";
    /** Creates a new instance of Publish */
    
    
    String fileName;
    String tempMETSfileName;
    File activeMapFile;
    //Frame owner;
    String label;
    public Publish() {
    }
    public Publish(String label) {
        super(label);
        this.label  = label;
         //edu.tufts.vue.dsm.impl.VueDataSourceManager.getInstance().addDataSourceListener(this);
   
    }
    
    
    /*
    public Publish(Frame owner,String label) {
        super(label);
        this.owner = owner;
        this.label  = label;
    }
     */
    public void act() {
        try {
            Publisher publisher = new Publisher(VUE.getDialogParentAsFrame(), LABEL);
            //Publisher publisher = new Publisher(owner,label);
        } catch (Exception ex) {
            VueUtil.alert(null, ex.getMessage(), "Publish Error");
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
    
}
