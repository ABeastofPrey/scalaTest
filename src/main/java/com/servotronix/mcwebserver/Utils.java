package com.servotronix.mcwebserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Utils {
  
  public static void ConsoleLog(String msg) {
    System.out.println(System.currentTimeMillis() + "\t" + msg);
  }

  public static long currentTimeMillis() {
    return System.nanoTime() / 1000000;
  }

  public static long safeCopy(InputStream src, Path path, boolean replace) throws IOException{
    File outFile = path.toFile();
    if (outFile.exists() && !replace) {
      throw new IOException("File exists");
    }
    // CREATE FOLDERS IF NEEDED
    String p = path.toString();
    int i = p.lastIndexOf('/');
    if (i > 0) {
      p = p.substring(0,i);
    }
    File folders = new File(p);
    folders.mkdirs();
    FileOutputStream out = new FileOutputStream(outFile);
    long nRead = 0L;
    byte[] buf = new byte[8192];
    int n;
    while ((n = src.read(buf)) > 0) {
      out.write(buf, 0, n);
      nRead += n;
    }
    out.flush();
    out.getFD().sync();
    out.close();
    return nRead;
  }
  
}
