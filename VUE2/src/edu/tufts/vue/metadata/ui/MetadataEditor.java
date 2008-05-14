
/*
 * Copyright 2003-2007 Tufts University  Licensed under the
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

package edu.tufts.vue.metadata.ui;

import edu.tufts.vue.metadata.CategoryModel;
import edu.tufts.vue.metadata.MetadataList;
import edu.tufts.vue.metadata.VueMetadataElement;
import edu.tufts.vue.ontology.OntType;
import edu.tufts.vue.rdf.RDFIndex;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.event.FocusAdapter;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import javax.swing.table.*;

import tufts.vue.ActiveEvent;
import tufts.vue.ActiveListener;
import tufts.vue.LWComponent;
import tufts.vue.LWGroup;
import tufts.vue.LWSelection;
import tufts.vue.VUE;
import tufts.vue.gui.GUI;

/*
 * MetadataEditor.java
 *
 * The Metadata Editor is primarily a keyword(/category) editor
 * in VUE 2.0.
 *
 * It has the potential to display VueMetadataElements
 * of non category type and was developed in initial demos
 * to be more flexible in this regard. If neccesary, it shouldn't 
 * be too difficult to recover such flexibility.
 *
 * See OntologicalMembershipPane for an example of 
 * an editor for other types of VueMetadataElement data
 *
 * A slightly thornier issue has arisen in terms of applying
 * keywords to multiple selections as this component now
 * listens to both the current Active and current Selection so
 * that it can display/edit either the current Component's 
 * MetadataList or that of an LWGroup - additionally
 * the behavior that propagates the editing/merging of data into the sub
 * components of the group currently resides here - at some
 * point it might make sense to factor this behavior into
 * a subclass, particularly if needed elsewhere. 
 *
 *
 * Created on June 29, 2007, 2:37 PM
 *
 * @author dhelle01
 */
