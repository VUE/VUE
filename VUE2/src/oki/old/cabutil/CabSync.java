/*
 *  A command-line cabinet sync utility.
 *  Just an interface to the SyncList class, which does
 *  all the real work of synchronizing cabinets.
 *
 *  Usage: CabSync <cabinet1> <cabinet2> [-debug] [-n]
 *      -n for take no action -- it will just tell you what it would do.
 */

import java.util.*;

import org.okip.service.filing.api.*;

public class CabSync
{
    private static boolean A_TAKE_ACTION = true;
    private static boolean A_VERBOSE = true;
    private static boolean A_DEBUG = false;

    public static void main(String args[])
    {
        ArrayList dirs = new ArrayList();
        CabUtil.parseCommandLine(args);
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.equals("-n"))
                A_TAKE_ACTION = false;
            else if (a.equals("-verbose"))
                A_VERBOSE = true;
            else if (a.equals("-debug")) {
                A_DEBUG = true;
                A_VERBOSE = true;
            }
            else if (!a.startsWith("-"))
                dirs.add(a);
        }
        
        String dirLocal = "./c/l";
        String dirRemote = "./c/r";
        if (dirs.size() > 1) {
            dirLocal = (String) dirs.get(0);
            dirRemote = (String) dirs.get(1);
        }
        Cabinet cabinet0 = CabUtil.getCabinetFromDirectory(dirLocal);
        Cabinet cabinet1 = CabUtil.getCabinetFromDirectory(dirRemote);
        if (A_DEBUG) {
            CabUtil.printCabinet(cabinet0);
            CabUtil.printCabinet(cabinet1);
        }
        outln("Syncing local " + cabinet0 + " with remote " + cabinet1);
        SyncList syncList = new SyncList(cabinet0, cabinet1, A_TAKE_ACTION, A_VERBOSE, A_DEBUG);
        if (A_DEBUG) outln("Got syncList " + syncList);
        syncList.refresh();
        try {
            syncList.doSynchronize();
        } catch (Exception e) {
            System.err.println(e);
        }
        //todo: doSync will do an auto-refresh at end --
        //that's a GUI-centric implementation -- we don't
        // need it here...
        
        System.exit(0);
    }

    private static void outln(String s) { System.out.println("CabSync: " + s); }
}
