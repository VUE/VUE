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

package edu.tufts.artifact.ui;
 
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import tufts.vue.VueResources;

public class ArtifactQueryEditor
extends javax.swing.JPanel
implements edu.tufts.vue.fsm.QueryEditor
{
    JTabbedPane tabbedPane;
    TitleSearchPane titleSearchPane;
    ArtistSearchPane artistSearchPane;

    public static final String DEFAULT_VALUE = "All";
    public static final String[] CLASSES = {DEFAULT_VALUE,"FAH001","FAH002","FAH004","FAH006","FAH007","FAH008","FAH009","FAH010","FAH011","FAH015","FAH020","FAH021","FAH023","FAH041A","FAH052","FAH053","FAH054","FAH055","FAH120","FAH127","FAH129","FAH192A","FAH192B","FAH192C","FAH45A","FAH54A","FAH61A","FAH80A"};
    public static final String[] SUBJECTS = {DEFAULT_VALUE, "aids","architecture","cinematography","environmental art","furnishings","graphics","installations","maps","metalwork","painting","performance art","photography","pottery","sculpture","stained glass","textiles"};
    public static final String[] ORIGIN = {DEFAULT_VALUE,"AFGHANISTAN","ALGERIA","ARGENTINA","ARMENIA","AUSTRIA","BELGIUM","BENIN","BOHEMIA","BRAZIL     ","CAMEROON","CENTRAL    ","CHI.TURK.","CHINA      ","CRUSADER","CUBA       ","CZECH.","DAHOMEY","EGYPT","EGYPT?SICIL","ENGLAND    ","EUROPE     ","FRANCE","GERMANY","GHANA      ","GREECE     ","GUATAMALA","HOLLAND","HUNGARY    ","INDIA      ","IRAN       ","IRAQ       ","IRELAND    ","ISRAEL     ","ITAL:SICILY","ITALY","IVORY COAST","JAPAN","JORDAN","KOREA","LA:CA:GUAT","LA:MEXICO","LA:SA:ARGEN","LA:SA:BOLIV","LA:SA:BRAZI","LA:SA:CHILE","LA:SA:COLOM","LA:SA:ECUAD","LA:SA:PERU","LA:SA:URUG","LA:SA:VENE","LA:SA:VENEZ","LA:WI:CUBA","LA:WI:DOMIN","MALI       ","MAURITANIA ","MEXICO","MOSAN","NETHERLANDS","NIGERIA    ","NORWAY     ","PALESTINE","RUSSIA","S.AFRICA","SAUDI ARAB","SCANDINAVIA","SICILY/EGYP","SIERRA LEON","SPAIN","SWITZERLAND","SYRIA      ","TIBET      ","TUNISIA    ","TURKESTAN  ","TURKEY     ","USA","USA:CT","USA:DC","USA:GA","USA:IL","USA:NY","USA:SC","WALES      ","WORLD","ZAIRE      ","ZIMBABWE"};
    public static final String[] NO_RESULTS = {DEFAULT_VALUE,"10","25","50","75","100"};

	private java.io.Serializable criteria = null;
	private org.osid.shared.Properties searchProperties = null;
	private org.osid.shared.Type searchType = null;
	
	protected javax.swing.event.EventListenerList listenerList = new javax.swing.event.EventListenerList();
	
	public ArtifactQueryEditor() {
		tabbedPane = new JTabbedPane();
		titleSearchPane = new TitleSearchPane();
		artistSearchPane = new ArtistSearchPane();
		tabbedPane.addTab("Title",titleSearchPane);
		tabbedPane.addTab("Artist",artistSearchPane);
		tabbedPane.setSelectedComponent(titleSearchPane);
		setLayout(new BorderLayout());
		add(tabbedPane,BorderLayout.CENTER);
		setVisible(true);
		try {
			searchProperties = new edu.tufts.vue.util.SharedProperties();
		} catch (Throwable t) {
		}		
	}

	public void refresh() {
		
	}
	
	public void setSearchType(org.osid.shared.Type searchType)
	{
		this.searchType = searchType;
	}
	
	public org.osid.shared.Type getSearchType()
	{
		return this.searchType;
	}
	
    public class TitleSearchPane extends JPanel implements ActionListener,KeyListener {
        JLabel titleLabel = new JLabel(VueResources.getString("jlabel.title"));
        JLabel classLabel = new JLabel(VueResources.getString("jlabel.class"));
        JLabel subjectLabel = new JLabel(VueResources.getString("jlabel.subject"));
        JLabel originLabel = new JLabel(VueResources.getString("jlabel.origin"));
        JLabel resultsLabel = new JLabel(VueResources.getString("jlabel.maxhits"));
        JTextField titleField = new JTextField("");
        JComboBox classField = new JComboBox(CLASSES);
        JComboBox subjectField = new JComboBox(SUBJECTS);
        JComboBox originField = new JComboBox(ORIGIN);
        JComboBox resultsField = new JComboBox(NO_RESULTS);
        JButton searchButton = new JButton(VueResources.getString("button.search.label"));
        
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
			criteria = getSearchURL();
			fireSearch(new edu.tufts.vue.fsm.event.SearchEvent(this));
        }
        
        public String getSearchURL() {
            String searchURL = "http://artifact.tufts.edu/vue_xml/search2.asp?";
            searchURL += "query="+titleField.getText();
            searchURL += "&class_num=";
            if(!(classField.getSelectedItem() == null || classField.getSelectedItem().toString().equals(ArtifactQueryEditor.DEFAULT_VALUE))) {
                searchURL += classField.getSelectedItem();
            }
            searchURL += "&subject=";
            if(!(subjectField.getSelectedItem() == null || subjectField.getSelectedItem().toString().equals(ArtifactQueryEditor.DEFAULT_VALUE))) {
                searchURL += subjectField.getSelectedItem();
            }
            searchURL += "&origin=";
            if(!(originField.getSelectedItem() == null || originField.getSelectedItem().toString().equals(ArtifactQueryEditor.DEFAULT_VALUE))) {
                searchURL += originField.getSelectedItem();
            }
            searchURL += "&max_return=";
            if(!(resultsField.getSelectedItem() == null || resultsField.getSelectedItem().toString().equals(ArtifactQueryEditor.DEFAULT_VALUE))) {
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
    	JLabel titleLabel = new JLabel(VueResources.getString("jlabel.title"));
        JLabel classLabel = new JLabel(VueResources.getString("jlabel.class"));
        JLabel subjectLabel = new JLabel(VueResources.getString("jlabel.subject"));
        JLabel originLabel = new JLabel(VueResources.getString("jlabel.origin"));
        JLabel resultsLabel = new JLabel(VueResources.getString("jlabel.maxhits"));
        JTextField titleField = new JTextField("");
        JComboBox classField = new JComboBox(CLASSES);
        JComboBox subjectField = new JComboBox(SUBJECTS);
        JComboBox originField = new JComboBox(ORIGIN);
        JComboBox resultsField = new JComboBox(NO_RESULTS);
        JButton searchButton = new JButton(VueResources.getString("button.search.label"));
        
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
			criteria = getSearchURL();
			fireSearch(new edu.tufts.vue.fsm.event.SearchEvent(this));
        }
        
        public String getSearchURL() {
            String searchURL = "http://artifact.tufts.edu/vue_xml/search.asp?";
            searchURL += "query="+titleField.getText();
            searchURL += "&class_num=";
            if(!(classField.getSelectedItem() == null || classField.getSelectedItem().toString().equals(ArtifactQueryEditor.DEFAULT_VALUE))) {
                searchURL += classField.getSelectedItem();
            }
            searchURL += "&subject=";
            if(!(subjectField.getSelectedItem() == null || subjectField.getSelectedItem().toString().equals(ArtifactQueryEditor.DEFAULT_VALUE))) {
                searchURL += subjectField.getSelectedItem();
            }
            searchURL += "&origin=";
            if(!(originField.getSelectedItem() == null || originField.getSelectedItem().toString().equals(ArtifactQueryEditor.DEFAULT_VALUE))) {
                searchURL += originField.getSelectedItem();
            }
            searchURL += "&max_return=";
            if(!(resultsField.getSelectedItem() == null || resultsField.getSelectedItem().toString().equals(ArtifactQueryEditor.DEFAULT_VALUE))) {
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
	
	public void addSearchListener(edu.tufts.vue.fsm.event.SearchListener listener)
	{
		listenerList.add(edu.tufts.vue.fsm.event.SearchListener.class, listener);
	}
    
	public void removeSearchListener(edu.tufts.vue.fsm.event.SearchListener listener)
	{
		listenerList.remove(edu.tufts.vue.fsm.event.SearchListener.class, listener);
	}
    
	private void fireSearch(edu.tufts.vue.fsm.event.SearchEvent evt) 
	{
		Object[] listeners = listenerList.getListenerList();
		for (int i=0; i<listeners.length; i+=2) {
			if (listeners[i] == edu.tufts.vue.fsm.event.SearchListener.class) {
				((edu.tufts.vue.fsm.event.SearchListener)listeners[i+1]).searchPerformed(evt);
			}
		}
	}
	
	public java.io.Serializable getCriteria() {
		return criteria;
	}
	
	public void setCriteria(java.io.Serializable searchCriteria) {
	}
	
	public org.osid.shared.Properties getProperties() {
		return this.searchProperties;
	}
	
	public void setProperties(org.osid.shared.Properties searchProperties) {
		this.searchProperties = searchProperties;
	}

	public String getSearchDisplayName() {
		// return the criteria, no longer than 20 characters worth
		String s =  (String)getCriteria();
		if (s.length() > 20) s = s.substring(0,20) + "...";
		return s;
	}
}