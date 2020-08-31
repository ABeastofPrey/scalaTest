package MCJCommLib;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;

import org.bouncycastle.util.Arrays;

/**
 *
 * @author Doron and Omri Soudry
 */
public class MCJMessageUtils {

  static final int StaticSerialNum = 0x7e8118e7;
  static byte[] StaticSerialBytes;
  static final int MsgHeaderLength = 19; //the total length of the MC frame header
  ArrayList<Byte> FrameTypeBytes = new ArrayList<>();
  MCJCallbacks callBacks = null;

  public MCJMessageUtils(MCJCallbacks callBacks) {
    this.callBacks = callBacks;
    StaticSerialBytes = BitConverter.getBytes(StaticSerialNum);
    byte[] FrameTypeBytesSrc = new byte[]{(byte) 0, (byte) 'A', (byte) 'D', (byte) 'B', (byte) 'C', (byte) 'P', (byte) 'E', (byte) 0xC5, (byte) 0xC4};
    for (byte b : FrameTypeBytesSrc) {
      FrameTypeBytes.add((Byte) b);
    }
  }

  // receive a string and the required message types and wraps it with an header and a CRC
  public byte[] MakeMessage(GenDefs.FrameTypeEnum frameType, String msg, int id) {
    // build basic header
    msg = msg + '\n';
    byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
    int serNum = StaticSerialNum;
    int useCount = (short) (msgBytes.length + MsgHeaderLength + 1);
    short totLen = (short) (useCount + 2); // save place for the CRC
    // convert to byte array
    byte[] outBuff = new byte[totLen];
    outBuff[0] = FrameTypeBytes.get(frameType.ordinal());

    System.arraycopy(BitConverter.getBytes(serNum), 0, outBuff, 1, 4);
    System.arraycopy(BitConverter.getBytes(totLen), 0, outBuff, 5, 2);
    System.arraycopy(BitConverter.getBytes(id), 0, outBuff, 7, 4);
    System.arraycopy(BitConverter.getBytes(0), 0, outBuff, 11, 4);
    System.arraycopy(BitConverter.getBytes((short) 1), 0, outBuff, 15, 2);
    System.arraycopy(BitConverter.getBytes((short) 1), 0, outBuff, 17, 2);
    System.arraycopy(msgBytes, 0, outBuff, MsgHeaderLength, msgBytes.length);
    outBuff[MsgHeaderLength + msgBytes.length + 1] = 0;
    int crc = makeCRC(outBuff, useCount);
    System.arraycopy(BitConverter.getBytes(crc), 0, outBuff, outBuff.length - 2, 2);

    return outBuff;
  }

  enum DecodePhaseEnum {

    Waiting, Header, Data
  }
  GenDefs.FrameTypeEnum currFrameType = GenDefs.FrameTypeEnum.INVALID_FRAME;
  DecodePhaseEnum decoderPhase = DecodePhaseEnum.Waiting;
  byte[] headerBytes = new byte[MsgHeaderLength];
  byte lastByte;
  int buffIdx = 0;
  int totalMessageIdx = 0;
  int expectedDataLen = 0;
  int currPackage = 0;
  int expectedPackages = 0;
  byte[] frameBuffer = null;
  byte[] totalMessage = null;

  /*
    Header format:
    0:      Frame Type
    1-4:    StaticSerialNumber
    5-6:    Frame byte[] total size
    7-10:   ???
    11-12:  Sender ID
    13-14:  Receiver ID
    15-16:  Package number
    17-18:  Total packages
  */


  /* receives the incoming frame byte by byte, if the decoder decides a frame has finished correctly, it calls a 'callback' delegate 
   * placing the incoming message in a synchronize queue for processing  */

