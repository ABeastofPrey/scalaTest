package com.servotronix.serverMapping;

import MCAuth.DatabaseOperations;
import static MCAuth.MCAuth.verifyToken;
import static MCAuth.TokenManager.hasPrivateAPIAccess;
import static MCAuth.TokenManager.hasPublicAPIAccess;
import com.servotronix.mcwebserver.MCDefs;
import com.servotronix.mcwebserver.Utils;

import static com.servotronix.mcwebserver.MCDefs.ALLOWED_EXTENSIONS;
import static com.servotronix.mcwebserver.MCDefs.ALLOWED_FW_EXTENSIONS;
import static com.servotronix.mcwebserver.MCDefs.BACKUPS;
import static com.servotronix.mcwebserver.MCDefs.MC_FOLDER;
import static com.servotronix.mcwebserver.MCDefs.PKGD_PATH;
import static com.servotronix.mcwebserver.MCDefs.ZIPFILE_PATH;
import static com.servotronix.mcwebserver.MCDefs.ZIPFILE_SYS_PATH;
import static com.servotronix.mcwebserver.MCErrors.*;
import com.servotronix.mcwebserver.ZipMaker;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Part;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Response;
import org.ini4j.Ini;
import org.ini4j.IniPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.post;

public class MapFileOperations {
  
  private static ZipMaker zipMaker = new ZipMaker(MC_FOLDER);
  private static boolean stopSearch = false;
  
