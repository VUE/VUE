package org.okip.service.filing.impl.rfs;

import org.okip.service.filing.api.*;
import org.okip.service.shared.api.Agent;
import org.okip.util.agents.RemoteAgent;

/**
   Copyright (c) 2002 Massachusetts Institute of Technology

   This work, including software, documents, or other related items (the
   "Software"), is being provided by the copyright holder(s) subject to
   the terms of the MIT OKI&#153; API Implementation License. By
   obtaining, using and/or copying this Software, you agree that you have
   read, understand, and will comply with the following terms and
   conditions of the MIT OKI&#153; API Implementation License:

   Permission to use, copy, modify, and distribute this Software and its
   documentation, with or without modification, for any purpose and
   without fee or royalty is hereby granted, provided that you include
   the following on ALL copies of the Software or portions thereof,
   including modifications or derivatives, that you make:

    *  The full text of the MIT OKI&#153; API Implementation License in a
       location viewable to users of the redistributed or derivative
       work.

    *  Any pre-existing intellectual property disclaimers, notices, or
       terms and conditions. If none exist, a short notice similar to the
       following should be used within the body of any redistributed or
       derivative Software:
       "Copyright (c) 2002 Massachusetts Institute of Technology
       All Rights Reserved."

    *  Notice of any changes or modifications to the MIT OKI&#153;
       Software, including the date the changes were made. Any modified
       software must be distributed in such as manner as to avoid any
       confusion with the original MIT OKI&#153; Software.

   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
   IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
   CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
   TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
   SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

   The name and trademarks of copyright holder(s) and/or MIT may NOT be
   used in advertising or publicity pertaining to the Software without
   specific, written prior permission. Title to copyright in the Software
   and any associated documentation will at all times remain with the
   copyright holders.

   The export of software employing encryption technology may require a
   specific license from the United States Government. It is the
   responsibility of any person or organization contemplating export to
   obtain such a license before exporting this Software.
*/

/*
 * $Source: /home/svn/cvs2svn-2.1.1/at-cvs-repo/VUE2/src/oki/old/filing/RfsFactory.java,v $
 */

/**
 * Remote file system CabinetFactory implementation.
 *
 * <p>
 * Licensed under the {@link org.okip.service.ApiImplementationLicenseMIT MIT OKI&#153; API Implementation License}.
 *
 * @version $Name: not supported by cvs2svn $ / $Revision: 1.1 $ / $Date: 2003-04-14 20:48:28 $
 */
