package com.servotronix.mcwebserver;

import static com.servotronix.mcwebserver.MCDefs.MC_FOLDER;
import static com.servotronix.mcwebserver.MCDefs.ZIPFILE_PATH;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipMaker {
  
  private final static int BUFFSIZE = 4096;
  
  private String srcFolder;

  public ZipMaker(String src) {
    srcFolder = src;
  }
    
  // If the filesString is NULL - will zip ALL files in SSMC folder.
  public boolean createZip(String filesString, boolean singleFolder) {
    String[] fileList = (filesString == null) ? null : filesString.split(",");
    try {
      byte[] buffer = new byte[1024];
      File zipFile = new File(ZIPFILE_PATH);
      zipFile.createNewFile();
      FileOutputStream fos = new FileOutputStream(zipFile);
      ZipOutputStream zos = new ZipOutputStream(fos);
      zos.setLevel(0);
      if (fileList == null) {
        zipDir(srcFolder,zos, false);
      } else if (singleFolder) {
        zipDir(srcFolder + filesString,zos, true);
      } else {
        for (String path: fileList) {
          path = path.toUpperCase().replaceAll("\\$\\$","/");
          File f = new File(srcFolder+path);
          if (f.exists() && f.isDirectory()) {
            System.out.println("ADDING FOLDER: " + path + "/ ...");
            zos.putNextEntry(new ZipEntry(path + "/"));
            continue;
          } else if (f.exists()) {
            FileInputStream fis = new FileInputStream(f);
            System.out.println("ADDING FILE: " + path + " ...");
            zos.putNextEntry(new ZipEntry(path));
            int length = 0;
            while ((length = fis.read(buffer)) > 0) {
              zos.write(buffer, 0, length);
            }
            fis.close();
          }
          zos.closeEntry();
        }
      }
      zos.close();
      return true;
    } catch (Exception e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
      return false;
    }
  }
  
  public boolean createSysZip(String userInfo, String sysInfo, String history) {
    try {
      // STEP 1 - CREATE ZIP FILE
      File zipFile = new File(MCDefs.ZIPFILE_SYS_PATH);
      zipFile.createNewFile();
      FileOutputStream fos = new FileOutputStream(zipFile);
      ZipOutputStream zos = new ZipOutputStream(fos);
      zos.setLevel(0);    
      // STEP 2 - ADD USER AND SYS DATA AS TEXT FILES
      ZipEntry ze = new ZipEntry("user-info.txt");
      zos.putNextEntry(ze);
      zos.write(userInfo.getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();
      ze = new ZipEntry("sys-info.txt");
      zos.putNextEntry(ze);
      zos.write(sysInfo.getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();
      ze = new ZipEntry("err-history.txt");
      zos.putNextEntry(ze);
      zos.write(history.getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();
      // STEP 3 - ATTACH SYSTEM FILES
      addFileToZip(srcFolder+"LOGGER.DAT","LOGGER.TXT",zos);
      addFileToZip(MCDefs.PKGD_PATH,"PKGD.TXT",zos);
      addFileToZip("/var/sys.log","SYSLOG.TXT",zos);
      // STEP 4 - ATTACH SHELL COMMANDS
      addCmdToZip("dmesg",zos);
      addCmdToZip("netstat",zos);
      addCmdToZip("ifconfig",zos);
      zos.close();
      return true;
    } catch (Exception e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
      return false;
    }
  }
  
  private void addCmdToZip(String cmd, ZipOutputStream zos) {
    Runtime run = Runtime.getRuntime();
    try {
      Process pr = run.exec(cmd);
      pr.waitFor();
      byte[] buffer = new byte[1024];
      int length = 0;
      ZipEntry ze = new ZipEntry(cmd + ".txt");
      zos.putNextEntry(ze);
      InputStream is = pr.getInputStream();
      while ((length = is.read(buffer)) > 0) {
        zos.write(buffer, 0, length);
      }
      zos.closeEntry();
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }
  
  private void addFileToZip(String path, String showAs, ZipOutputStream zos) throws IOException{
    byte[] buffer = new byte[1024];
    File f = new File(path);
    if (f.exists()) {
      FileInputStream fis = new FileInputStream(f);
      zos.putNextEntry(new ZipEntry(showAs));
      int length = 0;
      while ((length = fis.read(buffer)) > 0) {
        zos.write(buffer, 0, length);
      }
      fis.close();
      zos.closeEntry();
    }
  }
  
  /*
    ZIPS THE GIVEN FOLDER RECUESIVELY INTO zos
  */
  private void zipDir(String dir2zip, ZipOutputStream zos, boolean onParent) { 
    try {
      File zipDir = new File(dir2zip);
      String[] dirList = zipDir.list(); 
      byte[] readBuffer = new byte[BUFFSIZE]; 
      int bytesIn = 0;
      for(int i=0; i<dirList.length; i++) { 
        File f = new File(zipDir, dirList[i]);
        String name = f.getPath().substring(srcFolder.length());
        if (onParent) {
          int idx = name.indexOf('/');
          if (idx != -1 && name.length() > idx+1)
            name = name.substring(idx + 1);
        }
        if(f.isDirectory()) {
          ZipEntry anEntry = new ZipEntry(name + "/");
          zos.putNextEntry(anEntry); 
          String filePath = f.getPath();
          zipDir(filePath, zos, onParent); 
          continue; 
        } 
        // f is a FILE
        FileInputStream fis = new FileInputStream(f);
        ZipEntry anEntry = new ZipEntry(name);
        zos.putNextEntry(anEntry); 
        while((bytesIn = fis.read(readBuffer)) != -1) 
          zos.write(readBuffer, 0, bytesIn); 
        fis.close();
      } 
    } catch(Exception e) {
      throw new Error("can't zip folder");
    }
  }
  
  public boolean createBackup(String name, String filesString) {
    List<String> fileList = (filesString == null) ? null : Arrays.asList(filesString.split(","));
    try {
      byte[] buffer = new byte[1024];
      File zipFile = new File(MCDefs.BACKUPS + name + "$$" + System.currentTimeMillis() +".ZIP");
      zipFile.createNewFile();
      FileOutputStream fos = new FileOutputStream(zipFile);
      ZipOutputStream zos = new ZipOutputStream(fos);
      File dir = new File(srcFolder);
      File[] files = dir.listFiles();
      for (int i = 0; i < files.length; i++) {
        if (files[i].isDirectory() || (fileList!=null && !fileList.contains(files[i].getName())))
          continue;
        FileInputStream fis = new FileInputStream(files[i]);
        zos.putNextEntry(new ZipEntry(files[i].getName()));
        int length = 0;
        while ((length = fis.read(buffer)) > 0) {
          zos.write(buffer, 0, length);
        }
        zos.closeEntry();
        fis.close();
      }
      zos.close();
      return true;
    } catch (Exception e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
      return false;
    }
  }
  
  public boolean restore(String name) {
    try {
      ZipFile zipFile = new ZipFile(MCDefs.BACKUPS+name);
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while(entries.hasMoreElements()){
        ZipEntry entry = entries.nextElement();
        InputStream stream = zipFile.getInputStream(entry);
        byte[] buffer = new byte[1024];
        int len;
        File targetFile = new File(MC_FOLDER + entry.getName());
        OutputStream outStream = new FileOutputStream(targetFile);
        while ((len = stream.read(buffer)) > 0) {
          outStream.write(buffer,0,len);
        }
        stream.close();
        outStream.close();
      }
      zipFile.close();
      return true;
    } catch (Exception ex) {
      ex.printStackTrace();
      System.out.println("Error:"+ex.getMessage());
      return false;
    }
  }
  
  private static void mkdirs(File outdir,String path)
  {
    File d = new File(outdir, path);
    if( !d.exists() )
      d.mkdirs();
  }
  private static void extractFile(ZipInputStream in, File outdir, String name) throws IOException
  {
    byte[] buffer = new byte[1024];
    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(outdir,name)));
    int count = -1;
    while ((count = in.read(buffer)) != -1)
      out.write(buffer, 0, count);
    out.close();
  }
  
  private static String dirpart(String name)
  {
    int s = name.lastIndexOf( File.separatorChar );
    return s == -1 ? null : name.substring( 0, s );
  }
  
  public String verifyProject(String name) {
    File zipFile = new File(MCDefs.MC_FOLDER+name);
    ZipEntry entry;
    String mainFolder = null, entryName, folder;
    boolean appMngrFound = false;
    try {
      ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile));
      while((entry = zin.getNextEntry()) != null){
        entryName = entry.getName();
        int s = entryName.indexOf(File.separatorChar );
        folder = s == -1 ? null : entryName.substring( 0, s );
        if (folder == null) { // A FILE EXISTS ON THE TOP LEVEL
          appMngrFound = false;
          break;
        }
        if (mainFolder == null)
          mainFolder = folder;
        else if (!mainFolder.equals(folder)) { // ANOTHER FOLDER EXISTS ON THE TOP LEVEL
          appMngrFound = false;
          break;
        }
        if (entryName.equals(mainFolder + File.separatorChar + "APP_MNGR.DAT"))
          appMngrFound = true;
      }
      zin.close();
    } catch (Exception e) {
      System.out.println("Verify Project error:");
      e.printStackTrace();
    }
    if (!appMngrFound)
      return null;
    return mainFolder;
  }

  
  public boolean unzipProject(String name) {
    try {
      File outdir = new File(MCDefs.MC_FOLDER);
      File zipFile = new File(MCDefs.MC_FOLDER+name);
      ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile));
      ZipEntry entry;
      String entryName, dir;
      while((entry = zin.getNextEntry()) != null){
        entryName = entry.getName();
        if( entry.isDirectory() )
        {
          mkdirs(new File(MCDefs.MC_FOLDER),entryName);
          continue;
        }
        dir = dirpart(entryName);
        if( dir != null ) {
          mkdirs(new File(MCDefs.MC_FOLDER),dir);
        }
        extractFile(zin, outdir, entryName);
      }
      zin.close();
      zipFile.delete();
      return true;
    } catch (Exception ex) {
      ex.printStackTrace();
      System.out.println("Error:"+ex.getMessage());
      return false;
    }
  }
  
  private static void addDirToZipArchive(ZipOutputStream zos, File fileToZip, String parrentDirectoryName) throws Exception {
    if (fileToZip == null || !fileToZip.exists()) {
      return;
    }
    String zipEntryName = fileToZip.getName();
    if (parrentDirectoryName!=null && !parrentDirectoryName.isEmpty()) {
      zipEntryName = parrentDirectoryName + "/" + fileToZip.getName();
    }
    if (fileToZip.isDirectory()) {
      ZipEntry folderEntry = new ZipEntry(zipEntryName + "/");
      zos.putNextEntry(folderEntry);
      for (File file : fileToZip.listFiles()) {
        addDirToZipArchive(zos, file, zipEntryName);
      }
    } else {
      byte[] buffer = new byte[1024];
      FileInputStream fis = new FileInputStream(fileToZip);
      zos.putNextEntry(new ZipEntry(zipEntryName));
      int length;
      while ((length = fis.read(buffer)) > 0) {
        zos.write(buffer, 0, length);
      }
      zos.closeEntry();
      fis.close();
    }
  }
  
  public boolean createZipFromFolder(String folderName) {
    if (folderName == null || folderName.length() == 0)
      return false;
    try {
      File zipFile = new File(ZIPFILE_PATH);
      zipFile.createNewFile();
      FileOutputStream fos = new FileOutputStream(zipFile);
      ZipOutputStream zos = new ZipOutputStream(fos);
      File dir = new File(MCDefs.MC_FOLDER + folderName);
      addDirToZipArchive(zos, dir, null);
      zos.flush();
      fos.flush();
      zos.close();
      fos.close();
      return true;
    } catch (Exception e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
      return false;
    }
  }
}
