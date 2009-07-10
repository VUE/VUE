package edu.tufts.vue.collab.im;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.tufts.vue.collab.im.*;
import edu.tufts.vue.collab.im.security.SecureSession;
import edu.tufts.vue.collab.im.security.SecureSessionException;

import sun.net.ProgressSource.State;
import tufts.vue.VueResources;
import tufts.vue.VueUtil;


import net.kano.joscar.ByteBlock;
import net.kano.joscar.FileWritable;
import net.kano.joscar.OscarTools;
import net.kano.joscar.flap.ClientFlapConn;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.net.ClientConnListener;
import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rvcmd.InvitationMessage;
import net.kano.joscar.rvcmd.RvConnectionInfo;
import net.kano.joscar.rvcmd.chatinvite.ChatInvitationRvCmd;
import net.kano.joscar.rvcmd.directim.DirectIMReqRvCmd;
import net.kano.joscar.rvcmd.getfile.GetFileReqRvCmd;
import net.kano.joscar.rvcmd.sendbl.SendBuddyListGroup;
import net.kano.joscar.rvcmd.sendbl.SendBuddyListRvCmd;
import net.kano.joscar.rvcmd.sendfile.FileSendBlock;
import net.kano.joscar.rvcmd.sendfile.FileSendReqRvCmd;
import net.kano.joscar.rvcmd.trillcrypt.TrillianCryptReqRvCmd;
import net.kano.joscar.rvcmd.voice.VoiceReqRvCmd;
import net.kano.joscar.snac.ClientSnacProcessor;
import net.kano.joscar.snac.SnacRequest;
import net.kano.joscar.snac.SnacRequestListener;
import net.kano.joscar.snaccmd.DefaultClientFactoryList;
import net.kano.joscar.snaccmd.DirInfo;
import net.kano.joscar.snaccmd.ExtraInfoBlock;
import net.kano.joscar.snaccmd.ExtraInfoData;
import net.kano.joscar.snaccmd.FullRoomInfo;
import net.kano.joscar.snaccmd.FullUserInfo;
import net.kano.joscar.snaccmd.MiniRoomInfo;
import net.kano.joscar.snaccmd.acct.AcctInfoRequest;
import net.kano.joscar.snaccmd.acct.AcctModCmd;
import net.kano.joscar.snaccmd.acct.ConfirmAcctCmd;
import net.kano.joscar.snaccmd.chat.ChatMsg;
import net.kano.joscar.snaccmd.conn.ServiceRequest;
import net.kano.joscar.snaccmd.conn.SetExtraInfoCmd;
import net.kano.joscar.snaccmd.icbm.InstantMessage;
import net.kano.joscar.snaccmd.icbm.OldIconHashInfo;
import net.kano.joscar.snaccmd.icbm.SendImIcbm;
import net.kano.joscar.snaccmd.icon.UploadIconCmd;
import net.kano.joscar.snaccmd.invite.InviteFriendCmd;
import net.kano.joscar.snaccmd.loc.GetDirInfoCmd;
import net.kano.joscar.snaccmd.loc.GetInfoCmd;
import net.kano.joscar.snaccmd.loc.SetDirInfoCmd;
import net.kano.joscar.snaccmd.loc.SetInterestsCmd;
import net.kano.joscar.snaccmd.rooms.ExchangeInfoReq;
import net.kano.joscar.snaccmd.rooms.JoinRoomCmd;
import net.kano.joscar.snaccmd.rooms.RoomRightsRequest;
import net.kano.joscar.snaccmd.search.InterestListReq;
import net.kano.joscar.snaccmd.search.SearchBuddiesCmd;
import net.kano.joscar.snaccmd.ssi.CreateItemsCmd;
import net.kano.joscar.snaccmd.ssi.DeleteItemsCmd;
import net.kano.joscar.snaccmd.ssi.ModifyItemsCmd;
import net.kano.joscar.snaccmd.ssi.SsiItem;
import net.kano.joscar.ssiitem.BuddyItem;
import net.kano.joscar.ssiitem.DenyItem;
import net.kano.joscar.ssiitem.GroupItem;
import net.kano.joscar.ssiitem.IconItem;
import net.kano.joscar.ssiitem.PermitItem;
import net.kano.joscar.ssiitem.PrivacyItem;
import net.kano.joscar.ssiitem.RootItem;
import net.kano.joscar.ssiitem.VisibilityItem;

public class VUEAim {

    protected LoginConn loginConn = null;
    protected static final int DEFAULT_SERVICE_PORT = 5190;