  public void DecodeMessage(byte inByte) {
    switch (decoderPhase) {
      case Waiting: // wait for message start
        if (inByte == StaticSerialBytes[0]) {
          headerBytes[0] = lastByte;
          headerBytes[1] = inByte;
          buffIdx = 2;
          decoderPhase = DecodePhaseEnum.Header;
        }
        break;
      case Header: // message start detected, trying ot decode header
        headerBytes[buffIdx++] = inByte;
        if (buffIdx == MsgHeaderLength) { // probably filled header
          // check magic number
          boolean frameOk
                  = headerBytes[1] == StaticSerialBytes[0] && headerBytes[2] == StaticSerialBytes[1]
                  && headerBytes[3] == StaticSerialBytes[2] && headerBytes[4] == StaticSerialBytes[3];
          int ftIdx = FrameTypeBytes.indexOf(headerBytes[0]);
          frameOk = frameOk && ftIdx > 0;
          if (!frameOk) {
            decoderPhase = DecodePhaseEnum.Waiting;
            buffIdx = 0;
            return;
          }
          currFrameType = GenDefs.FrameTypeEnum.values()[ftIdx];
          decoderPhase = DecodePhaseEnum.Data;
          try {
            expectedDataLen = (int) BitConverter.toInt16(headerBytes, 5);
            currPackage = (int) BitConverter.toInt16(headerBytes, 15);
            expectedPackages = (int)  BitConverter.toInt16(headerBytes, 17);
          } catch (Exception e) {
            return;
          }
          if (expectedDataLen == 0) {
            decoderPhase = DecodePhaseEnum.Waiting;
            buffIdx = 0;
            return;
          }
          frameBuffer = new byte[expectedDataLen];
          //System.out.println("Frame " + currPackage + " / " + expectedPackages + "...");
          if (currPackage == 1) {
            totalMessage = new byte[expectedPackages * 250];
            totalMessageIdx = 0;
          }
          System.arraycopy(headerBytes, 0, frameBuffer, 0, MsgHeaderLength);
          buffIdx = MsgHeaderLength;
        }
        break;
      case Data: // retrieving message data and looking for message end
        frameBuffer[buffIdx++] = inByte;
        if (buffIdx == expectedDataLen) {
          int calcedCrc = makeCRC(frameBuffer, expectedDataLen - 2);
          int inCrc = 0;
          try {
            byte [] tbts = new byte [] {frameBuffer[expectedDataLen - 2], frameBuffer[expectedDataLen - 1], 0, 0};
            inCrc = BitConverter.toInt32(tbts, 0);
          } catch (Exception e) {
            return;
          }
          if (calcedCrc != inCrc) { // message was finished but with an error
            currFrameType = GenDefs.FrameTypeEnum.INVALID_FRAME;
            decoderPhase = DecodePhaseEnum.Waiting;
            buffIdx = 0;
            totalMessageIdx = 0;
            return;
          }
          // Frame is OK and done - let's see if it was the last frame in this package
          try {
            int len = expectedDataLen - MsgHeaderLength - 3;
            //System.out.print("Copying " + len + " bytes from frameBuffer (" + MsgHeaderLength + ") to totalMessage (" + totalMessageIdx + ")");
            System.arraycopy(frameBuffer, MsgHeaderLength, totalMessage, totalMessageIdx, len);
            totalMessageIdx += len;
            if (currPackage == expectedPackages) { // last package
              if (callBacks!=null) { // message received OK
                int id = 0;
                try {
                  id = (int) BitConverter.toInt16(headerBytes, 11);
                } catch (Exception e) {
                  
                }
                // totalMessage[totalMessageIdx] = '\0';
                // System.out.println("Total Message:\n");
                // for (int i=0; i<totalMessage.length; i++) {
                //   System.out.println(i + ". " + String.format("%02X", totalMessage[i]));
                // }
                String msg = new String(Arrays.copyOfRange(totalMessage,0,totalMessageIdx),StandardCharsets.UTF_8);
                callBacks.OnMultiPurpDataCallback(currFrameType, msg, id);
                totalMessage = null;
                totalMessageIdx = 0;
              }
            }
          } catch (Exception e) {
            currFrameType = GenDefs.FrameTypeEnum.INVALID_FRAME;
            decoderPhase = DecodePhaseEnum.Waiting;
            buffIdx = 0;
            totalMessageIdx = 0;
            return;
          }
          decoderPhase = DecodePhaseEnum.Waiting;
          buffIdx = 0;
        }
        break;
    }
    lastByte = inByte;
  }

