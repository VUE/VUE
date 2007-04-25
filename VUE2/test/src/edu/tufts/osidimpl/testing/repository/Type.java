package edu.tufts.osidimpl.test.repository;

public class Type
extends org.osid.shared.Type
{

    public Type(String authority
                 , String domain
                 , String keyword
                 , String description)
    {
        super(authority, domain, keyword, description);        
    }

    public Type(String authority
                 , String domain
                 , String keyword)
    {
        super(authority, domain, keyword);        
    }
}
