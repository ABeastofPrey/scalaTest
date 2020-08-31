package com.servotronix.mcwebserver;

import DriveCommLib.DriveCommManager;
import DriveCommLib.DriveConnectionManager;
import DriveCommLib.TelnetResult;
import static MCAuth.MCAuth.verifyToken;
import MCAuth.TokenManager;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONException;
import org.json.JSONObject;

public class DriveWebSocketServer extends WebSocketServer {
        
  // Constructors
  public DriveWebSocketServer(int port) throws UnknownHostException {
    super(new InetSocketAddress(port));
  }

  public DriveWebSocketServer(InetSocketAddress address) {
    super(address);
  }

  // WebSocket Methods
  @Override
  public void onOpen(WebSocket ws, ClientHandshake handshake) {
    System.out.println("Drive onOpen");
  }

  @Override
  public void onClose(WebSocket ws, int code, String reason, boolean remote) {
    System.out.println("WS onClose");
    if (ws!=null && ws.getRemoteSocketAddress()!=null) {
      int port = ws.getRemoteSocketAddress().getPort();
      System.out.println("WS PORT:" + port);
      DriveCommManager conn = DriveConnectionManager.instance.getDriveCommManager(port);
      if (conn == null) {
        System.out.println("Warning: Couldn't find DriveCommManager");
        return;
      }
      conn.disconnect();
    }
  }

  @Override
  public void onMessage(WebSocket ws, String jsonString) {
      //long start = Calendar.getInstance().getTimeInMillis();
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
      Integer timeout;
      try { timeout = data.getInt("timeout"); } catch (JSONException e) { timeout = null; }
      boolean userHasValidToken = false;
      boolean hasWebAPI = MCServer.instance.getMCInfo().getFeatures().contains("WebAPI");
      try {
        String token = data.getString("token");
        userHasValidToken = (verifyToken(token) >= 0 || TokenManager.verifyToken(token));
        if (!userHasValidToken && !hasWebAPI) {
          System.out.println("Invalid TOKEN and no Web API license...");
          System.out.println("TOKEN:"+token);
          JSONObject result = new JSONObject();
          result.put("cmd_id", -1);
          result.put("cmd", msg);
          result.put("msg","This request requires a valid token.");
          ws.send(result.toString());
          ws.close();
          return;
        }
      } catch (JSONException e) {
        // token not found
        System.out.println("JSONException:"+e.getMessage());
        System.out.println("in:"+jsonString);
        if (!hasWebAPI) {
          ws.close();
          return;
        }
      }
      JSONObject result = new JSONObject();
      result.put("cmd_id", cmd_id);
      result.put("cmd", msg);
      TelnetResult ans = new TelnetResult();
      if (msg.startsWith("set_ip")) {
        String ip = msg.substring(7);
        if (ip.length() == 1) {
          ans.setAns("-1");
        } else {
          ip = ip.substring(0,ip.length()-1);
          DriveCommManager conn = DriveConnectionManager.instance.getDriveCommManager(ws);
          if (conn == null) {
            boolean init = DriveConnectionManager.instance.initDriveCommManager(ws,ip);
            ans.setAns(init ? "0" : "-1");
          }
        }
      } else {
        DriveCommManager conn = DriveConnectionManager.instance.getDriveCommManager(ws);
        if (conn != null) {
          ans = conn.send(msg,timeout,cmd_id);
        }
      }
      result.put("msg",ans.getAns());
      if (ans.getPrompt() != null)
        result.put("prompt",ans.getPrompt());
      try {
        ws.send(result.toString());
      } catch (Exception e) {
        System.out.println("CAN'T SEND ANSWER");
      }
  }

  @Override
  public void onError(WebSocket conn, Exception ex) {
      ex.printStackTrace();
      System.out.println("WEBSOCKET ONERROR - " + ex.getMessage());
  }
  
  @Override
  public void onStart() {
    System.out.println("Drive Websocket server started...");
  }
    
}