  // CRC calculator
  final int CRCInit = 0xA2;
  int makeCRC(byte[] buffer, int useCount) {
    int usCrc = CRCInit;
    int buffIdx = 0;
    while (useCount > 0) {//Start from end to begin
      useCount--;
      int index = ((usCrc & 0xffff) >>> 8 ^ buffer[buffIdx++]) & 0xff;
      usCrc = (CRCTable[index] ^ usCrc << 8) & 0xffff;   //Compute usCrc 
      //System.out.println(String.format("usecount:%d, index:%d, ti:%d" , useCount, index, usCrc));
    }
    return usCrc & 0xffff;
  }

  int makeCRCUS(byte[] buffer, int useCount) {
    UShort usCrc = new UShort(CRCInit);
    int buffIdx = 0;
    while (useCount > 0) {//Start from end to begin
      useCount--;
      int index = (usCrc.RShift(8) ^ buffer[buffIdx++]) & 0xff;
      int ti = CRCTable[index] ^ usCrc.LShift(8);   //Compute usCrc 
    }
    return usCrc.Val();
  }

  static final int[] CRCTable = {
    0, 4129, 8258, 12387, 16516, 20645, 24774, 28903, 33032, 37161,
    41290, 45419, 49548, 53677, 57806, 61935, 4657, 528, 12915, 8786,
    21173, 17044, 29431, 25302, 37689, 33560, 45947, 41818, 54205, 50076,
    62463, 58334, 9314, 13379, 1056, 5121, 25830, 29895, 17572, 21637,
    42346, 46411, 34088, 38153, 58862, 62927, 50604, 54669, 13907, 9842,
    5649, 1584, 30423, 26358, 22165, 18100, 46939, 42874, 38681, 34616,
    63455, 59390, 55197, 51132, 18628, 22757, 26758, 30887, 2112, 6241,
    10242, 14371, 51660, 55789, 59790, 63919, 35144, 39273, 43274, 47403,
    23285, 19156, 31415, 27286, 6769, 2640, 14899, 10770, 56317, 52188,
    64447, 60318, 39801, 35672, 47931, 43802, 27814, 31879, 19684, 23749,
    11298, 15363, 3168, 7233, 60846, 64911, 52716, 56781, 44330, 48395,
    36200, 40265, 32407, 28342, 24277, 20212, 15891, 11826, 7761, 3696,
    65439, 61374, 57309, 53244, 48923, 44858, 40793, 36728, 37256, 33193,
    45514, 41451, 53516, 49453, 61774, 57711, 4224, 161, 12482, 8419,
    20484, 16421, 28742, 24679, 33721, 37784, 41979, 46042, 49981, 54044,
    58239, 62302, 689, 4752, 8947, 13010, 16949, 21012, 25207, 29270,
    46570, 42443, 38312, 34185, 62830, 58703, 54572, 50445, 13538, 9411,
    5280, 1153, 29798, 25671, 21540, 17413, 42971, 47098, 34713, 38840,
    59231, 63358, 50973, 55100, 9939, 14066, 1681, 5808, 26199, 30326,
    17941, 22068, 55628, 51565, 63758, 59695, 39368, 35305, 47498, 43435,
    22596, 18533, 30726, 26663, 6336, 2273, 14466, 10403, 52093, 56156,
    60223, 64286, 35833, 39896, 43963, 48026, 19061, 23124, 27191, 31254,
    2801, 6864, 10931, 14994, 64814, 60687, 56684, 52557, 48554, 44427,
    40424, 36297, 31782, 27655, 23652, 19525, 15522, 11395, 7392, 3265,
    61215, 65342, 53085, 57212, 44955, 49082, 36825, 40952, 28183, 32310,
    20053, 24180, 11923, 16050, 3793, 7920
  };
}
