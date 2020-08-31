package MCJCommLib;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.servotronix.mcwebserver.Utils;

public class MCJCommManager implements MCJCallbacks {

  private ConcurrentLinkedQueue<Object> messagesQueue = new ConcurrentLinkedQueue<Object>();
  MCJSocketClient sockClient = null;
  MCJMessageUtils msgUtills = null;
  boolean bContinue = false;
  private final Object _monitor = new Object();
  private String address;
  private int port;
  private String name;
  private int queryMaxWait; // timeout for query result
  boolean isQueryMode = false;
  MCJCallbacks callBacks = null;
  StringBuilder resSB = new StringBuilder("");
  private final Object qryAre = new Object();
  Thread queueCleaanThread = null;
  private boolean isConnected = false;
  int localPort = -1;
  
  private HashMap<Integer,PendingCommand> commands = new HashMap();
  
  public int getPort() {
    return localPort;
  }
  
  public boolean isConnected() {
      return isConnected || (sockClient != null && !sockClient.socket.isClosed());
  }

  public MCJCommManager(MCJCallbacks callBacks) {
    this.callBacks = callBacks;
  }

  public boolean Init(String name, String address, int port, int queryMaxWait) {
    this.queryMaxWait = queryMaxWait;
    this.name = name;
    sockClient = new MCJSocketClient(this);
    msgUtills = new MCJMessageUtils(this);
    this.address = address;
    this.port = port;
    return true;
  }

  public void Finish() {
      System.out.println("CommMngr FINISH on port " + port);
    Disconnect();
  }

  public boolean Connect() {
    MCJRetBoolExt res = sockClient.Connect(address, port, queryMaxWait);
    bContinue = sockClient.bContinue;
    if (!res.b) {
      return false;
    }
    localPort = sockClient.socket.getLocalPort();
    queueCleaanThread = new Thread("Thread for reading") {
      // the queue cleaning thread
      @Override
      public void run() {
        synchronized(_monitor) {
          try {
            isConnected = true;
            while (bContinue) {
              try {
                _monitor.wait(4);
              } catch (InterruptedException ex) {
                System.out.println(ex.getMessage());
              }
              if (!bContinue) {
                return;
              }
              while (!messagesQueue.isEmpty()) {
                if (!bContinue) {
                  return;
                }
                byte[] buff = (byte[]) messagesQueue.poll();
                for (byte b : buff) {
                  msgUtills.DecodeMessage(b);
                }
                // if (messagesQueue.size() > 10)
                //   System.out.println("queue: " + messagesQueue.size());
              }
              bContinue = sockClient.bContinue;
            }
            isConnected = false;
            //System.out.println("Terminating thread for reading");
            callBacks.OnConnectionStateChangeCallback(false);
          } catch (Exception e) {
            e.printStackTrace();
            // something went wrong, close connections
            isConnected = false;
            System.out.println("Terminating thread for reading (" + e.getMessage() + " | " + e.getCause()+ ")");
            callBacks.OnConnectionStateChangeCallback(false);
          } finally {
            return;
          }
        }
      }
    };
    queueCleaanThread.start();
    return true;
  }

  public void Disconnect() {
    //System.out.println("Start to disconnect mngr...");
    isConnected = false;
    if (sockClient == null) {
      return;
    }
    synchronized(qryAre) { // WAIT FOR ONGOING QUERY TO FINISH
      while(isQueryMode) {
        try {
          qryAre.wait(2000);
          isQueryMode = false;
          bContinue = false;
          qryAre.notifyAll();
        } catch (InterruptedException ex) {
        }
      }  
    }
    bContinue = false;
    try {
      queueCleaanThread.join(500);
    } catch (Exception ex) {
    }
    sockClient.Disconnect();
    try {
      Thread.sleep(600);
    } catch (InterruptedException e) { 
      System.out.println("can't sleep");
    }
    sockClient = null;
    callBacks.OnConnectionStateChangeCallback(false);
    System.out.println("MC CommMngr disconnect on port " + port);
  }

  @Override
  public void OnNewBufferCallback(byte[] buffer, int filledSize) {
    byte[] data = Arrays.copyOf(buffer, filledSize);
    try {
      messagesQueue.add(data);
    } catch (Exception ex) {
      System.out.println(String.format("Put failed = %s", ex.getMessage()));
    }
    synchronized(_monitor) {
      _monitor.notifyAll();
    }
  }