    protected DefaultClientFactoryList factoryList
            = new DefaultClientFactoryList();

    protected ClientFlapConn loginFlapConn = null, mainConn = null;
    protected ClientSnacProcessor loginSnacProcessor = null;

    protected List serviceConns = new ArrayList();

    protected String sn = null;
    protected String pass = null;
    protected boolean ignoreIMs = false;
	private boolean requireApproval = true;
    protected BosFlapConn bosConn = null;
    protected Set services = new HashSet();
    protected Map chats = new HashMap();
    private ClientConnListener connListener = null;
    private SecureSession secureSession = SecureSession.getInstance();

	public VUEAim(String username, String password)
	{	
	        ConsoleHandler handler = new ConsoleHandler();
	        handler.setLevel(Level.OFF);
	        Logger logger = Logger.getLogger("net.kano.joscar");
	        logger.addHandler(handler);
	        logger.setLevel(Level.OFF);
	        this.sn = username;
	        this.pass = password;	
	}
	
	public void connect()
	{
		if (loginConn == null)
		 loginConn = new LoginConn("login.oscar.aol.com", DEFAULT_SERVICE_PORT, this);

		 loginConn.connect();
	     //connected = true;
		
	}
	public void disconnect()
	{
		if (loginConn !=null)
		{
			loginConn.disconnect();
	        bosConn.disconnect();
		
		}
	}
	
	public void addConnectionListener(ClientConnListener c)
	{
		connListener =c ;
	}
	 public void startBosConn(String server, int port, ByteBlock cookie) {
	        bosConn = new BosFlapConn(server, port, this, cookie);
	        
	     //   new BosFlapConn()
	        if (connListener !=null)
	        bosConn.addConnListener(connListener);
	        bosConn.connect();
	    }

	    public void registerSnacFamilies(BasicConn conn) {
	        snacMgr.register(conn);
	    }

	    public void connectToService(int snacFamily, String host, ByteBlock cookie) {
	        ServiceConn conn = new ServiceConn(host, DEFAULT_SERVICE_PORT, this,
	                cookie, snacFamily);

	        conn.connect();
	        
	    }

	    public void serviceFailed(ServiceConn conn) 
	    {
	    	
	    }

	    public void serviceConnected(ServiceConn conn) {
	        services.add(conn);
	    }

	    public void serviceReady(ServiceConn conn) {
	        snacMgr.dequeueSnacs(conn);
	    }

	    public void serviceDied(ServiceConn conn) {
	        services.remove(conn);
	        snacMgr.unregister(conn);
	    }

	    void joinChat(int exchange, String roomname) {
	        FullRoomInfo roomInfo
	                = new FullRoomInfo(exchange, roomname, "us-ascii", "en");
	        handleRequest(new SnacRequest(new JoinRoomCmd(roomInfo), null));
	    }

	    public void connectToChat(FullRoomInfo roomInfo, String host,
	            ByteBlock cookie) {
	        ChatConn conn = new ChatConn(host, DEFAULT_SERVICE_PORT, this, cookie,
	                roomInfo);

	        conn.addChatListener(new MyChatConnListener());

	        conn.connect();
	    }

	    public ChatConn getChatConn(String name) {
	        return (ChatConn) chats.get(OscarTools.normalize(name));
	    }

	    protected SnacManager snacMgr = new SnacManager(new PendingSnacListener() {
	        public void dequeueSnacs(SnacRequest[] pending) {
	            System.out.println("dequeuing " + pending.length + " snacs");
	            for (int i = 0; i < pending.length; i++) {
	                handleRequest(pending[i]);
	            }
	        }
	    });

	    public synchronized void handleRequest(SnacRequest request) {
	        int family = request.getCommand().getFamily();
	        if (snacMgr.isPending(family)) {
	            snacMgr.addRequest(request);
	            return;
	        }

	        BasicConn conn = snacMgr.getConn(family);

	        if (conn != null) {
	            conn.sendRequest(request);
	        } else {
	            // it's time to request a service
	            if (!(request.getCommand() instanceof ServiceRequest)) {
	                System.out.println("requesting " + Integer.toHexString(family)
	                        + " service.");
	                snacMgr.setPending(family, true);
	                snacMgr.addRequest(request);
	                request(new ServiceRequest(family));
	            } else {
	                System.out.println("eep! can't find a service redirector " +
	                        "server.");
	            }
	        }
	    }

	    public SnacRequest request(SnacCommand cmd) {
	        return request(cmd, null);
	    }

