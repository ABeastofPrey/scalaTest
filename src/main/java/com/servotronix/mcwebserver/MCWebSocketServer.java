package com.servotronix.mcwebserver;

import static MCAuth.MCAuth.verifyToken;
import MCAuth.TokenManager;
import static com.servotronix.mcwebserver.WebsocketErrorCode.*;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONException;
import org.json.JSONObject;

public class MCWebSocketServer extends WebSocketServer {
  
  int clients = 0;
        
  // Constructors
  public MCWebSocketServer(int port) throws UnknownHostException {
    super(new InetSocketAddress(port));
  }
  
  private boolean connectToESBP() {
    if (CyclicTaskHandler.isConnected())
      return true;
    return CyclicTaskHandler.connect();
  }

  public MCWebSocketServer(InetSocketAddress address) {
    super(address);
  }

  public static void sendToWebsocket(WebSocket ws, int cmd_id, String msg) {
    JSONObject obj = new JSONObject();
    obj.put("cmd_id", cmd_id);
    obj.put("msg", msg);
    ws.send(obj.toString());
  }

  // WebSocket Methods
  @Override
  public void onOpen(WebSocket ws, ClientHandshake handshake) {
    clients++;
    Thread t = new Thread(new Runnable(){
      public void run() {
        System.out.println("onOpen");
        MCConnection conn = MCConnectionManager.instance.getMCConnection(ws);
        if (conn == null) {
          MCConnectionManager.instance.initMCConnection(ws);
          MCConnectionManager.instance.sendToAll("%%%" + clients, ws);
        } else {
          ws.close(WS_ALREADY_EXISTS);
        }
      }
    });
    t.start();
  }

  @Override
  public void onClose(WebSocket ws, int code, String reason, boolean remote) {
    System.out.println("WS onClose, code=" + code + ", remote=" + remote);
    clients--;
    if (clients == 0)
      CyclicTaskHandler.disconnect();
    if (ws!=null && ws.getRemoteSocketAddress()!=null) {
      MCConnectionManager.instance.sendToAll("%%%" + clients, ws);
      int port = ws.getRemoteSocketAddress().getPort();
      System.out.println("WS PORT:" + port);
      MCConnection conn = MCConnectionManager.instance.getMCConnection(port);
      if (conn == null) {
        System.out.println("Warning: Couldn't find MCConnection");
        MCConnectionManager.instance.checkForInvalidConnections();
        return;
      }
      conn.disconnect(WS_NORMAL_CLOSE);
    } else if (ws!=null && ws.getLocalSocketAddress()!=null) {
      MCConnectionManager.instance.removeMCConnection(ws, code);
    } else if (ws!=null) {
      System.out.println("WS IS NOT NULL BUT LOCAL AND REMOTE PORTS ARE NULL...");
    } else {
      System.out.println("WS IS NULL");
    }
  }
  
  private void handleMessage(WebSocket ws, String jsonString) {
    //long start = Calendar.getInstance().getTimeInMillis(); //start time
    JSONObject data = null;
    try {
      data = new JSONObject(jsonString);
    } catch (JSONException e) {
      System.out.println("Can't parse JSON:" + jsonString + "...");
    } finally {
      if (data == null)
        return;
    }
    int cmd_id = data.getInt("cmd_id");
    String msg = data.getString("msg");
    MCConnection conn = MCConnectionManager.instance.getMCConnection(ws);
    int permission = 3;
    if (conn == null)
      return;
    if (!conn.isAuth()) {
      boolean userHasValidToken = false;
      boolean hasWebAPI = MCServer.instance.getMCInfo().getFeatures().contains("WebAPI");
      try {
        String token = data.getString("token");
        permission = verifyToken(token);
        userHasValidToken = (permission >= 0 || TokenManager.verifyToken(token));
        if (!userHasValidToken && !hasWebAPI) {
          System.out.println("Invalid TOKEN and no Web API license...");
          System.out.println("TOKEN:"+token);
          JSONObject result = new JSONObject();
          result.put("cmd_id", -1);
          result.put("cmd", msg);
          result.put("msg","This request requires a valid token.");
          ws.send(result.toString());
          ws.close(WS_INVALID_TOKEN);
          return;
        }
      } catch (JSONException e) {
        // token not found
        if (!hasWebAPI) {
          System.out.println("TOKEN NOT FOUND...Force disconnect!\nQuery was:" + jsonString);
          ws.close(WS_INVALID_TOKEN);
          return;
        }
      }
      conn.setAuth();
      // SET AUTH
      int correctPermission = permission == 99 ? 0 : permission;
      String permissionSet = conn.getMngr().QueryMCSynced("?user ISSPL(" + correctPermission + ",0,0)",0);
      System.out.println("PERMISSION SET TO " + correctPermission + " >>> RESULT:" + permissionSet);
    }
    JSONObject result = new JSONObject();
    result.put("cmd_id", cmd_id);
    result.put("cmd", msg);
    String ans = "";
    // SPECIAL COMMANDS
    if (msg.startsWith("java")) {
      if (msg.equals("java_ver")) {
        ans = MCDefs.VER;
      } else if (msg.equals("java_port")) {
        ans = String.valueOf(conn.getEntryStationPort());
      } else if (msg.startsWith("java_es")) {
        ans = "" + clients;
      }
    } else if (msg.startsWith("cyc")) {
      //long start2 = Calendar.getInstance().getTimeInMillis();
      if (connectToESBP()) {
        ans = CyclicTaskHandler.QueryMC("{"+msg.substring(3)+"}");
      } else {
        ans = "";
        //System.out.println("COULD NOT GET ANSWER FOR QUERY:" + msg);
      }
      //long diff = (Calendar.getInstance().getTimeInMillis() - start2);
      //System.out.println(msg + ">>>" + diff);
    } else {
      //start = Calendar.getInstance().getTimeInMillis();
      ans = conn.getMngr().QueryMCSynced(msg, cmd_id);
    }
    result.put("msg",ans);
    try {
      if (ws.isOpen())
        ws.send(result.toString());
      //long now = Calendar.getInstance().getTimeInMillis();
      //System.out.println(now + ": " + msg + " >>> " + (now - start));
    } catch (Exception e) {
      System.out.println("CAN'T SEND ANSWER >> " + e.getMessage());
    }
  }

  @Override
  public void onMessage(WebSocket ws, String jsonString) {
    if (jsonString.equals("X") && ws.isOpen()) { // PING REQUEST
      ws.send("O");
      return;
    }
    //handleMessage(ws, jsonString);
    new Thread(new Runnable() {
      @Override
      public void run() {
        handleMessage(ws, jsonString);
      }
    }).start();
  }

  @Override
  public void onError(WebSocket conn, Exception ex) {
      ex.printStackTrace();
      System.out.println("WEBSOCKET ONERROR - " + ex.getMessage());
  }
  
  @Override
  public void onStart() {
    System.out.println("Websocket server started...");
  }
    
}
