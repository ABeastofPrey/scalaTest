package com.servotronix.MCInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.json.JSONObject;

/*
  THIS CAN PRASE SYS.INFO AND SAVE ALL VALUES IN THE INFO OBJECT
*/


public class MCInfo {
    
    private String cpu, ver, platform, hash = "N/A", sn="N/A", name="N/A", ip="N/A";
    private long cpuFreq, diskSize, freeDiskSpace, ramSize, freeRamSpace, maxAxes, realAxes;
    private float cycleTime;
    private List<String> features = new ArrayList<>();
    
    public MCInfo (String info, String sn, String name, String ip) {
      this.sn = sn;
      this.name = name;
      this.ip = ip;
      String[] lines = info.split("\n");
      for (String line : lines) {
        line = line.trim();
        if (line.startsWith("CPU type"))
          cpu = line.substring(8).trim();
        else if (line.startsWith("CPU freq")) {
          line = line.substring(13).trim();
          line = line.substring(0,line.indexOf(" "));
          try { cpuFreq = Long.parseLong(line); } catch (NumberFormatException e) { e.printStackTrace();}
        } else if (line.startsWith("Flash disk size")) {
          line = line.substring(15).trim();
          line = line.substring(0,line.indexOf(" "));
          try { diskSize = Long.parseLong(line); } catch (NumberFormatException e) { e.printStackTrace();}
        } else if (line.startsWith("Flash disk free")) {
          line = line.substring(21).trim();
          line = line.substring(0,line.indexOf(" "));
          try { freeDiskSpace = Long.parseLong(line); } catch (NumberFormatException e) { e.printStackTrace();}
        } else if (line.startsWith("RAM size")) {
          line = line.substring(8).trim();
          line = line.substring(0,line.indexOf(" "));
          try { ramSize = Long.parseLong(line); } catch (NumberFormatException e) { e.printStackTrace();}
        } else if (line.startsWith("RAM drive")) {
          line = line.substring(20).trim();
          line = line.substring(0,line.indexOf(" "));
          try { freeRamSpace = Long.parseLong(line); } catch (NumberFormatException e) { e.printStackTrace();}
        } else if (line.startsWith("Bus")) {
          line = line.substring(22).trim();
          line = line.substring(0,line.indexOf(" "));
          try { cycleTime = Float.parseFloat(line); } catch (NumberFormatException e) { e.printStackTrace();}
        } else if (line.startsWith("Version")) {
          ver = line.substring(14).trim();
        } else if (line.startsWith("Platform")) {
          platform = line.substring(16).trim();
        } else if (line.startsWith("Maximum")) {
          line = line.substring(22).trim();
          try { maxAxes = Long.parseLong(line); } catch (NumberFormatException e) { e.printStackTrace();}
        } else if (line.startsWith("Real")) {
          line = line.substring(19).trim();
          try { realAxes = Long.parseLong(line); } catch (NumberFormatException e) { e.printStackTrace();}
        } else if (line.startsWith("Features")) {
          line = line.substring(8).trim();
          System.out.println("GETTING LICENSES..." + line + "...DONE!");
          features = new ArrayList<String>(Arrays.asList(line.split(";")));
          for (Iterator<String> iterator = features.iterator(); iterator.hasNext();) {
            String feature = iterator.next();
            if (feature.length() == 0)
              iterator.remove();
          }
        } else if (line.startsWith("Hash")) {
          hash = line.substring(4).trim();
        }
      }
    }

    @Override
    public String toString() {
      return "MCInfo{" + "cpu=" + cpu + ", ver=" + ver + ", platform=" + platform +
            ", cpuFreq=" + cpuFreq + ", diskSize=" + diskSize + ", freeDiskSpace=" +
            freeDiskSpace + ", ramSize=" + ramSize + ", freeRamSpace=" + freeRamSpace +
            ", maxAxes=" + maxAxes + ", realAxes=" + realAxes + ", cycleTime=" +
            cycleTime + ", features=" + features + ", hash=" + hash + '}';
    }
    
    
    public List<String> getFeatures() {
        return features;
    }

    public String getCpu() {
        return cpu;
    }

    public String getVer() {
        return ver;
    }

    public String getPlatform() {
        return platform;
    }

    public long getCpuFreq() {
      return cpuFreq;
    }

    public long getDiskSize() {
      return diskSize;
    }

    public long getFreeDiskSpace() {
      return freeDiskSpace;
    }

    public long getRamSize() {
      return ramSize;
    }

    public long getFreeRamSpace() {
      return freeRamSpace;
    }

    public long getMaxAxes() {
      return maxAxes;
    }

    public long getRealAxes() {
      return realAxes;
    }

    public float getCycleTime() {
      return cycleTime;
    }
    
    public String getHash() {
      return hash;
    }
    
    public JSONObject getInfoAsJSON() {
      JSONObject obj = new JSONObject();
      obj.put("cpu", cpu);
      obj.put("ver", ver);
      obj.put("platform", platform);
      obj.put("cpuFreq", cpuFreq);
      obj.put("diskSize", diskSize);
      obj.put("freeDiskSpace", freeDiskSpace);
      obj.put("ramSize", ramSize);
      obj.put("freeRamSpace", freeRamSpace);
      obj.put("maxAxes", maxAxes);
      obj.put("realAxes", realAxes);
      obj.put("cycleTime", cycleTime);
      obj.put("features", features.toArray());
      obj.put("hash", hash);
      obj.put("sn", sn);
      obj.put("ip", ip);
      obj.put("name", name);
      return obj;
    }
    
}
