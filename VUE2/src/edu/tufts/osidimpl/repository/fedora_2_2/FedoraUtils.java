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

/**
 *
 * @author  akumar03
 */
package  edu.tufts.osidimpl.repository.fedora_2_2;

import java.io.*;
import java.net.*;
import java.util.prefs.Preferences;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;

public class FedoraUtils {
    
    /** Creates a new instance of FedoraUtils */
    public static final String SEPARATOR = ",";
    public static final String NOT_DEFINED = "Property not defined";
    public static final String OP_ACTUALS = "fedora.search.advanced.operators.actuals";
    
    private static java.util.Map prefsCache = new java.util.HashMap();
    
    public static java.util.Vector stringToVector(String str) {
        java.util.Vector vector = new java.util.Vector();
        java.util.StringTokenizer  st = new java.util.StringTokenizer(str,SEPARATOR);
        while(st.hasMoreTokens()){
            vector.add(st.nextToken());
        }
        return vector;
    }
       public static String getFedoraProperty(Repository repository,String pLookupKey)
    throws org.osid.repository.RepositoryException {
        try {
            return getPreferences(repository).get(pLookupKey, NOT_DEFINED);
        } catch (Exception ex) {
            throw new org.osid.repository.RepositoryException("FedoraUtils.getFedoraProperty: " + ex);
        }
    }
    
    public static Preferences getPreferences(Repository repository)
    throws java.io.FileNotFoundException, java.io.IOException, java.util.prefs.InvalidPreferencesFormatException {
		//if(repository.getPrefernces() != null) {
//			return repository.getPrefernces();
//		} else {
			String conf = repository.getConf();
			Preferences prefs = (Preferences) prefsCache.get(conf);
			if (prefs != null)
				return prefs;
			Class clazz = new FedoraUtils().getClass();
			prefs = Preferences.userNodeForPackage(clazz);
			//System.out.println("trying to find preferences " + conf);
			InputStream stream = null;
			try {
				stream = new FileInputStream(conf);
			} catch (java.io.FileNotFoundException fex) {
				String path = null;
				try {
					edu.tufts.vue.dsm.OsidFactory factory = edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance();
					path = factory.getResourcePath(conf);
				} catch (Throwable t) {
				}
				stream = new FileInputStream(path);
			}
			prefs.importPreferences(stream);
			prefsCache.put(conf, prefs);
			stream.close();
			return prefs;
//		}
    }
    public static  String processId(String pid ) {
        java.util.StringTokenizer st = new java.util.StringTokenizer(pid,":");
        String processString = "";
        while(st.hasMoreTokens()) {
            processString += st.nextToken();
        }
        return processString;
    }
    
    public static String[] getFedoraPropertyArray(Repository repository,String pLookupKey)
    throws org.osid.repository.RepositoryException {
        String pValue = getFedoraProperty(repository,pLookupKey);
        return pValue.split(SEPARATOR);
    }
    
    public static String[] getAdvancedSearchFields(Repository repository)  throws org.osid.repository.RepositoryException{
        return getFedoraPropertyArray(repository,"fedora.search.advanced.fields");
    }
    public static String[] getAdvancedSearchOperators(Repository repository)  throws org.osid.repository.RepositoryException{
        return getFedoraPropertyArray(repository,"fedora.search.advanced.operators");
        
    }
    public static String getAdvancedSearchOperatorsActuals(Repository repository,String pOperator) throws org.osid.repository.RepositoryException{
        String[] pOperators =   getAdvancedSearchOperators(repository);
        String[] pOperatorsActuals = getFedoraPropertyArray(repository,OP_ACTUALS);
        String pValue = NOT_DEFINED;
        boolean flag = true;
        for(int i =0;i<pOperators.length && flag;i++) {
            if(pOperators[i].equalsIgnoreCase(pOperator)) {
                pValue = pOperatorsActuals[i];
                flag = false;
            }
        }
        return pValue;
    }
    
    
    
    
    public static String getSaveFileName(org.osid.shared.Id objectId,org.osid.shared.Id behaviorId,org.osid.shared.Id disseminationId) throws org.osid.OsidException {
        String saveFileName = processId(objectId.getIdString()+"-"+behaviorId.getIdString()+"-"+disseminationId.getIdString());
        return saveFileName;
    }
    
    
    public static AbstractAction getFedoraAction(org.osid.repository.Record record,org.osid.repository.Repository repository) throws org.osid.repository.RepositoryException {
        final Repository mRepository = (Repository)repository;
        final Record mRecord = (Record)record;
        
        try {
            AbstractAction fedoraAction = new AbstractAction(record.getId().getIdString()) {
                public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
                    try {
                        org.osid.shared.Id id = new PID(getFedoraProperty(mRepository, "DisseminationURLInfoPartId"));
                        org.osid.repository.PartIterator partIterator = mRecord.getParts();
                        while (partIterator.hasNextPart()) {
                            org.osid.repository.Part part = partIterator.nextPart();
                            String fedoraUrl = part.getValue().toString();
                            URL url = new URL(fedoraUrl);
                            URLConnection connection = url.openConnection();
                            tufts.Util.openURL(fedoraUrl);
                            break;
                            
                        }
                    } catch(Throwable t) {  }
                }
            };
            return fedoraAction;
        } catch(Throwable t) {
            throw new org.osid.repository.RepositoryException("FedoraUtils.getFedoraAction "+t.getMessage());
        }
    }
    
    private FedoraUtils() {}
}