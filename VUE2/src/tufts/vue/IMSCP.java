/*
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

/*
 * IMSCP.java
 *
 * Created on December 4, 2003, 12:34 PM
 */

package tufts.vue;


import java.net.*;
import java.io.*;
import java.util.zip.*;



/**
 *
 * @author  akumar03
 */
public class IMSCP {
    
    public static final String MANIFEST_FILE = "imsmanifest.xml";
    public static final String MAP_FILE = "concept_map.vue";
    public static final String RESOURCE_FILES = "resource-files";
    private final int BUFFER_SIZE = 10240;

    private URL location;
    private File tempFile;
    private ZipFile zipFile;
    private ZipOutputStream zos;
    
    public IMSCP() throws  IOException{
        tempFile = File.createTempFile("vueCMAP",".zip");
        zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile)));
    }
    /**
    public IMSCP(tufts.vue.ConceptMap map,URL location) {
    }
    **/
    public IMSCP(URL location) throws ZipException,IOException{
        this.location = location;
        createTempFile();
    }
    
    
    public InputStream getManifest() throws ZipException,IOException{
        return getContent(MANIFEST_FILE);
    }
    
    public InputStream getConceptMap() throws ZipException,IOException {

        return getContent(MAP_FILE);
    }
    
    public InputStream getResource(String fileName) throws ZipException, IOException{
       return getContent(RESOURCE_FILES+File.separator+fileName);
    }
    
    public File getFile() throws IOException {
        return tempFile;
    }
    
    public void putEntry(String entryName,File file) throws ZipException, IOException {
        
        byte[] buf = new byte[1024]; // buffer for reading file
        int len;
        if(zos == null) 
            throw new ZipException("IMSCP.putEntry(File file): ZipOutputstream not initialized");
        try {
        ZipEntry ze = new ZipEntry(entryName);
        zos.putNextEntry(ze);
        //BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file.getAbsolutePath().replaceAll("%20"," ")));
        BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
        while ((len = fis.read(buf)) > 0) {
           zos.write(buf, 0, len);
        }
        fis.close();
        zos.closeEntry();
        } catch(ZipException ex) {
           if(!ex.getMessage().startsWith("duplicate")) // ignoring duplicate entries
               throw ex;
        }
    }
    
    public void putEntry(File file) throws ZipException, IOException {
        putEntry(file.getName(),file);
    }
    
    private InputStream getContent(String fileName) throws ZipException, IOException {
        InputStream inputStream;
        zipFile = new ZipFile(tempFile);
        ZipEntry entry = zipFile.getEntry(fileName);
        inputStream = zipFile.getInputStream(entry);
        return inputStream;
    }
    
    private void createTempFile() throws ZipException, IOException{
        String fileName = location.getFile();
        String prefix = fileName.substring(fileName.lastIndexOf("/")+1,fileName.indexOf("."));
        String suffix = fileName.substring(fileName.indexOf("."));
        this.tempFile = File.createTempFile(prefix, suffix);
        tempFile.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(tempFile);
      
        InputStream inputStream = location.openStream();
        BufferedInputStream bufIS = new BufferedInputStream(inputStream);
        byte[] buf = new byte[BUFFER_SIZE];
        int len = BUFFER_SIZE;
        byte b;
        while ((len  = bufIS.read(buf,0,len)) > 0)  
           fos.write(buf,0,len);
        fos.close();
        System.out.println("prefix = "+prefix+ " suffix = "+suffix+ " location  = "+tempFile.getAbsolutePath());
    }
     
    public void close() throws IOException{
        zipFile.close();
    }
    
    public void closeZOS() throws IOException {
        zos.close();
    }
    
    protected void finalize() throws Throwable{
        super.finalize();
        close();
    }
}
