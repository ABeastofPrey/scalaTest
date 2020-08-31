package DriveCommLib;

import java.util.HashMap;
import org.java_websocket.WebSocket;

public class DriveConnectionManager {
    
  public static DriveConnectionManager instance;

  // This map maps the websockets port to the corresponding DriveCommManager
  private HashMap<Integer,DriveCommManager> connectionMap = new HashMap<>();

  public DriveConnectionManager() {
    instance = this;
  }
    
  public synchronized DriveCommManager getDriveCommManager(WebSocket ws) {
    if (ws != null)
      return connectionMap.get(ws.getRemoteSocketAddress().getPort());
    return connectionMap.get(-1);
  }
  
  public synchronized DriveCommManager getDriveCommManager(int port) {
    return connectionMap.get(port);
  }
    
  public synchronized boolean initDriveCommManager(WebSocket ws, String ip) {
    DriveCommManager mngr = new DriveCommManager(ws);
    if (mngr.connect(ip)){
      System.out.println("success!");
      connectionMap.put(ws.getRemoteSocketAddress().getPort(), mngr);
      return true;
    }
    ws.close();
    System.out.println("Error in initDriveCommManager: Can't connect to drive");
    return false;
  }

  public synchronized void removeDriveCommManager(WebSocket ws) {
    try {
      System.out.println("Removing Drive Connection For WS " + ws.getRemoteSocketAddress().getPort());
      DriveCommManager conn = getDriveCommManager(ws);
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