	    private SnacRequest request(SnacCommand cmd, SnacRequestListener listener) {
	        SnacRequest req = new SnacRequest(cmd, listener);
	        handleRequest(req);
	        return req;
	    }

	    protected SortedMap cmdMap = new TreeMap();

	    OldIconHashInfo oldIconInfo;
	    File iconFile = null;
	    { if (false) {
	        try {
	            ClassLoader classLoader = getClass().getClassLoader();
	            URL iconResource = classLoader.getResource("images/beck.gif");
	            String ext = iconResource.toExternalForm();
	            System.out.println("ext: " + ext);
	            URI uri = new URI(ext);
	            iconFile = new File(uri);
	            oldIconInfo = new OldIconHashInfo(iconFile);
	        } catch (IOException e) {
	            e.printStackTrace();
	        } catch (URISyntaxException e) {
	            e.printStackTrace();
	        }
	    }
	    }

	    protected static byte[] hashIcon(String filename) throws IOException {
	        FileInputStream in = new FileInputStream(filename);
	        try {
	            byte[] block = new byte[1024];
	            MessageDigest md;
	            try {
	                md = MessageDigest.getInstance("MD5");
	            } catch (NoSuchAlgorithmException e) {
	                e.printStackTrace();
	                return null;
	            }
	            for (;;) {
	                int count = in.read(block);
	                if (count == -1) break;

	                md.update(block, 0, count);
	            }
	            return md.digest();
	        } finally {
	            in.close();
	        }
	    }

	    private String aimexp = "the60s";
	    

	    public void sendIM(String nick, String text) {
	        request(new SendImIcbm(nick, text));
	    }

	    public SecureSession getSecureSession() { return secureSession; }

	    private class MyChatConnListener implements ChatConnListener {
	        public void connFailed(ChatConn conn, Object reason) { }

	        public void connected(ChatConn conn) { }

	        public void joined(ChatConn conn, FullUserInfo[] members) {
	            String name = conn.getRoomInfo().getName();
	            chats.put(OscarTools.normalize(name), conn);

	            System.out.println("*** Joined "
	                    + conn.getRoomInfo().getRoomName() + ", members:");
	            for (int i = 0; i < members.length; i++) {
	                System.out.println("  " + members[i].getScreenname());
	            }
	        }

	        public void left(ChatConn conn, Object reason) {
	            String name = conn.getRoomInfo().getName();
	            chats.remove(OscarTools.normalize(name));

	            System.out.println("*** Left "
	                    + conn.getRoomInfo().getRoomName());
	        }

	        public void usersJoined(ChatConn conn, FullUserInfo[] members) {
	            for (int i = 0; i < members.length; i++) {
	                System.out.println("*** " + members[i].getScreenname()
	                        + " joined " + conn.getRoomInfo().getRoomName());
	            }
	        }

	        public void usersLeft(ChatConn conn, FullUserInfo[] members) {
	            for (int i = 0; i < members.length; i++) {
	                System.out.println("*** " + members[i].getScreenname()
	                        + " left " + conn.getRoomInfo().getRoomName());
	            }
	        }

	        public void gotMsg(ChatConn conn, FullUserInfo sender,
	                ChatMsg msg) {
	            String msgStr = msg.getMessage();
	            String ct = msg.getContentType();
	            if (msgStr == null && ct.equals("application/pkcs7-mime")) {
	                ByteBlock msgData = msg.getMessageData();

	                try {
	                    msgStr = secureSession.parseChatMessage(conn.getRoomName(),
	                            sender.getScreenname(), msgData);
	                } catch (SecureSessionException e) {
	                    e.printStackTrace();
	                }
	            }
	            //System.out.println("<" + sender.getScreenname()
	              //      + ":#" + conn.getRoomInfo().getRoomName() + "> "
	                //    + );
	         
	            
	        }


	    }

		public String getScreenname() {
			return this.sn;
		}

		public void loginFailed(String reason) {
			String message = VueResources.getString("im.login.error.message") + " : " + reason;
	    	VueUtil.alert(message, VueResources.getString("im.login.error.title"));
			
		}

		public String getPassword() {
			return this.pass;
		}

		public void setScreennameFormat(String screenname) {
			 sn = screenname;
			
		}

		public boolean isConnected() {
			if (bosConn !=null)
			{

				return bosConn.getState().toString().equals(State.CONNECTED.toString());
			}
			else
				return false;
		}

		public void ignoreIMs(boolean b) {
			ignoreIMs = b;
			
		}

		public void requireApprovalToCollaborate(boolean b) {
			requireApproval = b;
			
		}
}
