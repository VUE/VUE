package edu.tufts.vue.xml;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
/**
 * A Utility class for creating instances of classes via reflection.
 * @author Aneliya Ticheva
 *
 */



public class Factory
{
	 @SuppressWarnings("unchecked")
	    public static Object createInstance(String clazzName)
	    {
	        Class providerClass = null;
	        
	        try {
	            providerClass = Class.forName(clazzName);
	        }
	        catch (ClassNotFoundException e) {
	            try {
	                providerClass = Class.forName(clazzName,
	                        true,
	                        Thread.currentThread().getContextClassLoader());
	            }
	            catch (ClassNotFoundException ne) {
	                throw new RuntimeException("Provider's class not found in classpath..."+clazzName, ne);
	            }
	        }
	        
	        Constructor providerConstructor = null;
	        
	        try {
	            Class[] param = new Class[] {};
	            providerConstructor = providerClass.getConstructor(param);
	        }
	        catch (NoSuchMethodException nsme) {
	            throw new RuntimeException(
	                    "The provider class should have a constuctor which takes a single java.util.Map argument...",
	                    nsme);
	        }
	        
	        Object result;
	        try {
	            result = providerConstructor.newInstance(new Object[] {});
	        }
	        catch (InvocationTargetException ite) {
	            throw new RuntimeException("cannot invoke the constructor a InferenceService!", ite);
	        }
	        catch (IllegalAccessException ile) {
	            throw new RuntimeException("cannot access the constructor!", ile);
	        }
	        catch (InstantiationException inse) {
	            throw new RuntimeException("cannot instantiate a InferenceService!", inse);
	        }
	        
	        if (!providerClass.isInstance(result)) {
	            throw new RuntimeException("the supplied class '" + clazzName + "' does not support the InferenceService API!");
	        }
	        return result;
	    }
	 private static Object createHandlerService(String clazzName, Class [] paramTypes, Object [] paramValues)
	    {
	        Class providerClass = null;
	        
	        try {
	            providerClass = Class.forName(clazzName);
	        }
	        catch (ClassNotFoundException e) {
	            try {
	                providerClass = Class.forName(clazzName,
	                        true,
	                        Thread.currentThread().getContextClassLoader());
	            }
	            catch (ClassNotFoundException ne) {
	                throw new RuntimeException("Provider's class not found in classpath..."+clazzName, ne);
	            }
	        }
	        
	        Constructor providerConstructor = null;
	        
	        try {
	            
	            providerConstructor = providerClass.getConstructor(paramTypes);
	        }
	        catch (NoSuchMethodException nsme) {
	            throw new RuntimeException(
	                    "The provider class should have a constuctor which takes a single java.util.Map argument...",
	                    nsme);
	        }
	        
	        Object result;
	        try {
	            result = providerConstructor.newInstance(paramValues);//new Object[] {paramValues});
	        }
	        catch (InvocationTargetException ite) {
	            throw new RuntimeException("cannot invoke the constructor a InferenceService!", ite);
	        }
	        catch (IllegalAccessException ile) {
	            throw new RuntimeException("cannot access the constructor!", ile);
	        }
	        catch (InstantiationException inse) {
	            throw new RuntimeException("cannot instantiate a InferenceService!", inse);
	        }
	        
	        if (!providerClass.isInstance(result)) {
	            throw new RuntimeException("the supplied class '" + clazzName + "' does not support the InferenceService API!");
	        }
	        return result;
	    }
}
