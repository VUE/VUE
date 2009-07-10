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

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.OscarTools;
import net.kano.joscar.flap.FlapCommand;
import net.kano.joscar.flap.FlapPacketEvent;
import net.kano.joscar.flapcmd.LoginFlapCmd;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.ratelim.RateLimitingQueueMgr;
import net.kano.joscar.rv.NewRvSessionEvent;
import net.kano.joscar.rv.RecvRvEvent;
import net.kano.joscar.rv.RvProcessor;
import net.kano.joscar.rv.RvProcessorListener;
import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rv.RvSessionListener;
import net.kano.joscar.rv.RvSnacResponseEvent;
import net.kano.joscar.rvcmd.AbstractRequestRvCmd;
import net.kano.joscar.rvcmd.DefaultRvCommandFactory;
import net.kano.joscar.rvcmd.RvConnectionInfo;
import net.kano.joscar.rvcmd.addins.AddinsReqRvCmd;
import net.kano.joscar.rvcmd.chatinvite.ChatInvitationRvCmd;
import net.kano.joscar.rvcmd.directim.DirectIMReqRvCmd;
import net.kano.joscar.rvcmd.getfile.GetFileReqRvCmd;
import net.kano.joscar.rvcmd.icon.SendBuddyIconRvCmd;
import net.kano.joscar.rvcmd.sendbl.SendBuddyListRvCmd;
import net.kano.joscar.rvcmd.sendfile.FileSendReqRvCmd;
import net.kano.joscar.rvcmd.trillcrypt.AbstractTrillianCryptRvCmd;
import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.snac.SnacRequest;
import net.kano.joscar.snac.SnacRequestListener;
import net.kano.joscar.snac.SnacResponseEvent;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.snaccmd.ExtraInfoBlock;
import net.kano.joscar.snaccmd.ExtraInfoData;
import net.kano.joscar.snaccmd.FullUserInfo;
import net.kano.joscar.snaccmd.MiniUserInfo;
import net.kano.joscar.snaccmd.SnacFamilyInfoFactory;
import net.kano.joscar.snaccmd.buddy.BuddyOfflineCmd;
import net.kano.joscar.snaccmd.buddy.BuddyStatusCmd;
import net.kano.joscar.snaccmd.conn.ClientReadyCmd;
import net.kano.joscar.snaccmd.conn.ClientVersionsCmd;
import net.kano.joscar.snaccmd.conn.RateAck;
import net.kano.joscar.snaccmd.conn.RateChange;
import net.kano.joscar.snaccmd.conn.RateClassInfo;
import net.kano.joscar.snaccmd.conn.RateInfoCmd;
import net.kano.joscar.snaccmd.conn.RateInfoRequest;
import net.kano.joscar.snaccmd.conn.ServerReadyCmd;
import net.kano.joscar.snaccmd.conn.SnacFamilyInfo;
import net.kano.joscar.snaccmd.conn.WarningNotification;
import net.kano.joscar.snaccmd.icbm.InstantMessage;
import net.kano.joscar.snaccmd.icbm.RecvImIcbm;
import net.kano.joscar.snaccmd.icbm.RecvRvIcbm;
import net.kano.joscar.snaccmd.icbm.RvCommand;
import net.kano.joscar.snaccmd.icbm.RvResponse;
import net.kano.joscar.snaccmd.icbm.SendImIcbm;
import net.kano.joscar.snaccmd.rooms.RoomInfoReq;

import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;

import tufts.vue.LWComponent;
import tufts.vue.LWNode;
import tufts.vue.LayoutAction;
import tufts.vue.VUE;
import tufts.vue.NodeTool.NodeModeTool;
import edu.tufts.vue.collab.im.security.SecureSession;
import edu.tufts.vue.collab.im.security.SecureSessionException;
import edu.tufts.vue.metadata.MetadataList;

public abstract class BasicConn extends AbstractFlapConn {
    protected final ByteBlock cookie;
    protected boolean sentClientReady = false;

