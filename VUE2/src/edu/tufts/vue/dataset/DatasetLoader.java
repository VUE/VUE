/*
 * DatasetLoader.java
 *
 * Created on July 23, 2008, 2:31 PM
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
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2008
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
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
                }  else {
                    ds  = new ListDataset();
                }
                loadDataset();
                dialog.setVisible(false);
            }
        });
        
        return createPane("Please select the dataset type",radioButtons, loadButton);
        
        
    }
    
    private void loadDataset() {
        try {
            
            ds.rowList = new ArrayList<ArrayList<String>>();
            ds.label = fileName;
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String line;
            int count = 0;
            while((line=reader.readLine()) != null && count < ds.MAX_SIZE) {
                ArrayList<String> row = new ArrayList<String>();
                String[] words = line.split(",");
                for(int i =0;i<words.length;i++) {
                    if(words[i].length() > ds.MAX_LABEL_SIZE) {
                        row.add(words[i].substring(0,ds.MAX_LABEL_SIZE)+"...");
                    } else {
                        row.add(words[i]);
                    }
                }
                ds.rowList.add(row);
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
