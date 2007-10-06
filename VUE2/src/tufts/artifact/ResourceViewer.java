/*
 * ResourceViewer.java
 *
 * Created on August 5, 2004, 1:10 PM
 */

package  tufts.artifact;


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

/**
 *
 * @author  akumar03
 *
 * This class is to view the resources generated from Artifact Search.
 * The resources generated are similar to google resources.
 */
public class ResourceViewer extends JPanel implements ActionListener,KeyListener{
    public static final String PATH = "/tufts/artifact/";
    public static final String CASTOR_MAPPING = PATH+"artifact.xml";
    public static final String ARTIFACT_URL = "http://artifact.tufts.edu/vue_xml/search2.asp?";
    public static final String DEFAULT_VALUE = "All";
    public static final String[] CLASSES = {DEFAULT_VALUE,"FAH001","FAH002","FAH004","FAH006","FAH007","FAH008","FAH009","FAH010","FAH011","FAH015","FAH020","FAH021","FAH023","FAH041A","FAH052","FAH053","FAH054","FAH055","FAH120","FAH127","FAH129","FAH192A","FAH192B","FAH192C","FAH45A","FAH54A","FAH61A","FAH80A"};
    public static final String[] SUBJECTS = {DEFAULT_VALUE, "aids","architecture","cinematography","environmental art","furnishings","graphics","installations","maps","metalwork","painting","performance art","photography","pottery","sculpture","stained glass","textiles"};
    public static final String[] ORIGIN = {DEFAULT_VALUE,"AFGHANISTAN","ALGERIA","ARGENTINA","ARMENIA","AUSTRIA","BELGIUM","BENIN","BOHEMIA","BRAZIL     ","CAMEROON","CENTRAL    ","CHI.TURK.","CHINA      ","CRUSADER","CUBA       ","CZECH.","DAHOMEY","EGYPT","EGYPT?SICIL","ENGLAND    ","EUROPE     ","FRANCE","GERMANY","GHANA      ","GREECE     ","GUATAMALA","HOLLAND","HUNGARY    ","INDIA      ","IRAN       ","IRAQ       ","IRELAND    ","ISRAEL     ","ITAL:SICILY","ITALY","IVORY COAST","JAPAN","JORDAN","KOREA","LA:CA:GUAT","LA:MEXICO","LA:SA:ARGEN","LA:SA:BOLIV","LA:SA:BRAZI","LA:SA:CHILE","LA:SA:COLOM","LA:SA:ECUAD","LA:SA:PERU","LA:SA:URUG","LA:SA:VENE","LA:SA:VENEZ","LA:WI:CUBA","LA:WI:DOMIN","MALI       ","MAURITANIA ","MEXICO","MOSAN","NETHERLANDS","NIGERIA    ","NORWAY     ","PALESTINE","RUSSIA","S.AFRICA","SAUDI ARAB","SCANDINAVIA","SICILY/EGYP","SIERRA LEON","SPAIN","SWITZERLAND","SYRIA      ","TIBET      ","TUNISIA    ","TURKESTAN  ","TURKEY     ","USA","USA:CT","USA:DC","USA:GA","USA:IL","USA:NY","USA:SC","WALES      ","WORLD","ZAIRE      ","ZIMBABWE"};
    public static final String[] NO_RESULTS = {DEFAULT_VALUE,"10","25","50","75","100"};
    
    JTabbedPane tabbedPane;
    ResultsPane resultsPane;
    TitleSearchPane titleSearchPane;
    ArtistSearchPane artistSearchPane;
    SearchCriteria saerchCriteria;
    /** Creates a new instance of ResourceViewer */
    public ResourceViewer() {
        tabbedPane = new JTabbedPane();
        resultsPane = new ResultsPane();
        titleSearchPane = new TitleSearchPane();
        artistSearchPane = new ArtistSearchPane();
        tabbedPane.addTab("Title",titleSearchPane);
        tabbedPane.addTab("Artist",artistSearchPane);
        tabbedPane.addTab("Results",resultsPane);
        tabbedPane.setSelectedComponent(titleSearchPane);
        setLayout(new BorderLayout());
        add(tabbedPane,BorderLayout.CENTER);
        setVisible(true);
    }
    
