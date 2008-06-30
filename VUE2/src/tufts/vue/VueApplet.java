/*
 * Copyright 2003-2008 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package tufts.vue;

import java.awt.*;
import java.applet.*;
import javax.swing.*;


// To get to run from classpath: had to be sure to create a symlink to VueResources.properties
// from the build/classes tree to the source tree.

// Of course, now need all the damn support libraries...

/**
 * Experimental VUE applet.
 *
 * @version $Revision: 1.4 $ / $Date: 2008-06-30 20:52:54 $ / $Author: mike $ 
 */
public class VueApplet extends JApplet implements Runnable {

    private JLabel loadLabel;
    private MapViewer viewer;
    // If want to have viewer left in same state when go forwrd/back in browser,
    // will need javacript assist to associate the viewer with the instance of a web browser page:
    // new applet & context objects are created even when you go forward/back pages.  

    public void init() {

        msg("init\n\tapplet=" + Integer.toHexString(hashCode()) + "\n\tcontext=" + getAppletContext());

        //setBackground(Color.blue);
        
        /*
        JPanel content= new JPanel();
        content.setBackground(Color.yellow);
        setContentPane(content);
        */
        
        if (viewer != null) {
            setContentPane(viewer);
        } else {
            //JPanel panel = new JPanel();
            //panel.setBackground(Color.orange);
            loadLabel = new JLabel("Loading VUE...");
            //label.setBackground(Color.orange);
            //Box box = Box.createHorizontalBox();
            getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.X_AXIS));
            //getContentPane().setLayout(new FlowLayout());
            //box.setOpaque(false);
            //setContentPane(box);
            getContentPane().add(Box.createHorizontalGlue());
            getContentPane().add(loadLabel);
            getContentPane().add(Box.createHorizontalGlue());
            //getContentPane().add(label, BorderLayout.CENTER);
            //setContentPane(label);

            new Thread(this).start();
            
        }
        //installMapViewer();

        msg("init completed");
    }

    // Load the MapViewer, triggering massive class loading...
    public void run() {
        msg("load thread started...");
        try {
            loadViewer();
        } catch (Throwable t) {
            loadLabel.setText(t.toString());
        }
        msg("load thread complete");
    }

    public void start() {
        msg("start");
    }
    
    public void stop() {
        msg("stop");
    }
    
    public void loadViewer() {
        VUE.setAppletContext(getAppletContext());
        viewer = getMapViewer();
        msg("got viewer");

        if (false) {
            JScrollPane scrollPane = new JScrollPane(viewer);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            setContentPane(scrollPane);
        } else {
            setContentPane(viewer);
        }
        
        msg("contentPane set");
        msg("setting menu bar...");
        setJMenuBar(new tufts.vue.gui.VueMenuBar());
        msg("validating...");
        validate();
        msg("loading complete");
        //getContentPane().add(viewer, BorderLayout.CENTER);
    }

    private void msg(String s) {
        System.out.println("VueApplet: " + s);
        //showStatus("VueApplet: " + s);
    }


    private void installMapViewer() {
        LWMap map = new LWMap("Applet Test Map");

        VUE.installExampleMap(map);

        MapViewer v = new MapViewer(map);

        setContentPane(v);
        //getContentPane().add(viewer, BorderLayout.CENTER);
    }

    private MapViewer getMapViewer() {
        LWMap map = new LWMap("Applet Test Map");

        VUE.installExampleMap(map);

        return new MapViewer(map);
    }
    
        

}