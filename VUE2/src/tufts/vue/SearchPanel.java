/*
 * SearchPanel.java
 *
 * Created on May 2, 2003, 9:03 PM
 */

package tufts.vue;


import tufts.google.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


/**
 *
 * @author  rsaigal
 */
public class SearchPanel extends JInternalFrame{
  
  
    
    public SearchPanel(int x, int y) {
        super("Vue Search Panel");
        setSize(x,y);
         final int timearound = 1;       
       //Create the query panel and result panel//      
  
        JPanel queryPanel =  new JPanel();        
     
        
        final JPanel resultPanel = new JPanel();
        resultPanel.setSize(400,400);
        resultPanel.setLayout(new BorderLayout());
         
        
        //Layout for the search panel  
        
        GridBagLayout myGridLayout = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();

        
        
        queryPanel.setLayout(myGridLayout);
        final JTextField queryBox = new JTextField();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
    
        myGridLayout.setConstraints(queryBox, c);
        queryPanel.add(queryBox);
 
        JButton searchButton = new JButton("Search"); 
        searchButton.setPreferredSize(new Dimension(100,30));
        c.gridy = 1;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0,150,0,150);
       
        myGridLayout.setConstraints(searchButton, c);
        queryPanel.add(searchButton);
       
       
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
       
        contentPane.add(queryPanel,BorderLayout.NORTH);
        contentPane.add(resultPanel,BorderLayout.CENTER);

        //Add action to the submint button
   
        
        searchButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                
                 
                
                String searchString = queryBox.getText();
                        
                
               if (!searchString.equals("")){
              
                  //resultPanel.invalidate();
                VueDragTree tree = new VueDragTree("Google Search Results",searchString);
                tree.setEditable(true);
                tree.setRootVisible(false);
              
                JTextField ja = new JTextField("Google Search Results");
                JScrollPane jsp = new JScrollPane(tree);
               
                resultPanel.add(ja,BorderLayout.NORTH);
                resultPanel.add(jsp,BorderLayout.CENTER); 
                    
                resultPanel.revalidate();
                //resultPanel.repaint();
                }
                
            
              
            }
            
            });
                
        
    }
    
    /*public static void main(String[] args){
       // System.out.println("Om Shri Ganeshaya Namah");
        
        //Initialize the Search Panel
       
     
        final SearchPanel searchPanel = new SearchPanel(400,600);
        searchPanel.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        searchPanel.pack();
        searchPanel.setSize(400,400);
        searchPanel.setVisible(true);
        
    }
     */
}