public class MetadataEditor extends JPanel implements ActiveListener,
                                                      MetadataList.MetadataListListener,
                                                      LWSelection.Listener {
                                                     
    
    private static final boolean DEBUG_LOCAL = false;
    
    public final static String CC_ADD_LOCATION_MESSAGE = "<html> Click [+] to add this <br> custom category to your own list. </html>";
    
    // for best results: modify next two in tandem (at exchange rate of one pirxl from ROW_GAP for 
    // each two in ROW_HEIGHT in order to maintain proper text box height
    // todo: define a text box height/ think about a layout that can combine control
    // over margins with great control/cross platform consistency for textbox height.
    public final static int ROW_HEIGHT = 31;
    public final static int ROW_GAP = 4;
    
    public final static int ROW_INSET = 5;
    
    public final static int BUTTON_COL_WIDTH = 35;
    
    public final static java.awt.Color SAVED_KEYWORD_BORDER_COLOR = new java.awt.Color(196,196,196);
    
    public final static boolean LIMITED_FOCUS = false;
    
    public final static int CC_ADD_LEFT = 0;
    public final static int CC_ADD_RIGHT = 1;
    
    //public final static String TAG_ONT = "http://vue.tufts.edu/vue.rdfs#Tag";
    // todo: use VueMetadataElement NONE_ONT exclusively (or perhaps put this in Ontology
    // class itself? Maybe also might fit in rdf package)
    public final static String NONE_ONT = "http://vue.tufts.edu/vue.rdfs#none";
    
    public final static Border insetBorder = BorderFactory.createEmptyBorder(ROW_GAP,ROW_INSET,ROW_GAP,ROW_INSET);
    public final static Border fullBox = BorderFactory.createMatteBorder(1,1,1,1,SAVED_KEYWORD_BORDER_COLOR);
    public final static Border verticalStartingBox = BorderFactory.createMatteBorder(0,1,1,1,SAVED_KEYWORD_BORDER_COLOR);
    public final static Border verticalFollowingBox = BorderFactory.createMatteBorder(1,1,0,1,SAVED_KEYWORD_BORDER_COLOR);
    public final static Border leftLeadingFullBox = BorderFactory.createMatteBorder(1,1,1,0,SAVED_KEYWORD_BORDER_COLOR);
    public final static Border leftLeadingVerticalFollowingBox = BorderFactory.createMatteBorder(1,1,0,0,SAVED_KEYWORD_BORDER_COLOR);
        
    private JTable metadataTable;
    private LWComponent current;
    private LWGroup currentMultiples;
    private LWComponent previousCurrent;
    private LWGroup previousMultiples;
    
    private int buttonColumn = 1;

    private boolean focusToggle = false;
   
    public MetadataEditor(tufts.vue.LWComponent current,boolean showOntologicalMembership,boolean followAllActive)
    {
   
        //VUE-846 -- special case related to VUE-845 (see comment on opening from keyword item in menu) 
        if(getSize().width < 100)
        {
   
           setSize(new java.awt.Dimension(300,200));
           
        }
        
        
        this.current = current;
        // clear gui below after create table.
        
        //if(DEBUG_LOCAL)
        //{
        //    System.out.println("MetadataEditor - just created new instance for (current,followActive) (" + current +"," + followAllActive + ")");
        //}
        
        // hopefully don't need to do this if successful load from inspector pane'
        /*if(VUE.getSelection().contents().size() > 1)
        {
            currentMultiples = LWGroup.createTemporary(VUE.getSelection());
            
            //!! also need to do rest of stuff in selection changed..
        }*/
        
        metadataTable = new JTable(new MetadataTableModel())
        {   
            public String getToolTipText(MouseEvent location)
            {
                if(getEventIsOverAddLocation(location))
                {
                    return CC_ADD_LOCATION_MESSAGE;
                }
                else
                {
                    return null;
                }
            }
        };
        
        ((MetadataTableModel)metadataTable.getModel()).clearGUIInfo();
        
        metadataTable.setShowGrid(false);
        metadataTable.setIntercellSpacing(new java.awt.Dimension(0,0));
        
        metadataTable.setBackground(getBackground());
        metadataTable.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter()
        {
            
                   public void mouseReleased(java.awt.event.MouseEvent evt)
                   {
                       java.awt.event.MouseEvent me = 
                       javax.swing.SwingUtilities.convertMouseEvent(metadataTable.getTableHeader(), 
                                                                    evt, headerAddButtonPanel);
                               
                               
                       //javax.swing.SwingUtilities.convertMouseEvent(metadataTable.getTableHeader(), 
                       //                                             evt, headerAddButton);
                       
                       me.translatePoint(evt.getX()-me.getX() - metadataTable.getWidth() + BUTTON_COL_WIDTH
                                        ,evt.getY()-me.getY());
                       headerAddButtonPanel.dispatchEvent(me);
                   }
            
                   public void mousePressed(java.awt.event.MouseEvent evt)
                   {  
                       //tufts.vue.gui.VueButton addButton = 
                       //        MetadataTableHeaderRenderer.getButton();
                       
                       
                       java.awt.event.MouseEvent me = 
                       javax.swing.SwingUtilities.convertMouseEvent(metadataTable.getTableHeader(), 
                                                                    evt, headerAddButtonPanel);
                               
                               
                       //javax.swing.SwingUtilities.convertMouseEvent(metadataTable.getTableHeader(), 
                       //                                             evt, headerAddButton);
                       
                       me.translatePoint(evt.getX()-me.getX() - metadataTable.getWidth() + BUTTON_COL_WIDTH
                                        ,evt.getY()-me.getY());
                       //headerAddButton.dispatchEvent(me);
                       
                       //if(DEBUG_LOCAL)
                       //{
                       //    System.out.println("MetadataEditor -- header -- unconverted mouse event: " + evt);
                       //    System.out.println("MetadataEditor: converted mouse event: " + me);
                       //}
                       
                       headerAddButtonPanel.dispatchEvent(me);
                       
                       /*if(evt.getX()>metadataTable.getWidth()-BUTTON_COL_WIDTH)
                       {                     
                         addNewRow();                         
                       }*/
                       // no need for this else, should make more sense to save whenever
                       // adding a new row...
                       //else
                       //{                    	
                    	   int row = metadataTable.rowAtPoint(evt.getPoint());
                           ((MetadataTableModel)metadataTable.getModel()).setSaved(row,true);
                           if(metadataTable.getCellEditor() !=null)
                           {    
                             metadataTable.getCellEditor().stopCellEditing();
                           }
                           metadataTable.repaint();
                       //}
                   }
       });
       
       addMouseListener(new java.awt.event.MouseAdapter()
       {
          public void mousePressed(java.awt.event.MouseEvent evt)
          {
              int row = metadataTable.rowAtPoint(evt.getPoint());
              int lsr = ((MetadataTableModel)metadataTable.getModel()).lastSavedRow;
              if(lsr != row)
              {    
                ((MetadataTableModel)metadataTable.getModel()).setSaved(lsr,true);  
                ((MetadataTableModel)metadataTable.getModel()).lastSavedRow = -1;
              }
              ((MetadataTableModel)metadataTable.getModel()).refresh();
              
              TableCellEditor editor = metadataTable.getCellEditor();
              if(editor != null)
                  editor.stopCellEditing();
              
              metadataTable.repaint();
          }
       });
       
       metadataTable.addMouseListener(new java.awt.event.MouseAdapter()
       {
          public void mousePressed(MouseEvent evt)
          {        	
              MetadataTableModel model = (MetadataTableModel)metadataTable.getModel();
              if(model.lastSavedRow != metadataTable.rowAtPoint(evt.getPoint()))
                model.setSaved(model.lastSavedRow,true);
              metadataTable.repaint();
          }  
           
           
          public void mouseReleased(java.awt.event.MouseEvent evt)
          {        	
             /*if(evt.getX()>metadataTable.getWidth()-BUTTON_COL_WIDTH)
             {
               java.util.List<VueMetadataElement> metadataList = MetadataEditor.this.current.getMetadataList().getMetadata();
               int selectedRow = metadataTable.getSelectedRow();
                         
               // VUE-912, stop short-circuiting on row 0, delete button has also been returned
               if( metadataTable.getSelectedColumn()==buttonColumn && metadataList.size() > selectedRow)
               {
                 metadataList.remove(selectedRow);
                 metadataTable.repaint();
                 requestFocusInWindow();
               }
             }*/
                               
                       if(getEventIsOverAddLocation(evt))
                       {
                           
                           
                           CategoryModel cats = tufts.vue.VUE.getCategoryModel();
                           
                           
                           int row = metadataTable.rowAtPoint(evt.getPoint());//evt.getY()/metadataTable.getRowHeight();
                           
                           String category = ((VueMetadataElement)(metadataTable.getModel().getValueAt(
                                                              row,
                                                              0))).getKey();
                           
                           int separatorLocation = category.indexOf(RDFIndex.ONT_SEPARATOR);
                           if(separatorLocation != -1)
                           {
                               category = category.substring(separatorLocation + 1);
                           }
                           
                           cats.addCustomCategory(category);
                           cats.saveCustomOntology();
                           //((MetadataTableModel)metadataTable.getModel()).refresh();
                           if(metadataTable.getCellEditor() != null)
                             metadataTable.getCellEditor().stopCellEditing();
                           metadataTable.repaint();
                       }
                   }
        }); 

        adjustColumnModel();
        
        metadataTable.setDefaultRenderer(Object.class,new MetadataTableRenderer());
        metadataTable.setDefaultEditor(Object.class, new MetadataTableEditor());
        ((DefaultCellEditor)metadataTable.getDefaultEditor(java.lang.Object.class)).setClickCountToStart(2);
        //metadataTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        metadataTable.setRowHeight(ROW_HEIGHT);
        metadataTable.getTableHeader().setReorderingAllowed(false);

        //setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setLayout(new BorderLayout());
        
        JPanel metaPanel = new JPanel(new BorderLayout());
        JPanel tablePanel = new JPanel(new BorderLayout());
        
        //re-enable for VUE-953 (revert back to categories/keywords) 
        tablePanel.add(metadataTable.getTableHeader(),BorderLayout.NORTH);
        
        tablePanel.add(metadataTable);
        metaPanel.add(tablePanel);
        
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        
        //back to "assign categories" as per VUE-953
        //final JLabel optionsLabel = new JLabel("Use full metadata schema");
        final JLabel optionsLabel = new JLabel("Assign Categories");
        optionsLabel.setFont(GUI.LabelFace);
        //final JButton advancedSearch = new JButton(new ImageIcon(VueResources.getURL("advancedSearchMore.raw")));//tufts.vue.gui.VueButton("advancedSearchMore");
        final JCheckBox advancedSearch = new JCheckBox();
        //advancedSearch.setBorder(BorderFactory.createEmptyBorder(0,0,15,0));
        advancedSearch.addActionListener(new java.awt.event.ActionListener(){
           public void actionPerformed(java.awt.event.ActionEvent e)
           {
        	   
               MetadataTableModel model = (MetadataTableModel)metadataTable.getModel();           
               if(model.getColumnCount() == 2)
               {
                 buttonColumn = 2;
                 model.setColumns(3);
                 //advancedSearch.setIcon(new ImageIcon(VueResources.getURL("advancedSearchLess.raw")));
                 advancedSearch.setSelected(true);
                 //optionsLabel.setText("less options");
               }
               else
               {
                 buttonColumn = 1;
                 model.setColumns(2);
                 //advancedSearch.setIcon(new ImageIcon(VueResources.getURL("advancedSearchMore.raw")));
                 advancedSearch.setSelected(false);
                 //optionsLabel.setText("more options");
               }
               
               adjustColumnModel();

               //System.out.println("table model set to 3 columns");
               revalidate();
           }
        });
        optionsPanel.add(advancedSearch);
        optionsPanel.add(optionsLabel);
        
        JPanel controlPanel = new JPanel(new BorderLayout());
        
        controlPanel.add(optionsPanel);

        metaPanel.add(controlPanel,BorderLayout.SOUTH);
        
        add(metaPanel,BorderLayout.NORTH);
        
        // followAllActive needed for MapInspector, it only follows the map, 
        // not any particular component
        if(followAllActive)
        {
          tufts.vue.VUE.addActiveListener(tufts.vue.LWComponent.class,this);
          VUE.getSelection().addListener(this);
        }
        else
        {
          tufts.vue.VUE.addActiveListener(tufts.vue.LWMap.class,this); 
        }
        
        MetadataList.addListener(this);
        
        headerAddButtonPanel.addMouseListener(new java.awt.event.MouseAdapter(){
              public void mousePressed(java.awt.event.MouseEvent e)
              {
                super.mousePressed(e);  
                //if(DEBUG_LOCAL)
                //{
                //  System.out.println("MetadataEditor -- table header mouse pressed " +
                //        e);
                //  System.out.println(" components count " + headerAddButtonPanel.getComponentCount());
                //  if(headerAddButtonPanel.getComponentCount() > 0)
                //  {    
                //    System.out.println(" component 0 " + headerAddButtonPanel.getComponents()[0].getClass());
                //    System.out.println(" component 0 - location: " + headerAddButtonPanel.getComponents()[0].getLocation());
                //    System.out.println(" component 0 - location: " + headerAddButtonPanel.getComponents()[0].getSize());
                //    System.out.println(" component at location " + headerAddButtonPanel.getComponentAt(e.getPoint()));
                //  }
                //}
                
                
                       tufts.vue.gui.VueButton button= (tufts.vue.gui.VueButton)headerAddButtonPanel.getComponents()[0]; 
                
                       java.awt.event.MouseEvent me = 
                       javax.swing.SwingUtilities.convertMouseEvent(headerAddButtonPanel, 
                                                                    e, button);
                               
                               
                       //javax.swing.SwingUtilities.convertMouseEvent(metadataTable.getTableHeader(), 
                       //                                             evt, headerAddButton);
                       
                       me.translatePoint(-me.getX()
                                        ,-me.getY());
                       
                       //if(DEBUG_LOCAL)
                       //{
                       //    System.out.println("MetadataEditor converted event from panel to button: " + me);
                       //    System.out.println("MetadataEditor relation of e to button location: " +
                       //            (e.getPoint().getX() > button.getLocation().getX() &&
                       //             e.getPoint().getX() < button.getLocation().getX() + button.getWidth() &&
                       //             e.getPoint().getY() > button.getLocation().getY() &&
                       //             e.getPoint().getY() < button.getLocation().getY() + button.getHeight()) );
                       //   
                       //}
                       
                       if(e.getPoint().getX() > button.getLocation().getX() &&
                          e.getPoint().getX() < button.getLocation().getX() + button.getWidth() &&
                          e.getPoint().getY() > button.getLocation().getY() &&
                          e.getPoint().getY() < button.getLocation().getY() + button.getHeight() )
                       {
                         button.dispatchEvent(me);
                         addNewRow();
                         //metadataTable.repaint();
                       }
                
                
                
              }
              
              public void mouseReleased(java.awt.event.MouseEvent e)
              {
                //if(DEBUG_LOCAL)
                //{
                //  System.out.println("MetadataEditor -- table header mouse released " +
                //        e);
                //}
                
                                       tufts.vue.gui.VueButton button= (tufts.vue.gui.VueButton)headerAddButtonPanel.getComponents()[0]; 
                
                       java.awt.event.MouseEvent me = 
                       javax.swing.SwingUtilities.convertMouseEvent(headerAddButtonPanel, 
                                                                    e, button);
                               
                               
                       //javax.swing.SwingUtilities.convertMouseEvent(metadataTable.getTableHeader(), 
                       //                                             evt, headerAddButton);
                       
                       me.translatePoint(-me.getX()
                                        ,-me.getY());
                       
                       //if(DEBUG_LOCAL)
                       //{
                       //    System.out.println("MetadataEditor converted event from panel to button: " + me);
                       //    System.out.println("MetadataEditor relation of e to button location: " +
                       //            (e.getPoint().getX() > button.getLocation().getX() &&
                       //             e.getPoint().getX() < button.getLocation().getX() + button.getWidth() &&
                       //             e.getPoint().getY() > button.getLocation().getY() &&
                       //             e.getPoint().getY() < button.getLocation().getY() + button.getHeight()) );
                       //   
                       //}
                       
                       if(e.getPoint().getX() > button.getLocation().getX() &&
                          e.getPoint().getX() < button.getLocation().getX() + button.getWidth() &&
                          e.getPoint().getY() > button.getLocation().getY() &&
                          e.getPoint().getY() < button.getLocation().getY() + button.getHeight() )
                       {
                         button.dispatchEvent(me);
                         //metadataTable.repaint();
                       }
              }
            });
            
            /*headerAddButton.addActionListener(new java.awt.event.ActionListener(){
              public void actionPerformed(java.awt.event.ActionEvent e)
              {
                if(DEBUG_LOCAL)
                {
                  System.out.println("MetadataEditor -- table header add button pressed " +
                        e);
                }
              }
            });*/
            
            headerAddButton.addMouseListener(new java.awt.event.MouseAdapter(){
              public void mousePressed(java.awt.event.MouseEvent e)
              {
                super.mousePressed(e);
                //if(DEBUG_LOCAL)
                //{
                //  System.out.println("MetadataEditor -- table header mouse pressed on button " +
                //        e);
                //}
                headerAddButtonPanel.repaint();
                headerAddButton.repaint();
              }
            });   
        
        setBorder(BorderFactory.createEmptyBorder(10,8,15,6));
        
        validate();
    }
    public Dimension getMinimumSize()
    { 	   
 	   int height = 120;
 	   //int lines = 1;
 	  int rowCount = metadataTable.getRowCount(); 	   	  
 	  int rowHeight = metadataTable.getRowHeight();
 //	  System.out.println("rowHeight :"  + rowHeight);
 	  
 		   return new Dimension(300,(height+((rowCount-1) * rowHeight)));
 	      	
    }
    
    public Dimension getPreferredSize()
    { 	   
 	   return getMinimumSize();
    }
    public void refresh()
    {
    	validate();
        repaint();
    	((MetadataTableModel)metadataTable.getModel()).refresh();    
    }
    
    public JTable getMetadataTable()
    {
        return metadataTable;
    }
    
    public LWComponent getCurrent()
    {
        return current;
    }
    
    public LWGroup getCurrentMultiples()
    {
        return currentMultiples;
    }
    
    public void addNewRow()
    {
        VueMetadataElement vme = new VueMetadataElement();
        String[] emptyEntry = {NONE_ONT,""};
        vme.setObject(emptyEntry);
        vme.setType(VueMetadataElement.CATEGORY);
        //metadataTable.getModel().setValueAt(vme,metadataTable.getRowCount()+1,0);
        // wrong order?? not getting intial field in multiples and current could be
        // non empty when currentMultiples is also..
        /*if(current !=null)
        {    
          MetadataEditor.this.current.getMetadataList().getMetadata().add(vme);
        }
        else if(currentMultiples != null)
        {
                      
          currentMultiples.getMetadataList().getMetadata().add(vme);   
            
          if(DEBUG_LOCAL)
          {
              System.out.println("ME: current multiples is not null -- addNewRow " +
                      currentMultiples.getMetadataList().getCategoryListSize() );
          }
 
        }*/
        
        if(currentMultiples != null)
        {
                      
          currentMultiples.getMetadataList().getMetadata().add(vme);   
            
          if(DEBUG_LOCAL)
          {
              System.out.println("ME: current multiples is not null -- addNewRow " +
                      currentMultiples.getMetadataList().getCategoryListSize() );
          }
 
        }
        else
        if(current !=null)
        {    
          MetadataEditor.this.current.getMetadataList().getMetadata().add(vme);
        }
            
        ((MetadataTableModel)metadataTable.getModel()).refresh();
    }
    
    public void listChanged()
    {   
        //if(DEBUG_LOCAL)
        //{
        //    System.out.println("MetadataEditor: list changed ");
        //}
        
        ((MetadataTableModel)metadataTable.getModel()).refresh();
        validate();
    }
    
    public boolean getEventIsOverAddLocation(MouseEvent evt)
    {
       boolean locationIsOver =  evt.getX() > getCustomCategoryAddLocation(CC_ADD_LEFT) && 
                          evt.getX() < getCustomCategoryAddLocation(CC_ADD_RIGHT);
       
       int row = metadataTable.rowAtPoint(evt.getPoint());//evt.getY()/metadataTable.getRowHeight();
                           
       boolean categoryIsNotInList = !((MetadataTableModel)metadataTable.getModel()).getCategoryFound(row);
                               
       return locationIsOver && categoryIsNotInList;
       
    }
    
    /**
     *
     *  returns -1 if add widget is not available (i.e. if in basic
     *  mode and/or not in "assign categories" mode)
     *
     **/
    public int getCustomCategoryAddLocation(int ccLocationConstant)
    {
        if(metadataTable.getModel().getColumnCount() == 2)
        {
            return -1;
        }
        
        if(ccLocationConstant == CC_ADD_LEFT)
        {
            return metadataTable.getColumnModel().getColumn(0).getWidth() - 20;
        }
        else if(ccLocationConstant == CC_ADD_RIGHT)
        {
            return metadataTable.getColumnModel().getColumn(0).getWidth();
        }
        else
        {
            return -1;
        }
    }
    
    public void adjustColumnModel()
    {
        int editorWidth = MetadataEditor.this.getWidth();
        //if(MetadataEditor.this.getTopLevelAncestor() != null)
        //  editorWidth = MetadataEditor.this.getTopLevelAncestor().getWidth();
        
        if(metadataTable == null)
            return;
        
        if(metadataTable.getModel().getColumnCount() == 2)
        {
          metadataTable.getColumnModel().getColumn(0).setHeaderRenderer(new MetadataTableHeaderRenderer());
          metadataTable.getColumnModel().getColumn(1).setHeaderRenderer(new MetadataTableHeaderRenderer());
          metadataTable.getColumnModel().getColumn(0).setWidth(editorWidth-BUTTON_COL_WIDTH);
          metadataTable.getColumnModel().getColumn(1).setMaxWidth(BUTTON_COL_WIDTH);   
        }
        else
        {
          metadataTable.getColumnModel().getColumn(0).setHeaderRenderer(new MetadataTableHeaderRenderer());
          metadataTable.getColumnModel().getColumn(1).setHeaderRenderer(new MetadataTableHeaderRenderer());
          metadataTable.getColumnModel().getColumn(2).setHeaderRenderer(new MetadataTableHeaderRenderer());
          metadataTable.getColumnModel().getColumn(0).setWidth(editorWidth/2-BUTTON_COL_WIDTH/2);
          metadataTable.getColumnModel().getColumn(1).setWidth(editorWidth/2-BUTTON_COL_WIDTH/2);
          metadataTable.getColumnModel().getColumn(2).setMaxWidth(BUTTON_COL_WIDTH); 
        }
        
        //place holder for boolean to only create header renderers once
        //renderersCreated = true;
    }
    
    public void repaint()
    {
        super.repaint();
      //  adjustColumnModel();
    }
    
    public void selectionChanged(LWSelection selection)
    {
        if(DEBUG_LOCAL)
        {
            System.out.println("MDE: selection changed" + selection);
        }
        
        if(!selection.isEmpty())
        {
            if(selection.contents().size() > 1)
            {
                
         //------------------
                       focusToggle = false;  
           
         //if(DEBUG_LOCAL)
         //{
         //    System.out.println("MetadataEditor: active changed - " + e + "," + this);
         //}
           
         //LWSelection currentSelection = selection;
         
         metadataTable.removeEditor();
         
         //((MetadataTableModel)metadataTable.getModel()).clearGUIInfo();
         
         previousMultiples = currentMultiples;
         
         //--------
                
              currentMultiples = LWGroup.createTemporary(selection);
              
              
              java.util.List<VueMetadataElement> shared = new java.util.ArrayList<VueMetadataElement>();
              
              // the following intersection of lists might take a while? 
              //perhaps want to put this in another thread..
              // but probably would need some additional gui work to make clear what is 
              // happening
              // also: could get these components from the selection to speed things up
              java.util.Iterator<LWComponent> children = 
                      currentMultiples.getAllDescendents().iterator();
              
              LWComponent firstComp = children.next();
              shared.addAll(firstComp.getMetadataList().getCategoryList().getList());
              
              if(DEBUG_LOCAL)
              {
                  System.out.println("MDE: shared initialized -- " + firstComp.getMetadataList().
                          getMetadataAsHTML(VueMetadataElement.CATEGORY));
              }
              
              while(children.hasNext())
              {
                  LWComponent comp = children.next();
                  
                  // actually could now skip this step to add some efficiency just use the loop
                  // below, don't add if frequency zero in any spot
                  shared.retainAll(comp.getMetadataList().getCategoryList().getList());
                  
                  //next part is inefficient, but need to do something quickly for 2.1 release
                  //and need to handle duplicates consistently:
                  
                  // create set from shared
                  //  use set iterator -- use element,count(frequency) in shared,count in comp
                  // then remove lastIndexOf from shared as manhy times
                  // as needed for each element
                  java.util.Set uniqueShares = new java.util.HashSet(shared);
                  java.util.Iterator<VueMetadataElement> us = uniqueShares.iterator();
                  while(us.hasNext())
                  {
                      VueMetadataElement next = us.next();
                      int sharedCount = java.util.Collections.frequency(shared,next);
                      int compCount = java.util.Collections.frequency(comp.getMetadataList().getCategoryList().getList(),next);
                      int numberToRemove = Math.abs(sharedCount - compCount);
                      for(int i=0;i<numberToRemove;i++)
                      {
                          if(shared.indexOf(next) != -1)
                            shared.remove(shared.indexOf(next));
                      }
                  }
                  
              }
              
              java.util.List<VueMetadataElement> cmList = 
                       currentMultiples.getMetadataList().getMetadata();
              
              if(DEBUG_LOCAL)
              {
                  System.out.println("MDE: after shared is constructed: " + currentMultiples.getMetadataList().
                                             getMetadataAsHTML(VueMetadataElement.CATEGORY));
              }
              for(int i=0;i<shared.size();i++)
              {
                  cmList.add(shared.get(i));
              }
              
              //((MetadataTableModel)metadataTable.getModel()).refresh();
              MetadataTableModel model = (MetadataTableModel)metadataTable.getModel();
              
              
              if(DEBUG_LOCAL)
              {
                  System.out.println("MDE shared calculated -- what is its size? : " + shared.size());
              }
              
              //maybe add an empty item to the group instead.
              // it would never have to get added unless changed
              // actually only do this if the intersection of lists calculated above
              // is empty..
              if(shared.size() == 0) // tried cmList...
                addNewRow();
              
              //why is this needed? --**--
              //for(int i=0;i<model.getRowCount();i++)
              //{
              //   model.setSaved(i,true);    
              //}    

              
              
              
              //this should do the same thing.. (its what activeChanged does)
              ((MetadataTableModel)metadataTable.getModel()).clearGUIInfo();
         
              ((MetadataTableModel)metadataTable.getModel()).refresh();
              

              
              // maybe also need to null out current in active changed (depends on order)
              // better to allow current to be non null..
              //current = null;
              
              

            }
            else
            {
              currentMultiples = null;
            }
        }
        else
        {
            currentMultiples = null;
        }
        

    }
    
    public void activeChanged(ActiveEvent e)
    {
        
       if(e!=null)
       {
         focusToggle = false;  
           
         //if(DEBUG_LOCAL)
         //{
         //    System.out.println("MetadataEditor: active changed - " + e + "," + this);
         //}
           
         LWComponent active = (LWComponent)e.active;
         
         if(active instanceof tufts.vue.LWSlide)
         {
             active = null;
         }
         
         metadataTable.removeEditor();
         
         previousCurrent = current;
         
         current = active;
         
         
         if(current!=null && MetadataEditor.this.current.getMetadataList().getCategoryListSize() == 0)
         {
           //VueMetadataElement vme = new VueMetadataElement();
           //String[] emptyEntry = {NONE_ONT,""};
           //vme.setObject(emptyEntry);
           //vme.setType(VueMetadataElement.CATEGORY);

           MetadataEditor.this.current.getMetadataList().getMetadata().add(VueMetadataElement.getNewCategoryElement());
         }
        

         
         // clear focus and saved information
         ((MetadataTableModel)metadataTable.getModel()).clearGUIInfo();
         
         ((MetadataTableModel)metadataTable.getModel()).refresh();
         //((OntologyTypeListModel)ontologyTypeList.getModel()).refresh();
        
         
         //adjustColumnModel();
       }
    }
    
    tufts.vue.gui.VueButton headerAddButton = new tufts.vue.gui.VueButton("keywords.button.add"); 

    JPanel headerAddButtonPanel = new JPanel();
    
    class MetadataTableHeaderRenderer extends DefaultTableCellRenderer
    {  
        
       // see below - getter could be supplied in stand alone class
       //static tufts.vue.gui.VueButton button = new tufts.vue.gui.VueButton("keywords.button.add"); 
        
       public MetadataTableHeaderRenderer()
       {
        headerAddButton.setBorderPainted(false);
        headerAddButton.setContentAreaFilled(false);
        headerAddButton.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        headerAddButton.setSize(new java.awt.Dimension(5,5));
        
            /*headerAddButton.addActionListener(new java.awt.event.ActionListener(){
              public void actionPerformed(java.awt.event.ActionEvent e)
              {
                if(DEBUG_LOCAL)
                {
                  System.out.println("MetadataEditor -- table header add button pressed " +
                        e);
                }
              }
            });*/
            
            /*headerAddButton.addMouseListener(new java.awt.event.MouseAdapter(){
              public void mousePressed(java.awt.event.MouseEvent e)
              {
                if(DEBUG_LOCAL)
                {
                  System.out.println("MetadataEditor -- table header mouse pressed " +
                        e);
                }
              }
            });*/
       }
       
       // can't do this statically in inner class but could be done
       // from wholly separate class - for now move the button out
       // into the metadata editor
       /*static tufts.vue.gui.VueButton getButton()
       {
           return button;
       }*/
       
       public java.awt.Component getTableCellRendererComponent(JTable table, Object value,boolean isSelected,boolean hasFocus,int row,int col)
       {
           JComponent comp = new JPanel();
           if(col == 0)
           {
               
               if(((MetadataTableModel)table.getModel()).getColumnCount() == 2)
               {    
                 // back to "Keywords:" -- VUE-953  
                 comp =  new JLabel("Keywords:");                 
               }
               else
               {
                 // back to "Categories" -- VUE-953
                 //comp = new JLabel("Fields:");
                 comp = new JLabel("Categories:");
               }
               comp.setFont(GUI.LabelFace);
           }
           else if(col == 1 && col != buttonColumn)
           {
               if(((MetadataTableModel)table.getModel()).getColumnCount() == 3)
               {    
                 comp =  new JLabel("Keywords:");
               }
               comp.setFont(GUI.LabelFace);
           }
           else if(col == buttonColumn)
           {
               //comp = new JLabel();    
               //((JLabel)comp).setIcon(tufts.vue.VueResources.getImageIcon("metadata.editor.add.up"));
               //comp = new JPanel();
               comp = headerAddButtonPanel;
               comp.add(headerAddButton);
           }
           else
           {
               comp = new JLabel("");            
           }

           comp.setOpaque(true);
           comp.setBackground(MetadataEditor.this.getBackground());
           comp.setBorder(BorderFactory.createEmptyBorder(ROW_GAP,ROW_INSET,ROW_GAP,ROW_INSET));
           return comp;
       }
    }
    
    public boolean findCategory(Object currValue,int row,int col,int n,JComboBox categories)
    {
    
               boolean found = false;
        
               //if(DEBUG_LOCAL)
               //{
               //    System.out.println("MetadataEditor findCategory - " + currValue);
               //}
        
               //Object currValue = table.getModel().getValueAt(row,col); //.toString();
               //System.out.println("Editor -- currValue: " + currValue);
               for(int i=0;i<n;i++)
               {
                   
                   Object item = categories.getModel().getElementAt(i);
                   String currLabel = "";
                   if(currValue instanceof OntType)
                       currLabel = ((OntType)currValue).getLabel();
                   else
                       currLabel = currValue.toString();
                   
                   if(item instanceof OntType &&
                           (((OntType)item).getBase()+"#"+((OntType)item).getLabel()).equals(currLabel))
                   {
                       categories.setSelectedIndex(i);
                       found = true;
                   }
                   
               }
               
               return found;
               
    }
    
    class MetadataTableRenderer extends DefaultTableCellRenderer
    {   
        
       private JComboBox categories = new JComboBox();
       private edu.tufts.vue.metadata.gui.MetaButton deleteButton = new 
               edu.tufts.vue.metadata.gui.MetaButton(MetadataEditor.this,"delete");
       private JPanel holder = new JPanel();
        
       public java.awt.Component getTableCellRendererComponent(JTable table, Object value,boolean isSelected,boolean hasFocus,int row,int col)
       {
           JPanel comp = new JPanel();
           //JComboBox categories = new JComboBox();
           categories.setFont(GUI.LabelFace);
           categories.setModel(new CategoryComboBoxModel());
           categories.setRenderer(new CategoryComboBoxRenderer());
           
           comp.setLayout(new BorderLayout());
           if(col == buttonColumn-2)
           {
               int n = categories.getModel().getSize();
               
               Object currObject = null;
               if(current != null)
                 currObject = current.getMetadataList().getCategoryList().get(row).getObject();
               else if(currentMultiples!=null)
               {    
                 VueMetadataElement ele = currentMultiples.getMetadataList().getCategoryList().get(row);
                 
                 if(DEBUG_LOCAL)
                 {
                     System.out.println("ME - getTableCellRendererComponent - ele: " + ele);
                 }
                 
                 currObject = ele.getObject();
                 
                 /*if(currObject !=null)
                   currObject = ele.getObject();
                 else
                 {
                   VueMetadataElement vme = VueMetadataElement.getNewCategoryElement();
                   currentMultiples.getMetadataList().getCategoryList().set(row,vme);
                     
                   currObject = vme.getObject();
                 }*/
               }
               
               Object currValue = (((String[])currObject)[0]);
               boolean found = findCategory(currValue,row,col,n,categories); 
              
               MetadataTableModel model = (MetadataTableModel)table.getModel();
               
               if((found) || !model.getIsSaved(row))
               {    
                 model.setCategoryFound(row,true);  
                   
                 if(!model.getIsSaved(row) )
                  comp.add(categories);
                 else
                 {
                   String customName = currValue.toString();
                   int ontSeparatorLocation = customName.indexOf(edu.tufts.vue.rdf.RDFIndex.ONT_SEPARATOR);
                   if( ontSeparatorLocation != -1 && ontSeparatorLocation != customName.length() - 1)
                   {
                       customName = customName.substring(ontSeparatorLocation + 1,customName.length());
                   }
                   JLabel custom = new JLabel(customName);
                   custom.setFont(GUI.LabelFace);
                   comp.add(custom);
                 }
               }
               else
               {
                 model.setCategoryFound(row,false);  
                 String customName = currValue.toString();
                 int ontSeparatorLocation = customName.indexOf(edu.tufts.vue.rdf.RDFIndex.ONT_SEPARATOR);
                 if( ontSeparatorLocation != -1 && ontSeparatorLocation != customName.length() - 1)
                 {
                     customName = customName.substring(ontSeparatorLocation + 1,customName.length());
                 }
                 JLabel custom = new JLabel(customName+"*");
                 custom.setFont(GUI.LabelFace);
                 comp.add(custom);
                 JLabel addLabel = new JLabel("[+]");
                 comp.add(addLabel,BorderLayout.EAST);
               }
               
           }
           else if(col == buttonColumn-1)
           {
               
               boolean saved = ((MetadataTableModel)metadataTable.getModel()).getIsSaved(row);
               
               if(value instanceof VueMetadataElement)
               {
                   VueMetadataElement vme = (VueMetadataElement)value;
                   if(saved == true)
                   {
                     JLabel custom = new JLabel(vme.getValue());
                     custom.setFont(GUI.LabelFace);
                     comp.add(custom);
                   }
                   else 
                   if(saved == false)
                   {
                     JTextField field = new JTextField(vme.getValue());
                     field.setFont(GUI.LabelFace);
                     comp.add(field);
                   }
               } 
               else
               {
                   if(DEBUG_LOCAL && value != null)
                   {
                       System.out.println("MetadataEditor -- renderer for field not vme: " + value.getClass());
                   }
               }
                 
           }
           else if(col == buttonColumn)               
           {
               //JLabel buttonLabel = new JLabel();
               
               // VUE-912, put delete button back -- also add back mouselistener
               //if(row!=0)
               //{    
                 //buttonLabel.setIcon(tufts.vue.VueResources.getImageIcon("metadata.editor.delete.up"));
               //}
               //comp.add(buttonLabel);
               
               //$
                 //comp.setOpaque(true);
                 //comp.setBackground(java.awt.Color.RED);
               //$
               
               //JPanel holder = new JPanel();
               holder.add(deleteButton);
               comp.add(holder);
           }
           
           comp.setOpaque(false);
           
           comp.setBorder(getMetadataCellBorder(row,col));
           
           return comp;
       } 
           
    }
    
    class MetadataTableEditor extends DefaultCellEditor
    {   
        
       // main entry point for editor interaction 
       public boolean isCellEditable(java.util.EventObject object)
       {
           
           int col = -1;
           
           int row = -1;
           
           if(object instanceof MouseEvent)
           {    
             java.awt.Point point = ((MouseEvent)object).getPoint();
             row = metadataTable.rowAtPoint(point);  
             col = metadataTable.columnAtPoint(point);
             if(col == buttonColumn)
             {
                 if(metadataTable.getCellEditor()!= null)
                   metadataTable.getCellEditor().stopCellEditing();
                 return true;
             }
             MouseEvent event = (MouseEvent)object;
             if(event.getClickCount() == 2 ) // || col == buttonColumn ) 
             {    
               ((MetadataTableModel)metadataTable.getModel()).setSaved(row,false);
               int lsr = ((MetadataTableModel)metadataTable.getModel()).lastSavedRow;
               if(lsr != row)
               {    
                 ((MetadataTableModel)metadataTable.getModel()).setSaved(lsr,true);
               }
               
               ((MetadataTableModel)metadataTable.getModel()).refresh();
               metadataTable.repaint();
               
               return true;
             }
             else if( row != -1 &&
                      !(((MetadataTableModel)metadataTable.getModel()).getIsSaved(row)) )
             {
                 ((MetadataTableModel)metadataTable.getModel()).setSaved(row,false);
                 return true;
             }
             else
             {
               return false;
             }
           }
           else
             return false;
       }
        
        
       private JTextField field; 
       //private JComboBox categories = new JComboBox();
       //private JLabel categoryLabel = new JLabel();
       //private JLabel notEditable = new JLabel();
       //private JPanel comp = new JPanel();
       
       private int currentRow;
       private edu.tufts.vue.metadata.gui.MetaButton deleteButton;
       private edu.tufts.vue.metadata.gui.MetaButtonPanel metaButtonPanel;
      //private JPanel tempPanel;
        
       public MetadataTableEditor()
       {
           super(new JTextField());
           field = (JTextField)getComponent();
           
           //categories.setFont(GUI.LabelFace);
           //categories.setModel(new CategoryComboBoxModel());
           //categories.setRenderer(new CategoryComboBoxRenderer());
           
           
           //tempPanel = new JPanel();//new ActionPanel(MetadataEditor.this,row);
           //tempPanel.setOpaque(true);
           //tempPanel.setBackground(java.awt.Color.BLUE);
           deleteButton = new edu.tufts.vue.metadata.gui.MetaButton(MetadataEditor.this,"delete");
           //tempPanel.add(deleteButton);
           // ***needs mouseListener that saves data unless point is over Button..
           //.addActionListener(deleteButton)
           //
           
           metaButtonPanel = new 
                   edu.tufts.vue.metadata.gui.MetaButtonPanel(MetadataEditor.this,"delete");
           
       }
       
       public int getRow()
       {
           return currentRow;
       }
       
       public void focusField()
       {           
           field.requestFocus();
       }
       
       public boolean stopCellEditing()
       {
           //if(DEBUG_LOCAL)
           //    Thread.dumpStack();
           
           previousCurrent = current;
           previousMultiples = currentMultiples;
           return super.stopCellEditing();
       } 
        
       public java.awt.Component getTableCellEditorComponent(final JTable table,final Object value,boolean isSelected,final int row,final int col)
       {
           final JTextField field = new JTextField();
           final JComboBox categories = new JComboBox();
           categories.setFont(GUI.LabelFace);
           categories.setModel(new CategoryComboBoxModel());
           categories.setRenderer(new CategoryComboBoxRenderer());
           final JLabel categoryLabel = new JLabel();
           final JLabel notEditable = new JLabel();
           final JPanel comp = new JPanel();
           
           //edu.tufts.vue.metadata.gui.MetaButton deleteButton = new 
           //                       edu.tufts.vue.metadata.gui.MetaButton(MetadataEditor.this,"delete");
           
           comp.addMouseListener(new MouseAdapter(){
              public void mousePressed(MouseEvent e)
              {
                  ((MetadataTableModel)metadataTable.getModel()).setSaved(row,true);
                  stopCellEditing();
              }
           });
           
           currentRow = row;
           
           notEditable.setFont(GUI.LabelFace);
           field.setFont(GUI.LabelFace);
           
           categories.addItemListener(new ItemListener() 
           {
               public void itemStateChanged(ItemEvent e) 
               {
                   if(DEBUG_LOCAL)
                   {    
                      System.out.println("MetdataEditor categories item listener itemStateChanged: " + e);
                   }
                   
                   
                   if(e.getStateChange() == ItemEvent.SELECTED)
                   {   
                       if(e.getItem() instanceof edu.tufts.vue.metadata.gui.EditCategoryItem)
                       {                           
                           JDialog ecd = new JDialog(VUE.getApplicationFrame(),"Edit Categories");
                           ecd.setModal(true);
                           ecd.add(new CategoryEditor(ecd,categories,MetadataEditor.this,current,row,col));
                           ecd.setBounds(475,300,300,250);
                           ecd.setVisible(true);
                           
                           metadataTable.getCellEditor().stopCellEditing();
                           ((MetadataTableModel)metadataTable.getModel()).refresh();
                       }
                   }
               }
           });
           
           categories.addFocusListener(new FocusAdapter(){
               public void focusLost(java.awt.event.FocusEvent fe)
               {      
                   MetadataTableModel model = (MetadataTableModel)metadataTable.getModel();
                   if(model.lastSavedRow != row)
                   {    
                     model.setSaved(row,true);
                   }
               }
           });
           
           categories.addItemListener(new java.awt.event.ItemListener(){
              public void itemStateChanged(java.awt.event.ItemEvent ie)
              {
                  
                  if(ie.getStateChange()==java.awt.event.ItemEvent.SELECTED)
                  {
                      
                    if(categories.getSelectedItem() instanceof edu.tufts.vue.metadata.gui.EditCategoryItem)
                    {
                        return;
                    }
                      
                    VueMetadataElement vme = new VueMetadataElement();
                    
                    String[] keyValuePair = {((OntType)categories.getSelectedItem()).getBase()+"#"+((OntType)categories.getSelectedItem()).getLabel(),
                                               ((VueMetadataElement)table.getModel().getValueAt(row,buttonColumn - 1)).getValue()};
                    
                    vme.setObject(keyValuePair);
                    vme.setType(VueMetadataElement.CATEGORY);
                    if(currentMultiples != null)
                    {
                      // also need to add/set for individuals in group.. todo: subclass LWGroup to do this?
                      // in the meantime just set these by hand
                      if(currentMultiples.getMetadataList().getCategoryListSize() > (row))
                      {
                        VueMetadataElement oldVME = currentMultiples.getMetadataList().getCategoryList().get(row);  
                          
                        currentMultiples.getMetadataList().getCategoryList().set(row,vme);
                        
                        java.util.Iterator<LWComponent> multiples = currentMultiples.getAllDescendents().iterator();
                        while(multiples.hasNext())
                        {
                          LWComponent multiple = multiples.next();
                          MetadataList.SubsetList md = multiple.getMetadataList().getCategoryList();
                          if(md.contains(oldVME))
                          {
                              md.set(md.indexOf(oldVME),vme);
                          }
                          else
                          {
                              multiple.getMetadataList().getMetadata().add(vme);
                          }
                        }
                        
                      }
                      else
                      {
                        currentMultiples.getMetadataList().getMetadata().add(vme); 
                        
                        java.util.Iterator<LWComponent> multiples = currentMultiples.getAllDescendents().iterator();
                        while(multiples.hasNext())
                        {
                          LWComponent multiple = multiples.next();
                          MetadataList.SubsetList md = multiple.getMetadataList().getCategoryList();
                          if(md.contains(vme))
                          {
                              md.set(md.indexOf(vme),vme);
                          }
                          else
                          {
                              multiple.getMetadataList().getMetadata().add(vme);
                          }
                        }
                      }  
                    }
                    else if(current !=null)
                    {    
                      if(current.getMetadataList().getCategoryListSize() > (row))
                      {
                        current.getMetadataList().getCategoryList().set(row,vme);
                      }
                      else
                      {
                        current.getMetadataList().getMetadata().add(vme); 
                      }
                    }
                  }
              }
           });
           
           field.addKeyListener(new java.awt.event.KeyAdapter(){
              public void keyPressed(java.awt.event.KeyEvent e)
              {
                  if(e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER)
                  {
                      ((MetadataTableModel)metadataTable.getModel()).setSaved(row,true);
                  }
              }
           });
           
           field.addFocusListener(new FocusAdapter(){
              public void focusLost(java.awt.event.FocusEvent fe)
              {  
                  if( ( current == null && currentMultiples == null) )
                          //|| VUE.getActiveMap() == null || VUE.getActiveViewer() == null)
                  {
                      return;
                  }
                  
                    if(DEBUG_LOCAL)
                    {
                      Object editor = metadataTable.getCellEditor();
                      
                      MetadataTableEditor mte = null;
                      
                      System.out.println("MetadataEditor -- field focus lost -- CellEditor: " + 
                                           editor);
                      
                      if(editor != null)
                      {
                          mte = (MetadataTableEditor)editor;
                          System.out.println("MetadataEditor -- field focus lost -- CellEditor row: " + 
                                           mte.getRow());
                      }
                      
                      
                    }
                  
                  
                   MetadataTableModel model = (MetadataTableModel)metadataTable.getModel();
                   if(model.lastSavedRow != row)
                   {    
                     model.setSaved(row,true);
                   }
                  
                  VUE.getActiveMap().markAsModified();
                  
                  if(DEBUG_LOCAL && fe != null)
                  {    
                     System.out.println("MDE field focus lost -- opposite component: " + fe.getOppositeComponent());
                  }
                  
                  /*if(currentMultiples != null)
                  {
                      return;
                  }*/
                                    
                  /*if(fe!= null && fe.getOppositeComponent() == categories)
                  {
                      return;
                  }*/
                  
                  // note: fix for currentMultiples != null and focus lost off bottom of info window (not
                  // as likely in the multiple selection case?)
                  if(fe.getOppositeComponent() instanceof tufts.vue.gui.DockWindow && currentMultiples == null)
                  {
                      model.setSaved(row,true);
                      TableCellEditor tce = metadataTable.getCellEditor();
                      
                      if(tce != null)
                      {    
                        metadataTable.getCellEditor().stopCellEditing();
                      }
                  }
                  
                  //java.util.List<VueMetadataElement> metadata = null;
                  MetadataList.SubsetList metadata = null;
                  
                  if(previousCurrent == null && current == null &&
                       previousMultiples == null && currentMultiples ==  null)
                  {
                      return;
                  }
                  else
                  if(previousCurrent != null && !focusToggle)
                  {
                     metadata = previousCurrent.getMetadataList().getCategoryList();
                  } 
                  else if(previousCurrent != null && !focusToggle)
                  {
                     metadata = previousCurrent.getMetadataList().getCategoryList();
                  }   
                  else 
                  {
                     if( currentMultiples!=null )
                     {    
                       metadata = currentMultiples.getMetadataList().getCategoryList();
                     }
                     else if( current!=null )
                     {
                       //going in this order should mean current == null is not
                       // needed on selection of more than one component..
                         
                       metadata = current.getMetadataList().getCategoryList();   
                     }
                  }
                  
                  VueMetadataElement currentVME = null;
                  
                  if( (current != null && current.getMetadataList().getCategoryListSize() > 0 ) ||
                          
                      (currentMultiples != null && currentMultiples.getMetadataList().getCategoryListSize() > 0 )      
                    )
                  {
                   currentVME = metadata.get(row);
                  }
                  
                  VueMetadataElement vme = new VueMetadataElement();
                  
                  if(currentVME==null)
                  {
                     String[] emptyEntry = {NONE_ONT,field.getText()};
                     vme.setObject(emptyEntry);
                     vme.setType(VueMetadataElement.CATEGORY);  
                  }
                  else
                  {
                    String[] obj = (String[])currentVME.getObject();  
                    String[] pairedValue = {obj[0],field.getText()};
                    vme.setObject(pairedValue);
                  }
                  if( (current != null && current.getMetadataList().getCategoryListSize() > (row)) ||
                     (currentMultiples != null && currentMultiples.getMetadataList().getCategoryListSize() > row) )
                  {
                    if(DEBUG_LOCAL)
                    {
                        System.out.println("ME: setting value from field -- " + row + ":" + vme.getObject() +
                                            " " + metadata);
                    }
                    
                    VueMetadataElement old = metadata.get(row);
                    
                    metadata.set(row,vme);
                    
                    if(currentMultiples != null) // !! must make sure there is more than one in group -- other
                                                 // wise is back to base case of single selection.
                    {
                        java.util.Collection selection = currentMultiples.getAllDescendents();
                        java.util.Iterator children = selection.iterator();
                        while(children.hasNext())
                        {
                            LWComponent child = (LWComponent)children.next();
                            MetadataList.SubsetList cMeta = child.getMetadataList().getCategoryList();
                            int size = cMeta.size();
                            if(size > 0 && cMeta.get(size-1).getValue().equals(""))
                            {
                                cMeta.set(size-1,vme);
                            }
                            // also need to set in condition where already in all the sub components?
                            // somehow need to detect edit here.. but condition in line above this
                            // one is not neccesarily equivalent..
                            else
                              if(cMeta.contains(old))
                              {
                                // should it always be the first index?
                                cMeta.set(cMeta.indexOf(old),vme);
                              }
                              else
                                child.getMetadataList().getMetadata().add(vme);
                            child.layout();
                            // also might need VUE activelistener repaint
                        }
                    }
                    
                  }
                  else
                  {
                    metadata.add(vme); 
                  }
                  
                  // done earlier for multiples.  // may also  need active viewer repaint
                  if(current !=null)
                     current.layout();
                  
                  
                  VUE.getActiveViewer().repaint();
                  
                  metadataTable.repaint();
              }
              
              public void focusGained(java.awt.event.FocusEvent fe)
              {
                  focusToggle = true;
              }
           });
           comp.setLayout(new BorderLayout());
           if(col == buttonColumn - 2)
           {
               final int n = categories.getModel().getSize();
 
               Object currObject = null;
               
               if(currentMultiples != null)
               {
                 currObject = currentMultiples.getMetadataList().getCategoryListElement(row).getObject();  
               }
               else if(current != null)
               {    
                 currObject = current.getMetadataList().getCategoryListElement(row).getObject();
               }
               final Object currValue = (((String[])currObject)[0]);
               Object currFieldValue = (((String[])currObject)[1]);
               
               /*if(false) /// with new isCellEditable() logic on cell editor...
               {
                   String displayString = currValue.toString();
                   int nameIndex = displayString.indexOf("#");
                   if(nameIndex != -1 && ( (nameIndex + 1)< displayString.length()) ) 
                   {
                     displayString = displayString.substring(nameIndex + 1);
                   }
                   categoryLabel.setText(displayString);
                   
                   categoryLabel.setFont(GUI.LabelFace);
                   
                   boolean found = findCategory(currValue,row,col,n,categories);
                   
                   if(found)
                   {    
                     comp.add(categoryLabel);
                   }
                   else
                   {
                     categoryLabel.setText(categoryLabel.getText() + "*"); 
                     comp.add(categoryLabel);
                   }    
                   comp.setBorder(getMetadataCellBorder(row,col));
                   
                   ((MetadataTableModel)table.getModel()).refresh();
                   metadataTable.repaint();
                   
                   return comp;
               }*/
               findCategory(currValue,row,col,n,categories); 
               

               comp.add(categories);
               
               comp.setBorder(getMetadataCellBorder(row,col));
               ((MetadataTableModel)table.getModel()).refresh();
               metadataTable.repaint();
           }
           else
           if(col == (buttonColumn - 1))
           {
               if(value instanceof String)
                 field.setText(value.toString());
               if(value instanceof VueMetadataElement)
               {
                 VueMetadataElement vme = (VueMetadataElement)value;
                 field.setText(vme.getValue());
               }
               if(true) // new isCellEditable stuff
               {
                 comp.add(field);
               }
               else
               {
                 notEditable.setText(field.getText());
                 comp.add(notEditable);
               }
           }
           else if(col ==  buttonColumn)               
           {
               //JLabel buttonLabel = new JLabel();
               //buttonLabel.setIcon(tufts.vue.VueResources.getImageIcon("metadata.editor.delete.up"));
               //comp.add(buttonLabel);
                              
               metaButtonPanel.setRow(row);
               //deleteButton.setRow(row);
               //comp.add(deleteButton);
               //comp = metaButtonPanel;
               comp.add(metaButtonPanel);
           }
           
           comp.setOpaque(false);
           comp.setBorder(getMetadataCellBorder(row,col));
           
           if(LIMITED_FOCUS)
           {
             field.addMouseListener(new MouseAdapter() {
                 public void mouseExited(MouseEvent me)
                 {
                       stopCellEditing();
                 }
             });
           }
           
           comp.setBorder(getMetadataCellBorder(row,col));
           return comp;
       }
       
    }
    
    public Border getMetadataCellBorder(int row,int col)
    {
      if(col == buttonColumn)
          return insetBorder;
      
      MetadataTableModel model = (MetadataTableModel)metadataTable.getModel();
      
      boolean saved = model.getIsSaved(row);
      
      if(saved)
      {
          boolean aboveSaved = false;
          
          if(row > 0)
              aboveSaved = model.getIsSaved(row - 1);
          else if(row == -1)
              aboveSaved = false;
          
          boolean belowSaved = false;
          
          if(row < model.getRowCount())
            belowSaved = model.getIsSaved(row + 1);
          else if(row == model.getRowCount() - 1 && row > 0)
            belowSaved = false;
          
          if(col == buttonColumn -1 )
          {    
            if(!aboveSaved && !belowSaved)
            {    
              return BorderFactory.createCompoundBorder(fullBox,insetBorder);
            }
            else if(!aboveSaved && belowSaved)
            {
              return BorderFactory.createCompoundBorder(verticalFollowingBox,insetBorder);
            }
            else if(aboveSaved && !belowSaved)
            {
              return BorderFactory.createCompoundBorder(fullBox,insetBorder);
            }
            else if(aboveSaved && belowSaved)
            {
              return BorderFactory.createCompoundBorder(verticalFollowingBox,insetBorder);
            }
          }
          else
          {
            if(!aboveSaved && !belowSaved)
            {    
              return BorderFactory.createCompoundBorder(leftLeadingFullBox,insetBorder);
            }
            else if(!aboveSaved && belowSaved)
            {
              return BorderFactory.createCompoundBorder(leftLeadingVerticalFollowingBox,insetBorder);
            }
            else if(aboveSaved && !belowSaved)
            {
              return BorderFactory.createCompoundBorder(leftLeadingFullBox,insetBorder);
            }
            else if(aboveSaved && belowSaved)
            {
              return BorderFactory.createCompoundBorder(leftLeadingVerticalFollowingBox,insetBorder);
            }
          }    
      }
      
      return insetBorder;
      
     }
    
    // the following was for the MapInfoPanel usage of this
    // component -- currently using a more direct approach
    // with JTable filtering built into JDK 1.6 the following
    // approach might make more sense (and if additional
    // filtering is needed and/or additional flexibility/
    // modifiability of filters)
    /*public class CreatorFilterModel extends MetadataTableModel
    {
        private int firstCreatorRow = -1;
        
        public int findFirstCreatorRow()
        {
            if(current == null)
            {
                return -1;
            }
            
            // todo: put proper dublic core category here
            //return current.getMetadataList().findCategory("");
            
            return -1;
        }
        
        public int getRowCount()
        {
            return super.getRowCount() - 1;
        }
        
        public Object getValueAt(int row,int column)
        {
            findFirstCreatorRow();
            if(row< firstCreatorRow)
                return super.getValueAt(row,column);
            else
                return super.getValueAt(row-1,column); 
        }
    }*/
    
    /**
     *
     * watch out for current == null
     *
     **/
    public class MetadataTableModel extends AbstractTableModel
    {
         //default:
         private int cols = 2;
         
         private java.util.ArrayList<Boolean> categoryIncluded = new java.util.ArrayList<Boolean>();
         private java.util.ArrayList<Boolean> saved = new java.util.ArrayList<Boolean>();

         private int lastSavedRow = -1;
         
         public void clearGUIInfo()
         {
             saved = new java.util.ArrayList<Boolean>();
             
             for(int i=0;i<getRowCount();i++)
             {
                 saved.add(Boolean.TRUE);
             }
             
             lastSavedRow = -1;
         }
         
         public boolean getIsSaved(int row)
         {
            try
            {  
              return Boolean.parseBoolean(saved.get(row).toString()) && 
                       ((VueMetadataElement)getValueAt(row,buttonColumn -1)).getValue().length() != 0;
            }
            catch(Exception e)
            {
              return Boolean.FALSE;
            }
         }
         
         public void setSaved(int row,boolean isSaved)
         {
             
             //Thread.dumpStack();
             
             if(DEBUG_LOCAL)
             {
                 System.out.println("MDE - table - setSaved (row,lsr) (" + row + "," + lastSavedRow + ")");
             }
             
             if(row == -1)
                 return;
             
             if(saved.size() <= row)
             {

                 //saved.ensureCapacity(row + 1);
                 //for(int i = 0;i<row-saved.size() + 1;i++)
                 for(int i = 0;i<row + 1;i++)
                 {
                     saved.add(Boolean.TRUE);
                 }
             } 
            

            saved.set(row,Boolean.valueOf(isSaved));
            lastSavedRow = row;
         }
         
         public boolean getCategoryFound(int row)
         {
            try
            {  
              return Boolean.parseBoolean(categoryIncluded.get(row).toString());
            }
            catch(Exception e)
            {
              return Boolean.TRUE;
            }
         }
         
         public void setCategoryFound(int row,boolean found)
         {
             
             if(categoryIncluded.size() <= row)
             {
                 for(int i = categoryIncluded.size();i<row + 1;i++)
                 {
                     categoryIncluded.add(Boolean.TRUE);
                 }
             } 
             
            categoryIncluded.set(row,Boolean.valueOf(found));
         }
         
         public int getRowCount()
         {
             if(current !=null)
             {   
               //MetadataList.CategoryFirstList list = (MetadataList.CategoryFirstList)current.getMetadataList().getMetadata();
               int size = current.getMetadataList().getCategoryListSize();
               //int size = current.getMetadataList().getMetadata().size();
               if(size > 0)
                   return size;
               else
                   return 1;
             }
             else if(currentMultiples !=null)
             {
               //MetadataList.CategoryFirstList list = (MetadataList.CategoryFirstList)currentMultiples.getMetadataList().getMetadata();
               int size = currentMultiples.getMetadataList().getCategoryListSize();
               //int size = current.getMetadataList().getMetadata().size();
               if(size > 0)
                   return size;
               else
                   return 1;                 
             }
             else
             { 
                //return 1;
                return 0;
             }   

         }
         
         public int getColumnCount()
         {
             return cols;
         }
         
         public void setColumns(int cols)
         {

             this.cols = cols;
             fireTableStructureChanged();
         }
         
         public void fireTableStructureChanged()
         {

        	 super.fireTableStructureChanged();
         }
         
         /*
          * 
          * note: CellEditor also has isCellEditable (main entry
          * point for mouse actions on metadatatable)
          * actually the table has this method through its cell editor
          * 
          * */
         public boolean isCellEditable(int row,int column)
         {
             // old -- for combined view with ont types non editable -- now
             // in seperate panel
             //if(getValueAt(row,column) instanceof OntType)
             //    return false;
             
             // deletebutton is now editable...
             //if( (column == buttonColumn -1) || (column == buttonColumn - 2) )
             //    return true;
             //else
             //    return false;
             return true;
         }
         
         public Object getValueAt(int row, int column)
         {
             
             categoryIncluded.trimToSize();
             
             if(current == null && currentMultiples == null)
                 return "null";
             else if(currentMultiples != null)
             {
               /*java.util.List<VueMetadataElement> list = currentMultiples.getMetadataList().getMetadata();
             
               if(list.size() == 0)
                 addNewRow();
               else
               if(list.size() < row + 1)
                 return null;*/
                 
               if(currentMultiples.getMetadataList().getCategoryListSize() == 0)
                 addNewRow();
               else
                 if(currentMultiples.getMetadataList().getCategoryListSize() < row + 1)
                   return null;
                 
               //while(list.size() < row + 1)
               //  addNewRow();
             
               return currentMultiples.getMetadataList().getCategoryListElement(row);
               //return current.getMetadataList().getMetadata().get(row);                 
             }
             
             /*java.util.List<VueMetadataElement> list = current.getMetadataList().getMetadata();
             
             if(list.size() == 0)
                 addNewRow();
             else
             if(list.size() < row + 1)
                 return null; */
             
             if(current.getMetadataList().getCategoryListSize() == 0)
                 addNewRow();
             else
                 if(current.getMetadataList().getCategoryListSize() < row + 1)
                   return null;
             
             //while(list.size() < row + 1)
             //  addNewRow();
             
             return current.getMetadataList().getCategoryListElement(row);
             //return current.getMetadataList().getMetadata().get(row);
         }
         
         public void setValueAt(Object value,int row, int column)
         {
             //fireTableDataChanged(); 
         }
         
         public void refresh()
         {
             fireTableDataChanged();
         }
         
    }
    
}
