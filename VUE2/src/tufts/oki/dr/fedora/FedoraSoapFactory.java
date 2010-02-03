/*
* Copyright 2003-2010 Tufts University  Licensed under the
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
 * FedoraSoapFactory.java
 *
 * Created on September 22, 2003, 4:17 PM
 */

package tufts.oki.dr.fedora;

/**
 *
 * @author  akumar03
 */
import osid.dr.*;
import java.net.*;
import java.io.*;
import java.util.Vector;

import javax.xml.namespace.QName;

import fedora.server.types.gen.*;
import fedora.server.utilities.DateUtility;

//axis files
import org.apache.axis.encoding.ser.*;
import java.net.*;
import java.io.*;
import org.apache.axis.types.NonNegativeInteger;
import org.apache.axis.client.Service;
import org.apache.axis.client.Call;
import javax.xml.rpc.ServiceException;
import java.rmi.RemoteException ;

public class FedoraSoapFactory {
    // preferences for Fedora
    
    /** Creates a new instance of FedoraSoapFactory */
   
    public static  Vector getDissemintionInfoRecords(String pid,osid.dr.InfoStructure infoStructure,DR dr)   throws osid.dr.DigitalRepositoryException  {
        Call call;
        Vector disseminationList = new Vector();
        try {
            call = getCallMethods(dr);
            ObjectMethodsDef[] objMethods= (ObjectMethodsDef[]) call.invoke(new Object[] {pid} );
            if(objMethods == null)
                throw new osid.dr.DigitalRepositoryException("tufts.dr.FedoraObject():No Disseminations  returned");
            else {
                for(int i=0;i<objMethods.length;i++){
                    InfoRecord infoRecord = new InfoRecord(new PID(objMethods[i].getMethodName()),infoStructure);
                    infoRecord.createInfoField(((DisseminationInfoStructure)infoStructure).getBDEFInfoPart().getId(),objMethods[i].getBDefPID());
                    String disseminationURL = dr.getFedoraProperties().getProperty("url.fedora.get")+pid+"/"+objMethods[i].getBDefPID()+"/"+objMethods[i].getMethodName();
                    infoRecord.createInfoField(((DisseminationInfoStructure)infoStructure).getDisseminationURLInfoPart().getId(), disseminationURL);
                    disseminationList.add(infoRecord);
                }
            }
        } catch(Exception ex) {
            throw new osid.dr.DigitalRepositoryException("FedoraSoapFactory.getDisseminators "+ex.getMessage());
        }
        return disseminationList;
    }
    
 
    public static  FedoraObjectIterator search(DR dr,SearchCriteria lSearchCriteria)  throws osid.dr.DigitalRepositoryException {
        String term = lSearchCriteria.getKeywords();
        String maxResults = lSearchCriteria.getMaxReturns();
        String searchOperation = lSearchCriteria.getSearchOperation();
        String token = lSearchCriteria.getToken();
        
        Call call;
        String fedoraApiUrl = dr.getFedoraProperties().getProperty("url.fedora.api");
        
        FieldSearchResult searchResults=new FieldSearchResult();
        NonNegativeInteger maxRes=new NonNegativeInteger(maxResults);
        
        FieldSearchResult methodDefs = null;
        
        String[] resField=new String[4];
        resField[0]="pid";
        resField[1]="title";
        resField[2]="description";
        resField[3]="cModel";
        try {
            call = getCallSearch(dr);
            call.setOperationName(new QName(fedoraApiUrl,searchOperation));
            FieldSearchQuery query=new FieldSearchQuery();
            query.setTerms(term);
            java.util.Vector resultObjects = new java.util.Vector();
            if(searchOperation == SearchCriteria.FIND_OBJECTS) {
                methodDefs =    (FieldSearchResult) call.invoke(new Object[] {resField,maxRes,query} );
                ListSession listSession = methodDefs.getListSession();
                if(listSession != null)
                    lSearchCriteria.setToken(listSession.getToken());
                else
                    lSearchCriteria.setToken(null);
                
            }else {
                if(lSearchCriteria.getToken() != null) {
                    methodDefs =    (FieldSearchResult) call.invoke(new Object[] {lSearchCriteria.getToken()} );
                    ListSession listSession = methodDefs.getListSession();
                    if(listSession != null)
                        lSearchCriteria.setToken(listSession.getToken());
                    else
                        lSearchCriteria.setToken(null);
                }
            }
            
            if (methodDefs != null &&  methodDefs.getResultList().length > 0){
                ObjectFields[] fields= methodDefs.getResultList();
                lSearchCriteria.setResults(fields.length);
                for(int i=0;i<fields.length;i++) {
                    String title = "No Title";
                    if(fields[i].getTitle() != null)
                        title = fields[i].getTitle()[0];
                    resultObjects.add(new FedoraObject(dr,fields[i].getPid(),title,dr.getAssetType(fields[i].getCModel())));
                    
                    
                }
            } else {
                System.out.println("search returned no results");
            }
            
            
            return new FedoraObjectIterator(resultObjects) ;
        }catch(Exception ex) {
            ex.printStackTrace();
            throw new osid.dr.DigitalRepositoryException("FedoraSoapFactory.search"+ex.getMessage());
            
        }
    }
    
