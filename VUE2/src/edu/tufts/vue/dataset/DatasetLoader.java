/*
 * DatasetLoader.java
 *
 * Created on July 23, 2008, 2:31 PM
 *
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

/**
 *
 * @author akumar03
 */

package edu.tufts.vue.dataset;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;
import java.util.Properties;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import tufts.vue.DataSetViewer;
import tufts.vue.DataSourceViewer;
import tufts.vue.LWComponent;
import tufts.vue.LWMap;
import tufts.vue.VUE;
import tufts.vue.VueResources;
import tufts.vue.ds.DataAction;
import tufts.vue.ds.XmlDataSource;
import edu.tufts.vue.layout.RelRandomLayout;

public class DatasetLoader {
    public static final String LOADER_LABEL = VueResources.getString("dialog.datasettype.title");
    JDialog dialog;
    Dataset ds;
    private String fileName;
    /** Creates a new instance of DatasetLoader */
    public DatasetLoader() {
    }
    
    public Dataset load(String fileName) throws Exception {
        this.fileName = fileName;
        if(dialog == null) {
            dialog = new JDialog(tufts.vue.VUE.getApplicationFrame(),LOADER_LABEL);
            dialog.setContentPane(createDatasetsTypePanel());
            dialog.pack();
            dialog.setModal(true);
            dialog.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent we) {
                    
                }
            });
        }
        dialog.setVisible(true);
        return ds;
    }
    public Dataset load(File file) throws Exception {
        return load(file.getAbsolutePath());
    }
    
    private JPanel createDatasetsTypePanel() {
        final int numButtons = 3;
        JRadioButton[] radioButtons = new JRadioButton[numButtons];
        final ButtonGroup group = new ButtonGroup();
        final String listDS = "List";
        final String relationalDS= "Relational";
        final String folderDS = "Folder";
        
        
        radioButtons[0] = new JRadioButton(VueResources.getString("jbutton.list.label"));
        radioButtons[0].setActionCommand(listDS);
        
        radioButtons[1] = new JRadioButton(VueResources.getString("jbutton.relational.label"));
        radioButtons[1].setActionCommand(relationalDS);
        
        
        radioButtons[2] = new JRadioButton(VueResources.getString("jbutton.folder.label"));
        radioButtons[2].setActionCommand(folderDS);
        for (int i = 0; i < numButtons; i++) {
            group.add(radioButtons[i]);
        }
        radioButtons[0].setSelected(true);
        JButton loadButton = new JButton(VueResources.getString("jbutton.load.label"));
        loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String command = group.getSelection().getActionCommand();
                if (command == relationalDS) {
                    ds = new RelationalDataset();
                    ds.setLayout(new RelRandomLayout());
                } else if(command == folderDS) {
                    ds = new FolderDataset();
                }else {
                    ds  = new ListDataset();
                }
                ds.setFileName(fileName);
                try {
                    ds.loadDataset();
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
              //  ds.createOntology();
                dialog.setVisible(false);
            }
        });
        
        return createPane(VueResources.getString("pane.dataset.message"),radioButtons, loadButton);
        
        
    }
    
    
    
    
    private JPanel createPane(String description,
            JRadioButton[] radioButtons,
            JButton showButton) {
        
        int numChoices = radioButtons.length;
        JPanel box = new JPanel();
        JLabel label = new JLabel(description);
        
        box.setLayout(new BoxLayout(box, BoxLayout.PAGE_AXIS));
        box.add(label);
        
        for (int i = 0; i < numChoices; i++) {
            box.add(radioButtons[i]);
        }
        
        JPanel pane = new JPanel(new BorderLayout());
        pane.add(box, BorderLayout.PAGE_START);
        pane.add(showButton, BorderLayout.PAGE_END);
        return pane;
    }
    
    private  String getMapName(String fileName) {
        String mapName = fileName.substring(fileName.lastIndexOf(File.separator)+1,fileName.length());
        if(mapName.lastIndexOf(".")>0)
            mapName = mapName.substring(0,mapName.lastIndexOf("."));
        if(mapName.length() == 0)
            mapName = "Text Import";
        return mapName;
    }
    
}
