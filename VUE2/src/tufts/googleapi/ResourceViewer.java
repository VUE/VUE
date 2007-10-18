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
 * -------/*
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
 * -------/*
 * ResourceViewer.java
 *
 * Created on August 5, 2004, 1:10 PM
 */

package  tufts.googleapi;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import java.util.*;

import tufts.vue.*;

import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import java.io.OutputStreamWriter;

import org.xml.sax.InputSource;
import com.google.soap.search.*;
/**
 *
 * @author  akumar03
 *
 * This class is to view the resources generated from Artifact Search.
 * The resources generated are similar to google resources.
 */
public class ResourceViewer extends JPanel implements ActionListener,KeyListener{
    public static final String DIRECTIVE = "search";
     
    JTabbedPane tabbedPane;
    ResultsPane resultsPane;
    SearchPane searchPane;
    String key;
    /** Creates a new instance of ResourceViewer */
    public ResourceViewer(String key) {
        this.key = key;
        tabbedPane = new JTabbedPane();
        resultsPane = new ResultsPane();
        searchPane = new  SearchPane(key);
        tabbedPane.addTab("Search",searchPane);
        tabbedPane.addTab("Results",resultsPane);
        tabbedPane.setSelectedComponent(searchPane);
        setLayout(new BorderLayout());
        add(tabbedPane,BorderLayout.CENTER);
        setVisible(true);
    }
    
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        ResourceViewer rv = new ResourceViewer(args[1]);
        JFrame frame = new JFrame();
        frame.getContentPane().add(rv);
        frame.setVisible(true);
        // TODO code application logic here
    }
    
    public void actionPerformed(ActionEvent e) {
    }
    
    public void keyPressed(KeyEvent e) {
    }
    
    public void keyReleased(KeyEvent e) {
    }
    
    public void keyTyped(KeyEvent e) {
    }
    
    
    public class SearchPane extends JPanel implements ActionListener,KeyListener {
        JLabel keywordsLabel = new JLabel("Keywords: ");
        JTextField keywordsField = new JTextField("");
        //JComboBox resultsField = new JComboBox(NO_RESULTS);
        JButton searchButton = new JButton("Search");
        
        public SearchPane(String key) {
            JPanel innerPanel = new JPanel();
            searchButton.addActionListener(this);
            keywordsField.addKeyListener(this);
            
            GridBagLayout gridbag = new GridBagLayout();
            GridBagConstraints c = new GridBagConstraints();
            innerPanel.setLayout(gridbag);
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(3,5,3,5);
            
            c.gridwidth = GridBagConstraints.RELATIVE;
            c.weightx = 0.0;
            gridbag.setConstraints(keywordsLabel, c);
            innerPanel.add(keywordsLabel);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            gridbag.setConstraints(keywordsField, c);
            innerPanel.add(keywordsField);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 0.0;
            c.anchor = GridBagConstraints.EAST;
            gridbag.setConstraints(searchButton, c);
            innerPanel.add(searchButton);
            
            
            setLayout(new BorderLayout());
            add(innerPanel,BorderLayout.NORTH);
            
        }
        
        public void performSearch() {
            Thread t = new Thread() {
                public void run() {
                    searchButton.setEnabled(false);
                    try {
                        GoogleSearch s = new GoogleSearch();
                        s.setKey(ResourceViewer.this.key);
                        s.setQueryString(keywordsField.getText());
                        GoogleSearchResult r = s.doSearch();
                        resultsPane.displayResults(r);
                        tabbedPane.setSelectedComponent(resultsPane);
                    } catch(Exception ex){
                        VueUtil.alert(SearchPane.this,"Search returned no results. Try again.","No Results");
                    }finally {
                        searchButton.setEnabled(true);
                    }
                }
            };
            t.start();
        }
        
        
        public void actionPerformed(ActionEvent e) {
            if(e.getActionCommand().equals("Search")) {
                performSearch();
            }
        }
        
        public void keyPressed(KeyEvent e) {
        }
        
        public void keyReleased(KeyEvent e) {
        }
        
        public void keyTyped(KeyEvent e) {
            if(e.getKeyChar()== KeyEvent.VK_ENTER) {
                performSearch();
            }
        }
        
    }
    public class ResultsPane extends JPanel {
        public ResultsPane() {
            setLayout(new BorderLayout());
        }
        
        public void displayResults(com.google.soap.search.GoogleSearchResult searchResult) {
            GoogleSearchResultElement elements[] = searchResult.getResultElements();
            Vector resourceVector = new Vector();
            for(int i =0; i< elements.length; i++) {
                Resource resource = Resource.getFactory().get(elements[i].getURL());
                resource.setTitle(elements[i].getTitle().replaceAll("</*[a-zA-Z]>",""));
                //resource.setSpec(elements[i].getURL());
                resourceVector.add(resource);
            }
            
            VueDragTree tree = new VueDragTree(resourceVector,"Google Results");
            JScrollPane jsp = new JScrollPane(tree);
            tree.setRootVisible(false);
            removeAll();
            add(jsp,BorderLayout.CENTER);
        }
        
        
    }
    
    
}
