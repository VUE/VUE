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
 *
 * @author  RSaigal
 */
public class TuftsGoogle extends JPanel implements ActionListener{
    JTabbedPane googlePane; 
    JComboBox maxReturns;
    JPanel googleResultsPanel;
      JTextField keywords;
     String[] maxReturnItems = { 
            "5",
            "10",
            "20" 
      };
    private static  String searchURL;
    private static URL  XML_MAPPING = VueResources.getURL("mapping.google");
    private static String query;
    
    private static int NResults = 10;
    
    private static String result ="";
     private static URL url;
      
    /** Creates a new instance of TuftsGoogle */
    public TuftsGoogle() {
         
           
         setLayout(new BorderLayout());
         maxReturns = new JComboBox(maxReturnItems);
         maxReturns.setEditable(true);
         googlePane = new JTabbedPane();
         JPanel googleSearch = new JPanel(new BorderLayout());
         
         
        JPanel googleSearchPanel = new JPanel();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        googleSearchPanel.setLayout(gridbag);
        Insets defaultInsets = new Insets(2,2,2,2);
        c.fill = GridBagConstraints.HORIZONTAL;
        
        //adding the top label   
        c.weightx = 1.0;
        c.gridx=0;
        c.gridy=0;
        c.gridwidth=3;
        c.ipady = 10;
        c.insets = defaultInsets;
        c.anchor = GridBagConstraints.NORTH;
        JLabel topLabel = new JLabel("Search Tufts Google");
        gridbag.setConstraints(topLabel, c);
        googleSearchPanel.add(topLabel);
        
        
    //adding the search box and the button
        c.gridx=0;
        c.gridy=1;
        c.gridwidth=2;
        c.ipady=0;
       keywords = new JTextField();
        keywords.setPreferredSize(new Dimension(120,20));
        gridbag.setConstraints(keywords, c);
        googleSearchPanel.add(keywords);
        
        c.gridx=2;
        c.gridy=1;
        c.gridwidth=1;
        JButton searchButton = new JButton("Search");
        searchButton.setPreferredSize(new Dimension(80,20));
        searchButton.addActionListener(this);
        gridbag.setConstraints(searchButton,c);
        googleSearchPanel.add(searchButton);
        
      // adding the number of search results tab.
        c.gridx=0;
        c.gridy=2;
        c.gridwidth=2;
        JLabel returnLabel = new JLabel("Maximum number of returns?");
        gridbag.setConstraints(returnLabel, c);
        googleSearchPanel.add(returnLabel);
        
        c.gridx=2;
        c.gridy=2;
        c.gridwidth=1;
        maxReturns.setPreferredSize(new Dimension(40,20));
        gridbag.setConstraints(maxReturns,c);
        googleSearchPanel.add(maxReturns);
        //------------------
        
        
         googleSearch.add(googleSearchPanel,BorderLayout.NORTH);
         googlePane.addTab("Search",googleSearch);
         
          
         googleResultsPanel = new JPanel(new BorderLayout());
         googlePane.addTab("Search Results",googleResultsPanel);
         add(googlePane,BorderLayout.CENTER );
        
//--------------------------------------------------------------------
            searchURL = VueResources.getString("url.google");
            

    }
    
    public void actionPerformed(ActionEvent e) {
        
          int index = 0;  
              
                
                String searchString = keywords.getText();
                
                        
                
               if (!searchString.equals("")){
                
                try {
                    
                   
       url = new URL(searchURL+"&num="+maxReturns.getSelectedItem().toString()+"&q="+searchString);
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
               resultVector.add(r);
              //resultVector.add(r.get());
              // resultVector.add(r.getTitle());
               System.out.println(r.getTitle()+" "+r.getUrl());

             
           } 
          
                VueDragTree tree = new VueDragTree(resultVector.iterator(),"GoogleSearchResults");
                tree.setEditable(true);
                tree.setRootVisible(false);
              
            

                JScrollPane jsp = new JScrollPane(tree);
         
                    
                
                 googleResultsPanel.add(jsp,BorderLayout.CENTER,0);
                 googlePane.setSelectedComponent(googleResultsPanel);    
                    
              
        } catch (Exception ex) {System.out.println("cannot connect google");
        
        
                               }
 
                }
                
        
        
       
    }
    
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
    
    
}
