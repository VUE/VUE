/*
 * Logger.java
 * Scott Fraize, July 2002
 */

import java.io.*;

class Logger {
    Class c;
    String className;
    String label;
    String errLabel;
    String prefix;
    public boolean v = false;
    public boolean d = false;
    public boolean action = true;

    Logger(Class c)
    {
        this(c, false, false);
    }
    Logger(Class c, boolean v, boolean d)
    {
        this.c = c;
        this.v = v;
        this.d = d;
        setLabels();
    }
    private void setLabels()
    {
        className = c.getName();
        String s;
        if (prefix != null)
            s = className + "[" + prefix + "]";
        else
            s = className;
        label = s + ": ";
        errLabel = s + "(error): ";
    }

    public void copyStates(Logger l)
    {
        this.v = l.v;
        this.d = l.d;
        action = l.action;
    }
    
    public void setPrefix(String s)
    {
        prefix = s;
        setLabels();
    }

    public void parseArgs(String args[])
    {
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.equals("-debug")) {
                this.d = true;
                this.v = true;
            } else if (a.equals("-verbose"))
                this.v = true;
            else if (a.equals("-n"))
                action = false;
        }
    }
    
    public void setDebug(boolean d) { this.d = d; }
    public void setVerbose(boolean v) { this.v = v; }
    void outln(String s) { System.out.println(label + s); }
    void verbose(String s) { if (this.v) outln(s); }
    void verbose(Object o) { if (this.v) outln(o == null ? "<null>" : o.toString()); }
    void debug(String s) { if (this.d) outln(s); }
    void debug(Object o) { if (this.d) outln(o == null ? "<null>" : o.toString()); }
    void debug(Object o, String s) { if (this.d) System.out.println(o.getClass().getName() + ": " + s); }

    void errout(String s) { System.err.println(errLabel+ s); }
    void errout(Exception e, String s) { errout(s + ": " + e); errout(e); }
    void errout(Exception e)
    {
        if (e instanceof org.okip.service.shared.api.Exception) {
            ((org.okip.service.shared.api.Exception)e).printChainedTrace();
        } else {
            errout(e.toString());
            e.printStackTrace();
        }
    }
}

