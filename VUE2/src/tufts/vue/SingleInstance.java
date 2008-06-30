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

package tufts.vue;

import java.net.*;
import java.io.*;

import tufts.vue.gui.VueFrame;

/**
 *
 * @author  akumar03
 * @version $Revision: 1.7 $ / $Date: 2008-06-30 20:52:56 $ / $Author: mike $ 
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
                    if (VueUtil.isWindowsPlatform())
                    {
                    	VUE.getApplicationFrame().setVisible(true);
                    	((VueFrame)VUE.getApplicationFrame()).setExtendedState(java.awt.Frame.ICONIFIED);
                        ((VueFrame)VUE.getApplicationFrame()).setExtendedState(java.awt.Frame.NORMAL);
                    	VUE.getApplicationFrame().toFront();
                    }
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
