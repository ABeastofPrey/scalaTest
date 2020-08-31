/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MCJCommLib;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author omri.soudry
 */
public class DetectionMessage {
    
    public byte[] data = new byte[64];

    public DetectionMessage(byte[] data) {
      this.data = data;
    }
    
    public DetectionMessage() {
        for (int i=0; i<data.length; i++) {
            data[i] = 0;
        }
    }
    
    public byte getOpCode() {
        return data[0];
    }
    
    public void setOpCode(byte value) {
        data[0] = value;
    }
    
    public int getHostIP() {
        try {
            return BitConverter.toInt32(data, 20);
        } catch (Exception ex) {
            return 0;
        }
    }
    
    public void setHostIP(byte[] ip) {
        System.arraycopy(ip, 0, data, 20, 4);
    }
    
    public String getMCName() {
        byte[] bytes = new byte[12];
        System.arraycopy(data, 44, bytes, 0, 12);
        String str = new String(bytes);
        return str.replaceAll("\\s+$", "");
    }

    public int getMCIP() {
        try {
            return BitConverter.toInt32(data, 12);
        } catch (Exception e) {
            return 0;
        }
    }
    
    public void setMCIP(byte[] ip) {
        System.arraycopy(ip, 0, data, 12, 4);
    }

    public byte getCheckSum() {
        return data[3];
    }
    
    public void setCheckSum(byte b) {
        data[3] = b;
    }
    
    public short getHostSig() {
        try {
            return BitConverter.toInt16(data, 60);
        } catch (Exception e) {
            return -1;
        }
    }
    
    public void setHostSig(byte[] hostSig) {
        System.arraycopy(hostSig, 0, data, 60, 2);
    }
    
    public int getMagic() {
        try {
            return BitConverter.toInt32(data, 4);
        } catch (Exception e) {
            return 0;
        }
    }
    
    public void setMagic(byte[] magic) {
        System.arraycopy(magic, 0, data, 4, 4);
    }
    
    public String getSerial() {
        byte[] bytes = new byte[20];
        System.arraycopy(data, 24, bytes, 0, 20);
        return new String(bytes);
    }

    @Override
    public String toString() {
        String str = "data:\n---------\n";
        for (int i=0; i<data.length; i++) {
            str += "[" + i + "] - " + (data[i] & (0xff)) + "\n";
        }
        return str;
    }
    
    

}
