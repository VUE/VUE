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
 * TuftsGoogle.java
 *
 * Created on December 1, 2003, 9:05 PM
 */

package tufts.vue;

import tufts.google.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import java.util.Vector;
import java.util.Iterator;



import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import java.io.OutputStreamWriter;

import org.xml.sax.InputSource;

/**
 * @author  RSaigal
 * @version $Revision: 1.20 $ / $Date: 2007-10-06 03:06:57 $ / $Author: sfraize $ 
 */
public class TuftsGoogle extends JPanel implements ActionListener,KeyListener{
    JTabbedPane googlePane;
    JComboBox maxReturns;
    JPanel googleResultsPanel;
    String searchURL;
    int prevStartIndex = 0;
    int nextStartIndex = 0;
    JScrollPane jsp = new JScrollPane();
    JTextField keywords;
    JButton prevButton;
    JButton nextButton;
    JButton searchButton;
    String[] maxReturnItems = {
        "10",
        "20",
        "30"
    };
    //private static  String searchURL;
    private static URL  XML_MAPPING = VueResources.getURL("mapping.google");
    private static String query;
    private static int NResults = 10;
    private static String result ="";
    private static URL url;
    
    /** Creates a new instance of TuftsGoogle */
    public TuftsGoogle(String displayName, String inputURL) {
        setLayout(new BorderLayout());
        searchURL = inputURL;
        if (DEBUG.INIT || DEBUG.IO)
            System.out.println("TuftsGoogle: \"" + displayName + "\" url [" + inputURL + "]");
        maxReturns = new JComboBox(maxReturnItems);
        maxReturns.setEditable(true);
        googlePane = new JTabbedPane();
        JPanel googleSearch = new JPanel(new BorderLayout());
        JLabel returnLabel = new JLabel("Maximum number of returns?");
        returnLabel.setFont(new Font("Arial",Font.PLAIN, 12));
        JPanel googleSearchPanel = new JPanel();
        /**
         * @Setup  searchPanel
         */
        // googleSearch = new JPanel(new BorderLayout());
        googleSearch.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        googleSearchPanel.setLayout(gridbag);
        Insets defaultInsets = new Insets(2,2,2,2);
        
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 1;
        c.insets = new Insets(10,2,2, 2);
        JLabel keyLabel = new JLabel("Keywords: ");
        keyLabel.setFont(new Font("Arial",Font.PLAIN, 12));
        gridbag.setConstraints(keyLabel,c);
        googleSearchPanel.add(keyLabel);
        
        
        //adding the search box
        c.gridx=1;
        c.gridy=1;
        c.gridwidth=2;
        c.insets = new Insets(10, 2,2, 2);
        keywords = new JTextField();
        //keywords.setPreferredSize(new Dimension(120,20));
        keywords.addKeyListener(this);
        gridbag.setConstraints(keywords, c);
        googleSearchPanel.add(keywords);
        
        // adding the number of search results tab.
        c.gridx=0;
        c.gridy=2;
        c.gridwidth=2;
        c.insets = defaultInsets;
        
        gridbag.setConstraints(returnLabel, c);
        googleSearchPanel.add(returnLabel);
        
        c.gridx=2;
        c.gridy=2;
        c.gridwidth=1;
         maxReturns.setPreferredSize(new Dimension(100,20));
        gridbag.setConstraints(maxReturns,c);
        googleSearchPanel.add(maxReturns);
        
        c.gridx=2;
        c.gridy=3;
        c.insets = new Insets(10, 2,2,2);
        searchButton = new JButton("Search");
        //searchButton.setPreferredSize(new Dimension(40,20));
        searchButton.addActionListener(this);
        gridbag.setConstraints(searchButton,c);
        googleSearchPanel.add(searchButton);
        
        googleSearch.add(googleSearchPanel,BorderLayout.NORTH);
        googlePane.addTab("Search",googleSearch);
        
        
        googleResultsPanel = new JPanel(new BorderLayout());
        googlePane.addTab("Search Results",googleResultsPanel);
        add(googlePane,BorderLayout.CENTER );
        //JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,2, 0));
        
        prevButton = new JButton("Previous");
        prevButton.setPreferredSize(new Dimension(120,20));
        prevButton.addActionListener(this);
        prevButton.setEnabled(false);
        nextButton = new JButton("Next");
        nextButton.setPreferredSize(new Dimension(80,20));
        nextButton.addActionListener(this);
        
        
    }
    
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().toString() == "Search") {
            Thread t = new Thread() {
                public void run() {
                    performSearch(0);
                }
            };
            t.start();
            nextStartIndex = nextStartIndex+ Integer.parseInt(maxReturns.getSelectedItem().toString());
            prevStartIndex = 0;
        }
        if (e.getActionCommand().toString() == "Previous"){
            Thread t = new Thread() {
                public void run() {
                    performSearch(prevStartIndex);
                }
            };
            t.start();
            if ( prevStartIndex == 0){prevButton.setEnabled(false);}
            else {
                prevStartIndex = prevStartIndex - Integer.parseInt(maxReturns.getSelectedItem().toString());
                nextStartIndex = nextStartIndex  - Integer.parseInt(maxReturns.getSelectedItem().toString());
            }
        }
        if (e.getActionCommand().toString() == "Next"){
            if (nextStartIndex >0 )prevButton.setEnabled(true);
            Thread t = new Thread() {
                public void run() {
                    performSearch(nextStartIndex);
                }
            };
            prevStartIndex = nextStartIndex - Integer.parseInt(maxReturns.getSelectedItem().toString());
            nextStartIndex = nextStartIndex+ Integer.parseInt(maxReturns.getSelectedItem().toString());
        }
    }
    
    public void performSearch(int searchStartIndex){
        int index = 0;
        String searchString = keywords.getText();
        searchButton.setEnabled(false);
        if (!searchString.equals("")){
            try {
                url = new URL(searchURL+"&num="+maxReturns.getSelectedItem().toString()+"&start="+searchStartIndex+"&q="+searchString);
                System.out.println("Google search = "+url);
                InputStream input = url.openStream();
                int c;
                while((c=input.read())!= -1) {
                    result = result + (char) c;
                }
                String googleResultsFile = VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separatorChar+VueResources.getString("save.google.results");
                FileWriter fileWriter = new FileWriter(googleResultsFile);
                fileWriter.write(result);
                fileWriter.close();
                result = "";
                
                GSP gsp = loadGSP(googleResultsFile);
                Iterator i = gsp.getRES().getResultList().iterator();
                Vector resultVector = new Vector();
                
                while(i.hasNext()) {
                    Result r = (Result)i.next();
                    Resource resource = Resource.getFactory().get(r.getUrl());
                    if (r.getTitle() != null)
                        resource.setTitle(r.getTitle().replaceAll("</*[a-zA-Z]>",""));
                    else
                        resource.setTitle(r.getUrl().toString());
                    resultVector.add(resource);
                    System.out.println(r.getTitle()+" "+r.getUrl());
                    
                }
                VueDragTree tree = new VueDragTree(resultVector.iterator(),"GoogleSearchResults");
                tree.setEditable(true);
                tree.setRootVisible(false);
                googleResultsPanel.remove(jsp);
                jsp = new JScrollPane(tree);
                JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,2, 0));
                //JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,2, 0));
                bottomPanel.add(prevButton);
                bottomPanel.add(nextButton);
                googleResultsPanel.add(bottomPanel,BorderLayout.SOUTH);
                googleResultsPanel.add(jsp,BorderLayout.CENTER);
                googleResultsPanel.validate();
                googlePane.setSelectedComponent(googleResultsPanel);
            } catch (Exception ex) {
                System.out.println("cannot connect google");
                
                googleResultsPanel.remove(jsp);
                JPanel jp = new JPanel(new BorderLayout());
                jp.setBackground(Color.WHITE);
                JLabel jl = new JLabel("No Match Found");
                jp.add(jl,BorderLayout.NORTH);
                JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,2, 0));
                //JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,2, 0));
                
                googleResultsPanel.add(jp,BorderLayout.CENTER);
                googleResultsPanel.validate();
                googlePane.setSelectedComponent(googleResultsPanel);
                
                
            }
        }
        searchButton.setEnabled(true);
        
    }
    // Functions to support marshalling and unmarshalling of the reults generated through search using castor.
    private static GSP loadGSP(String filename) {
        
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
    
    private static GSP loadGSP(URL url) {
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
    private static Unmarshaller getUnmarshaller() {
        if (unmarshaller == null) {
            unmarshaller = new Unmarshaller();
            Mapping mapping = new Mapping();
            System.out.println("XML_MAPPING =" +XML_MAPPING);
            try {
                mapping.loadMapping(XML_MAPPING);
                unmarshaller.setMapping(mapping);
            } catch (Exception e) {
                System.out.println("TuftsGoogle.getUnmarshaller: " + XML_MAPPING+e);
            }
        }
        return unmarshaller;
    }
    public void keyPressed(KeyEvent e) {
    }
    
    public void keyReleased(KeyEvent e) {
    }
    
    public void keyTyped(KeyEvent e) {
        if(e.getKeyChar()== KeyEvent.VK_ENTER) {
            
            searchButton.doClick();
        }
    }
}



