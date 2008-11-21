package tufts.vue.gui.renderer;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import tufts.vue.VueUtil;

public class SavedSearchTableRenderer extends DefaultTableCellRenderer{

	public java.awt.Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,boolean hasFocus,int row,int col)
    {		
		JPanel searchPanel = new JPanel();
		searchPanel.setLayout(new BorderLayout());
		JPanel runPanel = new JPanel();
		runPanel.setLayout(new BorderLayout());
		Font macFont = new Font("Lucinda Grande", Font.BOLD, 11);
		Font windowsFont = new Font("Lucida Sans Unicode", Font.BOLD, 11);
		if (isSelected) {
			searchPanel.setBackground(new Color(188,212,255));	
			runPanel.setBackground(new Color(188,212,255));	
        } else {
        	searchPanel.setBackground(Color.white);
        	searchPanel.setForeground(table.getForeground());
        	runPanel.setBackground(Color.white);
        	runPanel.setForeground(table.getForeground());
        }
		JPanel linePanel = new JPanel() {
            protected void paintComponent(java.awt.Graphics g) {
            	//setSize(300,20);
                g.setColor(java.awt.Color.DARK_GRAY);                       
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
			String lblStr = "Search"+" "+row;
			JLabel searchLbl = new JLabel();
			searchLbl.setText(lblStr);
			searchLbl.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));			
			searchPanel.add(searchLbl, BorderLayout.WEST);
			searchPanel.add(linePanel,BorderLayout.SOUTH);	
			
    		boolean isWindows = VueUtil.isWindowsPlatform();    		    		
    		if(isWindows){
    			searchPanel.setFont(windowsFont);
    		}else{
    			searchPanel.setFont(macFont);
    		}
			return searchPanel;
		}else{
			String runStr = "<html><body><font color=\"Blue\"><u>run</u></font></body></html>";		
			JLabel runLbl = new JLabel();
			runLbl.setText(runStr);		
			runLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
			runPanel.add(runLbl, BorderLayout.EAST);			
			runPanel.add(linePanel,BorderLayout.SOUTH);			
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