  @Override
  public void OnNewMessageCAllback(String newMsg) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  // receives decoded message
  @Override
  public void OnMultiPurpDataCallback(GenDefs.FrameTypeEnum frameType, String msg, int id) {
    //System.out.println(Calendar.getInstance().getTimeInMillis() + "Msg:" + msg + ", ID: " + id);
    if (!bContinue) {
      synchronized(qryAre) {
        qryAre.notifyAll();
      }
      return;
    }
    switch (frameType) {
        case DATA_FRAME:
            if (commands.get(id)!=null)
              commands.get(id).append(msg);
            if (!isQueryMode) {
              if (callBacks != null) {
                callBacks.OnMultiPurpDataCallback(GenDefs.FrameTypeEnum.DATA_FRAME, msg, id);
              }
            } else {
              synchronized(qryAre) {
                resSB.append(msg.trim());
              }
            }
            break;
        case PROMPT_FRAME:
            if (isQueryMode) {
              isQueryMode = false;
              synchronized(qryAre) {
                qryAre.notifyAll();
              }
            }
            if (commands.get(id) != null)
              commands.get(id).setDone(true);
            if (callBacks != null) {
              callBacks.OnMultiPurpDataCallback(GenDefs.FrameTypeEnum.PROMPT_FRAME, msg, id);
            }
            break;
        case ACKNOWLEDGE_FRAME:
          if (!isQueryMode) {
            if (callBacks != null) {
              callBacks.OnMultiPurpDataCallback(GenDefs.FrameTypeEnum.ACKNOWLEDGE_FRAME, msg, id);
            }
          }
          break;
        case ERROR_FRAME:
            if (commands.get(id)!=null)
              commands.get(id).appendError(msg);
            if (!isQueryMode) {
              if (callBacks != null) {
                callBacks.OnMultiPurpDataCallback(GenDefs.FrameTypeEnum.ERROR_FRAME, msg, id);
              }
            } else {
              synchronized(qryAre) {
                resSB.append("$ERRORFRAME$" + msg);
              }
            }
            break;
        case ASYNC_FRAME:
        case ASYNC2_FRAME:
            if (callBacks != null) {
              callBacks.OnMultiPurpDataCallback(GenDefs.FrameTypeEnum.ASYNC_FRAME, msg, id);
            }
            break;
        case ASYNC_OTHER:
            if (callBacks != null)
                callBacks.OnMultiPurpDataCallback(GenDefs.FrameTypeEnum.ASYNC_FRAME, msg, id);
            break;
        default:
            break;
    }
  }

  @Override
  public void OnConnectionStateChangeCallback(boolean connected) {
    
  }

  // send message
  public boolean Send(String msg, int id) {
    if (sockClient == null) {
      return false;
    }
    byte[] msgBytes = msgUtills.MakeMessage(GenDefs.FrameTypeEnum.DATA_FRAME, msg, id);
    if (msgBytes == null) {
      return false;
    }
    return sockClient.Write(msgBytes);
  }

  final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

  public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }
  
    // send data and wait for reply (synchronous)
    public synchronized String QueryMC(String cmd, int id){
      final long start = Utils.currentTimeMillis();
      resSB = new StringBuilder();
      isQueryMode = true;
      Send(cmd, id);
      synchronized(qryAre) {
        while (isQueryMode && bContinue) {
          try {
            qryAre.wait();
          } catch (InterruptedException ex) {
            return "";
          }
        }
      }
      final long diff = Utils.currentTimeMillis() - start;
      if (diff > 100) {
        System.out.println(cmd + " >>> " + diff + "(queue:" + this.messagesQueue.size() + ")");
      }
      if (bContinue) {
        return resSB.toString().trim();
      }
      return "";
    }
  
    public String QueryMCSynced(String cmd, int id){
      commands.put(id, new PendingCommand());
      if (commands.get(id)!=null)
        commands.get(id).setDone(false);
      Send(cmd, id);
      while (bContinue && commands.get(id)!=null && !commands.get(id).isDone()) {
        try { Thread.sleep(4); } catch (Exception e) {}
      }
      if (bContinue) {
        if (commands.get(id) == null) 
          return "";
        final String result = commands.get(id).toString();
        commands.remove(id);
        return result;
      }
      return "";
    }
}
