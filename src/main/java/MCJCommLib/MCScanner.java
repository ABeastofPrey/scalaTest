/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MCJCommLib;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author omri.soudry
 */
public class MCScanner {
    final int MCPort = 5002;
    // message constants
    final int OpGet = 0;
    final int OpReply = 1;
    final int MSGSize = 64;
    final int MagicNum = 0x7f5e9017;

    //AutoResetEvent are = new AutoResetEvent(false);
    //ManualResetEvent signal = new ManualResetEvent(false);
    List<String> mcs = new ArrayList<>();
    Object syncObj = new Object();

    public List<DetectedMCDesc> UDPDetect(String myAddress, int timeOutMS, int totalTimeoutMS) {
        // IPAddress = String, IPEndPoint = InetSocketAddress
        byte[] buff = new byte[MSGSize];
        DatagramPacket receivePacket = new DatagramPacket(buff, MSGSize);
        List<DetectedMCDesc> lmcs = new ArrayList<>();
        InetSocketAddress ep = new InetSocketAddress("255.255.255.255",MCPort);
        Calendar c = Calendar.getInstance();
        DetectionMessage msg;
        try {
            msg = CreateBroadcastMessage(InetAddress.getByName(myAddress));
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
        DatagramSocket udp;
        try {
            udp = new DatagramSocket();
        } catch (SocketException ex) {
            ex.printStackTrace();
            return null;
        }
        try {
            udp.setSoTimeout(timeOutMS);
        } catch (SocketException ex) {
            ex.printStackTrace();
            return null;
        }
        DatagramPacket dp = new DatagramPacket(msg.data, msg.data.length, ep);
        try {
            udp.setBroadcast(true);
            udp.send(dp);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        while (Calendar.getInstance().getTimeInMillis() - c.getTimeInMillis() < totalTimeoutMS) {
            try {
                udp.receive(receivePacket);
                DetectedMCDesc retMCDesc = ParseIncommingMessasge(receivePacket.getData());
                if (retMCDesc!=null)
                    lmcs.add(retMCDesc);
            } catch (IOException ex) {
            }
        }
        udp.close();
        return lmcs;
    }

    // create the broadcast message
    DetectionMessage CreateBroadcastMessage(InetAddress myAddress) {
        byte[] bytes = new byte[MSGSize];
        DetectionMessage msg = new DetectionMessage();
        msg.setOpCode((byte)OpGet);
        try {
            msg.setHostIP(BitConverter.getBytes(BitConverter.toInt32(myAddress.getAddress(), 0)));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        msg.setCheckSum(calcDetMsgCS(msg.data,0));
        msg.setHostSig(BitConverter.getBytes(1234));
        msg.setMagic(BitConverter.getBytes(MagicNum));
        return msg;
    }

    DetectedMCDesc ParseIncommingMessasge(byte[] message) {
      //mcDesc = null;
      if (message.length != MSGSize)
        return null;
      DetectionMessage dm = new DetectionMessage(message);
      if (dm.getMagic() != MagicNum)
        return null;
      if (dm.getOpCode() != OpReply)
        return null;
      byte[] bts = BitConverter.getBytes(dm.getMCIP());
      String hostIP = String.format("%1$d.%2$d.%3$d.%4$d", bts[0], bts[1], bts[2], bts[3]);
      DetectedMCDesc desc = new DetectedMCDesc();
      desc.setName(dm.getMCName());
      desc.setIP(hostIP);
      desc.setSerial(dm.getSerial().trim());
      return desc;
    }

    // calculate checksum
    byte calcDetMsgCS(byte[] msg, int sz) {
        sz = 0;
        byte uc_cs = 0;
        byte pc_aux = msg[0];
        int len = sz > 0 ? sz : msg.length;
        for (int idx = 0; idx < len; idx++)
            uc_cs += msg[idx];
        return (byte)(uc_cs + 1);
    }

}
