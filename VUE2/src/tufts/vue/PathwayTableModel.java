/*
 * PathwayTableModel.java
 *
 * Created on December 3, 2003, 1:15 PM
 */

package tufts.vue;

import javax.swing.table.*;
import javax.swing.*;
import java.awt.*;
import java.util.*;

/**
 *
 * @author  Jay Briedis
 */
public class PathwayTableModel extends DefaultTableModel{
    
    private PathwayTab tab = null;
    private LWPathwayManager manager = null;
    
    
    public PathwayTableModel(PathwayTab tab){
       this.tab = tab;
       this.addTableModelListener(tab);
    }

    public LWPathwayManager getManager(){
        if(manager == null && VUE.getActiveMap() != null) {
            this.manager = VUE.getActiveMap().getPathwayManager();
        }
        return manager;
    }
    
    public void setPathwayManager(LWPathwayManager manager){
        this.manager = manager;
        this.fireTableDataChanged();
    }
    
    public void addPathway(LWPathway pathway){
        if(this.getManager() != null){  
            this.getManager().addPathway(pathway);         
            this.fireTableDataChanged();
            tab.updateControlPanel();
        }        
    }
    
    public LWPathway getCurrentPathway(){
        if(this.getManager() != null)
            return this.getManager().getCurrentPathway();
        else
            return null;
    }

    public Object getElement(int row){
        /*
        Iterator i = pathways.iterator();
        int idx = 0;
        while (i.hasNext()) {
            LWPathway p = (LWPathway) i.next();
            if (cnt == row)
                return p;
            idx++;
        }
        */
        
        if(this.getManager() != null)
            return this.getManager().getPathwaysElement(row);
        return null;
    }
    
    public void addElement(LWComponent comp){
        if(this.getManager() != null){
            this.getManager().addPathwayElement(comp, this.getCurrentPathway());
            this.fireTableDataChanged();
            tab.updateControlPanel();
        }
    }
    
    public void addElements(LWComponent[] array){
        if(this.getManager() != null){
            this.getManager().addPathwayElements(array, this.getCurrentPathway());
            this.fireTableDataChanged();
            tab.updateControlPanel();
        }
    }
    
    public void removeElement(LWComponent comp){
        if(this.getManager() != null){
            this.getManager().removeElement(comp);
            this.fireTableDataChanged();
            /*if (this.getManager().getCurrentPathway().getCurrent() == null){
                tab.removeElement.setEnabled(false);
            }*/
            tab.updateControlPanel();
        }
    }
    
    public void removePathway(LWPathway pathway){
        if(this.getManager() != null){
            
            this.getManager().removePathway(pathway);
            this.fireTableDataChanged();
            tab.updateControlPanel();
            /*if (this.getManager().getCurrentPathway() == null){
                tab.removeElement.setEnabled(false);
                tab.addElement.setEnabled(false);
            }else if(this.getManager().getCurrentPathway().getCurrent() == null){
                tab.removeElement.setEnabled(false);
            }else{
                tab.removeElement.setEnabled(true);
                tab.addElement.setEnabled(true);
            }*/
        }
    }
    
    public synchronized int getRowCount(){
        if(this.getManager() != null)
            return manager.length();
        return 0;
    }

    public int getColumnCount(){
        return 6;
    }
    
    public String getColumnName(int col){
        try{
            switch(col){
                case 0:
                    return "A";
                case 1:
                    return "B";
                case 2:
                    return "C";
                case 3:
                    return "D";
                case 4:
                    return "E";
                case 5:
                    return "F";
            }
        }catch (Exception e){
            System.err.println("exception in the table model:" + e);
        }
        return "";
    }

    public Class getColumnClass(int col){
        if(col == 1)
            return Color.class;
        else if(col == 0 || col == 2 || col == 4 || col == 5)
            return ImageIcon.class;
        else if(col == 3)
            return Object.class;
        else
            return null;
    }
    
    public void setCurrentPathway(LWPathway path){
        if(this.getManager() != null){           
            this.getManager().setCurrentPathway(path);
            this.fireTableDataChanged();
            tab.updateControlPanel();
        }
    }
    
    public boolean isCellEditable(int row, int col){
        LWPathway path = null;
        if(this.getManager() != null){
            if(this.getManager().getPathwaysElement(row) instanceof LWPathway){
                path = (LWPathway)this.getManager().getPathwaysElement(row);
            }else{
                path = this.getManager().getPathwayforElementAt(row);
            }
            if(path != null && path.getLocked())
                return false;
            return (col == 1 || col == 3);
        }
        return false;
    }
    
    public boolean isRepeat(String name){
        boolean isRep = false;
        if(this.getManager() != null){
            Iterator iter = this.getManager().getPathwayIterator();
            while(iter.hasNext()){
                Object obj = iter.next();
                if(obj instanceof LWPathway){
                    LWPathway path = (LWPathway)obj;
                    if(path.getLabel().equals(name))
                        isRep = true;
                }
            }
        }
        return isRep;
    }
    
    public synchronized Object getValueAt(int row, int col){
        
        if(this.getManager() != null){
            Object elem = this.getManager().getPathwaysElement(row);
            if(elem instanceof LWPathway){
                LWPathway pathway = (LWPathway)elem;

                boolean hasNotes = true;
                if(pathway.getNotes().equals(null) || pathway.getNotes().equals(""))
                    hasNotes = false;

                boolean isDisplayed = false;
                if(pathway.getShowing())
                    isDisplayed = true;

                boolean isOpen = false;
                if(pathway.getOpen())
                    isOpen = true;
                
                boolean isLocked = false;
                if(pathway.getLocked())
                    isLocked = true;

                try{
                    switch(col){
                        case 0:
                            return new Boolean(isDisplayed);
                        case 1:
                            return pathway.getBorderColor();
                        case 2:
                            return new Boolean(isOpen);
                        case 3:
                            return pathway.getLabel();
                        case 4:
                            return new Boolean(hasNotes);
                        case 5:
                            return new Boolean(isLocked);
                    }
                }catch (Exception e){
                    System.err.println("exception in the table model, setting pathway cell:" + e);
                } 
            }else if (elem instanceof LWComponent){
                LWComponent comp = (LWComponent)elem;
                try{
                    switch(col){
                        case 3:
                            return comp.getLabel();
                    }
                }catch (Exception e){
                    System.err.println("exception in the table model, setting pathway element cell:" + e);
                }  
            }
        }
        return null; 
    }
    
    public void setValueAt(Object aValue, int row, int col){
        if(this.getManager() != null){
            Object elem = this.getManager().getPathwaysElement(row);
            if(elem instanceof LWPathway){

                LWPathway path = (LWPathway)elem;

                if(path != null){
                    if(col == 0){
                        //path.setShowing();
                        path.toggleShowing();
                    }
                    else if(col == 1){
                        path.setBorderColor((Color)aValue);
                    }
                    else if(col == 2){
                        this.getManager().setPathOpen(path);
                        //manager.setCurrentPathway(path);                   
                    }
                    else if(col == 3){
                        path.setLabel((String)aValue);
                        //manager.setCurrentPathway(path);
                    }
                    else if(col == 5){
                        path.setLocked();
                    }
                    
                }
            }
            else if (elem instanceof LWComponent){
                LWComponent comp = (LWComponent)elem;
                if(col == 3){
                    comp.setLabel((String)aValue);
                }
            }
            this.fireTableDataChanged();         
        }   
    }
}
