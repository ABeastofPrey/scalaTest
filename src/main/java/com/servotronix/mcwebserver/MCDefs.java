package com.servotronix.mcwebserver;

public class MCDefs {
  public final static String VER = "v3.6.0 2020-08-31";
  public final static int PORT = 3010;
  public final static int DRIVE_WS_PORT = 3020;
  public final static int CABINET_UPDATE_WS_PORT = 3030;
  public final static String MC_FOLDER = "/FFS0/SSMC/";
  public final static String FWCONFIG = "/FFS0/FWCONFIG";
  public final static String WWW_FOLDER = "/var/home/mc/www/";
  public final static String EXT_WWW_FOLDER = "/usr/share/";
  public final static String BACKGROUND_IMAGE_PATH = "/tp/pics/robot_bg.jpg";
  public final static String PREFS_NAME = "softTP";
  public final static String MCDB_FOLDER = "/var/home/mcdb/";
  public final static String PKGD_PATH = "/var/home/pkgd.log";
  public final static String BACKUPS = MCDB_FOLDER + "backups/";
  public final static String PROJECTMNGR = MCDB_FOLDER + "projects.dat";
  public final static String PROFILE_PICS = MCDB_FOLDER + "profilePics/";
  public final static String THEME_FILE = MCDB_FOLDER + "theme";
  public final static String AVATAR_PATH = "/pics/avatar.png";
  public final static String ZIPFILE_PATH = "/var/tmp/MCFiles.zip";
  public final static String ZIPFILE_SYS_PATH = "/var/tmp/CSBug.zip";
  public final static String TRNERR = "/RAM/TRN.ERR";
  public final static String RECORDFILE = "/RAM/CSRECORD.CSR";
  public final static String REC_FOLDER = "/RAM/";
  public final static String SC_FOLDER = "/FFS0/SSMC/SC/";
  public final static String[] ALLOWED_EXTENSIONS = {"PRG","UPG","LIB","ULB","BLK","TPS","TXT","VAR","DEF","DAT","OBJ","LOG","BKG","SIM","INI","CDC","JSON","UPS"};
  public final static String[] ALLOWED_FW_EXTENSIONS = {"IPK","RPM","ZIP","ipk","rpm","zip","tar.gz","TAR.GZ"};
  public final static String[] ALLOWED_IMG_EXTENSIONS = {"JPG","JPEG","BMP","PNG","GIF"};
}