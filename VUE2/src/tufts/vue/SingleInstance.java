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

package tufts.vue;

import java.net.*;
import java.io.*;

/**
 *
 * @author  akumar03
 * @version $Revision: 1.3 $ / $Date: 2006-01-20 20:07:34 $ / $Author: sfraize $ 
 */
public class SingleInstance {
    
    /** Creates a new instance of SingleInstance */
    
    public static boolean running = false;
    private static final int port = 12000;
    private static int count = 0;
    Socket socket = null;
    Socket client = null;
    ServerSocket server;
    String[] args;
    public SingleInstance(String[] args) {
        try {
            if(args.length > 0) {
                System.out.println("Creating client");
                client =  new Socket("localhost", port);
                PrintWriter writer = new PrintWriter(client.getOutputStream(),true);
                writer.write(args[0]);
                writer.close();
                client.close();
            }
        } catch(Exception ex) {
            //ignore
        }
        try {
            if(client == null) {
                createServerSocket(args);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        running = true;
        Thread t = new Thread() {
            public void run() {
                while(running) {
                    try {
                        running = tufts.vue.VUE.getApplicationFrame() != null;
                        Thread.sleep(2000);
                    } catch(Exception e) {
                        System.out.println("Error checking instance of vue: running="+running);
                        running = false;
                        
                    }
                    System.out.println(running);
                }
            }
        };
        //t.start();
        System.out.println("Starting Single Instance");
        SingleInstance singleInstance = new SingleInstance(args);
    }
    
    private void createServerSocket(String[] args) {
        this.args = args;
        System.out.println("Create Server Socket");
        try {
            server = new ServerSocket(port);
            
            Thread vueThread = new Thread() {
                public void run() {
                    tufts.vue.VUE.main(SingleInstance.this.args);
                }
            };
            vueThread.start();
            while(running) {
                try {
                    Socket socket = server.accept();
                    System.out.println("New connection accepted " +
                    socket.getInetAddress() +
                    ":" + socket.getPort());
                    //if(socket.getInetAddress().equals("localhost")) {
                        BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        
                        while(running) {
                            String message = input.readLine();
                            if (message==null) break;
                            tufts.vue.action.OpenAction.displayMap(new File(message));
                        }
                    //}
                    socket.close();
                    System.out.println("Connection closed by client");
                    
                }
                catch (Exception e) {
                    System.out.println(e);
                }
                
            }
            
            System.out.println("SOCKET CLOSED");
            
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    
    
}
