// CabUtil.java -- utilities for access cabinets

import java.io.*;
import java.util.*;
import java.net.*;
import java.rmi.*;

import org.okip.util.agents.RemoteAgent;
import org.okip.util.romi.client.api.Client;
import org.okip.service.filing.api.*;
import org.okip.service.shared.api.FactoryManager;
import org.okip.service.shared.api.Agent;

public class CabUtil
{
    public static final CapabilityType FSReadWrite = new CapabilityType("MIT", "readwrite");
    public static final CapabilityType FSMetaCaching = new CapabilityType("MIT", "metaDataCache");
    public static final CapabilityType FSLocalAccess = new CapabilityType("MIT", "localAccess");
    public static final CapabilityType FSRemoteAccess = new CapabilityType("MIT", "remoteAccess");

    public static boolean USE_CACHING_FILESYSTEM = true;
    public static boolean NEW_FILESYSTEMS = true;
    
    private static CabinetFactory lfsFactory = null;
    private static boolean verbose = false;
    
    public static Cabinet getCabinetFromDirectory(String path)
    {
        return getCabinet(path);
    }
    
    public static Cabinet getCabinet(String path)
    {
        return (Cabinet) getCabinetEntry(path, true);
    }
    
    public static ByteStore getByteStore(String path)
    {
        return (ByteStore) getCabinetEntry(path, false);
    }
    
    public static CabinetEntry getCabinetEntry(String path, boolean isCabinet)
    {
        if (path.indexOf(':') >= 0) {
            URL url;
            try {
                url = new URL(path);
            } catch (MalformedURLException e) {
                errout(e);
                return null;
            }
            if (url.getProtocol().equals("ftp")) {
                // "ftp" means use passthru RFS code with no actual connection
                return getRfsCabinetFromDirectory(url.getPath());
            } else
                return getRfsCabinetEntryFromURL(url, isCabinet);
            
        } else {
            return getEntryFromJavaFile(new File(path), isCabinet);
        }
    }
    
    /*
     *  Get a Cabinet given a directory in the local filesystem
     */
    public static CabinetEntry getEntryFromJavaFile(File file, boolean isCabinet)
    {
        if (isCabinet && !file.isDirectory()) {
            errout("not a directory: " + file);
            return null;
        } else if (!isCabinet && !file.isFile()) {
            errout("not a file: " + file);
            return null;
        }

        if (lfsFactory == null)
            lfsFactory = getLfsCabinetFactory();
        CabinetEntry entry = null;

        outln("Fetching local entry [" + file + "] thru " + lfsFactory.getClass());
        
        try {
            String path = file.getPath();
            ID id = lfsFactory.idFromString(path);
            if (verbose) outln("Got ID [" + id + "] from path '" + path + "'");
            if (isCabinet)
                entry = lfsFactory.getCabinet(id);
            else
                entry = lfsFactory.getByteStore(id);
        } catch (Exception e) {
            errout(e);
            return null;
        } 
        
        if (verbose) outln("Got local file '" + file + "' as CabinetEntry " + entry);

        return entry;
    }
    