    protected int[] snacFamilies = null;
    protected SnacFamilyInfo[] snacFamilyInfos;
    protected RateLimitingQueueMgr rateMgr = new RateLimitingQueueMgr();
    public RvProcessor rvProcessor = new RvProcessor(snacProcessor);
    protected RvProcessorListener rvListener = new RvProcessorListener() {
        public void handleNewSession(NewRvSessionEvent event) {
            System.out.println("new RV session: " + event.getSession());

            event.getSession().addListener(rvSessionListener);
        }
    };
    public Map trillianEncSessions = new HashMap();

    public RvSessionListener rvSessionListener = new RvSessionListener() {
        public void handleRv(RecvRvEvent event) {
            RvCommand cmd = event.getRvCommand();

            RvSession session = event.getRvSession();
            SnacCommand snaccmd = event.getSnacCommand();
            if (!(snaccmd instanceof RecvRvIcbm)) return;
            RecvRvIcbm icbm = (RecvRvIcbm) snaccmd;
            System.out.println("got rendezvous on session <" + session + ">");
            System.out.println("- command: " + cmd);

            if (cmd instanceof FileSendReqRvCmd) {
                FileSendReqRvCmd rv = (FileSendReqRvCmd) cmd;

                RvConnectionInfo connInfo = rv.getConnInfo();
                InetAddress ip = connInfo.getExternalIP();
                int port = connInfo.getPort();

                if (ip != null && port != -1) {
                    System.out.println("starting ft thread..");
                    long cookie = icbm.getIcbmMessageId();
                    new RecvFileThread(tester, ip, port, session, cookie,
                            connInfo.isEncrypted()).start();
                }

            } else if (cmd instanceof AbstractTrillianCryptRvCmd) {
                String key = OscarTools.normalize(session.getScreenname());
                TrillianEncSession encSession = (TrillianEncSession)
                        trillianEncSessions.get(key);
                if (encSession == null) {
                    encSession = new TrillianEncSession(session);
                    trillianEncSessions.put(key, encSession);
                }
                encSession.handleRv(event);
            } else if (cmd instanceof DirectIMReqRvCmd) {
                if (((DirectIMReqRvCmd) cmd).getRequestType()
                        == AbstractRequestRvCmd.REQTYPE_INITIALREQUEST) {
                    new DirectIMSession(tester.getScreenname(), session, event);
                }
            } else if (cmd instanceof SendBuddyIconRvCmd) {
                /*
                SendBuddyIconRvCmd iconCmd = (SendBuddyIconRvCmd) cmd;
                ByteBlock iconData = iconCmd.getIconData();
                if (iconData != null) {
                    try {
                        File dir = new File("buddy-icons");
                        if (!dir.isDirectory()) dir.mkdir();

                        File file = new File(dir, session.getScreenname()
                                + ".icon");
                        System.out.println("writing icon to " + file + "!!");

                        FileOutputStream out = new FileOutputStream(file);
                        iconData.write(out);
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                */
            } else if (cmd instanceof SendBuddyListRvCmd) {
                session.sendResponse(RvResponse.CODE_NOT_ACCEPTING);
            } else if (cmd instanceof GetFileReqRvCmd) {
                if (((GetFileReqRvCmd) cmd).getCode() != -1) {
                    new HostGetFileThread(session, event).start();
                }
            } else if (cmd instanceof AddinsReqRvCmd) {
                session.sendRv(cmd);
            } else if (cmd instanceof ChatInvitationRvCmd) {
                ChatInvitationRvCmd circ = (ChatInvitationRvCmd) cmd;

                ByteBlock securityInfo = circ.getSecurityInfo();
                if (securityInfo != null) {
                    String sn = icbm.getSender().getScreenname();
                    String cookie = circ.getRoomInfo().getCookie();
                    String roomName = OscarTools.getRoomNameFromCookie(cookie);
                    try {
                        SecureSession secureSession = tester.getSecureSession();
                        secureSession.setChatKey(roomName,
                                secureSession.extractChatKey(sn, securityInfo));
                    } catch (SecureSessionException e) {
                        e.printStackTrace();
                    }
                }
                tester.request(new RoomInfoReq(circ.getRoomInfo()));
            }
        }
        public void handleSnacResponse(RvSnacResponseEvent event) {
            System.out.println("got SNAC response for <"
                    + event.getRvSession() + ">: "
                    + event.getSnacCommand());
        }
    };

