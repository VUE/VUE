package edu.tufts.osidimpl.repository.fedora_2_0;

public class Utilities
{
	public static String formatObjectUrl(String objectId,String methodId,Repository repository) throws org.osid.repository.RepositoryException {
        String  url = "";
        try {
            url = repository.getFedoraProperties().getProperty("url.fedora.get")+objectId+"/"+methodId;
        }catch (Throwable t) {
            t.printStackTrace();
        }
        return url;
    }
}