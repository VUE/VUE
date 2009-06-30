package tufts.vue.gui.renderer;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import tufts.Util;
import tufts.vue.SearchData;
import tufts.vue.VUE;
import tufts.vue.VueUtil;
import tufts.vue.gui.GUI;
import tufts.vue.gui.WidgetStack;

public class SavedSearchTableRenderer extends DefaultTableCellRenderer{
	SearchResultTableModel searchResultTableModel;
	String runStr = "<html><body><b><u>run</u></b></body></html>";
	public SavedSearchTableRenderer(SearchResultTableModel searchResultTableModel){
		this.searchResultTableModel = searchResultTableModel;
	}
	public java.awt.Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,boolean hasFocus,int row,int col)
    {		
		JPanel searchPanel = new JPanel();
		searchPanel.setLayout(new BorderLayout());		
		//java.util.List<SearchData> searchResultArrLst = (List<SearchData>) searchResultTableModel.getData(row);
		SearchData data = searchResultTableModel.getData(row);   		
		JPanel runPanel = new JPanel();
		runPanel.setLayout(new BorderLayout());
		Font macFont = new Font("Lucinda Grande", Font.BOLD, 11);
		Font windowsFont = new Font("Lucida Sans Unicode", Font.BOLD, 11);
		if (isSelected) {
			searchPanel.setBackground(new Color(188,212,255));	
			runPanel.setBackground(new Color(188,212,255));	
        } else {
        	searchPanel.setBackground(Color.white);
        	searchPanel.setForeground(WidgetStack.BottomGradient);
        	runPanel.setBackground(Color.white);
        	runPanel.setForeground(WidgetStack.BottomGradient);
        }
		JPanel linePanel = new JPanel() {
            protected void paintComponent(java.awt.Graphics g) {
            	//setSize(300,20);
                g.setColor(java.awt.Color.GRAY);                       
                //g.drawLine(5,getHeight()/2, getWidth()-15, getHeight()/2);
                int x1 = -5;
                int x2 = getWidth();
                int y1 = getHeight()/2+4;
                int y2 = getHeight()/2+4;
                int dashlength = 5;
                int spacelength = 5;
                if((x1==x2)&&(y1==y2)) {
            		g.drawLine(x1,y1,x2,y2);
            		return;
            		}
            		double linelength=Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1));
            		double yincrement=(y2-y1)/(linelength/(dashlength+spacelength));
            		double xincdashspace=(x2-x1)/(linelength/(dashlength+spacelength));
            		double yincdashspace=(y2-y1)/(linelength/(dashlength+spacelength));
            		double xincdash=(x2-x1)/(linelength/(dashlength));
            		double yincdash=(y2-y1)/(linelength/(dashlength));
            		int counter=0;
            		for (double i=0;i<linelength-dashlength;i+=dashlength+spacelength){
            		g.drawLine((int) (x1+xincdashspace*counter),
            		  (int) (y1+yincdashspace*counter),
            		  (int) (x1+xincdashspace*counter+xincdash),
            		  (int) (y1+yincdashspace*counter+yincdash));
            		counter++;
            		}
            		if ((dashlength+spacelength)*counter<=linelength)
            		g.drawLine((int) (x1+xincdashspace*counter),
            		 (int) (y1+yincdashspace*counter),
            		 x2,y2);            	
            }
            
            public java.awt.Dimension getMinimumSize()
            {
                 return new java.awt.Dimension(this.getWidth(),30);
            }
		};
		if(col == 0){			
			String lblStr = data.getSearchSaveName();
			JLabel searchLbl = new JLabel();			
			searchLbl.setFont(tufts.vue.gui.GUI.LabelFace);
			searchLbl.setText(lblStr);
			searchLbl.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));			
			searchPanel.add(searchLbl, BorderLayout.WEST);			
    		boolean isWindows = VueUtil.isWindowsPlatform();    		    		
    		if(isWindows){
    			searchPanel.setFont(windowsFont);
    		}else{
    			searchPanel.setFont(macFont);
    		}
			return searchPanel;
		}else{			
						
			JLabel runLbl = new JLabel();
			runLbl.setFont(GUI.TitleFace);
			runLbl.setForeground(WidgetStack.BottomGradient);			
			runLbl.setText(runStr);			
			runLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
			runPanel.add(runLbl, BorderLayout.EAST);						
    		boolean isWindows = VueUtil.isWindowsPlatform();    		    		
    		if(isWindows){
    			runPanel.setFont(windowsFont);
    		}else{
    			runPanel.setFont(macFont);
    		}
			return runPanel;
		}		
		
    }
	
}