    { // init
        snacProcessor.setSnacQueueManager(rateMgr);
        rvProcessor.registerRvCmdFactory(new DefaultRvCommandFactory());
        rvProcessor.addListener(rvListener);
    }

    public BasicConn(VUEAim tester, ByteBlock cookie) {
        super(tester);
        this.cookie = cookie;
    }

    public BasicConn(String host, int port, VUEAim tester,
            ByteBlock cookie) {
        super(host, port, tester);
        this.cookie = cookie;
    }

    public BasicConn(InetAddress ip, int port, VUEAim tester,
            ByteBlock cookie) {
        super(ip, port, tester);
        this.cookie = cookie;
    }

    protected DateFormat dateFormat
            = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                    DateFormat.SHORT);

    protected PrintWriter imLogger = null;
   
   
    private static Properties senders = new Properties();
    protected void handleFlapPacket(FlapPacketEvent e) {
        FlapCommand cmd = e.getFlapCommand();

        if (cmd instanceof LoginFlapCmd) {
            getFlapProcessor().sendFlap(new LoginFlapCmd(cookie));
        } else {
            System.out.println("got FLAP command on channel 0x"
                    + Integer.toHexString(e.getFlapPacket().getChannel())
                    + ": " + cmd);
        }
    }

    protected void handleSnacPacket(SnacPacketEvent e) {
        SnacPacket packet = e.getSnacPacket();
        System.out.println("got snac packet type "
                + Integer.toHexString(packet.getFamily()) + "/"
                + Integer.toHexString(packet.getCommand()) + ": "
                + e.getSnacCommand());

        SnacCommand cmd = e.getSnacCommand();
        if (cmd instanceof ServerReadyCmd) {
            ServerReadyCmd src = (ServerReadyCmd) cmd;

            setSnacFamilies(src.getSnacFamilies());

            SnacFamilyInfo[] familyInfos = SnacFamilyInfoFactory
                    .getDefaultFamilyInfos(src.getSnacFamilies());

            setSnacFamilyInfos(familyInfos);

            tester.registerSnacFamilies(this);

            request(new ClientVersionsCmd(familyInfos));
            request(new RateInfoRequest());

        } else if (cmd instanceof RecvImIcbm) {
            RecvImIcbm icbm = (RecvImIcbm) cmd;

            String sn = icbm.getSenderInfo().getScreenname();
            InstantMessage message = icbm.getMessage();
            String msg = null;
            if (message.isEncrypted()) {
                ByteBlock encData = message.getEncryptedData();
                System.out.println("got [" + encData.getLength() + "]");

                SecureSession secureSession = tester.getSecureSession();
                if (secureSession.hasCert(sn)) {
                    try {
                        msg = secureSession.decodeEncryptedIM(sn, encData);
                    } catch (SecureSessionException e1) {
                        e1.printStackTrace();
                    }
                } else {
                    System.out.println(sn + " tried sending an encrypted "
                            + "message, but I don't have his/her certificate "
                            + " - try typing 'getcertinfo " + sn + "'");
                }

            } else {
                msg = OscarTools.stripHtml(message.getMessage());

//                OldIconHashInfo iconInfo = icbm.getIconInfo();
//
//                if (iconInfo != null) {
//                    System.out.println("(" + sn
//                            + " has a buddy icon: " + iconInfo + ")");
//                }

                sendRequest(new SnacRequest(new SendImIcbm(sn, msg), null));
            }
            String str = dateFormat.format(new Date()) + " IM from "
                    + sn + ": " + msg;
            if (imLogger != null) {
                imLogger.println(str);
            }
           
            if (msg != null) {
                String encFlag = (message.isEncrypted() ? "**ENCRYPTED** " : "");
                System.out.println(encFlag + "*" + sn + "* " + msg);
            }
            LWNode newNode = NodeModeTool.createNewNode(OscarTools.stripHtml(msg));
            MetadataList mlist = new MetadataList();
            mlist.add("submitted by", sn);
            newNode.setMetadataList(mlist);
           
            String c = senders.getProperty(sn);
           Color color = null;
            if (c == null)
            {	 color = randomColor();
            	
            	senders.setProperty(sn, color.getRed() +","+color.getGreen()+","+color.getBlue());
            }
            else
            {
                StringTokenizer tokens = new StringTokenizer(c,",");
                
        		color = new Color(Integer.parseInt((String)tokens.nextElement()),Integer.parseInt((String)tokens.nextElement()),Integer.parseInt((String)tokens.nextElement()));
             
            }
            newNode.setFillColor(color);
            VUE.getActiveMap().add(newNode);
            List<LWComponent> compList = new ArrayList<LWComponent>();
            compList.add(newNode);
            LayoutAction.search.act(compList);

//            if (icbm.senderWantsIcon()) {
//                System.out.println(sn + " wants our icon.. sending.");
//                RvSession sess = rvProcessor.createRvSession(sn);
//                sess.sendRv(new SendBuddyIconRvCmd(tester.oldIconInfo,
//                        new FileWritable(tester.iconFile)));
//            }

        } else if (cmd instanceof WarningNotification) {
            WarningNotification wn = (WarningNotification) cmd;
            MiniUserInfo warner = wn.getWarner();
            if (warner == null) {
                System.out.println("*** You were warned anonymously to "
                        + wn.getNewLevel() + "%");
            } else {
                System.out.println("*** " + warner.getScreenname()
                        + " warned you up to " + wn.getNewLevel() + "%");
            }
        } else if (cmd instanceof BuddyStatusCmd) {
            BuddyStatusCmd bsc = (BuddyStatusCmd) cmd;

            FullUserInfo info = bsc.getUserInfo();

            String sn = info.getScreenname();

            ExtraInfoBlock[] extraInfos = info.getExtraInfoBlocks();

            if (extraInfos != null) {
                for (int i = 0; i < extraInfos.length; i++) {
                    ExtraInfoBlock extraInfo = extraInfos[i];
                    ExtraInfoData data = extraInfo.getExtraData();

//                    if ((hashInfo.getFlags() & ExtraInfoData.FLAG_ICON_PRESENT)
//                            != 0) {
//                        System.out.println(sn +
//                                " has an icon! requesting it.. (excode="
//                                + iconInfos[i].getExtraCode() + ")");

//                        request(new IconRequest(sn, iconInfos[i]));
//
//                        break;
//                    }

                    if (extraInfo.getType() == ExtraInfoBlock.TYPE_AVAILMSG) {
                        ByteBlock msgBlock = data.getData();
                        int len = BinaryTools.getUShort(msgBlock, 0);
                        byte[] msgBytes = msgBlock.subBlock(2, len).toByteArray();

                        String msg;
                        try {
                            msg = new String(msgBytes, "UTF-8");
                        } catch (UnsupportedEncodingException e1) {
                            e1.printStackTrace();
                            return;
                        }
                        if (msg.length() > 0) {
                            System.out.println(info.getScreenname()
                                    + " availability: " + msg);
                        }
                    }
                }
            }

            if (info.getCapabilityBlocks() != null) {
                List known = Arrays.asList(new CapabilityBlock[] {
                    CapabilityBlock.BLOCK_CHAT,
                    CapabilityBlock.BLOCK_DIRECTIM,
                    CapabilityBlock.BLOCK_FILE_GET,
                    CapabilityBlock.BLOCK_FILE_SEND,
                    CapabilityBlock.BLOCK_GAMES,
                    CapabilityBlock.BLOCK_GAMES2,
                    CapabilityBlock.BLOCK_ICON,
                    CapabilityBlock.BLOCK_SENDBUDDYLIST,
                    CapabilityBlock.BLOCK_TRILLIANCRYPT,
                    CapabilityBlock.BLOCK_VOICE,
                    CapabilityBlock.BLOCK_ADDINS,
                    CapabilityBlock.BLOCK_ICQCOMPATIBLE,
                    CapabilityBlock.BLOCK_SOMETHING,
                });

                List caps = new ArrayList(Arrays.asList(
                        info.getCapabilityBlocks()));
                caps.removeAll(known);
                if (!caps.isEmpty()) {
                    System.out.println(sn + " has " + caps.size()
                            + " unknown caps:");
                    for (Iterator it = caps.iterator(); it.hasNext();) {
                        System.out.println("- " + it.next());
                    }
                }
/*
                caps = new ArrayList(known);
                caps.removeAll(Arrays.asList(info.getCapabilityBlocks()));
                if (!caps.isEmpty()) {
                    System.out.println(sn + " is missing " + caps.size()
                            + " caps:");
                    for (Iterator it = caps.iterator(); it.hasNext();) {
                        System.out.println("- " + it.next());
                    }
                }
*/
            }
        } else if (cmd instanceof BuddyOfflineCmd) {
            BuddyOfflineCmd boc = (BuddyOfflineCmd) cmd;

        } else if (cmd instanceof RateChange) {
            RateChange rc = (RateChange) cmd;

            System.out.println("rate change: current avg is "
                    + rc.getRateInfo().getCurrentAvg());
        }
    }
    private static Random  rand = new Random();
    public Color randomColor()
    {
        return(new Color(rand.nextInt(256), 
                         rand.nextInt(256),
                         rand.nextInt(256)));
    }
    

    protected void handleSnacResponse(SnacResponseEvent e) {
        SnacPacket packet = e.getSnacPacket();
        System.out.println("got snac response type "
                + Integer.toHexString(packet.getFamily()) + "/"
                + Integer.toHexString(packet.getCommand()) + ": "
                + e.getSnacCommand());

        SnacCommand cmd = e.getSnacCommand();

        if (cmd instanceof RateInfoCmd) {
            RateInfoCmd ric = (RateInfoCmd) cmd;

            RateClassInfo[] rateClasses = ric.getRateClassInfos();

            int[] classes = new int[rateClasses.length];
            for (int i = 0; i < rateClasses.length; i++) {
                classes[i] = rateClasses[i].getRateClass();
//                System.out.println("- " + rateClasses[i] + ": " + Arrays.asList(rateClasses[i].getCommands()));
            }

            request(new RateAck(classes));
        }
    }

    public int[] getSnacFamilies() { return snacFamilies; }

    protected void setSnacFamilies(int[] families) {
        this.snacFamilies = (int[]) families.clone();
        Arrays.sort(snacFamilies);
    }

    protected void setSnacFamilyInfos(SnacFamilyInfo[] infos) {
        snacFamilyInfos = infos;
    }

    protected boolean supportsFamily(int family) {
        return Arrays.binarySearch(snacFamilies, family) >= 0;
    }

    protected void clientReady() {
        if (!sentClientReady) {
            sentClientReady = true;
            request(new ClientReadyCmd(snacFamilyInfos));
        }
    }

    protected SnacRequest dispatchRequest(SnacCommand cmd) {
        return dispatchRequest(cmd, null);
    }

    protected SnacRequest dispatchRequest(SnacCommand cmd,
            SnacRequestListener listener) {
        SnacRequest req = new SnacRequest(cmd, listener);
        dispatchRequest(req);
        return req;
    }

    protected void dispatchRequest(SnacRequest req) {
        tester.handleRequest(req);
    }

    protected SnacRequest request(SnacCommand cmd,
            SnacRequestListener listener) {
        SnacRequest req = new SnacRequest(cmd, listener);

        handleReq(req);

        return req;
    }

    private void handleReq(SnacRequest request) {
        int family = request.getCommand().getFamily();
        if (snacFamilies == null || supportsFamily(family)) {
            // this connection supports this snac, so we'll send it here
            sendRequest(request);
        } else {
            tester.handleRequest(request);
        }
    }

}