  public static void map() {
    
    // CREATE DIRS
    File backupDir = new File(BACKUPS);
    if (!backupDir.exists()) {
      backupDir.mkdir();
    }
    
    post("/cs/api/folder", (req, res) -> {
      res.type("application/json");
      if (!hasPublicAPIAccess(req)) {
        res.status(Response.SC_FORBIDDEN);
        return false;
      }
      JSONObject data = new JSONObject(req.body());
      String path = data.getString("path");
      if (path==null || path.contains("..")) 
        return false;
      path = path.toUpperCase();
      return new File(MC_FOLDER + path).mkdir();
    });
    
    post("/cs/api/move", (req, res) -> {
      res.type("application/json");
      if (!hasPublicAPIAccess(req)) {
        res.status(Response.SC_FORBIDDEN);
        return false;
      }
      JSONObject data = new JSONObject(req.body());
      String target = data.getString("target");
      String files = data.getString("files");
      if (target==null || files==null) 
        return false;
      target = target.replaceAll("\\$\\$","/") + "/";
      String[] fileArr = files.split(",");
      boolean result = true;
      try {
        for (String f : fileArr) {
          f = f.replaceAll("\\$\\$","/");
          File file = new File(MC_FOLDER+f);
          Path src = Paths.get(MC_FOLDER+f);
          Path fileTarget = Paths.get(MC_FOLDER + target + file.getName());
          if (file.exists()) {
            Files.move(src,fileTarget);
          }
        }
        return true;
      } catch (Exception e) {
        return false;
      }
    });
    
    post("/cs/api/copy", (req, res) -> {
      res.type("application/json");
      if (!hasPublicAPIAccess(req)) {
        res.status(Response.SC_FORBIDDEN);
        return false;
      }
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
      String from = data.getString("from");
      String to = data.getString("to");
      if (from==null || to==null) 
        return false;
      try {
        File file = new File(MC_FOLDER+from);
        File dest = new File(MC_FOLDER+to);
        if (file.exists()) {
          if (file.isDirectory()) {
            FileUtils.copyDirectory(file, dest);
            return dest.exists();
          } else {
            Path src = Paths.get(MC_FOLDER+from);
            Path fileTarget = Paths.get(MC_FOLDER + to);
            Files.copy(src,fileTarget);
            return true;
          }
        }
        return false;
      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
    });
    
    post("/cs/api/upload", "multipart/form-data", (request, response) -> {
      response.type("application/json");
      JSONObject uploadResponse = new JSONObject();
      uploadResponse.put("success", false);
      if (!hasPublicAPIAccess(request)) {
        response.status(Response.SC_FORBIDDEN);
        uploadResponse.put("err",ERROR_PERMISSION_DENIED);
        return uploadResponse;
      }
      String token = request.queryParams("token"); // USER TOKEN
      boolean keepLetterCases = "true".equals(request.queryParams("keepCase"));
      boolean overwrite = "true".equals(request.queryParams("overwrite")); // OVERRIDE IF EXISTS
      String path = request.queryParams("path"); // FILE WILL BE PUT IN SSMC/[PATH]
      int permission = verifyToken(token);
      if (permission == -1 || path == null || path.contains("..") || token == null) {
        System.out.println("Bad request: /cs/api/upload - permission:" + permission + ", path: " + path + ", token: " + token);
        response.status(Response.SC_FORBIDDEN);
        uploadResponse.put("err",ERROR_BAD_REQUEST);
        return uploadResponse;
      }
      long maxFileSize = 10000000;       // the maximum size allowed for uploaded files
      long maxRequestSize = 100000000;  // the maximum size allowed for multipart/form-data requests
      int fileSizeThreshold = 1024;     // the size threshold after which files will be written to disk
      MultipartConfigElement multipartConfigElement = new MultipartConfigElement(MCDefs.MC_FOLDER, maxFileSize, maxRequestSize, fileSizeThreshold);
      request.raw().setAttribute("org.eclipse.jetty.multipartConfig",multipartConfigElement);
      Collection<Part> parts = request.raw().getParts();
      String fName = "";
      Part uploadedFile = request.raw().getPart("file");
      try {
        fName = request.raw().getPart("file").getSubmittedFileName();
        if (!keepLetterCases) {
          fName = fName.toUpperCase();
        }
        // HANDLE FWCONFIG
        if ("FWCONFIG".equals(fName)) {
          // CHECK USER IS AN ADMIN
          if (permission > 0 && permission != 99) {
            response.status(Response.SC_FORBIDDEN);
            uploadResponse.put("err",ERROR_PERMISSION_DENIED);
            uploadedFile.delete();
            uploadedFile = null;
            return uploadResponse;
          }
          // TRY TO OVERWRITE FWCONFIG
          Path out = Paths.get(MCDefs.FWCONFIG);
          try (final InputStream in = uploadedFile.getInputStream()) {
            Utils.safeCopy(in, out, true);
            uploadResponse.put("success",true);
            DatabaseOperations.log(token, "upload;" + fName);
          } catch (IOException e) {
            System.out.println("/cs/api/upload - Error with InputStream: " + e.getMessage());
            uploadResponse.put("err",ERROR_UNKOWN);
          } finally {
            uploadedFile.delete();
            uploadedFile = null;
            multipartConfigElement = null;
            parts = null;
            return uploadResponse;
          }
        }
        // NOT FWCONFIG...
        String ext = fName.substring(fName.lastIndexOf(".")+1);
        if (!Arrays.asList(ALLOWED_EXTENSIONS).contains(ext.toUpperCase())) {
          response.status(Response.SC_FORBIDDEN);
          uploadResponse.put("err",ERROR_BAD_REQUEST);
          uploadedFile.delete();
          uploadedFile = null;
          return uploadResponse;
        }
        Path out = Paths.get(MCDefs.MC_FOLDER + path + fName);
        boolean success = false;
        try (final InputStream in = uploadedFile.getInputStream()) {
          Utils.safeCopy(in, out, overwrite);
          success = true;
        } catch (IOException e) {
          System.out.println("/cs/api/upload - Error with InputStream: " + e.getMessage());
          //e.printStackTrace();
          uploadResponse.put("err",ERROR_UNKOWN);
        } finally {
          uploadedFile.delete();
          uploadedFile = null;
          multipartConfigElement = null;
          parts = null;
          if (success) {
            DatabaseOperations.log(token, "upload;" + fName);
            uploadResponse.put("success",true);
          }
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
    
    post("/cs/api/uploadRec", "multipart/form-data", (request, response) -> {
      response.type("application/json");
      JSONObject uploadResponse = new JSONObject();
      uploadResponse.put("success", false);
      if (!hasPublicAPIAccess(request)) {
        response.status(Response.SC_FORBIDDEN);
        uploadResponse.put("err",ERROR_PERMISSION_DENIED);
        return uploadResponse;
      }
      String token = request.queryParams("token"); // USER TOKEN
      int permission = verifyToken(token);
      if (permission == -1 || token == null) {
        response.status(Response.SC_FORBIDDEN);
        uploadResponse.put("err",ERROR_BAD_REQUEST);
        return uploadResponse;
      }
      long maxFileSize = 10000000;       // the maximum size allowed for uploaded files
      long maxRequestSize = 100000000;  // the maximum size allowed for multipart/form-data requests
      int fileSizeThreshold = 1024;     // the size threshold after which files will be written to disk
      MultipartConfigElement multipartConfigElement = new MultipartConfigElement(MCDefs.MC_FOLDER, maxFileSize, maxRequestSize, fileSizeThreshold);
      request.raw().setAttribute("org.eclipse.jetty.multipartConfig",multipartConfigElement);
      Collection<Part> parts = request.raw().getParts();
      String fName = "";
      Part uploadedFile = request.raw().getPart("file");
      try {
        fName = request.raw().getPart("file").getSubmittedFileName().toUpperCase();
        String ext = fName.substring(fName.lastIndexOf(".")+1);
        if (!ext.equals("REC")) {
          response.status(Response.SC_FORBIDDEN);
          uploadResponse.put("err",ERROR_BAD_REQUEST);
          uploadedFile.delete();
          uploadedFile = null;
          return uploadResponse;
        }
        Path out = Paths.get(MCDefs.REC_FOLDER + fName);
        boolean success = false;
        try (final InputStream in = uploadedFile.getInputStream()) {
          Utils.safeCopy(in, out, true);
          success = true;
        } catch (IOException e) {
          uploadResponse.put("err",ERROR_UNKOWN);
        } finally {
          uploadedFile.delete();
          uploadedFile = null;
          multipartConfigElement = null;
          parts = null;
          if (success) {
            DatabaseOperations.log(token, "upload;" + fName);
            uploadResponse.put("success",true);
          }
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
    
    post("/cs/upload", "multipart/form-data", (request, response) -> {
      response.type("application/json");
      JSONObject uploadResponse = new JSONObject();
      uploadResponse.put("success", false);
      if (!hasPublicAPIAccess(request)) {
        response.status(Response.SC_FORBIDDEN);
        uploadResponse.put("err",ERROR_PERMISSION_DENIED);
        return uploadResponse;
      }
      String token = request.raw().getParameter("token"); // USER TOKEN
      long maxFileSize = 10000000;       // the maximum size allowed for uploaded files
      long maxRequestSize = 100000000;  // the maximum size allowed for multipart/form-data requests
      int fileSizeThreshold = 1024;     // the size threshold after which files will be written to disk
      MultipartConfigElement multipartConfigElement = new MultipartConfigElement(MCDefs.MC_FOLDER, maxFileSize, maxRequestSize, fileSizeThreshold);
      request.raw().setAttribute("org.eclipse.jetty.multipartConfig",multipartConfigElement);
      Collection<Part> parts = request.raw().getParts();
      String fName = "";
      Part uploadedFile = request.raw().getPart("file");
      try {
        fName = request.raw().getPart("file").getSubmittedFileName().toUpperCase();
        String ext = fName.substring(fName.lastIndexOf(".")+1);
        if (!Arrays.asList(ALLOWED_EXTENSIONS).contains(ext)) {
          response.status(Response.SC_FORBIDDEN);
          uploadResponse.put("err",ERROR_BAD_REQUEST);
          uploadedFile.delete();
          uploadedFile = null;
          return uploadResponse;
        }
        Path out = Paths.get(MCDefs.MC_FOLDER + fName);
        boolean success = false;
        try (final InputStream in = uploadedFile.getInputStream()) {
          Utils.safeCopy(in, out, false);
          success = true;
        } catch (IOException e) {
          System.out.println("/cs/upload - Error with InputStream: " + e.getMessage());
          if (out.toFile().exists())
            uploadResponse.put("err",ERROR_FILE_EXISTS);
          else
            uploadResponse.put("err",ERROR_UNKOWN);
        } finally {
          uploadedFile.delete();
          uploadedFile = null;
          multipartConfigElement = null;
          parts = null;
          if (success) {
            DatabaseOperations.log(token, "upload;" + fName);
            uploadResponse.put("success",true);
          }
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
    
    post("/cs/upload/overwrite", "multipart/form-data", (request, response) -> {
      response.type("application/json");
      JSONObject uploadResponse = new JSONObject();
      uploadResponse.put("success", false);
      if (!hasPublicAPIAccess(request)) {
        response.status(Response.SC_FORBIDDEN);
        uploadResponse.put("err",ERROR_PERMISSION_DENIED);
        return uploadResponse;
      }
      String token = request.raw().getParameter("token"); // USER TOKEN
      long maxFileSize = 10000000;       // the maximum size allowed for uploaded files
      long maxRequestSize = 100000000;  // the maximum size allowed for multipart/form-data requests
      int fileSizeThreshold = 1024;     // the size threshold after which files will be written to disk
      MultipartConfigElement multipartConfigElement = new MultipartConfigElement(MCDefs.MC_FOLDER, maxFileSize, maxRequestSize, fileSizeThreshold);
      request.raw().setAttribute("org.eclipse.jetty.multipartConfig",multipartConfigElement);
      Collection<Part> parts = request.raw().getParts();
      String fName = "";
      Part uploadedFile = request.raw().getPart("file");
      boolean success = false;
      try {
        fName = request.raw().getPart("file").getSubmittedFileName().toUpperCase();
        String ext = fName.substring(fName.lastIndexOf(".")+1);
        if (!Arrays.asList(ALLOWED_EXTENSIONS).contains(ext)) {
          response.status(Response.SC_FORBIDDEN);
          uploadResponse.put("err",ERROR_BAD_REQUEST);
          uploadedFile.delete();
          uploadedFile = null;
          return uploadResponse;
        }
        Path out = Paths.get(MCDefs.MC_FOLDER + fName);
        try (final InputStream in = uploadedFile.getInputStream()) {
          Utils.safeCopy(in, out, true);
          success = true;
        } catch (IOException e) {
          System.out.println("/cs/upload/overwrite - Error with InputStream: " + e.getMessage());
          uploadResponse.put("err", ERROR_UNKOWN);
        } finally {
          uploadedFile.delete();
          uploadedFile = null;
          multipartConfigElement = null;
          parts = null;
          if (success) {
            DatabaseOperations.log(token, "upload;" + fName);
            uploadResponse.put("success",true);
          }
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
    
    post("/cs/firmware", "multipart/form-data", (request, response) -> {
      response.type("application/json");
      JSONObject uploadResponse = new JSONObject();
      uploadResponse.put("success", false);
      if (!hasPublicAPIAccess(request)) {
        response.status(Response.SC_FORBIDDEN);
        uploadResponse.put("err",ERROR_PERMISSION_DENIED);
        return uploadResponse;
      }
      String token = request.raw().getParameter("token"); // USER TOKEN
      String path = request.queryParams("path"); // FILE WILL BE PUT IN SSMC/[PATH] (unless abs is true)
      int permission = verifyToken(token);
      if ((permission != 0 && permission != 99) || (path != null && path.contains(".."))) {
        System.out.println("Bad request: /cs/firmware - permission:" + permission + ", path: " + path + ", token: " + token);
        response.status(Response.SC_FORBIDDEN);
        uploadResponse.put("err",ERROR_BAD_REQUEST);
        return uploadResponse;
      }
      String absPath = path == null ? MCDefs.MC_FOLDER : path;
      long maxFileSize = 300000000;       // the maximum size - 300MB
      long maxRequestSize = 300000000;  // the maximum size allowed for multipart/form-data - 300MB
      int fileSizeThreshold = 1024;     // the size threshold after which files will be written to disk
      MultipartConfigElement multipartConfigElement = new MultipartConfigElement(absPath, maxFileSize, maxRequestSize, fileSizeThreshold);
      request.raw().setAttribute("org.eclipse.jetty.multipartConfig",multipartConfigElement);
      Collection<Part> parts = request.raw().getParts();
      String fName = "";
      Part uploadedFile = request.raw().getPart("file");
      boolean success = false;
      try {
        fName = request.raw().getPart("file").getSubmittedFileName();
        boolean isValidExtension = false;
        for (int i=0; i<ALLOWED_FW_EXTENSIONS.length; i++) {
          if (fName.endsWith(ALLOWED_FW_EXTENSIONS[i])) {
            isValidExtension = true;
            break;
          }
        }
        if (!isValidExtension) {
          response.status(Response.SC_FORBIDDEN);
          uploadResponse.put("err",ERROR_BAD_REQUEST);
          uploadedFile.delete();
          uploadedFile = null;
          return uploadResponse;
        }
        Path out = Paths.get(absPath + fName);
        try (final InputStream in = uploadedFile.getInputStream()) {
          Utils.safeCopy(in, out, true);
          success = true;
          try {
            new File(PKGD_PATH).delete();
          } catch (Exception e) {

          }
        } catch (IOException e) {
          System.out.println("/cs/firmware - Error with InputStream: " + e.getMessage());
          uploadResponse.put("err", ERROR_UNKOWN);
        } finally {
          uploadedFile.delete();
          uploadedFile = null;
          multipartConfigElement = null;
          parts = null;
          if (success) {
            DatabaseOperations.log(token, "firmware;" + fName);
            uploadResponse.put("success",true);
          }
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
  
    get("/cs/file/:filename", (req, res) -> {

      String filename = req.params(":filename");
      if (filename == null)
        return "";
      filename = filename.toUpperCase();

      // SPECIAL CASE - JSON and DAT FILES (FOR LANGUAGE)
      boolean specialCase = false;
      if (filename.endsWith(".JSON") || filename.endsWith(".DAT")) {
        specialCase = true;
      }
      
      if (!hasPublicAPIAccess(req) && !specialCase) {
        res.status(Response.SC_FORBIDDEN);
        return "";
      }
      if (filename.equals("FWCONFIG")) {
        // CHECK PERMISSION
        String token = req.headers("Authorization");
        if (token == null) {
          res.status(Response.SC_UNAUTHORIZED);
          return false;
        }
        token = token.split(" ")[1];
        int permission = verifyToken(token);
        if (permission > 0 && permission < 99) { // NOT ADMIN
          res.status(Response.SC_FORBIDDEN);
          return "";
        }
        try {
          String content = new String(Files.readAllBytes(Paths.get(MCDefs.FWCONFIG)),StandardCharsets.UTF_8).replaceAll("\0","");
          return content;
        } catch (Exception e) {
          //System.out.println(e.getMessage());
          return "";
        }
      }
      try {
        String content = new String(Files.readAllBytes(Paths.get(MCDefs.MC_FOLDER + filename)),StandardCharsets.UTF_8).replaceAll("\0","");
        return content;
      } catch (Exception e) {
        //System.out.println(e.getMessage());
        return "";
      }
    });
    
    get("/cs/path", (req, res) -> {
      // TODO: BLOCK ".." REQUESTS (i.e: ../FWCONFIG)
      if (!hasPublicAPIAccess(req)) {
        res.status(Response.SC_FORBIDDEN);
        System.out.println("Invalid Request");
        return "";
      }
      String filename = req.queryParams("path");
      if (filename == null) {
        System.out.println("Invalid filename:" + filename);
        return "";
      }
      filename = filename.toUpperCase();
      try {
        String content = new String(Files.readAllBytes(Paths.get(MCDefs.MC_FOLDER + filename)),StandardCharsets.UTF_8).replaceAll("\0","");
        return content;
      } catch (Exception e) {
        //System.out.println(e.getMessage());
        return "";
      }
    });
    
    delete("/cs/file/:filename", (req, res) -> {
      res.type("application/json");
      if (!hasPublicAPIAccess(req)) {
        res.status(Response.SC_FORBIDDEN);
        return false;
      }
      String usertoken = req.queryParams("token");
      String filename = req.params(":filename");
      if (filename == null)
        return false;
      filename = filename.replaceAll("\\$\\$","/").toUpperCase();
      String filenameLower;
      int i = filename.lastIndexOf("/");
      if (i>0) {
        filenameLower = filename.substring(0,i) + filename.substring(i).toLowerCase();
      } else {
        filenameLower = filename.toLowerCase();
      }
      Path path;
      boolean success = false;
      try {
        path = Paths.get(MCDefs.MC_FOLDER + filename);
        System.out.print("TRYING TO DELETE " + path.toString() + "...");
        success = Files.deleteIfExists(path);
        System.out.println(success ? "SUCCESS!" : "FAILED!");
        if (success) {
          DatabaseOperations.log(usertoken, "file_del;" + filename);
          return true;
        }
      } catch (Exception e) {
        System.out.println(e.getMessage());
        if (success)
          return true;
      }
      success = false;
      try {
        path = Paths.get(MCDefs.MC_FOLDER + filenameLower);
        System.out.print("TRYING TO DELETE " + path.toString() + "...");
        success = Files.deleteIfExists(path);
        System.out.println(success ? "SUCCESS!" : "FAILED!");
        if (success) {
          DatabaseOperations.log(usertoken, "file_del;" + filenameLower);
          return true;
        }
      } catch (Exception e) {
        System.out.println(e.getMessage());
        if (success)
          return true;
      }
      return false;
    });

    get("/cs/api/rawFile", (req, res) -> {
      String filename = req.queryParams("path");
      if (filename == null)
        return false;
      res.raw().setContentType("application/octet-stream");
      try {
        ServletOutputStream out = res.raw().getOutputStream();
        FileInputStream fis = new FileInputStream(MC_FOLDER + filename);
        byte[] buffer = new byte[1024];
        res.header("Content-Disposition", "attachment; filename=\""+filename+"\"");
        int len;
        while ((len = fis.read(buffer)) > 0) {
          out.write(buffer,0,len);
        }
        fis.close();
        out.flush();
        out.close();
        return true;
      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
    });
    
    get("/cs/api/pkgd", (req, res) -> {
      if (!hasPublicAPIAccess(req)) {
        res.status(Response.SC_FORBIDDEN);
        return false;
      }
      try {
        String content = 
          new String(Files.readAllBytes(Paths.get(MCDefs.PKGD_PATH)),StandardCharsets.UTF_8);
        return content.indexOf("successfully") > 0;
      } catch (Exception e) {
        // file doesn't exist - no error was found
        return true;
      }
    });
    
    get("/cs/api/files", (req, res) -> {
      if (!hasPublicAPIAccess(req)) {
        res.status(Response.SC_FORBIDDEN);
        return "";
      }
      
      try {
        List<String> extensions = null;
        if (req.queryParams("ext") != null) {
           extensions = Arrays.asList(req.queryParams("ext").toUpperCase().split(","));
        } else {
          //extensions = Arrays.asList("LIB,PRG,UPG,ULB,DAT,DEF,ERR,LOG,VAR,XML,INI,JSON,BLK,TPS,TXT,LOG,BKG,SIM".split(","));
          extensions = null;
        }
        res.type("application/json");
        return PathToJSON("", extensions, req.queryParams("baseFolder"));
      } catch (Exception e) {
        System.out.println(e.getMessage());
        return "";
      }
    });

    /* SEARCH FOR A STRING IN A FILE */
    get("/cs/api/search", (req, res) -> {
      
      if (!hasPublicAPIAccess(req)) {
        res.status(Response.SC_FORBIDDEN);
        return "";
      }

      String text = req.queryParams("search");
      String path = req.queryParams("path");
      if (text == null || path == null) {
        res.status(Response.SC_BAD_REQUEST);
        return "";
      }

      res.type("application/json");
      stopSearch = false;
      return searchInPath(text, path);    
      
    });

    /* ABORT SEARCH IN PATH */
    get("/cs/api/search/abort", (req, res) -> {
      if (!hasPublicAPIAccess(req)) {
        res.status(Response.SC_FORBIDDEN);
        return "";
      }
      stopSearch = true;
      res.type("application/json");
      return true;
    });
    
    /* 
        RETURNS ALL THE FILES IN THE CONTROLLER.
        DEPRECATED - USE /CS/API/FILES INSTEAD!
      */
    get("/cs/files", (req, res) -> {
      
      if (!hasPublicAPIAccess(req)) {
        res.status(Response.SC_FORBIDDEN);
        return "";
      }
      
      try {
        List<String> extensions = null;
        if (req.queryParams("ext") != null)
           extensions = Arrays.asList(req.queryParams("ext").toUpperCase().split(","));
        else
          extensions = Arrays.asList("LIB,PRG,UPG,ULB,DAT,DEF,ERR,LOG,VAR,XML".split(","));
        boolean returnAsJSON = Boolean.parseBoolean(req.queryParams("asJSON"));
        JSONObject result = new JSONObject();
        JSONArray fileArr = new JSONArray();
        int fileCount = 0;
        String ans = "";
        File[] files = new File(MC_FOLDER).listFiles();
        Arrays.sort(files);
        for (final File fileEntry : files) {
          String fileName = fileEntry.getName().toUpperCase();
          int index = fileName.lastIndexOf(".") + 1;
          if (index > 0) {
            String ext = fileName.substring(index);
            if (extensions.contains(ext)) {
              if (returnAsJSON) {
                JSONObject f = new JSONObject();
                f.put("fileName", fileName);
                f.put("modified", fileEntry.lastModified());
                f.put("fileNameOnly", fileName.substring(0,fileName.length()-ext.length()-1));
                f.put("extension", ext);
                fileArr.put(fileCount,f);
              } else {
                ans += fileEntry.getName().toUpperCase() + ",";
              }
              fileCount++;
            }
          }
        }
        if (returnAsJSON) {
          res.type("application/json");
          return fileArr;
        }
        ans = ans.substring(0,ans.length()-1);
        return ans;
      } catch (Exception e) {
        System.out.println(e.getMessage());
        return "";
      }
    });
    
    get("/cs/trnerr", (req, res) -> {
      if (!hasPrivateAPIAccess(req)) {
        res.status(Response.SC_FORBIDDEN);
        return "";
      }
      try {
        String content = new String(Files.readAllBytes(Paths.get(MCDefs.TRNERR)),StandardCharsets.UTF_8);
        return content;
      } catch (Exception e) {
        System.out.println(e.getMessage());
        return "";
      }
    });
    
    get("/cs/file/exists/:filename", (req, res) -> {
      if (!hasPublicAPIAccess(req)) {
        res.status(Response.SC_FORBIDDEN);
        return null;
      }
      res.type("application/json");
      String fileName = req.params(":filename");
      if (fileName == null)
        return false;
      fileName = fileName.toUpperCase();
      File f = new File(MC_FOLDER + fileName);
      return (f.exists() && !f.isDirectory());
    });
    
    post("/cs/mczip", (req, res) -> {
      res.type("application/json");
      if (!hasPrivateAPIAccess(req)) {
        res.status(Response.SC_FORBIDDEN);
        return false;
      }
      String files = req.queryParams("files");
      boolean singleFolder = "true".equals(req.queryParams("singleFolder"));
      if (files==null) {
        return zipMaker.createZip(null, false);
      }
      return zipMaker.createZip(files, singleFolder);
    });
    
    get("/cs/backup", (req, res) -> {
      res.type("application/json");
      if (!hasPrivateAPIAccess(req)) {
        res.status(Response.SC_FORBIDDEN);
        return false;
      }
      String files = req.queryParams("files");
      String name = req.queryParams("name");
      if (files==null || name==null) 
        return false;
      return zipMaker.createBackup(name,files);
    });
    
    post("/cs/restore/file/", "multipart/form-data", (request, response) -> {
      response.type("application/json");
      JSONObject uploadResponse = new JSONObject();
      uploadResponse.put("success", false);
      if (!hasPrivateAPIAccess(request)) {
        response.status(Response.SC_FORBIDDEN);
        uploadResponse.put("err",ERROR_PERMISSION_DENIED);
        return uploadResponse;
      }
      long maxFileSize = 100000000;       // the maximum size - 100MB
      long maxRequestSize = 100000000;  // the maximum size allowed for multipart/form-data - 100MB
      int fileSizeThreshold = 1024;     // the size threshold after which files will be written to disk
      MultipartConfigElement multipartConfigElement = new MultipartConfigElement(MCDefs.MC_FOLDER, maxFileSize, maxRequestSize, fileSizeThreshold);
      request.raw().setAttribute("org.eclipse.jetty.multipartConfig",multipartConfigElement);
      Collection<Part> parts = request.raw().getParts();
      String fName = "";
      Part uploadedFile = request.raw().getPart("file");
      boolean success = false;
      try {
        fName = request.raw().getPart("file").getSubmittedFileName().toUpperCase();
        String ext = fName.substring(fName.lastIndexOf(".")+1);
        if (!ext.equals("ZIP")) {
          response.status(Response.SC_FORBIDDEN);
          uploadResponse.put("err",ERROR_BAD_REQUEST);
          uploadedFile.delete();
          uploadedFile = null;
          return uploadResponse;
        }
        Path out = Paths.get(MCDefs.BACKUPS + fName);
        try (final InputStream in = uploadedFile.getInputStream()) {
          Utils.safeCopy(in, out, true);
          success = zipMaker.restore(fName);
          Files.delete(out);
        } catch (IOException e) {
          System.out.println("/cs/restore/file/ - Error with InputStream: " + e.getMessage());
          uploadResponse.put("err", ERROR_UNKOWN);
        } finally {
          uploadedFile.delete();
          uploadedFile = null;
          multipartConfigElement = null;
          parts = null;
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
    
    get("/cs/restore/:name", (req, res) -> {
      res.type("application/json");
      if (!hasPrivateAPIAccess(req)) {
        res.status(Response.SC_FORBIDDEN);
        return false;
      }
      String name = req.params(":name");
      System.out.println("RESTORE " + name);
      if (name==null) 
        return false;
      return zipMaker.restore(name);
    });
    
    get("/cs/backup/list", (req, res) -> {
      res.type("application/json");
      JSONArray result = new JSONArray();
      if (!hasPrivateAPIAccess(req)) {
        res.status(Response.SC_FORBIDDEN);
        return result;
      }
      try {
        File dir = new File(BACKUPS);
        if (dir == null)
          return result;
        File[] files = dir.listFiles();
        Pair[] pairs = new Pair[files.length];
        for (int i = 0; i < files.length; i++)
          pairs[i] = new Pair(files[i]);
        Arrays.sort(pairs);
        for (int i = 0; i < files.length; i++) {
          try {
            files[i] = pairs[i].f;
            JSONObject backup = new JSONObject();
            String name = files[i].getName();
            String[] parts = name.split("\\$\\$");
            int index = parts[1].indexOf(".");
            long date = Long.parseLong(parts[1].substring(0, index));
            backup.put("name", name);
            backup.put("shortName", parts[0]);
            backup.put("date", date);
            result.put(backup);
          } catch (Exception e) {
            e.printStackTrace();
            System.out.println("INVALID BACKUP FILE:" + files[i].getName());
          }
        }
      } catch (Exception e) {
        System.out.println(e.getMessage());
      } finally {
        return result;
      }
    });
    
    get("/cs/backup/file/:name", (req,res) -> {
      String name = req.params(":name");
      String result = "";
      if (!hasPrivateAPIAccess(req)) {
        res.status(Response.SC_FORBIDDEN);
        return result;
      }
      if (name == null)
        return result;
      try {
      ZipFile zipFile = new ZipFile(MCDefs.BACKUPS+name);
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while(entries.hasMoreElements()){
        ZipEntry entry = entries.nextElement();
        result += entry.getName() + ",";
      }
      result = result.substring(0,result.length()-1); // TRIM LAST COMMA
      zipFile.close();
    } catch (IOException ex) {
      System.out.println(ex.getMessage());
    } finally {
      return result;
    }
    });
    
    delete("/cs/backup/file/:name", (req,res) -> {
      res.type("application/json");
      String name = req.params(":name");
      if (!hasPrivateAPIAccess(req)) {
        res.status(Response.SC_FORBIDDEN);
        return false;
      }
      if (name == null)
        return false;
      try {
      File file = new File(MCDefs.BACKUPS+name);
      return file.delete();
    } catch (Exception ex) {
      return false;
    }
    });
    
    get("/cs/api/zipProject/:name", (req, res) -> {
      String name = req.params(":name");
      if (name == null)
        return false;
      if (!zipMaker.createZipFromFolder(name))
          return false;
      res.raw().setContentType("application/zip");
      try {
        ServletOutputStream out = res.raw().getOutputStream();
        FileInputStream fis = new FileInputStream(ZIPFILE_PATH);
        byte[] buffer = new byte[1024];
        res.header("Content-Disposition", "attachment; filename=\""+name+".ZIP\"");
        int len;
        while ((len = fis.read(buffer)) > 0) {
          out.write(buffer,0,len);
        }
        fis.close();
        out.flush();
        out.close();
        return true;
      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
    });
    
    get("/cs/api/zipFile", (req, res) -> {
      res.raw().setContentType("application/zip");
      try {
        ServletOutputStream out = res.raw().getOutputStream();
        FileInputStream fis = new FileInputStream(ZIPFILE_PATH);
        byte[] buffer = new byte[4096];
        res.header("Content-Disposition", "attachment; filename=\"MCFiles.ZIP\"");
        int len;
        while ((len = fis.read(buffer)) > 0) {
          out.write(buffer,0,len);
        }
        fis.close();
        out.flush();
        out.close();
        return true;
      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
    });
    
    get("/cs/api/zipSysFile", (req, res) -> {
      res.raw().setContentType("application/zip");
      try {
        ServletOutputStream out = res.raw().getOutputStream();
        FileInputStream fis = new FileInputStream(ZIPFILE_SYS_PATH);
        byte[] buffer = new byte[4096];
        res.header("Content-Disposition", "attachment; filename=\"CSBugReport.ZIP\"");
        int len;
        while ((len = fis.read(buffer)) > 0) {
          out.write(buffer,0,len);
        }
        fis.close();
        out.flush();
        out.close();
        return true;
      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
    });
    
    post("/cs/api/verifyProject", "multipart/form-data", (request, response) -> {
      response.type("application/json");
      JSONObject uploadResponse = new JSONObject();
      uploadResponse.put("success", false);
      if (!hasPrivateAPIAccess(request)) {
        response.status(Response.SC_FORBIDDEN);
        uploadResponse.put("err",ERROR_PERMISSION_DENIED);
        return uploadResponse;
      }
      long maxFileSize = 100000000;       // the maximum size - 100MB
      long maxRequestSize = 100000000;  // the maximum size allowed for multipart/form-data - 100MB
      int fileSizeThreshold = 1024;     // the size threshold after which files will be written to disk
      MultipartConfigElement multipartConfigElement = new MultipartConfigElement(MCDefs.MC_FOLDER, maxFileSize, maxRequestSize, fileSizeThreshold);
      request.raw().setAttribute("org.eclipse.jetty.multipartConfig",multipartConfigElement);
      Collection<Part> parts = request.raw().getParts();
      String fName = "";
      Part uploadedFile = request.raw().getPart("file");
      boolean success = false;
      try {
        fName = request.raw().getPart("file").getSubmittedFileName().toUpperCase();
        String ext = fName.substring(fName.lastIndexOf(".")+1);
        if (!ext.equals("ZIP")) {
          response.status(Response.SC_FORBIDDEN);
          uploadResponse.put("err",ERROR_BAD_REQUEST);
          uploadedFile.delete();
          uploadedFile = null;
          return uploadResponse;
        }
        Path out = Paths.get(MCDefs.MC_FOLDER + fName);
        try (final InputStream in = uploadedFile.getInputStream()) {
          Utils.safeCopy(in, out, true);
          String projectName = zipMaker.verifyProject(fName);
          success = projectName != null;
          uploadResponse.put("success", success);
          uploadResponse.put("project", projectName);
          uploadResponse.put("file", fName);
          if (!success)
            Files.delete(out);
        } catch (IOException e) {
          System.out.println("/cs/api/verifyProject/ - Error with InputStream: " + e.getMessage());
          uploadResponse.put("err", ERROR_UNKOWN);
        } finally {
          uploadedFile.delete();
          uploadedFile = null;
          multipartConfigElement = null;
          parts = null;
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
    
    get("/cs/api/importProject", (request, response) -> {
      response.type("application/json");
      JSONObject res = new JSONObject();
      String fileName = request.queryParams("fileName");
      res.put("success", fileName != null ? zipMaker.unzipProject(fileName) : false);
      return res;
    });
    
    delete("/cs/api/projectZip", (request, response) -> {
      response.type("application/json");
      JSONObject res = new JSONObject();
      String fileName = request.queryParams("fileName");
      if (fileName == null || !fileName.endsWith(".ZIP")) {
        System.out.println("INVALID PROJECTZIP DELETION REQUEST");
        res.put("success", false);
        return res;
      }
      try {
        boolean result = new File(MCDefs.MC_FOLDER+fileName).delete();
        res.put("success", result);
      } catch (Exception e) {
        e.printStackTrace();
        res.put("success", false);
      }
      return res;
    });
    
    get("/cs/backup/download/:name", (req, res) -> {
      if (!hasPrivateAPIAccess(req)) {
        res.status(Response.SC_FORBIDDEN);
        return false;
      }
      String name = req.params(":name");
      if (name == null)
        return false;
      res.raw().setContentType("application/zip");
      try {
        ServletOutputStream out = res.raw().getOutputStream();
        FileInputStream fis = new FileInputStream(BACKUPS+name);
        String shortName = name.split("\\$\\$")[0];
        byte[] buffer = new byte[1024];
        res.header("Content-Disposition", "attachment; filename=\""+shortName+".ZIP\"");
        int len;
        while ((len = fis.read(buffer)) > 0) {
          out.write(buffer,0,len);
        }
        fis.close();
        out.flush();
        out.close();
        return true;
      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
    });
    
    post("/cs/api/bugreport", (req, res) -> {
      res.type("application/json");
      JSONObject data = new JSONObject(req.body());
      String token = req.headers("Authorization");
      if (token == null) {
        res.status(Response.SC_UNAUTHORIZED);
        return false;
      }
      token = token.split(" ")[1];
      int permission = verifyToken(token);
      if (permission == -1) {
        res.status(Response.SC_FORBIDDEN);
        return false;
      }
      String user = data.getString("user");
      String sysinfo = data.getString("info");
      String history = data.getString("history");
      if (user == null || sysinfo == null || history == null) {
        res.status(Response.SC_BAD_REQUEST);
        return false;
      }
      // CREATE ZIP FILE
      return zipMaker.createSysZip(user, sysinfo, history);
    });



    get("/cs/api/ini/ini2cdc", (req, res) -> {
      if (!hasPublicAPIAccess(req)) {
        res.status(Response.SC_FORBIDDEN);
        System.out.println("Invalid Request");
        return false;
      }
      String filename = req.queryParams("fileName");
      if (filename == null || filename.contains("/") || filename.contains("..")) {
        System.out.println("Invalid filename:" + filename);
        return false;
      }
      filename = filename.toUpperCase();
      if (!filename.endsWith(".INI")) {
        res.status(Response.SC_BAD_REQUEST);
        return false;
      }
      try {
        Process pr = Runtime.getRuntime().exec("cdc2ini -f " + MCDefs.SC_FOLDER + filename + " --cdc --nosim",null,new File("/FFS0/SSMC"));
        pr.waitFor();
        String file = filename.substring(0, filename.length() - 3) + "cdc";
        File f = new File(MCDefs.SC_FOLDER + file);
        return f.exists();
      } catch (Exception e) {
        System.out.println(e.getMessage());
        return false;
      }
    });

    get("/cs/api/ini/cdc2ini", (req, res) -> {
      if (!hasPublicAPIAccess(req)) {
        res.status(Response.SC_FORBIDDEN);
        System.out.println("Invalid Request");
        return false;
      }
      String filename = req.queryParams("fileName");
      if (filename == null || filename.contains("/") || filename.contains("..")) {
        System.out.println("Invalid filename:" + filename);
        return false;
      }
      if (!filename.toLowerCase().endsWith(".cdc")) {
        res.status(Response.SC_BAD_REQUEST);
        return false;
      }
      try {
        String cdcFilePath = MCDefs.SC_FOLDER + filename;
        File cdcFile = new File(cdcFilePath);
        final String cmd = "cdc2ini -f " + cdcFilePath + " --cdc2ini";
        Process pr = Runtime.getRuntime().exec(cmd,null,new File("/FFS0/SSMC"));
        int ret = pr.waitFor();
        final String file = filename.substring(0, filename.length() - 4) + "G.ini";
        File f = new File(MCDefs.SC_FOLDER + file);
        if (!f.exists()) {
          return false;
        }
        String upperCase = f.getAbsolutePath().toUpperCase();
        File upperFile = new File(upperCase);
        upperFile.delete();
        return f.renameTo(upperFile);
      } catch (Exception e) {
        System.out.println("CDC2INI error: " + e.getMessage());
        return false;
      }
    });
        
  }

  private static JSONArray searchInPath(String search, String path) {
    if (stopSearch) {
      stopSearch = false;
      return new JSONArray();
    }
    search = search.toLowerCase();
    try {
      JSONArray result = new JSONArray();
      File[] fileEntries = new File(MC_FOLDER + path).listFiles();
      Arrays.sort(fileEntries);
      for (final File fileEntry : fileEntries) {
        if (stopSearch) {
          stopSearch = false;
          return new JSONArray();
        }
        String fileName = fileEntry.getName().toUpperCase();
        if (!fileEntry.isDirectory()) {
          // READ FILE CONTENT AND LOOK FOR THE STRING
          String content[] = new String(Files.readAllBytes(Paths.get(fileEntry.getAbsolutePath())),StandardCharsets.UTF_8).replaceAll("\0","").split("\n");
          JSONObject file = new JSONObject();
          file.put("name", fileName);
          file.put("path", fileEntry.getPath());
          JSONArray lines = new JSONArray();
          for (int i=0; i<content.length; i++) {
            if (stopSearch) {
              stopSearch = false;
              return new JSONArray();
            }
            if (content[i].toLowerCase().contains(search)) {
              JSONObject line = new JSONObject();
              line.put("index", i);
              line.put("line", content[i]);
              lines.put(line);
            }
          }
          if (lines.length() > 0) {
            file.put("lines",lines);
            result.put(file);
          }
        } else {
          if (stopSearch) {
            stopSearch = false;
            return new JSONArray();
          }
          JSONArray children = searchInPath(search, path + "/" + fileName);
          for (int i=0; i<children.length(); i++) {
            result.put(children.get(i));
          }
        }
      }
      return result;
    } catch (Exception e) {
      return new JSONArray();
    }
  }
  
  private static JSONObject PathToJSON(String path, List<String> extensions, String baseFolder) {
    JSONObject result = new JSONObject();
    JSONArray files = new JSONArray();
    JSONArray children = new JSONArray();
    String ext;
    try {
      String folder = (baseFolder == null ? MC_FOLDER : baseFolder) + path;
      File[] fileEntries = new File(folder).listFiles();
      Arrays.sort(fileEntries);
      for (final File fileEntry : fileEntries) {
        String fileName = fileEntry.getName();
        if (!fileEntry.isDirectory()) {
          if (extensions != null) {
            ext = fileName.toUpperCase().substring(fileName.lastIndexOf(".")+1);
            if (!extensions.contains(ext)) continue;
          }
          files.put(fileName);
        } else {
          children.put(PathToJSON(path+"/"+fileName, extensions, baseFolder));
        }
      }
    } catch (Exception e) {
    } finally {
      result.put("children",children);
      result.put("files", files);
      int i = path.lastIndexOf("/");
      if (i != -1 && i < path.length() - 1) {
        path = path.substring(i+1);
      }
      result.put("path", path);
      return result;
    }
  }
}

/* USED FOR SORTING FILES BY DATE */
class Pair implements Comparable {
    public long t;
    public File f;

    public Pair(File file) {
        f = file;
        t = file.lastModified();
    }

    public int compareTo(Object o) {
        long u = ((Pair) o).t;
        return t > u ? -1 : t == u ? 0 : 1;
    }
};