/*
 * AssetViewer.java
 *
 * Created on July 2, 2003, 9:36 AM
 */

package tufts.vue;

/**
 *
 * @author  akumar03
 */

import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.io.*;
import java.util.Vector;
import javax.swing.filechooser.FileSystemView;
import tufts.dr.fedora.*;
import java.net.*;
import osid.dr.*;
import osid.OsidException;

public class AssetViewer extends JFrame {
    public AssetViewer(Asset asset) { 
        super("Asset Viewer");
        JTabbedPane assetPane= new JTabbedPane();
        getContentPane().setLayout(new BorderLayout());
        InfoRecordIterator i;
       
        try {
            i = asset.getInfoRecords();
            while(i.hasNext()) {
                  InfoRecord infoRecord = i.next();
                  InfoFieldIterator inf = infoRecord.getInfoFields();
                   JTabbedPane infoRecordPane = new JTabbedPane();
                  //infoRecordPane.setTabPlacement(JTabbedPane.LEFT);
            
                  while(inf.hasNext()) {
                      InfoField infoField = inf.next();
                      String method = asset.getId().getIdString()+"/"+infoRecord.getId().getIdString()+"/"+infoField.getValue().toString();
                      DisplayPane dPane  = new DisplayPane(tufts.vue.VUE.prefs.get("url.fedora.get", "")+method);
                      infoRecordPane.addTab(infoField.getValue().toString(),dPane);
                  }
                  assetPane.addTab(infoRecord.getId().getIdString(),infoRecordPane);
            }
        } catch(Exception e) { System.out.println("MapViewer.getAssetMenu"+e);}
        getContentPane().add(assetPane,BorderLayout.CENTER);
    }
}
class DisplayPane extends JPanel{
    
    /** Creates a new instance of EditorPaneDemo */
    static JEditorPane editorPane;  
    static URL url;
    static  ImageIcon image;
    public DisplayPane(String location) {
      
        image = new ImageIcon();
        setLayout(new BorderLayout());
        setSize(400,400);
        try {
            url = new URL(location);
            URLConnection uConn = url.openConnection();
            if((uConn.getContentType().equals("text/html")) || (uConn.getContentType().equals("text/xml")) || uConn.getContentType().equals("text/html; charset=UTF-8")) {
                editorPane = new JEditorPane();
                editorPane.setEditable(false);
                editorPane.setSize(400,400);
                editorPane.setPage(url);
                JScrollPane jSP = new JScrollPane(editorPane);
                add(jSP,BorderLayout.CENTER);
            } else if (uConn.getContentType().equals("image/jpeg") || uConn.getContentType().equals("image/gif")){   
                image = new ImageIcon(url);
                JButton imageButton = new JButton(image);
                imageButton.setBorderPainted(false);
                add(imageButton,BorderLayout.CENTER);
            } else {
                add(new JLabel("Not Implemented"),BorderLayout.CENTER);
            }
        } catch (Exception e) {
            System.err.println("Attempted to read a bad URL: " + url);
        }
       
    }

    
}