    /*
     * Get a Cabinet given a directory in the local filesystem,
     * yet get it through a client-less RFS Cabinet.  (Any
     * reason we'd ever do this except for testing?)
     */
    public static Cabinet getRfsCabinetFromDirectory(String path)
    {
        //outln("Fetching RFS passthru cabinet from path: [" + path + "]");
        File directory = new File(path);

        if (!directory.isDirectory()) {
            errout("not a directory: " + directory);
            return null;
        }
        outln("Fetching RFS passthru cabinet from directory: [" + directory + "]");

        CabinetFactory rfsFactory = getRfsCabinetFactory(null);
        Cabinet cabinet = null;

        try {
            cabinet = rfsFactory.getCabinet(rfsFactory.idFromString(directory.getPath()));
        } catch (Exception e) {
            errout(e);
            return null;
        }
        
        if (verbose) outln("Got remote dir [" + directory + "] thru cabinet [" + cabinet + "]");

        return cabinet;
    }

    
    /*
     *  Get a Local FilesSystem Cabinet Factory
     */
    public static CabinetFactory getLfsCabinetFactory()
    {
        org.okip.service.filing.api.Factory fileFactory = getFilingServiceFactory();
        CabinetFactory lfsFactory = null;
        
        try {
            /*
             * Here's an example of getting a cabinet factory by type
             */
            if (NEW_FILESYSTEMS)
                lfsFactory = fileFactory.getCabinetFactoryByType(new org.okip.service.filing.impl.rfs.RfsType());
            else
                lfsFactory = fileFactory.getCabinetFactoryByType(new org.okip.service.filing.impl.localfilesystem.LfsType());
            if (false && verbose) { // listRoots debug
                outln("got lfsFactory by type [" + lfsFactory + "]");
                Cabinet[] roots = lfsFactory.listRoots();
                System.err.println("root count: " + roots.length);
                for (int i=0;i<roots.length;i++)
                    System.err.println("root cabinet #" + i + ": " + roots[i]);
            }
            return lfsFactory;
        } catch (Exception e) {
            errout(e);
            return null;
        }
    }

    private static Factory getFilingServiceFactory()
    {
        org.okip.service.filing.api.Factory fileFactory;

        try {
            fileFactory = (Factory)
                FactoryManager.getDefaultFactory("org.okip.service.filing.api", getUserAgent());
        } catch (Exception e) {
            errout(e);
            return null;
        }
        return fileFactory;
    }

    private static Agent getUserAgent()
    {
        Agent userAgent = new Agent();
        String userName = System.getProperty("user.name");
        try {
            String hostName = java.net.InetAddress.getLocalHost().toString();
            userName += "@" + hostName;
        } catch (java.net.UnknownHostException e) {}
        userAgent.setName(userName);
        return userAgent;
    }

    
    /*
     *  Get a RemoteFileSystem Cabinet Factory
     */
    public static CabinetFactory getRfsCabinetFactory(Agent agent)
    {
        Factory fileFactory = getFilingServiceFactory();

        try{        
            java.util.Map props = new java.util.Hashtable();
            props.put(FSReadWrite, Boolean.TRUE);
            props.put(FSLocalAccess, Boolean.TRUE);
            props.put(FSRemoteAccess, Boolean.TRUE);
            if (USE_CACHING_FILESYSTEM)
                props.put(FSMetaCaching, Boolean.TRUE);
            else
                props.put(FSMetaCaching, Boolean.FALSE);
            //System.err.println("looking for props: " + props);
            CabinetFactory[] matches = fileFactory.getCabinetFactory(props);
            if (matches.length == 0 && USE_CACHING_FILESYSTEM) {
                if (verbose) outln("couldn't find factory that supports " + FSMetaCaching);
                props.remove(FSMetaCaching);
                matches = fileFactory.getCabinetFactory(props);
            }
            /*
            System.err.println("matches=" + matches.length);
            for (int i = 0; i < matches.length; i++)
                System.err.println("match " + i + " = " + matches[i]);
            */
            CabinetFactory rfsFactory = matches[0];
            Agent userAgent = getUserAgent();
            if (agent != null)
                userAgent.addProxy(agent);
            rfsFactory.setOwner(userAgent);
            if (verbose) outln("got RfsFactory [" + rfsFactory + "] that matched properties " + props);

            return rfsFactory;
        
        } catch (Exception e) {
            errout(e);
            return null;
        }
    }

