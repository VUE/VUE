import org.okip.service.filing.api.*;
import java.util.ArrayList;
import java.util.Iterator;

public class CabList
{

    public static void main(String args[])
    {
        ArrayList names = CabUtil.parseCommandLine(args);
        if (names.size() < 1)
            names.add(".");
        Iterator i = names.iterator();
        while (i.hasNext()) {
            String cabName = (String) i.next();
            Cabinet cabinet = CabUtil.getCabinet(cabName);
            CabUtil.printCabinet(cabinet);
        }
    }
}