    public static  FedoraObjectIterator advancedSearch(DR dr,SearchCriteria lSearchCriteria)  throws osid.dr.DigitalRepositoryException {
        Condition cond[] = lSearchCriteria.getConditions();
        String maxResults = lSearchCriteria.getMaxReturns();
        
        Call call;
        FieldSearchResult searchResults=new FieldSearchResult();
        NonNegativeInteger maxRes=new NonNegativeInteger(maxResults);
        String[] resField=new String[4];
        resField[0]="pid";
        resField[1]="title";
        resField[2]="description";
        resField[3]="cModel";
        try {
            call = getCallAdvancedSearch(dr);
            FieldSearchQuery query=new FieldSearchQuery();
            //query.setTerms(term);
            query.setConditions(cond);
            java.util.Vector resultObjects = new java.util.Vector();
            FieldSearchResult methodDefs =    (FieldSearchResult) call.invoke(new Object[] {resField,maxRes,query} );
            if (methodDefs != null){
                ObjectFields[] fields= methodDefs.getResultList();
                lSearchCriteria.setResults(fields.length);
                for(int i=0;i<fields.length;i++) {
                    String title = "No Title";
                    if(fields[i].getTitle() != null)
                        title = fields[i].getTitle()[0];
                    resultObjects.add(new FedoraObject(dr,fields[i].getPid(),title,dr.getAssetType(fields[i].getCModel())));
                }
            } else {
                System.out.println("search return no results");
            }
            return new FedoraObjectIterator(resultObjects) ;
        }catch(Exception ex) {
            throw new osid.dr.DigitalRepositoryException("FedoraSoapFactory.advancedSearch"+ex.getMessage());
        }
    }
    
   
    private static  Call getCallMethods(DR dr)  throws osid.dr.DigitalRepositoryException  {
        //creates the new service and call instance
        Call call;
        try {
            String fedoraTypeUrl = dr.getFedoraProperties().getProperty("url.fedora.type");
            String fedoraApiUrl = dr.getFedoraProperties().getProperty("url.fedora.api");
            Service service = new Service();
            call=(Call)service.createCall();
            call.setTargetEndpointAddress(new URL(dr.getFedoraProperties().getProperty("url.fedora.soap.access")));
            //specify what method to call on the server
            call.setOperationName(new QName(fedoraApiUrl,"GetObjectMethods"));
            //create namingspaces for user defined types
            QName qn1=new QName(fedoraTypeUrl, "ObjectMethodsDef");
            QName qn2=new QName(fedoraTypeUrl, "ObjectProfile");
            QName qn3=new QName(fedoraTypeUrl, "MethodParmDef");
            // Any Fedora-defined types required by the SOAP service must be registered
            // prior to invocation so the SOAP service knows the appropriate
            // serializer/deserializer to use for these types.
            call.registerTypeMapping(ObjectMethodsDef.class, qn1,new BeanSerializerFactory(ObjectMethodsDef.class, qn1),
            new BeanDeserializerFactory(ObjectMethodsDef.class, qn1));
            call.registerTypeMapping(ObjectProfile.class, qn2,new BeanSerializerFactory(ObjectProfile.class, qn2),
            new BeanDeserializerFactory(ObjectProfile.class, qn2));
            call.registerTypeMapping(MethodParmDef.class, qn3,new BeanSerializerFactory(MethodParmDef.class, qn3),
            new BeanDeserializerFactory(MethodParmDef.class, qn3));
        }catch (Exception ex) {
            throw new DigitalRepositoryException("FedoraSoapFactory.getCallMethods "+ex.getMessage());
        }
        return call;
    }
   
