package com.servotronix.mcwebserver;

import DriveCommLib.DriveConnectionManager;
import static MCAuth.DatabaseOperations.clearOldLogEntries;
import MCAuth.MCUserManager;
import com.servotronix.MCInfo.MCInfo;
import static com.servotronix.mcwebserver.WebsocketErrorCode.*;
import com.servotronix.serverMapping.MapCS;
import com.servotronix.serverMapping.MapCSUtils;
import com.servotronix.serverMapping.MapCabinetUpdate;
import com.servotronix.serverMapping.MapDriveStudio;
import com.servotronix.serverMapping.MapFileOperations;
import com.servotronix.serverMapping.MapTP;
import freemarker.template.Configuration;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.BasicConfigurator;

import CabinetUpdateCommLib.CabinetUpdateConnectionManager;
import spark.ModelAndView;
import static spark.Spark.*;

public class MCServer {
   
  public static MCServer instance; // MCServer singleton

  private MCWebSocketServer wsServer;
  private DriveWebSocketServer driveServer;
  private CabinetUpdateWebSocketServer cabinetServer;
  private MCConnectionManager connMngr;
  private DriveConnectionManager driveMngr;
  private CabinetUpdateConnectionManager cabinetMngr;
  private Configuration cfg;
  private MCConnection conn;
  private MCInfo MCInfo;

  public MCInfo getMCInfo() {
    return MCInfo;
  }

  public MCServer() {
    
    instance = this;
    
    BasicConfigurator.configure();
    System.out.println("\n\n--------------------------------------------------");
    System.out.println("\t\tMC WEB SERVER " + MCDefs.VER);
    System.out.println("--------------------------------------------------\n");
    try {
      wsServer = new MCWebSocketServer(MCDefs.PORT); // listens to websocket connections
      wsServer.setConnectionLostTimeout(20);
      wsServer.setTcpNoDelay(true);
      wsServer.setReuseAddr(true);
      connMngr = new MCConnectionManager(); // creates a different connection to MC for each websocket
      wsServer.start(); // starts the websocket connection
      
      // START DRIVE WEBSOCKET SERVER
      driveServer = new DriveWebSocketServer(MCDefs.DRIVE_WS_PORT); // listens to websocket connections
      driveServer.setConnectionLostTimeout(20);
      driveServer.setTcpNoDelay(true);
      driveServer.setReuseAddr(true);
      driveMngr = new DriveConnectionManager();
      driveServer.start();

      // START CABINET UPDATE WEBSOCKET SERVER
      cabinetServer = new CabinetUpdateWebSocketServer(MCDefs.CABINET_UPDATE_WS_PORT); // listens to websocket connections
      cabinetServer.setConnectionLostTimeout(60);
      cabinetServer.setTcpNoDelay(true);
      cabinetServer.setReuseAddr(true);
      cabinetMngr = new CabinetUpdateConnectionManager();
      cabinetServer.start();
      
      boolean isMCRunning = false;
      while (!isMCRunning) {
        try {
          (new Socket("127.0.0.1", 5001)).close();
          isMCRunning = true;
        } catch (SocketException e0) {
          try {
            (new Socket("127.0.0.1", 5003)).close();
            isMCRunning = true;
          } catch(SocketException e) {
            try {
              (new Socket("127.0.0.1", 5004)).close();
              isMCRunning = true;
            } catch(SocketException e1) {
              try {
                (new Socket("127.0.0.1", 5005)).close();
                isMCRunning = true;
              } catch(SocketException e2) {
                isMCRunning = false;
              }
            }
          }
        }
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {}
      }
      if (MCConnectionManager.instance.initMCConnection()) {
        conn = MCConnectionManager.instance.getMCConnection(null);
        final String sysinfo = conn.getMngr().QueryMC("?sys.information",0);
        final String ip = conn.getMngr().QueryMC("?sys.IPAddressMask",0);
        final String name = conn.getMngr().QueryMC("?sys.name",0);
        final String sn = conn.getMngr().QueryMC("?sys.serialnumber",0);
        MCInfo = new MCInfo(sysinfo,sn,name,ip);
        Thread.sleep(500);
        conn.disconnect(WS_NORMAL_CLOSE);
      }
      else {
        System.out.println("Can't connect to MC");
        return;
      }
      initWebServer(); // start the dynamic web server
    } catch (Exception e) {
      System.out.println("MCServer error - STACK TRACE:");
      e.printStackTrace();
    }
    System.out.println("Initializing MCUserManager...");
    MCUserManager.init();
    System.out.println("Initializing Theme Manager...");
    MCThemeManager.init();
    System.out.println("MCUserManager initialization DONE.");
    clearOldLogEntries();
    System.out.println("--------------------------------------------------\nWEB SERVER IS READY.\n--------------------------------------------------");
  }

  public static void main( String[] args ) throws UnknownHostException{
    MCServer s = new MCServer();        
  }

  private void initWebServer() {
    FreeMarkerEngine engine = new FreeMarkerEngine();
    port(1207);
    //secure("/var/home/mcdb/keystore.jks", "softMC3010", null, null);
    staticFiles.externalLocation(MCDefs.EXT_WWW_FOLDER);
    staticFiles.location("/web");
    staticFiles.header("server", "Servotronix Motion Control");
    options("/*", (request, response) -> {

        String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
        if (accessControlRequestHeaders != null) {
          response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
        }

        String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
        if (accessControlRequestMethod != null) {
          response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
        }

        return "OK";
    });
    
    before((request, response) -> {
      response.header("Access-Control-Allow-Origin", "*");
    });
    
    /* TP MAPPING */
    MapTP.map(engine, MCInfo);
    
    /* CS MAPPING */
    MapCS.map(engine,MCInfo);
    MapCSUtils.map(engine,MCInfo);
    
    /* DriveStudio MAPPING */
    MapDriveStudio.map(engine, MCInfo);
    
    /* File Mapping */
    MapFileOperations.map();

    /* Cabinet Update MAPPING */
    MapCabinetUpdate.map(engine, MCInfo);
    
    /* Genral Mappings */
    get("/ver", (req, res) -> {
      return MCDefs.VER;
    });
  }
    
}
