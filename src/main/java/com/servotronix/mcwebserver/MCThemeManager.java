
package com.servotronix.mcwebserver;

import static com.servotronix.mcwebserver.MCDefs.THEME_FILE;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class MCThemeManager {
  
  private static final String DEFAULT_THEME = "kuka";
  
  public static String theme = DEFAULT_THEME;
  
  /**
    * Checks MC-Theme file to get the current theme, or create the file if it doesn't exists.
    * Default value: kuka
  */
  public static void init() {
    // READ THE FILE
    try {
      BufferedReader reader = new BufferedReader(new FileReader(THEME_FILE));
      String line = reader.readLine();
      if (line == null)
        createThemeFile(DEFAULT_THEME);
      else
        theme = line.trim();
    } catch (Exception e) {
      createThemeFile(DEFAULT_THEME);
    }
  }
  
  public static void createThemeFile(String t) {
    try {
      String t2 = t == null ? DEFAULT_THEME : t;
      FileWriter writer = new FileWriter(THEME_FILE,false);
      writer.write(t);
      writer.close();
      theme = t2;
    } catch (IOException e2) {
      System.out.println("COULDN'T CREATE THEME FILE!");
    }
  }
  
}