    private static void ensureSecurityManager()
    {
        Properties sysProps = System.getProperties();
        if (sysProps.get("java.security.policy") == null) {
            String defaultPolicyFile = "all.policy";
            if (new File(defaultPolicyFile).exists()) {
                sysProps.put("java.security.policy", defaultPolicyFile);
            } else {
                try {
                    File tmpPolicy = File.createTempFile(defaultPolicyFile, null);
                    String allPermission = "grant { permission java.security.AllPermission \"\",\"\"; };\n";
                    FileOutputStream fout = new FileOutputStream(tmpPolicy);
                    fout.write(allPermission.getBytes());
                    fout.close();
                    tmpPolicy.deleteOnExit();
                    sysProps.put("java.security.policy", tmpPolicy.getPath());
                } catch (Exception e) {
                    errout(e);
                }
            }
            if (verbose) outln("defaulted: java.security.policy=" + sysProps.get("java.security.policy"));
        }
        System.setSecurityManager(new RMISecurityManager());
        if (verbose) outln("RMISecurityManager in place.");
        securityManagerSet = true;
    }
    
    /*
     * Get a Cabinet given a directory in a REMOTE filesystem
     */
    private static boolean securityManagerSet = false;
    public static CabinetEntry getRfsCabinetEntryFromURL(URL url, boolean cabinet)
    {
        if (!securityManagerSet)
            ensureSecurityManager();

        CabinetFactory rfsFactory = getRfsCabinetFactory(new RemoteAgent(url));

        outln("Fetching RFS cabinet from URL: [" + url + "] thru " + rfsFactory.getClass());

        CabinetEntry entry = null;
        try {
            if (cabinet)
                entry = rfsFactory.getCabinet(rfsFactory.idFromString(url.getPath()));
            else
                entry = rfsFactory.getByteStore(rfsFactory.idFromString(url.getPath()));
        } catch (Exception e) {
            errout(e);
            return null;
        } 
        
        if (verbose) outln("Got REMOTE url [" + url + "] as [" + entry + "]");

        return entry;
        
    }
    private static void getAllCabinetFactories()
    {
        outln("getAllCabinetFactories");
        try {
            CabinetFactory cabinetFactory = null;
        
            // get factory for implmentation
            Factory fileFactory = (Factory)
                FactoryManager.getDefaultFactory("org.okip.service.filing.api", null);
            
            List cfs = fileFactory.getAvailableCabinetFactories( (org.okip.service.shared.api.Agent) null );
            Iterator iter = cfs.iterator();
            int i = -1;
            while (iter.hasNext()) {
                Object o = iter.next();
                i++;
                outln("CF#" + i + ": " + o + " (" + o.getClass() + ")");
                try {
                    CabinetFactory cf = (CabinetFactory) o;
                    outln("CF#" + i + ": props=" + cf.getProperties());
                } catch (Exception e) { errout(e.toString()); }
            }
        } catch (Exception e) {
            errout(e);
        }
    }
    
    /*
     * example code that gets a cabinet factory via capabilities
     * -- note that as written and with the current underlying implementation,
     * this method will currently return the local filesystem factory.
     */
    private static CabinetFactory getCabinetFactoryByProps()
    {
        try {
            CabinetFactory cabinetFactory = null;
        
            // get factory for implmentation
            Factory fileFactory = (Factory)
                FactoryManager.getDefaultFactory("org.okip.service.filing.api", null);
            // if I get this by TYPE, representable as a string, 
            
            java.util.Map desiredProperties = new java.util.Hashtable();
            desiredProperties.put(new CapabilityType("MIT", "readwrite"), new Boolean(true));
            CabinetFactory cabinetFactories[] = fileFactory.getCabinetFactory(desiredProperties);
            
            outln("got " + cabinetFactories.length + " cabinetFactory matching properties " + desiredProperties);
            if (cabinetFactories.length <= 0) {
                errout("unable to obtain CabinetFactory with properties [" + desiredProperties + "]");
                return null;
            }
            for (int i = 0; i < cabinetFactories.length; i++) {
                outln("CF#" + i + ": " + cabinetFactories[i]);
                outln("CF#" + i + ": props=" + cabinetFactories[i].getProperties());
            }
            
            cabinetFactory = cabinetFactories[0];
            //outln("roots of cabinetFactory " + cabinetFactory + " are " + cabinetFactory.listRoots());
            outln("returning cabinetFactory " + cabinetFactory + " PROPS=" + cabinetFactory.getProperties());
            return cabinetFactory;
        
        } catch (Exception e) {
            errout(e);
            return null;
        }
    }
    
