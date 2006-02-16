package edu.tufts.vue.util;

/*
 * -----------------------------------------------------------------------------
 *
 * This file is based on code developed as part of the Lionshare project and
 * distributed under the GNU General Public License.
 *
 * -----------------------------------------------------------------------------
 */

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

import java.util.List;
import java.util.ArrayList;
import java.util.jar.JarFile;

public class OsidClassLoader extends java.lang.ClassLoader
{
  private List jar_files = null;
  private File osid_jar_files = null;
  private java.lang.ClassLoader parent = null; 
  
  public OsidClassLoader(java.lang.ClassLoader parent, String plugins_dir)
  {
    super(parent);
    jar_files = new ArrayList();
    osid_jar_files = new File(plugins_dir);
    this.parent = parent;
    init();
  }

  public Class loadClass(String name, boolean resolve) 
    throws ClassNotFoundException
  {
    Class c;
    if ((c = findClass(name)) == null)
      c = super.loadClass(name, resolve);
      //throw new ClassNotFoundException(name);
    if (resolve)
      resolveClass(c);
    return (c);
  }
  
  public void init()
  {
	  if( osid_jar_files.isDirectory() )
	  {
		  File[] files = osid_jar_files.listFiles();
		  for(int i=0; i<files.length; i++)
		  {
			  //System.out.println("examining " + files[i]);
			  if( files[i].getName().endsWith(".jar") ) {
				try
				{
					//System.out.println("adding to dynamic classpath " + files[i]);
					jar_files.add(new JarFile( files[i] ));
				}
				catch(IOException ioe)
				{
					  ioe.printStackTrace();
					  //We don't really care if we have a corrupt jar file, if
					  //the class don't exist, it don't exist, not our fault.
				}
			  } 
			  else if (files[i].isDirectory())
			  {
				  // search subdirectories -- one level only
				  //System.out.println("searching one level lower");
				  File[] subfiles = files[i].listFiles();
				  for(int j=0; j<subfiles.length; j++)
				  {
					  if( subfiles[j].getName().endsWith(".jar") )
					  {
						  try
						  {
							  //System.out.println("adding to dynamic classpath " + subfiles[j]);
							  jar_files.add(new JarFile( subfiles[j] ));
						  }
						  catch(IOException ioe)
						  {
							  ioe.printStackTrace();
							  //We don't really care if we have a corrupt jar file, if
							  //the class don't exist, it don't exist, not our fault.
						  }
					  }
				  }
			  }
		  }
	  }
  }
  
  public Class findClass(String name) throws ClassNotFoundException
  {
    for( int i=0; i<jar_files.size(); i++)
    {
      JarFile jar = (JarFile)jar_files.get(i);
	  String entry = name;
	  int index = -1;
	  while ( (index = entry.indexOf(".")) != -1 ) {
			entry = entry.substring(0,index) + "/" + entry.substring(index+1);
	  }
//      String entry = name.replaceAll("[.]", "/");
      entry = entry + ".class";
      if( jar.getEntry( entry ) != null )
      {
        try
        {
          byte[] data = loadClassData(entry, jar);
          if( data.length == 0 )
          {
            continue;
          }
          return defineClass(name, data, 0, data.length);
        }
        catch(IOException ioe)
        {
          continue;
        }
      }
    }
    return null;
  }
  

  private byte[] loadClassData(String name, JarFile file) throws IOException 
  {
    BufferedInputStream in = new BufferedInputStream(
        file.getInputStream(file.getEntry(name)));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] results = new byte[0];
    if( in != null )
    {
      while(true)
      {
        byte[] bytes = new byte[4096];
        int read = in.read( bytes );
        if( read < 0 )
        {
          break;
        }
        out.write(bytes, 0, read);
      }
      results = out.toByteArray();
    }
    in.close();
    out.close();
    return results;
  }
}
