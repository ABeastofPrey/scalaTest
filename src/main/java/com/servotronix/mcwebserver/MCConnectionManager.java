package com.servotronix.mcwebserver;

import static com.servotronix.mcwebserver.MCWebSocketServer.*;
import static com.servotronix.mcwebserver.WebsocketErrorCode.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.java_websocket.WebSocket;
import org.json.JSONObject;

public class MCConnectionManager {
    
  public static MCConnectionManager instance;

  // This map maps the websockets port to the corresponding MCConnection
  private HashMap<Integer,MCConnection> connectionMap = new HashMap<>();

  public MCConnectionManager() {
    instance = this;
  }
    

  public synchronized MCConnection getMCConnection(WebSocket ws) {
    try {
      if (ws != null) {
        if (ws.getRemoteSocketAddress() != null)
          return connectionMap.get(ws.getRemoteSocketAddress().getPort());
        return null;
      }
      return connectionMap.get(-1);
    } catch (Exception e) {
      System.out.println("ERROR IN GET MC Connection...WS NULL:" + (ws == null) + ", connectionMap null:" + (connectionMap == null) + "...STACK TRACE:");
      e.printStackTrace();
      System.out.println("--------------------------");
      return null;
    }
  }
  
  public synchronized MCConnection getMCConnection(int port) {
    return connectionMap.get(port);
  }
    
  public synchronized boolean initMCConnection(WebSocket ws) {
    int port = 5001;
    MCConnection conn;
    while (port < 5006) {
      System.out.print("Websocket " + ws.getRemoteSocketAddress().getPort() + " is trying to connect on port " + port + "...");
      conn = new MCConnection(ws,port);
      if (conn.connectToMC()){
        System.out.println("success! (" + ws.getRemoteSocketAddress().getPort() + ")");
        connectionMap.put(ws.getRemoteSocketAddress().getPort(), conn);
        return true;
      } else {
        port++;
      }
    }
    ws.close(WS_ENTRYSTATION_FULL);
    System.out.println("Error in initMCConnection: NO AVAILABLE PORTS");
    return false;
  }

  public synchronized boolean initMCConnection() {
    int port = 5001;
    MCConnection conn;
    while (port < 5006) {
      conn = new MCConnection(port);
      if (conn.connectToMC()){
        connectionMap.put(-1, conn);
        return true;
      } else {
        port++;
      }
    }
    System.out.println("No available ports to connect...");
    return false;
  }
  
  public synchronized void removeMCConnection(MCConnection conn, boolean checkInvalid) {
    if (checkInvalid) {
      checkForInvalidConnections();
    }
    connectionMap.values().remove(conn);
  }

  public synchronized void removeMCConnection(WebSocket ws, int reason) {
    try {
      if (ws.getRemoteSocketAddress() != null) {
        System.out.println("Removing CONN For WS " + ws.getRemoteSocketAddress().getPort());
        MCConnection conn = getMCConnection(ws);
        if (conn != null) {
          conn.disconnect(reason);
          if (ws != null) {
            connectionMap.remove(ws.getRemoteSocketAddress().getPort());
          }
          else {
            connectionMap.remove(-1);
          }
        } else {
        }
      } else {
        System.out.println("WS HAS NO REMOTE ADDRESS (PROBABLY BECAUSE NETWORK DISCONNECT)");
        System.out.println("TRYING TO FIND IT IN ANOTHER WAY...");
        checkForInvalidConnections();
      }
    } catch (Exception e) {
    }
  }

  public void checkForInvalidConnections() {
    Iterator mIterator = connectionMap.entrySet().iterator();
    while (mIterator.hasNext()) {
      Map.Entry mapElement = (Map.Entry)mIterator.next(); 
      MCConnection conn = (MCConnection)mapElement.getValue();
      if (!conn.test()) { // SOMETHING IS WRONG - CLOSE CONNECTION
        conn.disconnect(WS_ABNORMAL_CLOSE);
        removeMCConnection(conn, true);
      }
    }
  }

  public synchronized void sendToAll( String text ) {
    JSONObject json = new JSONObject();
    json.put("cmd_id", -1); // THIS MEANS IT'S A SERVER-ORIGINATED MESSAGE
    json.put("msg", text);
    for(Map.Entry<Integer, MCConnection> entry : connectionMap.entrySet()) {
      entry.getValue().send(json.toString());
    }
  }
  
  /*
    SEND TO EVERY WEBSOCKET OTHER THAN THE WEBSOCKET ws
  */
  public synchronized void sendToAll(String msg, WebSocket ws) {
    JSONObject json = new JSONObject();
    json.put("cmd_id", -1); // THIS MEANS IT'S A SERVER-ORIGINATED MESSAGE
    json.put("msg", msg);
    for(Map.Entry<Integer, MCConnection> entry : connectionMap.entrySet()) {
      try {
        if (entry.getValue().getWSPort() == ws.getRemoteSocketAddress().getPort())
          continue;
        entry.getValue().send(json.toString());
      } catch (Exception e) {
      }
    }
  }
    
}