    public static void printCabinet(Cabinet cabinet)
    {
        outln("The cabinet " + cabinet + " has the following entries:");
        try {
            Iterator i = cabinet.entries();
            while (i.hasNext())
                printCabinetEntry((CabinetEntry) i.next());
        } catch (FilingException e) {
            System.err.println(e);
        }
    }
    
    public static void printCabinetEntry(CabinetEntry ce)
        throws FilingException
    {
        printCabinetEntry(ce, System.out);
    }
    
    public static void printCabinetEntry(CabinetEntry ce, PrintStream ps)
        throws FilingException
    {
        ps.print((ce.isByteStore() ? " ByteStore " : " Cabinet   "));

        char type = '?';
        if (ce.isCabinet())
            type = 'd';
        else if (ce.isByteStore())
            type = '-';
        ps.print(type);
        ps.print(ce.canRead() ? 'r' : '-');
        ps.print(ce.canWrite() ? 'w' : '-');
        ps.print(" [" + ce.getName() + "]");

        //for (int i = 0; i < (12 - ce.getName().length()); i++) ps.print(' ');
        ps.print('\t');

        if (ce.isByteStore())
        {
            try {
                ByteStore bs = (ByteStore) ce;
                ps.print(new Date(bs.getLastModifiedTime()) + " ");
                long len = bs.length();
                for (int i = 100000; i > 1; i /= 10)
                    if (len < i) ps.print(' ');
                ps.print(" " + len + " bytes");
                // ps.print(bs.getOwner().getName()); unimplemented
                // ps.print(bs.getManifestAttributes()); no toString
                //ps.print(" MimeType " + bs.getMimeType());
                
//                 OkiInputStream is = bs.getOkiInputStream();
//                 byte[] b = new byte[16];
//                 int nRead = is.read(b);
//                 if (nRead >= 0) {
//                     ps.print("\tContents=["
//                         + formatForSource(new String(b, 0, nRead))
//                         + "]");
//                 }
                
            } catch (Exception e) {
                errout("error on " + ce + ": " + e);
                ps.println("error: " + ce + ": " + e);
            }
        }
        ps.println();
    }

    public static ArrayList parseCommandLine(String args[])
    {
        String laf = null;

        if (System.getProperty("CABUTIL_OLDFS", "false").equalsIgnoreCase("true"))
            USE_CACHING_FILESYSTEM = NEW_FILESYSTEMS = false;

        ArrayList words = new ArrayList();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.equals("-laf_java"))
                laf = javax.swing.UIManager.getCrossPlatformLookAndFeelClassName();
            else if (a.equals("-laf_windows"))
                laf = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
            else if (a.equals("-verbose"))
                verbose = true;
            else if (a.equals("-newfs")) {
                USE_CACHING_FILESYSTEM = true;
                NEW_FILESYSTEMS = true;
            } else if (a.equals("-oldfs")) {
                USE_CACHING_FILESYSTEM = false;
                NEW_FILESYSTEMS = false;
            }
            else if (a.equals("-debug"))
                verbose = true;
            else if (a.charAt(0) != '-')
                words.add(a);
        }
        if (NEW_FILESYSTEMS)
            errout("Using the new filesystem implementation.");
        else
            errout("Using OLD filesystem implementations.");

        if (laf != null) {
            try {
                outln("Setting look & feel to: " + laf);
                javax.swing.UIManager.setLookAndFeel(laf);
            } catch (Exception e) {
                errout(e);
            }
        }

        return words;
    }
    
    private static void outln(String s) { System.err.println("CabUtil: " + s); }
    private static void errout(String s) { System.err.println("CabUtil: " + s); }
    private static void errout(Exception e)
    {
        if (e instanceof org.okip.service.shared.api.Exception)
            ((org.okip.service.shared.api.Exception)e).printChainedTrace();
        else {
            errout(e.toString());
            e.printStackTrace();
        }
    }
    
    
}