public class RfsFactory extends org.okip.service.shared.api.PRBReader
    implements org.okip.service.filing.api.CabinetFactory
{
    private org.okip.service.shared.api.Agent owner = null;
    private transient org.okip.util.agents.RemoteAgent remoteAgent = null;
    private transient org.okip.util.romi.client.api.Client client = null;

    private transient static int    RFS_IO_BUFFER_SIZE = 16384; // io buffer size in bytes
    private transient static int    RFS_MAX_CACHE_AGE = 7*1000; // maximum allowable age of cache data in milliseconds
    private transient static String ROMI_API = "org.okip.util.romi.client.api";
    private transient static String ROMI_IMPL = "edu.mit.is.util.romi.client";
    private transient static String ROMI_CLIENT_HOST = "oki1.mit.edu";
    private transient static int    ROMI_CLIENT_PORT = 1898;
    private transient static String ROMI_SERVER_NAME = "edu.mit.is.util.romi.server.Server";
    private transient static boolean trace = false;

    protected int getIOBufferSize()
    {
        readProperties();
        return RFS_IO_BUFFER_SIZE;
    }
    protected int getMaxCacheAge()
    {
        readProperties();
        return RFS_MAX_CACHE_AGE;
    }
    
    private transient static boolean propertiesRead = false;
    private void readProperties()
    {
        if (propertiesRead)
            return;

        String value;
        int intValue;

        if ((value = getClassProperty("ROMI_API")) != null)
            ROMI_API = value;
        if ((value = getClassProperty("ROMI_IMPL")) != null)
            ROMI_IMPL = value;
        if ((value = getClassProperty("ROMI_CLIENT_HOST")) != null)
            ROMI_CLIENT_HOST = value;
        if ((intValue = getClassInteger("ROMI_CLIENT_PORT")) >= 0)
            ROMI_CLIENT_PORT = intValue;
        if ((value = getClassProperty("ROMI_CLIENT_SERVER")) != null)
            ROMI_SERVER_NAME = value;
        if ((intValue = getClassInteger("RFS_MAX_CACHE_AGE")) >= 0) 
            RFS_MAX_CACHE_AGE = intValue;
        if ((intValue = getClassInteger("RFS_IO_BUFFER_SIZE")) > 0)
            RFS_IO_BUFFER_SIZE = intValue;

        trace = getClassBoolean("RFS_TRACE", false);

        propertiesRead = true;
        
    }
    
  /**
   * Return owner of this Factory.
   *
   *
   * @return org.okip.service.shared.api.Agent Owner
   *
   */
  public org.okip.service.shared.api.Agent getOwner() {
    return this.owner;
  }

    /**
     * Set the owner of this Factory.
     *
     * @param owner
     *
     */
    public void setOwner(org.okip.service.shared.api.Agent owner)
    {
        this.owner = owner;
        if (owner != null) {
            // We check for a RemoteAgent here so that getClient()
            // isn't traversing a proxy chain every time
            // it's called just in case there is a
            // RemoteAgent in the list.
            this.remoteAgent = (RemoteAgent)
                getOwner().getProxy(org.okip.util.agents.RemoteAgent.class);
        }
  }

    /**
     * List all root Cabinets currently available in this CabinetFactory
     * implementation.
     *
     * @return list of Cabinets
     * @throws FilingException - if error occurs.
     *
     */
    public Cabinet[] listRoots()
        throws FilingException
    {
        if (this.getClient() != null)
            return (Cabinet[]) this.invoke(this, "listRoots");
        else
            return listRootsLocally();
    }
    
    protected Cabinet[] listRootsLocally()
        throws FilingException
    {
        java.io.File[] roots = java.io.File.listRoots();
        Cabinet[] rootCabinets = new Cabinet[roots.length];
        int gotRoots = 0;
        for (int i = 0; i < roots.length; i++) {
            String idStr;
            try {
                idStr = roots[i].getCanonicalPath();
            } catch (Exception e) {
                System.err.println(e);
                continue;
            }
            rootCabinets[gotRoots++] = new RfsCabinet(this, idStr);
        }
        if (gotRoots != roots.length) {
            Cabinet[] oldRoots = rootCabinets;
            rootCabinets = new Cabinet[gotRoots];
            System.arraycopy(oldRoots, 0, rootCabinets, 0, gotRoots);
        }
        return rootCabinets;
    }

  /**
   * Construct ID from a String.
   * <p> Each RemotelyUniqueIdentifer has a unique string
   * representation.  This method constructs a valid Filing API ID
   * from a string, if the string represents a valid ID string.
   *
   *
   * @param id
   *
   * @return ID object whose String representation is the supplied argument string
   *
   * @throws FilingException - if the string does not represent a valid ID
   */
  public ID idFromString(String id)
  throws FilingException {
    return new RfsID(id);
  }

    /**
     * Get Cabinet by ID.
     *
     * @param id
     *
     * @return Cabinet with given ID
     *
     * @throws FilingPermissionDeniedException - if Factory Owner
     * does not have read permission on the Cabinet with this ID.
     * @throws FilingIOException - if an IO error occurs accessing
     * the Cabinet with this ID.
     * @throws NotFoundException - if there is no Cabinet with this ID.
     *
     * todo?: convert the factory get calls to first get the root
     * cabinet, then do the gets within that -- tho if there are
     * multiple roots, which one would we use?
     */
    private transient java.util.HashMap cabinets = new java.util.HashMap();
    public Cabinet getCabinet(ID id)
        throws FilingException
    {
        RfsCabinet existingCabinet = (RfsCabinet) cabinets.get(id.toString());
        if (existingCabinet != null)
            return existingCabinet;
        else {
            RfsCabinet newCab = new RfsCabinet(this, id.toString());
            cabinets.put(id.toString(), newCab);
            return newCab;
        }
    }

    /**
     * Get ByteStore by ID.
     *
     * @param id
     *
     * @return ByteStore with given ID
     *
     * @throws FilingPermissionDeniedException - if Factory Owner
     * does not have read permission on the ByteStore with this ID.
     * @throws FilingIOException - if an IO error occurs accessing
     * the ByteStore with this ID.
     * @throws NotFoundException - if there is no ByteStore with this ID.
     */
    public ByteStore getByteStore(ID id)
        throws FilingException
    {
        return new RfsByteStore(this, id.toString());
    }

  /**
   * Method createRootCabinet
   *
   * create new empty root cabinet in this implementation
   *
   * @return Cabinet
   *
   * @throws FilingPermissionDeniedException - if Factory Owner
   * does not have permission to create a new root Cabinet.
   * @throws FilingIOException - if an IO error occurs creating
   * the new root Cabinet.
   *
   */
    public Cabinet createRootCabinet()
        throws FilingException
    {
        try {
            return new RfsCabinet(this, ""+getSeparatorChar());
        } catch (NotFoundException e) {
            return null;
        }
    }

  /**
   * Treat a jar (or zip) file in a ByteStore as a Cabinet.
   *
   * @param jar
   *
   * @return Cabinet usable to access jar/zip file contained in ByteStore
   *
   * @throws FilingPermissionDeniedException - if Factory Owner
   * does not have read permission on the ByteStore.
   * @throws FilingIOException - if an IO error occurs accessing
   * the ByteStore.
   */
  public Cabinet getCabinetFromJar(ByteStore jar)
  throws FilingException {
      throw new UnsupportedOperationException();
  }

    /**
     * Return all capability properties of this CabinetFactory.
     *
     * Capability properties may include:<br>
     * <ul>
     * <li>replication (true/false)
     * <li>versioning (true/false)
     * <li>exclusive control to OKI system (true/false)
     * <li>supports quota (true/false)
     * <li>readwrite (true/false)
     * <li>space limit (true/false)
     * <li>degree of replication (Integer)
     * <li>number of versions supported (Integer)
     * <li>space available (Long)
     * </ul>
     * @return java.util.Map
     *
     */
    public java.util.Map getProperties()
        throws FilingException
    {
        return getStaticProperties();
    }
    
    private static transient final java.util.Map properties = new java.util.Hashtable();
    protected static java.util.Map getStaticProperties()
    {
        if (properties.isEmpty()) {
            /*
             * Properties of this implementation
             */
            properties.put(new CapabilityType("MIT", "remoteAccess"), Boolean.TRUE);
            properties.put(new CapabilityType("MIT", "localAccess"), Boolean.TRUE);
            properties.put(new CapabilityType("MIT", "metaDataCache"), Boolean.TRUE);
            
            // these negative capabilities are given as examples,
            // negative is the default so these are not actually needed
            // readwrite is provided as a positive
            properties.put(new CapabilityType("MIT", "replication"), Boolean.FALSE);
            properties.put(new CapabilityType("MIT", "quota"), Boolean.FALSE);
            properties.put(new CapabilityType("MIT", "readwrite"), Boolean.TRUE);
        }
            
        return java.util.Collections.unmodifiableMap(properties);
    }
  
    public void setClient(org.okip.util.romi.client.api.Client client)
    {
        this.client = client;
    }

    /**
     * getClient - gets romi Client if one exists.
     *
     * The existance of a romi Client indicates that this CabinetFactory
     * and the objects it creates exist on a romi Server.
     *
     * @return org.okip.util.romi.client.api.Client
     *
     * @throws org.okip.util.romi.client.api.ClientException
     */
    public org.okip.util.romi.client.api.Client getClient()
        throws FilingException
    {
        if (client == null && remoteAgent != null) {
            try {
                setClient(createClient(remoteAgent.getHost(), remoteAgent.getPort(), getOwner()));
            } catch (org.okip.service.shared.api.Exception e) {
                throw new FilingException(e, "Failed to create client for RemoteAgent " + remoteAgent);
            }
        }
        return client;
    }

    /**
     * createClient - create a romi client using given host
     * and port
     *
     * @return org.okip.util.romi.client.api.Client
     *
     * @throws org.okip.util.romi.client.api.Exception
     */
    public org.okip.util.romi.client.api.Client createClient(String host, int port, Agent agent)
        throws org.okip.service.shared.api.Exception
    {
        readProperties();
        org.okip.util.romi.client.api.Factory clientFactory;
        
        clientFactory = (org.okip.util.romi.client.api.Factory)
            org.okip.service.shared.api.FactoryManager.getFactory(ROMI_API, ROMI_IMPL, agent);
            
        if (host == null || host.length() == 0)
            host = ROMI_CLIENT_HOST;
        if (port < 0)
            port = ROMI_CLIENT_PORT;
        try {
            return clientFactory.createClient(host, port, ROMI_SERVER_NAME);
        } catch (org.okip.service.shared.api.Exception e) {
            throw new FilingException(e, "failed to create client: host="+host + " port="+port + " server="+ROMI_SERVER_NAME);
        }
    }
    
    /**
     * createDefaultClient - create a romi client using the pre-defined 
     * implementation configuration information.
     *
     * @return org.okip.util.romi.client.api.Client
     *
     * @throws org.okip.util.romi.client.api.Exception
     */
    public org.okip.util.romi.client.api.Client createDefaultClient()
        throws org.okip.service.shared.api.Exception
    {
        readProperties();
        return createClient(ROMI_CLIENT_HOST, ROMI_CLIENT_PORT, getOwner());
    }
    

    
    private transient char separatorChar = 0;
    public char getSeparatorChar()
        throws FilingException
    {
        if (separatorChar != 0)
            return separatorChar;
        if (this.getClient() != null) {
            try {
                return separatorChar = ((Character) invoke(this, "getSeparatorChar")).charValue();
            } catch (Exception e) {
                throw new FilingException(e);
            }
        } else
            return java.io.File.separatorChar;
    }

    private static String argparse(Object o)
    {
        String s;
        if (o == null)
            s = "null";
        else if (o instanceof byte[])
            s = "byte[" + ((byte[])o).length + "]";
        else if (o instanceof RfsEntry)
            s = ((RfsEntry)o).getNameInternal();
        else if (o instanceof String)
            s = "\"" + o + "\"";
        else
            s = o.toString();
        return s;
    }

    protected Object invoke(java.io.Serializable object, String methodName, Class[] argTypes, Object[] args)
        throws FilingException
    {
        // We use this common code to propagate network exceptions
        try {
            if (trace) {
                String msg = "INVOKE " + object.getClass().getName() + "::" + methodName + "(";
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) msg += ", ";
                    msg += argparse(args[i]);
                }
                msg += ")";
                if (object instanceof RfsEntry)
                    msg += " <" + ((RfsEntry)object).getNameInternal() + ">";
                else 
                    msg += " <" + object + ">";
                System.err.print(msg);
            }

            /*
             * Make the remote call
             */
            Object returnValue = this.getClient().invoke(object, methodName, argTypes, args);
            
            if (trace) System.err.println(" = " + argparse(returnValue));

            return returnValue;
            
        } catch (org.okip.util.romi.client.api.ClientException ce) {
            if (ce.getTarget() instanceof FilingException) {
                // a returned exception from the server has no stack trace, so be sure
                // to generate a new one here.
                if (trace) System.err.println(" =FE " + ce.getTarget());
                ce.getTarget().fillInStackTrace();
                throw (FilingException) ce.getTarget();
            } else {
                if (trace) System.err.println(" =ERR " + ce.getTarget());
                throw new FilingException(ce.getTarget(), "unexpected remote exception: " + ce.getTarget());
            }
        } catch (FilingException e) {
            if (trace) System.err.println(" =fe " + e);
            throw e;
        } catch (Exception e) {
            if (trace) System.err.println(" =e " + e);
            throw new FilingException(e);
        }
    }
    protected Object invoke(final java.io.Serializable object, final String methodName)
        throws FilingException
    {
        return invoke(object, methodName, noArgTypes, noArgs);
    }
    
    protected Object invoke(final java.io.Serializable object, final String methodName, final Class c, final Object arg0)
        throws FilingException
    {
        return invoke(object, methodName, new Class[]{c}, new Object[]{arg0});
    }
    
    protected static final Class[] noArgTypes = new Class[0];
    protected static final Object[] noArgs = new Object[0];

    public String toString()
    {
        String s = getClass().getName() + "[owner="+owner;
        if (client != null)
            s += " "+this.client;
        s += "]";
        return s;
    }
}
