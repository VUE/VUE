/*
 * FedoraObjectViewer.java
 *
 * Created on October 2, 2003, 11:15 AM
 */

package tufts.oki.dr.fedora;

/**
 *
 * @author  akumar03
 */

import osid.dr.*;
import java.net.*;
import java.io.*;
import javax.swing.*;
import javax.swing.text.html.*;
import java.awt.*;
import java.awt.event.*;


// these classses are required for soap implementation of 

import fedora.server.types.gen.*;
//axis files

public class FedoraObjectViewer extends JFrame {
    
    /** Creates a new instance of FedoraObjectViewer */
    public FedoraObjectViewer(Asset asset) {
        JTabbedPane assetPane= new JTabbedPane();
        getContentPane().setLayout(new BorderLayout());
        InfoRecordIterator i;
         try {
            i = asset.getInfoRecords();
           while(i.hasNext()) {
                  InfoRecord infoRecord = i.next();
                  System.out.println(infoRecord.getId().getIdString());
                  InfoFieldIterator inf = infoRecord.getInfoFields();
                  JTabbedPane infoRecordPane = new JTabbedPane();
                  //infoRecordPane.setTabPlacement(JTabbedPane.LEFT);
                  while(inf.hasNext()) {
                      InfoField infoField = inf.next();
                      String method = asset.getId().getIdString()+"/"+infoRecord.getId().getIdString()+"/"+infoField.getId().getIdString();
                      DisplayPane dPane  = new DisplayPane(((FedoraObject)asset).getDR().getFedoraProperties().getProperty("url.fedora.get")+method);
                     
                    //  ((Dissemination)infoField).setValue(FedoraSoapFactory.getDisseminaionStream((Dissemination) infoField));
                      //MIMETypedStream value =  (MIMETypedStream)infoField.getValue();
                     // String saveFileName = FedoraUtils.getSaveFileName(asset.getId(),infoRecord.getId(), infoField.getId());
                     // DisplayPane dPane = new DisplayPane(saveFileName,infoField);
//                      System.out.println("FOViewer : dissemination mimetype :"+value.getMIMEType());
                      infoRecordPane.addTab(infoField.getInfoPart().getDisplayName(),dPane);
                     
                      
                  }
                  infoRecordPane.setSelectedIndex(1);
                  assetPane.addTab(infoRecord.getInfoStructure().getDisplayName(),infoRecordPane);
          
                 
            }
        } catch(Exception ex) { System.out.println("FedoraObjectViewer "+ex);
                    ex.printStackTrace();
        }
         assetPane.setSelectedIndex(1);
        getContentPane().add(assetPane,BorderLayout.CENTER);
        setSize(800, 600);
        setVisible(true);
    }
}
class DisplayPane extends JPanel implements ActionListener{
    /** Creates a new instance of EditorPaneDemo */
    static JEditorPane editorPane;  
    static URL url;
    static  ImageIcon image;
    String disseminationName;
    InfoField infoField;
    static java.util.Properties cache = new java.util.Properties(); 
    public DisplayPane(String location) {
        super();
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
    
    public DisplayPane(String disseminationName,osid.dr.InfoField infoField) {
        this.disseminationName = disseminationName;
        this.infoField = infoField;
        JButton openDisseminationButton = new JButton("Open");
        openDisseminationButton.addActionListener(this);
        add(openDisseminationButton,BorderLayout.CENTER);
    }
    public DisplayPane(MIMETypedStream stream) {
        try {
             HTMLEditorKit editorKit = new HTMLEditorKit();
             HTMLDocument HTMLDoc = (HTMLDocument)editorKit.createDefaultDocument();

            editorPane = new JEditorPane();
            editorPane.setEditable(false);
            editorPane.setSize(400,400);
            editorPane.setContentType(stream.getMIMEType());
            editorPane.read(new ByteArrayInputStream(stream.getStream()),HTMLDoc);
            JScrollPane jSP = new JScrollPane(editorPane);
            add(jSP,BorderLayout.CENTER);
        } catch (Exception ex) {
            System.out.println("DisplayPane: Content "+stream.getMIMEType() + "Exception"+ex);
            ex.printStackTrace();
        }
    }
    
   
    public void openDissemination(){
        try {
            if(cache.containsKey(disseminationName)){
                FedoraUtils.openURL(cache.getProperty(disseminationName));
            } else {
                MIMETypedStream value =  (MIMETypedStream)infoField.getValue();
                String mimeType = value.getMIMEType();
                java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(value.getStream());
                String extension = mimeType.substring(mimeType.indexOf("/")+1);
                if(extension.equals("html")) {
                    String method = ((Dissemination)infoField).getBehavior().getFedoraObject().getId().getIdString()+"/"+((Dissemination)infoField).getBehavior().getId().getIdString()+"/"+infoField.getId().getIdString();
                     FedoraUtils.openURL(((Dissemination)infoField).getBehavior().getFedoraObject().getDR().getFedoraProperties().getProperty("url.fedora.get"));
                } else {
                    String fileName = System.getProperty("java.io.tmpdir")+disseminationName+"."+extension;
                    FileOutputStream fos = new FileOutputStream(fileName);
                    int c;
                    while((c= inputStream.read())!= -1)
                        fos.write(c);
                    fos.close();
                    FedoraUtils.openURL(fileName);
                    cache.setProperty(disseminationName, fileName); 
                }
            }
        }catch(Exception ex) {
              JOptionPane.showMessageDialog(this,
                                          "Cannot Open Dissemination \n " + ex.getMessage(),
                                          "FEDORA ObjectViewer Alert",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().equals("Open")) {
            System.out.println("Button Pressed "+ disseminationName);
            openDissemination();
        }
    }
    
}