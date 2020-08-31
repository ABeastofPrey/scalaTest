package CabinetUpdateCommLib;

import java.util.HashMap;
import org.java_websocket.WebSocket;

public class CabinetUpdateConnectionManager {
    
  public static CabinetUpdateConnectionManager instance;

  // This map maps the websockets port to the corresponding CabinetUpdateCommManager
  private HashMap<Integer,CabinetUpdateCommManager> connectionMap = new HashMap<>();

  public CabinetUpdateConnectionManager() {
    instance = this;
  }
    
  public synchronized CabinetUpdateCommManager getCabinetUpdateCommManager(WebSocket ws) {
    if (ws != null)
      return connectionMap.get(ws.getRemoteSocketAddress().getPort());
    return connectionMap.get(-1);
  }
  
  public synchronized CabinetUpdateCommManager getCabinetUpdateCommManager(int port) {
    return connectionMap.get(port);
  }
    
  public synchronized boolean initCabinetUpdateCommManager(WebSocket ws) {
    CabinetUpdateCommManager mngr = new CabinetUpdateCommManager(ws);
    if (mngr.connect()){
      System.out.println("success!");
      connectionMap.put(ws.getRemoteSocketAddress().getPort(), mngr);
      return true;
    }
    ws.close();
    System.out.println("Error in initCabinetUpdateCommManager: Can't connect to agent");
    return false;
  }

  public synchronized void removeCabinetUpdateCommManager(WebSocket ws) {
    try {
      System.out.println("Removing cabinet agent Connection For WS " + ws.getRemoteSocketAddress().getPort());
      CabinetUpdateCommManager conn = getCabinetUpdateCommManager(ws);
      if (conn != null) {
        conn.disconnect();
        if (ws != null) {
          connectionMap.remove(ws.getRemoteSocketAddress().getPort());
        }
        else {
          connectionMap.remove(-1);
        }
      } else {
      }
    } catch (Exception e) {
    }
  }
    
}