    private static Call getCallSearch(DR dr)  throws osid.dr.DigitalRepositoryException {
        Call call;
        try {
            String fedoraTypeUrl = dr.getFedoraProperties().getProperty("url.fedora.type");
            String fedoraApiUrl = dr.getFedoraProperties().getProperty("url.fedora.api");
            Service service = new Service();
            call=(Call) service.createCall();
            System.out.println("FEDORA ACCESS URL = "+dr.getFedoraProperties().getProperty("url.fedora.soap.access"));
            call.setTargetEndpointAddress(new URL(dr.getFedoraProperties().getProperty("url.fedora.soap.access")));
            
            QName qn1 = new QName(fedoraTypeUrl, "ObjectFields");
            QName qn2 = new QName(fedoraTypeUrl, "FieldSearchQuery");
            QName qn3 = new QName(fedoraTypeUrl, "FieldSearchResult");
            QName qn4 = new QName(fedoraTypeUrl, "Condition");
            QName qn5=new QName(fedoraTypeUrl, "ComparisonOperator");
            QName qn6=new QName(fedoraTypeUrl, "ListSession");
            call.registerTypeMapping(ObjectFields.class, qn1,
            new BeanSerializerFactory(ObjectFields.class, qn1),
            new BeanDeserializerFactory(ObjectFields.class, qn1));
            call.registerTypeMapping(FieldSearchQuery.class, qn2,
            new BeanSerializerFactory(FieldSearchQuery.class, qn2),
            new BeanDeserializerFactory(FieldSearchQuery.class, qn2));
            call.registerTypeMapping(FieldSearchResult.class, qn3,
            new BeanSerializerFactory(FieldSearchResult.class, qn3),
            new BeanDeserializerFactory(FieldSearchResult.class, qn3));
            call.registerTypeMapping(Condition.class, qn4,
            new BeanSerializerFactory(Condition.class, qn4),
            new BeanDeserializerFactory(Condition.class, qn4));
            call.registerTypeMapping(ComparisonOperator.class, qn5,
            new BeanSerializerFactory(ComparisonOperator.class, qn5),
            new BeanDeserializerFactory(ComparisonOperator.class, qn5));
            call.registerTypeMapping(ListSession.class, qn6,
            new BeanSerializerFactory(ListSession.class, qn6),
            new BeanDeserializerFactory(ListSession.class, qn6));
            return call;
        }catch (Exception ex) {
            throw new DigitalRepositoryException("FedoraSoapFactory.getCallSearch "+ex);
        }
    }
    
    private static Call getCallAdvancedSearch(DR dr)  throws osid.dr.DigitalRepositoryException {
        Call call;
        try {
            String fedoraTypeUrl = dr.getFedoraProperties().getProperty("url.fedora.type");
            String fedoraApiUrl = dr.getFedoraProperties().getProperty("url.fedora.api");
            Service service = new Service();
            call=(Call) service.createCall();
            call.setTargetEndpointAddress(new URL(dr.getFedoraProperties().getProperty("url.fedora.soap.access")));
            call.setOperationName(new QName(fedoraApiUrl,"findObjects"));
            QName qn1 = new QName(fedoraTypeUrl, "ObjectFields");
            QName qn2 = new QName(fedoraTypeUrl, "FieldSearchQuery");
            QName qn3 = new QName(fedoraTypeUrl, "FieldSearchResult");
            QName qn4 = new QName(fedoraTypeUrl, "Condition");
            QName qn5=new QName(fedoraTypeUrl, "ComparisonOperator");
            QName qn6=new QName(fedoraTypeUrl, "ListSession");
            call.registerTypeMapping(ObjectFields.class, qn1,
            new BeanSerializerFactory(ObjectFields.class, qn1),
            new BeanDeserializerFactory(ObjectFields.class, qn1));
            call.registerTypeMapping(FieldSearchQuery.class, qn2,
            new BeanSerializerFactory(FieldSearchQuery.class, qn2),
            new BeanDeserializerFactory(FieldSearchQuery.class, qn2));
            call.registerTypeMapping(FieldSearchResult.class, qn3,
            new BeanSerializerFactory(FieldSearchResult.class, qn3),
            new BeanDeserializerFactory(FieldSearchResult.class, qn3));
            call.registerTypeMapping(Condition.class, qn4,
            new EnumSerializerFactory(Condition.class, qn4),
            new EnumDeserializerFactory(Condition.class, qn4));
            call.registerTypeMapping(ComparisonOperator.class, qn5,
            new EnumSerializerFactory(ComparisonOperator.class, qn5),
            new EnumDeserializerFactory(ComparisonOperator.class, qn5));
            call.registerTypeMapping(ListSession.class, qn6,
            new BeanSerializerFactory(ListSession.class, qn6),
            new BeanDeserializerFactory(ListSession.class, qn6));
            return call;
        }catch (Exception ex) {
            throw new DigitalRepositoryException("FedoraSoapFactory.getCallSearch "+ex.getMessage());
        }
    }
    
}
