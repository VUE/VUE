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
    public static Vector getBehaviors(FedoraObject obj) throws osid.dr.DigitalRepositoryException {  
        Call call; 
        Vector behaviorList = new Vector();
        try {
            call = getCallBehavior(obj);
            String[] objMethods= (String[]) call.invoke(new Object[] {obj.getId().getIdString()} );
            if(objMethods == null)
                throw new osid.dr.DigitalRepositoryException("tufts.dr.FedoraObject():No Behaviours returned");
            else {
                for(int i=0;i<objMethods.length;i++) {
                    behaviorList.add(new Behavior(new PID(objMethods[i]),obj));
                }
            }
        }catch(Exception ex) {
            throw new osid.dr.DigitalRepositoryException("FedoraSoapFactory.getBehaviors"+ex.getMessage());
        }
        System.out.println("FedoraSoapFactory Behavior count ="+behaviorList.size());
        return behaviorList;
    }
    
    public static Vector getDisseminators(Behavior behavior) throws osid.dr.DigitalRepositoryException {
        Call call;
        Vector disseminationList = new Vector();
        try {
            String pid = behavior.getFedoraObject().getId().getIdString();
            call = getCallMethods(behavior);
            ;
            ObjectMethodsDef[] objMethods= (ObjectMethodsDef[]) call.invoke(new Object[] {pid} );
            if(objMethods == null)
                throw new osid.dr.DigitalRepositoryException("tufts.dr.FedoraObject():No Disseminations  returned");
            else {
                for(int i=0;i<objMethods.length;i++){
                   // System.out.println("PID ="+pid + " bdefid " + behavior.getId().getIdString()+"method "+objMethods[i].getMethodName());
                    if(objMethods[i].getBDefPID().equals(behavior.getId().getIdString())) {
                       if(!objMethods[i].getMethodName().equals("getImage") && !objMethods[i].getMethodName().equals("getItem") ) {
                       Dissemination dissemination =  new Dissemination(new PID(objMethods[i].getMethodName()),behavior);
                       dissemination.setValue(getDisseminationStream(dissemination));
                       disseminationList.add(new Dissemination(new PID(objMethods[i].getMethodName()),behavior));
                       }
                    }
                }
            }
        }
          catch(Exception ex) {
            throw new osid.dr.DigitalRepositoryException("FedoraSoapFactory.getDisseminators "+ex.getMessage());
        }
        return disseminationList;
    }
     
    public static  MIMETypedStream getDisseminationStream(Dissemination dissemination) throws osid.dr.DigitalRepositoryException {
        Call call;
        MIMETypedStream stream;
        try {
            call = getCallDissemination(dissemination);
            String pid = dissemination.getBehavior().getFedoraObject().getId().getIdString();
            String bPid = dissemination.getBehavior().getId().getIdString();
            String methodName = dissemination.getId().getIdString();
            Property userParams= null;
            stream = (MIMETypedStream) call.invoke(new Object[] {pid,bPid,methodName,userParams});
        }catch(Exception ex) {
            throw new osid.dr.DigitalRepositoryException("FedoraSoapFactory.getDisseminationStream"+ex.getMessage());
        }
        return stream;
        
    }
    // not used anymore
    /**
    public static  FieldSearchResult search(DR dr,String term,String maxResults,String[] resField)  throws osid.dr.DigitalRepositoryException {
        Call call;
        FieldSearchResult searchResults=new FieldSearchResult();   
        NonNegativeInteger maxRes=new NonNegativeInteger(maxResults);
        try { 
            call = getCallSearch(dr);
            FieldSearchQuery query=new FieldSearchQuery();
            query.setTerms(term);
            FieldSearchResult searchResult  =    (FieldSearchResult) call.invoke(new Object[] {resField,maxRes,query} );
            return searchResult ;
        }catch(Exception ex) {
            throw new osid.dr.DigitalRepositoryException("FedoraSoapFactory.search"+ex.getMessage());
        }  
    }
   **/
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
                lSearchCriteria.setToken(listSession.getToken());
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
                   
            if (methodDefs != null){
                    ObjectFields[] fields= methodDefs.getResultList(); 
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
                    for(int i=0;i<fields.length;i++) {
                        resultObjects.add(new FedoraObject(dr,fields[i].getPid(),fields[i].getTitle()[0],dr.getAssetType(fields[i].getCModel())));
                    }
            } else {
                System.out.println("search return no results");
            }
            return new FedoraObjectIterator(resultObjects) ;
        }catch(Exception ex) {
            throw new osid.dr.DigitalRepositoryException("FedoraSoapFactory.advancedSearch"+ex.getMessage());
        }  
    }
    
    private static Call  getCallBehavior(FedoraObject obj) throws osid.dr.DigitalRepositoryException{
        Call call;
        DR dr = obj.getDR();
        try {
            String fedoraTypeUrl = dr.getFedoraProperties().getProperty("url.fedora.type");
            
            Service service = new Service();
            call=(Call)service.createCall();
            call.setTargetEndpointAddress(new URL(dr.getFedoraProperties().getProperty("url.fedora.soap.access")));
            // creating namespacescall.setOperationName(new QName(FEDORA_API_URI,"findObjects"));
            
            QName qn1=new QName(fedoraTypeUrl, "ObjectMethodsDef");
            QName qn2=new QName(fedoraTypeUrl, "ObjectProfile");
            //registering namespaces
            call.registerTypeMapping(ObjectMethodsDef.class, qn1,new BeanSerializerFactory(ObjectMethodsDef.class, qn1),
        	new BeanDeserializerFactory(ObjectMethodsDef.class, qn1));
            call.registerTypeMapping(ObjectProfile.class, qn2,new BeanSerializerFactory(ObjectProfile.class, qn2),
        	new BeanDeserializerFactory(ObjectProfile.class, qn2));
            
            call.setOperationName(new QName(dr.getFedoraProperties().getProperty("url.fedora.api"),"getBehaviorDefinitions"));
            
        } catch (Exception ex) {
            throw new DigitalRepositoryException("FedoraSoapFactory.getCallBehavior"+ex.getMessage());
        }
        return call;
    }
    
    private static  Call getCallMethods(Behavior behavior)  throws osid.dr.DigitalRepositoryException  {
    	//creates the new service and call instance
        Call call;
        DR dr = behavior.getFedoraObject().getDR();
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
    
    private static Call getCallDissemination(Dissemination dissemination) throws osid.dr.DigitalRepositoryException {
        Call call;
        DR dr = dissemination.getBehavior().getFedoraObject().getDR();
        try {
            String fedoraTypeUrl = dr.getFedoraProperties().getProperty("url.fedora.type");
            String fedoraApiUrl = dr.getFedoraProperties().getProperty("url.fedora.api");
            Service service = new Service();
            call=(Call)service.createCall();
            call.setTargetEndpointAddress(new URL(dr.getFedoraProperties().getProperty("url.fedora.soap.access")));
            //specify what method to call on the server
            call.setOperationName(new QName(fedoraApiUrl,"GetDissemination"));	
            //create namingspaces for user defined types
            QName qn1 = new QName(fedoraTypeUrl, "MIMETypedStream");
            QName qn2 = new QName(fedoraTypeUrl, "Property");
            // Any Fedora-defined types required by the SOAP service must be registered
            // prior to invocation so the SOAP service knows the appropriate
            // serializer/deserializer to use for these types.
            call.registerTypeMapping(MIMETypedStream.class, qn1,new BeanSerializerFactory(MIMETypedStream.class, qn1),
            new BeanDeserializerFactory(MIMETypedStream.class, qn1));
            call.registerTypeMapping(Property.class, qn2,new BeanSerializerFactory(Property.class, qn2),
            new BeanDeserializerFactory(Property.class, qn2));
        }catch (Exception ex) {
            throw new DigitalRepositoryException("FedoraSoapFactory.getCallDissemination "+ex.getMessage());
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
            throw new DigitalRepositoryException("FedoraSoapFactory.getCallSearch "+ex.getMessage());
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
