package com.servotronix.serverMapping;

import static MCAuth.TokenManager.*;
import com.servotronix.MCInfo.MCInfo;
import com.servotronix.mcwebserver.FreeMarkerEngine;
import static com.servotronix.mcwebserver.MCDefs.MC_FOLDER;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import org.eclipse.jetty.server.Response;
import static spark.Spark.post;

public class MapTP {
  
  public static void map(FreeMarkerEngine engine, MCInfo info) {
    
    post("/tp/pallet/", (req, res) -> {
      if (!hasPublicAPIAccess(req)) { //TODO: CHANGE TO PRIVATE!!!
        res.status(Response.SC_FORBIDDEN);
        return "";
      }
      String text = req.queryParams("palletData");
      String fileName = req.queryParams("fileName");
      if (text == null) {
        res.status(Response.SC_BAD_REQUEST);
        return "";
      }
      if (fileName == null || !fileName.contains(".DAT")) {
        int i = 0;
        File f = new File(MC_FOLDER + "PLT_" + i + ".DAT");
        while (f.exists() && i < 100) {
          f = new File(MC_FOLDER + "PLT_" + i + ".DAT");
          i++;
        }
        if (i < 100) { // FILE NAME DOESN'T EXIST
          PrintWriter writer = new PrintWriter(f, "UTF-8");
          writer.println(text);
          writer.close();
          return f.getName();
        }
      } else {
        PrintWriter writer = new PrintWriter(new FileOutputStream(MC_FOLDER + fileName, false));
        writer.println(text);
        writer.close();
        return fileName;
      }
      return "";
    });
    
    post("/tp/pallet/:name", (req, res) -> {
      res.type("application/json");
      if (!hasPrivateAPIAccess(req)) {
        res.status(Response.SC_FORBIDDEN);
        return false;
      }
      String name = req.params(":name");
      String text = req.queryParams("palletData");
      if (text == null || name == null) {
        res.status(Response.SC_BAD_REQUEST);
        return false;
      }
      File f = new File(MC_FOLDER + name);
      if (!f.exists())
        return false;
      PrintWriter writer = new PrintWriter(new FileOutputStream(f,false));
      writer.println(text);
      writer.close();
      return true;
    });
    
  }
  
}
