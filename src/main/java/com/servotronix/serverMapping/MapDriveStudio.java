package com.servotronix.serverMapping;

import MCAuth.TokenManager;
import static MCAuth.MCAuth.verifyToken;
import com.servotronix.MCInfo.MCInfo;
import com.servotronix.mcwebserver.FreeMarkerEngine;
import com.servotronix.mcwebserver.MCDefs;
import com.servotronix.mcwebserver.Utils;

import static com.servotronix.mcwebserver.MCErrors.ERROR_BAD_REQUEST;
import static com.servotronix.mcwebserver.MCErrors.ERROR_UNKOWN;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.eclipse.jetty.server.Response;
import org.json.JSONObject;
import static spark.Spark.before;
import static spark.Spark.after;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.staticFiles;
import static spark.Spark.halt;

public class MapDriveStudio {

  private static void printResponse(spark.Response res) {
    String result = "";
    for (String s : res.raw().getHeaderNames()) {
      result += s + " >> " + res.raw().getHeader(s) + "\n";
    }
    
    System.out.println("Res:\n" + result);
  }
  
  public static void map(FreeMarkerEngine engine, MCInfo info) {

    /*
      THIS TRICK IS BECAUSE SPARK CONSUME STATIC ADDRESSES
    */
    before((req,res)->{
      String p =  req.pathInfo();
      boolean isDriveHome = p.equals("/drive") || p.equals("/drive/");
      boolean isCsHome = p.equals("/rs") || p.equals("/rs/");
      String paramJWT = null;
      if (isDriveHome || isCsHome) {
        paramJWT = req.queryParams("t");
        if (paramJWT != null) {
          paramJWT = paramJWT.trim();
        }
        String token = TokenManager.generateToken(); // TO AUTHENTICATE THIS SESSION
        if (token == null)
          return;
        staticFiles.header("Cache-Control", "no-store");
        staticFiles.header("ETag", "" + System.currentTimeMillis());
        req.session().attribute("securityToken",token);
        res.cookie("token", token);
      } else {
        staticFiles.header("Cache-Control", "public, max-age=86400, s-max-age=86400");
        staticFiles.header("ETag", MCDefs.VER);
      }
    });
    
    after("/drive/*",(req,res)->{
      if (res.type() != null || res.body() != null)
        return;
      String token = TokenManager.generateToken(); // TO AUTHENTICATE THIS SESSION
      if (token == null)
        return;
      req.session().attribute("securityToken",token);
      res.cookie("token", token);
      res.body(MapCS.getFileContent(MCDefs.EXT_WWW_FOLDER + "drive/index.html"));
      res.status(200);
    });

    post("/drive/api/send", (req, res) -> {
      res.type("application/json");
      // check permission
      String token = req.headers("Authorization");
      if (token == null) {
        res.status(Response.SC_UNAUTHORIZED);
        return false;
      }
      token = token.split(" ")[1];
      int permission = verifyToken(token);
      if (permission != 0 && permission != 99) {
        res.status(Response.SC_UNAUTHORIZED);
        return false;
      }
      JSONObject data = new JSONObject(req.body());
      String path = data.getString("path");
      String ip = data.getString("ip");
      if (ip==null || path==null) 
        return false;
      File file = new File(MCDefs.MC_FOLDER+path);
      if (!file.exists()) return false;
      FTPClient ftp = new FTPClient();
      try {
        ftp.connect(ip);
        int reply = ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply) || !ftp.login("user", "password")) {
          System.out.println("CAN'T CONNECT TO DRIVE FTP");
          ftp.disconnect();
          res.status(Response.SC_INTERNAL_SERVER_ERROR);
          return false;
        }
        final InputStream in = new FileInputStream(file);
        ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
        boolean result = ftp.storeFile("user.cdc", in);
        in.close();
        ftp.disconnect();
        return result;
      } catch (Exception e) {
        System.out.println("CAN'T CONNECT TO DRIVE FTP");
        ftp.disconnect();
        return false;
      }
    });

    post("/drive/api/copyToMC", (req, res) -> {
      res.type("application/json");
      // check permission
      String token = req.headers("Authorization");
      if (token == null) {
        res.status(Response.SC_UNAUTHORIZED);
        return false;
      }
      token = token.split(" ")[1];
      int permission = verifyToken(token);
      if (permission != 0 && permission != 99) {
        res.status(Response.SC_UNAUTHORIZED);
        return false;
      }
      JSONObject data = new JSONObject(req.body());
      String ip = data.getString("ip");
      String driveFile = data.getString("driveFile");
      String path = data.getString("path");
      if (ip==null || driveFile==null || path==null) 
        return false;
      FTPClient ftp = new FTPClient();
      ftp.connect(ip);
      int reply = ftp.getReplyCode();
      if (!FTPReply.isPositiveCompletion(reply) || !ftp.login("user", "password")) {
        ftp.disconnect();
        res.status(Response.SC_INTERNAL_SERVER_ERROR);
        return false;
      }
      ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
      File folder = new File(MCDefs.SC_FOLDER);
      folder.mkdirs();
      File f = new File(MCDefs.MC_FOLDER + path);
      f.delete();
      System.out.println("Getting " + MCDefs.MC_FOLDER + path);
      FileOutputStream fos = new FileOutputStream(f);
      boolean result = ftp.retrieveFile(driveFile, fos);
      System.out.println("Got file from drive (" + driveFile + "): " + result);
      if (fos != null) {
        fos.flush();
        fos.getFD().sync();
        fos.close();
      }
      ftp.disconnect();
      return result;
    });
    
    post("/drive/api/upload", "multipart/form-data", (request, response) -> {
      response.type("application/json");
      JSONObject uploadResponse = new JSONObject();
      uploadResponse.put("success", false);
      boolean overwrite = "true".equals(request.queryParams("overwrite")); // OVERRIDE IF EXISTS
      long maxFileSize = 100000000;     // the maximum size allowed for uploaded files
      long maxRequestSize = 100000000;  // the maximum size allowed for multipart/form-data requests
      int fileSizeThreshold = 1024;    // the size threshold after which files will be written to disk
      MultipartConfigElement multipartConfigElement = new MultipartConfigElement(MCDefs.MC_FOLDER, maxFileSize, maxRequestSize, fileSizeThreshold);
      request.raw().setAttribute("org.eclipse.jetty.multipartConfig",multipartConfigElement);
      Collection<Part> parts = request.raw().getParts();
      Part uploadedFile = request.raw().getPart("file");
      FTPClient ftp = new FTPClient();
      String ip = request.queryParams("ip");
      if (ip == null) {
        response.status(Response.SC_BAD_REQUEST);
        uploadedFile.delete();
        uploadedFile.delete();
        uploadedFile = null;
        multipartConfigElement = null;
        parts = null;
        return null;
      }
      try {
        String fName = request.raw().getPart("file").getSubmittedFileName();
        ftp.connect(ip);
        int reply = ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply) || !ftp.login("user", "password")) {
          ftp.disconnect();
          response.status(Response.SC_INTERNAL_SERVER_ERROR);
          return null;
        }
        try (final InputStream in = uploadedFile.getInputStream()) {
          ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
          ftp.storeFile(fName, in);
          in.close();
          uploadResponse.put("success",true);
        } catch (IOException e) {
          System.out.println("/drive/api/upload - Error with InputStream: " + e.getMessage());
          uploadResponse.put("err",ERROR_UNKOWN);
        } finally {
          uploadedFile.delete();
          uploadedFile = null;
          multipartConfigElement = null;
          parts = null;
          ftp.disconnect();
          return uploadResponse;
        }
      } catch (Exception e) {
        uploadedFile.delete();
        uploadedFile = null;
        multipartConfigElement = null;
        parts = null;
        uploadResponse.put("err",ERROR_BAD_REQUEST);
        return uploadResponse;
      }
    });
    
    get("/drive/api/file/:filename", (req, res) -> {
      String name = req.params(":filename");
      String ip = req.queryParams("ip");
      if (name == null || ip == null) {
        res.status(Response.SC_BAD_REQUEST);
        return "";
      }
      FTPClient ftp = new FTPClient();
      ftp.connect(ip);
      int reply = ftp.getReplyCode();
      if (!FTPReply.isPositiveCompletion(reply) || !ftp.login("user", "password")) {
        ftp.disconnect();
        res.status(Response.SC_INTERNAL_SERVER_ERROR);
        return "";
      }
      try {
        InputStream fis = ftp.retrieveFileStream(name);
        BufferedInputStream inbf = new BufferedInputStream(fis);
        byte buffer[] = new byte[1024];
        int readCount;
        byte result[] = null;
        int length = 0;
        StringBuilder builder = new StringBuilder();
        while((readCount = inbf.read(buffer)) > 0) {
          builder.append(new String(buffer,0,readCount));
        }
        ftp.disconnect();
        return builder.toString();
      } catch (Exception e) {
        res.status(Response.SC_NOT_FOUND);
        ftp.disconnect();
        e.printStackTrace();
      }
      ftp.disconnect();
      return "";
    });
    
  }
}
