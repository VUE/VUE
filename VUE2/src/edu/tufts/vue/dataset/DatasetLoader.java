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


import java.util.*;
import java.io.*;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import au.com.bytecode.opencsv.CSVReader;

import edu.tufts.vue.layout.*;

public class DatasetLoader {
    public static final String LOADER_LABEL = "Dataset Type";
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
        final int numButtons = 2;
        JRadioButton[] radioButtons = new JRadioButton[numButtons];
        final ButtonGroup group = new ButtonGroup();
        final String listDS = "List";
        final String  relationalDS= "Relational";
        
        
        radioButtons[0] = new JRadioButton("List");
        radioButtons[0].setActionCommand(listDS);
        
        radioButtons[1] = new JRadioButton("Relational");
        radioButtons[1].setActionCommand(relationalDS);
        
        for (int i = 0; i < numButtons; i++) {
            group.add(radioButtons[i]);
        }
        radioButtons[0].setSelected(true);
        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String command = group.getSelection().getActionCommand();
                if (command == relationalDS) {
                    ds = new RelationalDataset();
                    ds.setLayout(new RelRandomLayout());
                }  else {
                    ds  = new ListDataset();
                }
                ds.setFileName(fileName);
                loadDataset();
                ds.createOntology();
                dialog.setVisible(false);
            }
        });
        
        return createPane("Please select the dataset type",radioButtons, loadButton);
        
        
    }
    
    private void loadDataset() {
        try {
            
            ds.rowList = new ArrayList<ArrayList<String>>();
            ds.label = fileName;
            CSVReader reader;
            if(fileName.endsWith(".csv")) {
                reader = new CSVReader(new FileReader(fileName));
            } else {
                reader = new CSVReader(new FileReader(fileName),'\t');
            }
            String line;
            int count = 0;
            // add the first line to heading  of dataset
            
            String [] words;
            while((words = reader.readNext()) != null && count < ds.MAX_SIZE) {
                ArrayList<String> row = new ArrayList<String>();
                for(int i =0;i<words.length;i++) {
                    if(words[i].length() > ds.MAX_LABEL_SIZE) {
                        row.add(words[i].substring(0,ds.MAX_LABEL_SIZE)+"...");
                    } else {
                        row.add(words[i]);
                    }
                }
                if(count==0) {
                    ds.setHeading(row);
                }else {
                    ds.rowList.add(row);
                }
                count++;
            }
            reader.close();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
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
    
    
    
}
