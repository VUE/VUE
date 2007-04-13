
/*
 *
 * * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 *
 */

package edu.tufts.vue.ontology.ui;

import edu.tufts.vue.style.*;
import edu.tufts.vue.ontology.*;

import java.awt.Component;
import java.awt.datatransfer.*;
import java.awt.geom.Rectangle2D;
import java.awt.dnd.*;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionEvent;

import tufts.vue.*;
import tufts.vue.gui.*;

/*
 * TypeList.java
 *
 * modelled on tufts.vue.ui.ResourceList, this List will show
 * mapping of styles to ontologies and allow easy transfer
 * to LWMap - editing of individual  styles should also be possible
 * through the VUE gui.
 *
 * Created on March 7, 2007, 1:05 PM
 *
 * @author dhelle01
 */
public class TypeList extends JList {
    
    public static int count = 0;
    
    private static Ontology fedoraOntology;
    
    private DefaultListModel mDataModel;
    
    private LWComponent comp;

    private LWSelection selection;
    
    public TypeList() {
        
        mDataModel = new DefaultListModel();
        setModel(mDataModel);
        setCellRenderer(new TypeCellRenderer());
        
        javax.swing.DefaultListSelectionModel selectionModel = new javax.swing.DefaultListSelectionModel();
        selectionModel.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        setSelectionModel(selectionModel);
        selectionModel.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent e)
            {
                comp = ((LWComponent)getSelectedValue()).duplicate();
                comp.setParentStyle(comp);
                VUE.getSelection().setTo(comp);
            }
        });
        
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {    
                public void mouseDragged(java.awt.event.MouseEvent me) {
                    System.out.println("TypeList: mouse dragged");
                    System.out.println("TypeList: selected label " + ((LWComponent)(getSelectedValue())).getLabel());
                    
                    if(comp instanceof LWNode)
                    {
                      VueToolbarController vtc = VueToolbarController.getController();
                      VueTool rTool = vtc.getTool("nodeTool");
                      //rTool.setSelectedSubTool(vtc.getTool("rect"));
                      VueToolbarController.getController().setSelectedTool(rTool);
                    }
;                    
                    GUI.startLWCDrag(TypeList.this,
                                     me,
                                     comp,
                                      VUE.getActiveViewer().getTransferableSelection());
                 
                }
         });
    
    }
    
    public void addType(LWComponent typeComponent)
    {
        mDataModel.addElement(typeComponent);
    }
    
    class TypeCellRenderer implements ListCellRenderer
    { 
        public java.awt.Component getListCellRendererComponent(JList jList, Object value, int i, boolean isSelected, boolean hasFocus) 
        {
            JPanel p = new JPanel();
            java.awt.GridLayout grid = new java.awt.GridLayout(1,2);
            p.setLayout(grid);
           // p.setLayout(new java.awt.BorderLayout());
            
            p.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(200,200,200)));
                    
            if(value instanceof LWComponent)
            {
                LWComponent comp = (LWComponent)value;
                LWComponent noLabelComp = (LWComponent)comp.duplicate();
                if(comp instanceof LWNode)
                  noLabelComp.setLabel("   ");
                else
                  noLabelComp.setLabel("");
                String truncatedLabel = comp.getLabel();
                if(truncatedLabel.length() > 10)
                {
                    truncatedLabel = truncatedLabel.substring(0,10) + "...";
                }
                JLabel label = new JLabel(truncatedLabel);
                //java.awt.Image im = comp.getAsImage();
                java.awt.Image im = noLabelComp.getAsImage();
                JLabel imageLabel = new JLabel(new javax.swing.ImageIcon(im));
                p.add(imageLabel);
                p.add(label);
            }
            
            if(value == getSelectedValue())
            {
                p.setBackground(new java.awt.Color(230,230,230));
            }
            else
                p.setBackground(new java.awt.Color(255,255,255));
            return p;
        }
    }
    
    public void loadOntology(URL ontologyURL,URL cssURL,int ontType,boolean fromFile)
    {
        Ontology ontology = OntManager.getOntManager().readOntologyWithStyle(ontologyURL,
                                                      cssURL,
                                                      ontType);
        
        //OntManager.getOntManager().getOntList().add(ontology);
        //System.out.println("TypeList: ontology list size: " + OntManager.getOntManager().getOntList().size());
        
        
        
        fillList(ontology);
        
    }
    
    
    public void loadOntology(String ontologyLocation,String cssLocation,int ontType,boolean fromFile)
    {
        Ontology ontology = OntManager.getOntManager().readOntologyWithStyle(VueResources.getURL(ontologyLocation),
                                                      VueResources.getURL(cssLocation),
                                                      ontType);
        
        //OntManager.getOntManager().getOntList().add(ontology);
        
        //System.out.println("TypeList: ontology list size: " + OntManager.getOntManager().getOntList().size());
        
        fillList(ontology);
        
    }
    
    /**
     *
     * May be handled by LWComponent in future.
     *
     * Right now OntManager appears to read only link.localname 
     * styles, so check for node.localname and let default
     * style get applied for now.
     *
     **/
    private static boolean isNode(OntType type)
    {
        //Style style = type.getStyle();
        //System.out.println("tl: isNode: style name: " + style.getName());
                
        //System.out.println("tl: isNode type id: " + type.getId());
        
        // id no longer carries link. and node. info
        //if(StyleMap.getStyle("node."+type.getId())!=null)
        //    return true;
        //else 
        //    return false;
        
        //return type.getType().equals(edu.tufts.vue.style.SelectorType.getNodeType());
        
        System.out.println("tl: type.getStyle.getClass() " + type.getStyle().getClass());
               
        return (type.getStyle() instanceof NodeStyle);
       
        
      
    }
    
    private void fillList(Ontology ontology)
    {
        List<OntType> types = ontology.getOntTypes();
        
        //System.out.println("TypeList: types size " + types.size());
        
        
        //$
          int count = 0;
        //$
        
        Iterator<OntType> iter = types.iterator();
        while(iter.hasNext() && (count++) < 1000)
        {
            OntType ot = iter.next();
            
            System.out.println("TypeList: " + ot.getComment());
            System.out.println("tl: isNode: " + ot.getId() + " isNode?:" + isNode(ot));
            
            Style style = ot.getStyle();
            if(isNode(ot))
            {
                addNode(ot,ontology,style);
                
                /*LWNode node = new LWNode(ot.getLabel());
                  
                //node.setLabel(ot.getLabel());

                node.setAutoSized(false);
                
                node.setAbsoluteSize(25,25);
                

                NodeTool.SubTool st = NodeTool.getActiveSubTool();
                node.setShape(st.getShape());
                node.applyCSS(style);
                addType(node);*/
            }
            else
            {   
              addLink(ot,ontology,style);  
                
              /*LWLink link = new LWLink();
              link.setLabel(ot.getLabel() + "-->" + count);
              link.setHeadPoint(10,25);
              link.setTailPoint(40,25);
              link.setAbsoluteSize(30,50);
              //link.setArrowState(LWLink.ARROW_HEAD);
              //link.setWeight(Integer.parseInt(style.getAttribute("weight")));
              link.applyCSS(style);
              addType(link);*/
            }
        }
        
        /*if(ontology == fedoraOntology)
        {
          OntType fedoraObject = new OntType();  
          Style style = new NodeStyle("fedora object");
          addNode(fedoraObject,ontology,style);
        }*/
        
    }
    
    private void addNode(OntType ontType,Ontology ontology,Style style)
    {
          /*if(ontology == fedoraOntology)
          {
            LWNode node = new LWNode("Fedora Object");
            node.setAbsoluteSize(25,50);
            NodeTool.SubTool st = NodeTool.getActiveSubTool();
            node.setShape(st.getShape());
            addType(node);
          }
          else
          {*/
            /*LWNode node = new LWNode(ontType.getLabel());
            //node.setLabel(ot.getLabel());
                
            node.setAbsoluteSize(25,50);
            NodeTool.SubTool st = NodeTool.getActiveSubTool();
            node.setShape(st.getShape());
            node.applyCSS(style);
            addType(node);*/
              
                LWNode node = new LWNode(ontType.getLabel());
                  
                //node.setLabel(ot.getLabel());

                node.setAutoSized(false);
                
                node.setAbsoluteSize(25,25);
                

                NodeTool.SubTool st = NodeTool.getActiveSubTool();
                node.setShape(st.getShape());
                node.applyCSS(style);
                addType(node);
   
          //}
          
    }
    
    private void applyDefaultNodeStyle(LWNode node)
    {
        
    }
    
    private void addLink(OntType ontType,Ontology ontology, Style style)
    {
        
              LWLink link = new LWLink();
              link.setLabel(ontType.getLabel() + "-->" + count);
              link.setHeadPoint(10,25);
              link.setTailPoint(40,25);
              link.setAbsoluteSize(30,50);
              
              //link.setHeadPoint(10,25);
              //link.setTailPoint(140,25);
              //link.setAbsoluteSize(150,50);
              
              //link.setArrowState(LWLink.ARROW_HEAD);
              //link.setWeight(Integer.parseInt(style.getAttribute("weight")));
              applyDefaultLinkStyle(link);
              //applyStyleSheetLinkStyle(link,style);
              //System.out.println("t1: addLink about to apply style for: " + ontType.getLabel());
              //System.out.println("tl: addLink applying style: " + ontType.getStyle());
              link.applyCSS(ontType.getStyle());
              addType(link);
        
    }
    
    private void applyStyleSheetLinkStyle(LWLink link,Style style)
    {
              //link.setArrowState(LWLink.ARROW_HEAD);
              if(style.getAttribute("stroke-width") != null)
                link.setStrokeWidth(Float.parseFloat(style.getAttribute("stroke-width")));
    }
    
    private void applyDefaultLinkStyle(LWLink link)
    {
              link.setArrowState(LWLink.ARROW_TAIL);
              link.setWeight(1);
    }
    
    
    /**
     *
     * just for testing/demo purposes
     *
     */
    public static void main(String[] args)
    {
        
        // add a mouselistener to bring up a color chooser (and possibly
        // the text formatter or a generic one?).
        // attempt to edit an LWComponent directly on the List
        // if this works, try it all in VUE (and add ability to drag 
        // types to LWMap to create instances)
        
        TypeList tlist = new TypeList();
        javax.swing.JPanel panel = createTestPanel(tlist);//new javax.swing.JFrame();
        javax.swing.JFrame f = new javax.swing.JFrame();
        f.getContentPane().add(panel);
        
        //show the frame
        f.setBounds(100,100,150,300);
        f.setVisible(true);
    }
    
    public static javax.swing.JPanel createTestPanel(TypeList tlist)
    {
        javax.swing.JPanel testPanel = new javax.swing.JPanel();
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(tlist);
        testPanel.add(scroll);
        
        //add some demo data to the List
        /*LWNode rectangle = new LWNode("Test Node");
        rectangle.setShape( new java.awt.Rectangle(5,5,50,20));
        LWLink link = new LWLink();
        link.setStartPoint(10,10);
        link.setEndPoint(90,10);
        link.setAbsoluteSize(100,15);
        link.setArrowState(LWLink.ARROW_BOTH);
        link.setWeight(2);
        link.setLabel("contains");
        LWNode oval = new LWNode("Test Node 2");
        oval.setShape(new java.awt.geom.Ellipse2D.Double(5,5,50,20));

        tlist.addType(rectangle);
        tlist.addType(link);
        tlist.addType(oval);*/
        
        //add Fedora types and an extra node
        

        
        //Ontology ontology = OntManager.getFedoraOntologyWithStyles();
        
        Ontology ontology = null;
        
        
        try
        {  
        
        System.out.println("fedora url: " + VueResources.getURL("fedora.ontology.url"));
        System.out.println("fedora url: " + VueResources.getString("fedora.ontology.url"));
        
         ontology = OntManager.getOntManager().readOntologyWithStyle(VueResources.getURL("fedora.ontology.rdf"),
                                                      //new java.net.URL("http://www.fedora.info/definitions/1/0/fedora-relsext-ontology.rdfs"),
                                                      VueResources.getURL("fedora.ontology.css"),
                                                      OntManager.RDFS);
          
         /*ontology = OntManager.getOntManager().readOntologyWithStyle(//VueResources.getURL("fedora.ontology.rdf"),
                                                      new java.net.URL("http://www.atl.lmco.com/projects/ontology/ontologies/animals/animalsA.owl"),
                                                      new java.net.URL("http://vue-dev.uit.tufts.edu/ontology/css/animalsAStyle.css"),
                                                      OntManager.OWL);*/
         
         
          /*ontology = OntManager.getOntManager().readOntologyWithStyle(//VueResources.getURL("fedora.ontology.rdf"),
                                                      new java.net.URL("http://vue-dev.uit.tufts.edu/ontology/season.rdfs.xml"),
                                                      new java.net.URL("http://vue-dev.uit.tufts.edu/ontology/css/season.css"),
                                                      OntManager.OWL);*/

          
        }
        //catch(java.net.MalformedURLException urlException)
        catch(Exception urlException)
        {
            System.out.println("Ontology Manager: Malformed URL:" + urlException);
            VueUtil.alert("Ontology Load Failed - improper URL","Ontology Load Failed - improper URL");
            //ontology = OntManager.getFedoraOntologyWithStyles();
        }
        
        List<OntType> types = ontology.getOntTypes();
        
        System.out.println("TypeList: types size " + types.size());
        
        Iterator<OntType> iter = types.iterator();
        while(iter.hasNext())
        {
            OntType ot = iter.next();
            Style style = ot.getStyle();
            
            if(isNode(ot))
            {
                LWNode node = new LWNode();
                node.applyCSS(style);
                tlist.addType(node);
            }
            else
            {    
              LWLink link = new LWLink();
              link.setLabel(ot.getLabel() + "-->" + count);
              link.setHeadPoint(10,25);
              link.setTailPoint(140,25);
              link.setAbsoluteSize(150,50);
              //link.setArrowState(LWLink.ARROW_HEAD);
              //link.setWeight(Integer.parseInt(style.getAttribute("weight")));
              
              System.out.println("t1: test panel about to apply style for: " + ot.getLabel());
              System.out.println("tl: test panel applying style: " + ot.getStyle());
              
              link.applyCSS(style);
              tlist.addType(link);
            }
            
            
            //System.out.println("tl: ot: " + ot + " style: " + style.getName());
            /*System.out.println("tl: isNode: " + ot.getId() + " isNode?:" + isNode(ot));
            System.out.println("tl: description: " + ot.getComment());
            LWLink link = new LWLink();
            link.setLabel(ot.getLabel());
            link.setHeadPoint(10,25);
            link.setTailPoint(140,25);
            link.setAbsoluteSize(150,50);
            //link.setArrowState(LWLink.ARROW_HEAD);
            //link.setWeight(Integer.parseInt(style.getAttribute("weight")));
            link.applyCSS(style);
            tlist.addType(link);*/
        }
        
        LWNode node = new LWNode("Fedora Object");
        //node.setLabel("Fedora Object");
        node.setAbsoluteSize(150,50);
        //node.setShape( new java.awt.Rectangle(5,5,135,45));
        NodeTool.SubTool st = NodeTool.getActiveSubTool();
        node.setShape(st.getShape());
        tlist.addType(node);
        
        return testPanel;
    }
    
}
