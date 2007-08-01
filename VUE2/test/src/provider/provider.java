package provider;

import java.util.Properties;

import org.osid.*;
import org.osid.provider.*;
import org.osid.shared.*;


public class provider {
    
    ProviderControlManager pcm;

    public provider(String impl) {

	OsidContext context = new OsidContext();
	try {
	    context.assignContext("com.harvestroad.authentication.username","vue");
	    context.assignContext("com.harvestroad.authentication.password","vue");
	    context.assignContext("com.harvestroad.authentication.host","bazzim.mit.edu");
	    context.assignContext("com.harvestroad.authentication.port","80");
	} catch (OsidException e) {
	    System.err.println("this should never happen");
	}


	
	try {
	    this.pcm = (ProviderControlManager) edu.mit.osidimpl.OsidLoader.getManager("org.osid.provider.ProviderControlManager", 
										       impl, context, 
										       new Properties());
	} catch (OsidException e) {
	    System.err.println("cannot load impl " + impl + ": " + e.getMessage());
	    e.printStackTrace();
	}
    }
    
    
    public void list() {
	try {
		System.out.println("List of all providers");
	    ProviderLookupManager plm = this.pcm.getProviderLookupManager();
	    ProviderIterator pi = plm.getProviders();
	    while (pi.hasNextProvider()) {
		printProvider(pi.getNextProvider());
	    }
	} catch (org.osid.provider.ProviderException pe) {
	    pe.printStackTrace();
	}
    }


    public void installed() {
        try {
			System.out.println("List of installed providers");
            ProviderInstallationManager pim = this.pcm.getProviderInstallationManager();
            ProviderIterator pi = pim.getInstalledProviders();
            while (pi.hasNextProvider()) {
                printProvider(pi.getNextProvider());
            }
        } catch (org.osid.provider.ProviderException pe) {
            pe.printStackTrace();
        }
    }

    public void needingUpdate() {
        try {
			System.out.println("List of providers needing update");
            ProviderInstallationManager pim = this.pcm.getProviderInstallationManager();
            ProviderIterator pi = pim.getInstalledProvidersNeedingUpdate();
            while (pi.hasNextProvider()) {
                printProvider(pi.getNextProvider());
            }
        } catch (org.osid.provider.ProviderException pe) {
            pe.printStackTrace();
        }
    }
	
	
    public void install() {

    }

    public void remove() {

    }

    private void printProvider(Provider p) {
	try {
	    System.out.println("\nProvider:    " + p.getDisplayName() + " (" + p.getId().getIdString() + ")");
	    System.out.println("Description: " + p.getDescription());
	    System.out.println("Version: " + p.getVersion());
	    System.out.println("Next Version: " + p.getNextVersion());
	    System.out.println("Previous Version: " + p.getPreviousVersion());
	    System.out.println("Osid: " + p.getOsidName());
	    System.out.println("Osid Version: " + p.getOsidBindingVersion());
	    System.out.println("Osid Binding: " + p.getOsidBinding());
	    System.out.println("Copyright: " + p.getCopyright());
	    System.out.println("License: " + p.getLicense());
	    System.out.println("License Acknowlegement Required?: " + p.requestsLicenseAcknowledgement());
	    System.out.println("Publisher: " + p.getPublisher());
	    System.out.println("Creator: " + p.getCreator());
	    System.out.println("Release Date: " + p.getReleaseDate());
		
		System.out.println("\n\n ------------------------------------------------");
		org.osid.shared.PropertiesIterator pi = p.getProperties();
		while (pi.hasNextProperties()) {
			org.osid.shared.Properties props = pi.nextProperties();
			org.osid.shared.ObjectIterator oi = props.getKeys();
			while (oi.hasNextObject()) {
				java.io.Serializable o = oi.nextObject();
				System.out.println(o + " : " + props.getProperty(o));
				
				String key = (String)o;
				try {
					if (key.equals("icon16x16")) {
						ProviderInvocationManager providerInvocationManager = this.pcm.getProviderInvocationManager();
						String path = providerInvocationManager.getResourcePath((String)props.getProperty(o));
						System.out.println("path " + path);
					}
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}
	} catch (SharedException se) {
	    System.err.println(se.getMessage());
	}
	
	return;
    }



    public static void main(String[] args) {

//	argparser.ArgParser parser = new argparser.ArgParser("provider");

//	parser.matchAllArgs(args, 0, argparser.ArgParser.EXIT_ON_ERROR);

	try {
	    provider p = new provider("edu.mit.osidimpl.provider.repository");
	    p.list();
	    p.installed();
		p.needingUpdate();
	} catch (Throwable t) {
	    t.printStackTrace();
	}
    }
}
