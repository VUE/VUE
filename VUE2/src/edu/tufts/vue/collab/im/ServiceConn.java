/*
 *  Copyright (c) 2002-2003, The Joust Project
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without 
 *  modification, are permitted provided that the following conditions 
 *  are met:
 *
 *  - Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer. 
 *  - Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution. 
 *  - Neither the name of the Joust Project nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 *  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 *  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
 *  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT 
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 *  ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 *  POSSIBILITY OF SUCH DAMAGE.
 *
 *  File created by keith @ Mar 26, 2003
 *
 */

package edu.tufts.vue.collab.im;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.flap.ClientFlapConn;
import net.kano.joscar.flap.FlapPacketEvent;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.net.ClientConnEvent;
import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.snac.SnacRequestAdapter;
import net.kano.joscar.snac.SnacRequestListener;
import net.kano.joscar.snac.SnacResponseEvent;
import net.kano.joscar.snaccmd.DirInfo;
import net.kano.joscar.snaccmd.FullRoomInfo;
import net.kano.joscar.snaccmd.MiniRoomInfo;
import net.kano.joscar.snaccmd.ExchangeInfo;
import net.kano.joscar.snaccmd.conn.RateInfoCmd;
import net.kano.joscar.snaccmd.conn.ServiceRedirect;
import net.kano.joscar.snaccmd.conn.ServiceRequest;
import net.kano.joscar.snaccmd.icon.IconDataCmd;
import net.kano.joscar.snaccmd.rooms.RoomResponse;
import net.kano.joscar.snaccmd.search.InterestInfo;
import net.kano.joscar.snaccmd.search.InterestListCmd;
import net.kano.joscar.snaccmd.search.SearchResultsCmd;

import java.awt.Image;
import java.awt.Toolkit;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class ServiceConn extends BasicConn {
    protected int serviceFamily;

    public ServiceConn(VUEAim tester, ByteBlock cookie,
            int serviceFamily) {
        super(tester, cookie);
        this.serviceFamily = serviceFamily;
    }

    public ServiceConn(String host, int port, VUEAim tester,
            ByteBlock cookie, int serviceFamily) {
        super(host, port, tester, cookie);
        this.serviceFamily = serviceFamily;
    }

    public ServiceConn(InetAddress ip, int port, VUEAim tester,
            ByteBlock cookie, int serviceFamily) {
        super(ip, port, tester, cookie);
        this.serviceFamily = serviceFamily;
    }

    protected void clientReady() {
        tester.serviceReady(this);
        super.clientReady();
    }

    protected void handleStateChange(ClientConnEvent e) {
        System.out.println("0x" + Integer.toHexString(serviceFamily)
                + " service connection state changed to " + e.getNewState()
                + ": " + e.getReason());

        if (e.getNewState() == ClientFlapConn.STATE_FAILED) {
            tester.serviceFailed(this);
        } else if (e.getNewState() == ClientFlapConn.STATE_CONNECTED) {
            tester.serviceConnected(this);
        } else if (e.getNewState() == ClientFlapConn.STATE_NOT_CONNECTED) {
            tester.serviceDied(this);
        }
    }

    protected void handleFlapPacket(FlapPacketEvent e) {
        super.handleFlapPacket(e);
    }

    protected void handleSnacPacket(SnacPacketEvent e) {
        super.handleSnacPacket(e);
    }

    protected void handleSnacResponse(SnacResponseEvent e) {
        super.handleSnacResponse(e);

        SnacCommand cmd = e.getSnacCommand();

        if (cmd instanceof RateInfoCmd) {
            // this is all we need.
            clientReady();

        } else if (cmd instanceof InterestListCmd) {
            InterestListCmd ilc = (InterestListCmd) cmd;

            InterestInfo[] infos = ilc.getInterests();

            if (infos != null) {
                Map children = new HashMap();

                for (int i = 0; i < infos.length; i++) {
                    if (infos[i].getType() == InterestInfo.TYPE_CHILD) {
                        int parentCode = infos[i].getParentId();
                        Integer parent = new Integer(parentCode);

                        List interests = (List) children.get(parent);

                        if (interests == null) {
                            interests = new LinkedList();
                            children.put(parent, interests);
                        }

                        interests.add(infos[i]);
                    }
                }
                for (int i = 0; i < infos.length; i++) {
                    if (infos[i].getType() == InterestInfo.TYPE_PARENT) {
                        Integer id = new Integer(infos[i].getParentId());
                        List interests = (List) children.get(id);

                        System.out.println("- " + infos[i].getName());
                        if (interests != null) {
                            for (Iterator it = interests.iterator();
                                 it.hasNext();) {
                                InterestInfo info = (InterestInfo) it.next();
                                System.out.println("  - " + info.getName());
                            }
                        }
                    }
                }
                List toplevels = (List) children.get(new Integer(0));
                if (toplevels != null) {
                    for (Iterator it = toplevels.iterator(); it.hasNext();) {
                        System.out.println("  "
                                + ((InterestInfo) it.next()).getName());
                    }
                }
            }

        } else if (cmd instanceof SearchResultsCmd) {
            SearchResultsCmd src = (SearchResultsCmd) cmd;

            DirInfo[] results = src.getResults();

            for (int i = 0; i < results.length; i++) {
                System.out.println("result " + (i + 1) + ": " + results[i]);
            }

        } else if (cmd instanceof IconDataCmd) {
            IconDataCmd idc = (IconDataCmd) cmd;

            String sn = idc.getScreenname();

            byte[] data = idc.getIconData().toByteArray();
            Image icon = Toolkit.getDefaultToolkit().createImage(data);

//            tester.getUserInfo(sn).setIcon(icon);

        } else if (cmd instanceof RoomResponse) {
            RoomResponse rr = (RoomResponse) cmd;

            final FullRoomInfo roomInfo = rr.getRoomInfo();

            if (roomInfo != null) {
                System.out.println("requesting chat service for room "
                        + roomInfo.getName());

                MiniRoomInfo miniInfo = new MiniRoomInfo(roomInfo);

                ServiceRequest request = new ServiceRequest(miniInfo);

                SnacRequestListener listener = new SnacRequestAdapter() {
                    public void handleResponse(SnacResponseEvent e) {
                        SnacCommand cmd = e.getSnacCommand();

                        System.out.println("got chat service request " +
                                "response: " + e.getSnacPacket() + " ( "
                                + e.getSnacCommand() + ")");
                        if (cmd instanceof ServiceRedirect) {
                            ServiceRedirect sr = (ServiceRedirect) cmd;

                            tester.connectToChat(roomInfo, sr.getRedirectHost(),
                                    sr.getCookie());
                        } else {
                            // pass it off to the default handler
                            handleSnacPacket(e);
                        }
                    }
                };

                dispatchRequest(request, listener);
            }
            ExchangeInfo[] exis = rr.getExchangeInfos();
            if (exis != null && exis.length > 0) {
                System.out.println("Exchange infos:");
                for (int i = 0; i < exis.length; i++) {
                    System.out.println("- " + exis[i]);
                }
            }
        }
    }
}
