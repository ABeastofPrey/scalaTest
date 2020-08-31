package com.servotronix.mcwebserver;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import static MCAuth.MCAuth.verifyToken;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONException;
import org.json.JSONObject;

import CabinetUpdateCommLib.CabinetUpdateCommManager;
import CabinetUpdateCommLib.CabinetUpdateConnectionManager;
import MCAuth.TokenManager;

public class CabinetUpdateWebSocketServer extends WebSocketServer {
        
  // Constructors
  public CabinetUpdateWebSocketServer(int port) throws UnknownHostException {
    super(new InetSocketAddress(port));
  }

  public CabinetUpdateWebSocketServer(InetSocketAddress address) {
    super(address);
  }

  // WebSocket Methods
  @Override
  public void onOpen(WebSocket ws, ClientHandshake handshake) {
    System.out.println("Cabinet agent onOpen");
    CabinetUpdateCommManager conn = CabinetUpdateConnectionManager.instance.getCabinetUpdateCommManager(ws);
    if (conn == null) {
      CabinetUpdateConnectionManager.instance.initCabinetUpdateCommManager(ws);
    } else {
      ws.close(4001);
    }
  }

  @Override
  public void onClose(WebSocket ws, int code, String reason, boolean remote) {
    System.out.println("WS onClose");
    if (ws!=null && ws.getRemoteSocketAddress()!=null) {
      int port = ws.getRemoteSocketAddress().getPort();
      System.out.println("WS PORT:" + port);
      CabinetUpdateCommManager conn = CabinetUpdateConnectionManager.instance.getCabinetUpdateCommManager(port);
      if (conn == null) {
        System.out.println("Warning: Couldn't find CabinetUpdateCommManager");
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
      try {
        String token = data.getString("token");
        userHasValidToken = (verifyToken(token) >= 0 || TokenManager.verifyToken(token));
        if (!userHasValidToken) {
          System.out.println("Invalid TOKEN...");
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
        ws.close();
        return;
      }
      JSONObject result = new JSONObject();
      result.put("cmd_id", cmd_id);
      result.put("cmd", msg);
      String ans = "";
      CabinetUpdateCommManager conn = CabinetUpdateConnectionManager.instance.getCabinetUpdateCommManager(ws);
      if (conn != null) {
        ans = conn.send(msg,timeout,cmd_id);
      }
      result.put("msg",ans);
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
    System.out.println("Cabinet Update Websocket server started...");
  }
    
}
