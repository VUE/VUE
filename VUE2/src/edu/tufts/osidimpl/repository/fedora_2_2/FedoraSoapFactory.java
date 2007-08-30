/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 *
 * @author  akumar03
 */
package  edu.tufts.osidimpl.repository.fedora_2_2;

import org.osid.repository.*;
import java.net.*;
import java.io.*;
import java.util.Vector;
import java.util.Calendar;



//axis packages
import java.net.*;
import java.io.*;
import org.apache.axis.types.NonNegativeInteger;
import java.rmi.RemoteException ;


// fedora packages
import fedora.client.APIMStubFactory;
import fedora.client.APIAStubFactory;
import fedora.server.management.FedoraAPIM;
import fedora.server.access.FedoraAPIA;
import fedora.server.types.gen.*;

public class FedoraSoapFactory {
    public static final String[] RESULT_FIELDS={"pid", "label","title","description","cModel"};
    
    public static  Vector getDisseminationRecords(String pid,org.osid.repository.RecordStructure recordStructure,Repository repository)   throws org.osid.repository.RepositoryException  {
        Vector disseminationList = new Vector();
        FedoraAPIA APIA;
        Calendar now = Calendar.getInstance();
        try {
            String date = now.get(Calendar.YEAR)+"-"+now.get(Calendar.MONTH)+"-"+now.get(Calendar.DAY_OF_MONTH)+"T"+now.get(Calendar.HOUR_OF_DAY)+":"+now.get(Calendar.MINUTE)+":"+now.get(Calendar.SECOND)+"."+now.get(Calendar.MILLISECOND)+"Z";
            APIA = APIAStubFactory.getStub(repository.getAddress(),repository.getPort(),"dummy","dummy");// username and password are not required, but fedora api is implmented that way
            ObjectMethodsDef[] objMethods= APIA.listMethods(pid,date);
            if(objMethods == null)
                throw new org.osid.repository.RepositoryException("tufts.oki.repository.Asset():No Disseminations  returned");
            else {
                for(int i=0;i<objMethods.length;i++){
                    Record record = new Record(new PID(objMethods[i].getMethodName()),recordStructure);
                    //record.createPart(((DisseminationRecordStructure)recordStructure).getBDEFPartStructure().getId(),objMethods[i].getBDefPID());
                    String disseminationURL = repository.getFedoraProperties().getProperty("url.fedora.get")+pid+"/"+objMethods[i].getBDefPID()+"/"+objMethods[i].getMethodName();
                    record.createPart(((DisseminationRecordStructure)recordStructure).getDisseminationURLPartStructure().getId(), disseminationURL);
                    disseminationList.add(record);
                }
            }
        } catch(Throwable t) {
            throw wrappedException("getDisseminators", t);
        }
        
        return disseminationList;
    }
    

    public static  org.osid.repository.AssetIterator search(Repository repository,SearchCriteria lSearchCriteria)  throws org.osid.repository.RepositoryException {
        FedoraAPIA APIA;
        try {
            APIA = APIAStubFactory.getStub(repository.getAddress(),repository.getPort(),"dummy","dummy");// username and password are not required, but fedora api is implmented that way.
            FieldSearchResult searchResults=new FieldSearchResult();
            FieldSearchQuery query=new FieldSearchQuery();
            NonNegativeInteger maxRes=new NonNegativeInteger(lSearchCriteria.getMaxReturns());
            query.setTerms(lSearchCriteria.getKeywords());
            ListSession listSession = null;
            if(lSearchCriteria.getSearchOperation() == SearchCriteria.FIND_OBJECTS) {
                searchResults = APIA.findObjects(RESULT_FIELDS,maxRes,query);
                listSession = searchResults.getListSession();
                
            }else {
                if(lSearchCriteria.getToken() != null) {
                    searchResults = APIA.resumeFindObjects(lSearchCriteria.getToken());
                    listSession = searchResults.getListSession();
                    
                }
            }
            // setting the token for continuing search
            if(listSession != null)
                lSearchCriteria.setToken(listSession.getToken());
            else
                lSearchCriteria.setToken(null);
            return getAssetIterator(repository, searchResults,lSearchCriteria);
        }catch(Throwable t) {
            throw wrappedException("search", t);
        }
    }
 
    public static org.osid.repository.AssetIterator advancedSearch(Repository repository,SearchCriteria lSearchCriteria)  throws org.osid.repository.RepositoryException {
        FedoraAPIA APIA;
        Condition cond[] = lSearchCriteria.getConditions();
        FieldSearchResult searchResults=new FieldSearchResult();
        NonNegativeInteger maxRes=new NonNegativeInteger(lSearchCriteria.getMaxReturns());
        
        try {
            APIA = APIAStubFactory.getStub(repository.getAddress(),repository.getPort(),"dummy","dummy");// username and password are not required, but fedora api is implmented that way.
            FieldSearchQuery query=new FieldSearchQuery();
            query.setConditions(cond);
            searchResults=   APIA.findObjects(RESULT_FIELDS,maxRes,query);
            java.util.Vector resultObjects = new java.util.Vector();
            return getAssetIterator(repository, searchResults,lSearchCriteria);
        }catch(Throwable t) {
            throw wrappedException("advancedSearch", t);
        }
    }

    private static org.osid.repository.AssetIterator getAssetIterator(Repository repository, FieldSearchResult searchResults, SearchCriteria searchCriteria) throws org.osid.repository.RepositoryException   {
        try {
            java.util.Vector resultObjects = new java.util.Vector();
            if (searchResults != null){
                ObjectFields[] fields= searchResults.getResultList();
                searchCriteria.setResults(fields.length);
                for(int i=0;i<fields.length;i++) {
                    String title = "No Title";
                    if(fields[i].getTitle() != null)
                        title = fields[i].getTitle()[0];
                    resultObjects.add(new Asset(repository,fields[i].getPid(),title,repository.getAssetType(fields[i].getCModel())));
                }
            } else {
                System.out.println("search return no results");
            }
            return new AssetIterator(resultObjects) ;
        }catch(Throwable t){
            throw wrappedException("getAssetIterator", t);
        }
    }


    private static org.osid.repository.RepositoryException wrappedException(String method, Throwable cause)
    {
        cause.printStackTrace();
        org.osid.repository.RepositoryException re =
            new org.osid.repository.RepositoryException("FedoraSoapFactory." + method + "; cause is " + cause);
        re.initCause(cause);
        return re;
    }

    
    
}