    public  ArtifactResult loadArtifactResult(String query) throws Exception{
        Unmarshaller unmarshaller = getUnmarshaller();
        unmarshaller.setValidation(false);
        URL url = new URL(query);
        InputStream stream = url.openStream();
        ArtifactResult artifactResult = (ArtifactResult) unmarshaller.unmarshal(new InputSource(stream));
        return artifactResult;
        
    }
    
    
    public  Unmarshaller unmarshaller = null;
    public  Unmarshaller getUnmarshaller() {
        if (unmarshaller == null) {
            unmarshaller = new Unmarshaller();
            Mapping mapping = new Mapping();
            try {
                mapping.loadMapping(getClass().getResource(CASTOR_MAPPING));
                unmarshaller.setMapping(mapping);
            } catch (Exception ex) {
                System.out.println("ResourceViwer.getUnmarshaller: " + CASTOR_MAPPING+ex);
            }
        }
        return unmarshaller;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        ResourceViewer rv = new ResourceViewer();
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
    
    
    public class TitleSearchPane extends JPanel implements ActionListener,KeyListener {
        JLabel titleLabel = new JLabel("Title: ");
        JLabel classLabel = new JLabel("Class: ");
        JLabel subjectLabel = new JLabel("Subject: ");
        JLabel originLabel = new JLabel("Origin: ");
        JLabel resultsLabel = new JLabel("Maximum Hits:");
        JTextField titleField = new JTextField("");
        JComboBox classField = new JComboBox(CLASSES);
        JComboBox subjectField = new JComboBox(SUBJECTS);
        JComboBox originField = new JComboBox(ORIGIN);
        JComboBox resultsField = new JComboBox(NO_RESULTS);
        JButton searchButton = new JButton("Search");
        
        public TitleSearchPane() {
            JPanel innerPanel = new JPanel();
            searchButton.addActionListener(this);
            titleField.addKeyListener(this);
            
            GridBagLayout gridbag = new GridBagLayout();
            GridBagConstraints c = new GridBagConstraints();
            innerPanel.setLayout(gridbag);
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(3,5,3,5);
            
            c.gridwidth = GridBagConstraints.RELATIVE;
            c.weightx = 0.0;
            gridbag.setConstraints(titleLabel, c);
            innerPanel.add(titleLabel);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            gridbag.setConstraints(titleField, c);
            innerPanel.add(titleField);
            
            c.gridwidth = GridBagConstraints.RELATIVE;
            c.weightx = 0.0;
            gridbag.setConstraints(classLabel, c);
            innerPanel.add(classLabel);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            gridbag.setConstraints(classField, c);
            innerPanel.add(classField);
            
            c.gridwidth = GridBagConstraints.RELATIVE;
            c.weightx = 0.0;
            gridbag.setConstraints(subjectLabel, c);
            innerPanel.add(subjectLabel);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            gridbag.setConstraints(subjectField, c);
            innerPanel.add(subjectField);
            
            c.gridwidth = GridBagConstraints.RELATIVE;
            c.weightx = 0.0;
            gridbag.setConstraints(originLabel, c);
            innerPanel.add(originLabel);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            gridbag.setConstraints(originField, c);
            innerPanel.add(originField);
            
            c.gridwidth = GridBagConstraints.RELATIVE;
            c.weightx = 0.0;
            gridbag.setConstraints(titleLabel, c);
            innerPanel.add(resultsLabel);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            gridbag.setConstraints(resultsField, c);
            innerPanel.add(resultsField);
            
            
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
                        String query = getSearchURL();
                        ArtifactResult artifactResult = ResourceViewer.this.loadArtifactResult(query);
                        resultsPane.displayResults(artifactResult);
                        tabbedPane.setSelectedComponent(resultsPane);
                    } catch(Exception ex){
                        VueUtil.alert(TitleSearchPane.this,"Search returned no results. Try again.","No Results");
                    }finally {
                        searchButton.setEnabled(true);
                    }
                }
            };
            t.start();
        }
        
        public String getSearchURL() {
            String searchURL = "http://artifact.tufts.edu/vue_xml/search2.asp?";
            searchURL += "query="+titleField.getText();
            searchURL += "&class_num=";
            if(!(classField.getSelectedItem() == null || classField.getSelectedItem().toString().equals(ResourceViewer.DEFAULT_VALUE))) {
                searchURL += classField.getSelectedItem();
            }
            searchURL += "&subject=";
            if(!(subjectField.getSelectedItem() == null || subjectField.getSelectedItem().toString().equals(ResourceViewer.DEFAULT_VALUE))) {
                searchURL += subjectField.getSelectedItem();
            }
            searchURL += "&origin=";
            if(!(originField.getSelectedItem() == null || originField.getSelectedItem().toString().equals(ResourceViewer.DEFAULT_VALUE))) {
                searchURL += originField.getSelectedItem();
            }
            searchURL += "&max_return=";
            if(!(resultsField.getSelectedItem() == null || resultsField.getSelectedItem().toString().equals(ResourceViewer.DEFAULT_VALUE))) {
                searchURL += resultsField.getSelectedItem();
            }
            searchURL += "&Submit=Submit";
            return searchURL;
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
    
    public class ArtistSearchPane extends JPanel implements ActionListener,KeyListener {
        JLabel titleLabel = new JLabel("Title: ");
        JLabel classLabel = new JLabel("Class: ");
        JLabel subjectLabel = new JLabel("Subject: ");
        JLabel originLabel = new JLabel("Origin: ");
        JLabel resultsLabel = new JLabel("Maximum Hits:");
        JTextField titleField = new JTextField("");
        JComboBox classField = new JComboBox(CLASSES);
        JComboBox subjectField = new JComboBox(SUBJECTS);
        JComboBox originField = new JComboBox(ORIGIN);
        JComboBox resultsField = new JComboBox(NO_RESULTS);
        JButton searchButton = new JButton("Search");
        
        public ArtistSearchPane() {
            JPanel innerPanel = new JPanel();
            searchButton.addActionListener(this);
            titleField.addKeyListener(this);
            
            GridBagLayout gridbag = new GridBagLayout();
            GridBagConstraints c = new GridBagConstraints();
            innerPanel.setLayout(gridbag);
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(3,5,3,5);
            
            c.gridwidth = GridBagConstraints.RELATIVE;
            c.weightx = 0.0;
            gridbag.setConstraints(titleLabel, c);
            innerPanel.add(titleLabel);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            gridbag.setConstraints(titleField, c);
            innerPanel.add(titleField);
            
            c.gridwidth = GridBagConstraints.RELATIVE;
            c.weightx = 0.0;
            gridbag.setConstraints(classLabel, c);
            innerPanel.add(classLabel);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            gridbag.setConstraints(classField, c);
            innerPanel.add(classField);
            
            c.gridwidth = GridBagConstraints.RELATIVE;
            c.weightx = 0.0;
            gridbag.setConstraints(subjectLabel, c);
            innerPanel.add(subjectLabel);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            gridbag.setConstraints(subjectField, c);
            innerPanel.add(subjectField);
            
            c.gridwidth = GridBagConstraints.RELATIVE;
            c.weightx = 0.0;
            gridbag.setConstraints(originLabel, c);
            innerPanel.add(originLabel);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            gridbag.setConstraints(originField, c);
            innerPanel.add(originField);
            
            c.gridwidth = GridBagConstraints.RELATIVE;
            c.weightx = 0.0;
            gridbag.setConstraints(titleLabel, c);
            innerPanel.add(resultsLabel);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            gridbag.setConstraints(resultsField, c);
            innerPanel.add(resultsField);
            
            
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
                        String query = getSearchURL();
                        ArtifactResult artifactResult = ResourceViewer.this.loadArtifactResult(query);
                        resultsPane.displayResults(artifactResult);
                        tabbedPane.setSelectedComponent(resultsPane);
                    } catch(Exception ex){
                        VueUtil.alert(ArtistSearchPane.this,"Search returned no results. Try again.","No Results");
                    }finally {
                        searchButton.setEnabled(true);
                    }
                }
            };
            t.start();
        }
        
        public String getSearchURL() {
            String searchURL = "http://artifact.tufts.edu/vue_xml/search.asp?";
            searchURL += "query="+titleField.getText();
            searchURL += "&class_num=";
            if(!(classField.getSelectedItem() == null || classField.getSelectedItem().toString().equals(ResourceViewer.DEFAULT_VALUE))) {
                searchURL += classField.getSelectedItem();
            }
            searchURL += "&subject=";
            if(!(subjectField.getSelectedItem() == null || subjectField.getSelectedItem().toString().equals(ResourceViewer.DEFAULT_VALUE))) {
                searchURL += subjectField.getSelectedItem();
            }
            searchURL += "&origin=";
            if(!(originField.getSelectedItem() == null || originField.getSelectedItem().toString().equals(ResourceViewer.DEFAULT_VALUE))) {
                searchURL += originField.getSelectedItem();
            }
            searchURL += "&max_return=";
            if(!(resultsField.getSelectedItem() == null || resultsField.getSelectedItem().toString().equals(ResourceViewer.DEFAULT_VALUE))) {
                searchURL += resultsField.getSelectedItem();
            }
            searchURL += "&Submit=Submit";
            return searchURL;
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
        Hit hit;
        public ResultsPane() {
            setLayout(new BorderLayout());
        }
        public void displayResults(ArtifactResult artifactResult){
            Vector results = artifactResult.getHitList();
            Vector resourceVector = new Vector();
            Iterator i  = results.iterator();
            while(i.hasNext()){
                hit = (Hit)i.next();
                
                /**
                    private String thumb = hit.thumb;
                    public JComponent getPreview() {
                        try {
                            return  new JLabel(new ImageIcon(new URL(thumb)));
                        } catch(Exception ex) {
                            System.out.println(ex);
                            return super.getPreview();
                        }
                    }
                    
                };
                 */
                Resource resource = Resource.getFactory().get(hit.artifact);
                //Properties properties = new Properties();
                String displayTitle = hit.title;
                if(hit.artist.length() > 0)
                    displayTitle += " - " +hit.artist;
                resource.setTitle(displayTitle);
                //resource.setSpec(hit.artifact);
                resource.setProperty("Title", hit.title);
                resource.setProperty("Artist", hit.artist);
                resource.setProperty("Culture" ,hit.culture);
                resource.setProperty("Current Location",hit.currentLocation);
                resource.setProperty("Material",hit.material);
                resource.setProperty("Origin", hit.origin);
                resource.setProperty("Period", hit.period);
                resource.setProperty("Subject",hit.subject);
                resource.setProperty("View",hit.view);
                //resource.setProperties(properties);
                resourceVector.add(resource);
            }
            VueDragTree tree = new VueDragTree(resourceVector.iterator(),"Artifact Results");
            JScrollPane jsp = new JScrollPane(tree);
            tree.setRootVisible(false);
            removeAll();
            add(jsp,BorderLayout.CENTER);
        }
        
        
    }
    
    
}
