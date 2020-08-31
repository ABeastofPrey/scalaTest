/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MCJCommLib;

/**
 *
 * @author User
 */
import java.lang.Exception;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BitConverter {
  
  static byte [] Reverse (byte [] data) {
    int l = data.length;
    int hl = l / 2;
    int idx=0, ridx = l-1;
    while (idx<hl) {
      byte b = data[idx];
      data[idx] = data[ridx];
      data[ridx] = b;
      idx++;
      ridx--;
    }
    return data;
  }
  
  public static byte[] getBytes(boolean x) {
    return new byte[]{
      (byte) (x ? 1 : 0)
    };
  }

  public static byte[] getBytes(char c) {
    return 
            Reverse (new byte[]{
      (byte) (c & 0xff),
      (byte) (c >> 8 & 0xff)});
  }

  public static byte[] getBytes(double x) {
    return Reverse (getBytes(
            Double.doubleToRawLongBits(x)));
  }

  public static byte[] getBytes(short x) {
    return Reverse (new byte[]{
      (byte) (x >>> 8),
      (byte) x
    });
  }

  public static byte[] getBytes(int x) {
    return Reverse (
            new byte[]{
      (byte) (x >>> 24),
      (byte) (x >>> 16),
      (byte) (x >>> 8),
      (byte) x
    });
  }

  public static byte[] getBytes(long x) {
    return Reverse (new byte[]{
      (byte) (x >>> 56),
      (byte) (x >>> 48),
      (byte) (x >>> 40),
      (byte) (x >>> 32),
      (byte) (x >>> 24),
      (byte) (x >>> 16),
      (byte) (x >>> 8),
      (byte) x
    });
  }

  public static byte[] getBytes(float x) {
    return Reverse (getBytes(
            Float.floatToRawIntBits(x)));
  }

  public static byte[] getBytes(String x) {
    return Reverse (x.getBytes());
  }

  public static long doubleToInt64Bits(double x) {
    return Double.doubleToRawLongBits(x);
  }

  public static double int64BitsToDouble(long x) {
    return (double) x;
  }

  public boolean toBoolean(byte[] bytes, int index) throws Exception {
    if (bytes.length < index + 1) {
      throw new Exception("The length of the byte array must be at least 1 byte long.");
    }
    return bytes[index] != 0;
  }

  public char toChar(byte[] bytes, int index) throws Exception {
    if (bytes.length < index + 2) {
      throw new Exception("The length of the byte array must be at least 2 bytes long.");
    }
    return (char) ((0xff & bytes[index])
            | (0xff & bytes[index + 1]) << 8);
  }

  public double toDouble(byte[] bytes, int index) throws Exception {
    if (bytes.length < index + 8) {
      throw new Exception("The length of the byte array must be at least 8 bytes long.");
    }
    return Double.longBitsToDouble(
            toInt64(bytes, index));
  }

  public static short toInt16(byte[] bytes, int index) throws Exception {
    if (bytes.length < index + 2) {
      throw new Exception("The length of the byte array must be at least 8 bytes long.");
    }
    ByteBuffer bb = ByteBuffer.wrap(bytes, index, 2);
    bb.order(ByteOrder.LITTLE_ENDIAN);
    return bb.getShort();
  }

  public static int toInt32(byte[] bytes, int index) throws Exception {
    if (bytes.length < index + 4) {
      throw new Exception("The length of the byte array must be at least 4 bytes long.");
    }
    ByteBuffer bb = ByteBuffer.wrap(bytes, index, 4);
    bb.order(ByteOrder.LITTLE_ENDIAN);
    return bb.getInt();
  }

  public static long toInt64(byte[] bytes, int index) throws Exception {
    if (bytes.length < index + 8) {
      throw new Exception("The length of the byte array must be at least 8 bytes long.");
    }
    return (long) ((long) (0xff & bytes[index])
            | (long) (0xff & bytes[index + 1]) << 8
            | (long) (0xff & bytes[index + 2]) << 16
            | (long) (0xff & bytes[index + 3]) << 24
            | (long) (0xff & bytes[index + 4]) << 32
            | (long) (0xff & bytes[index + 5]) << 40
            | (long) (0xff & bytes[index + 6]) << 48
            | (long) (0xff & bytes[index + 7]) << 56);
  }

  public static float toSingle(byte[] bytes, int index) throws Exception {
    if (bytes.length < index + 4) {
      throw new Exception("The length of the byte array must be at least 4 bytes long.");
    }
    return Float.intBitsToFloat(
            toInt32(bytes, index));
  }

  public static String toString(byte[] bytes) throws Exception {
    if (bytes == null) {
      throw new Exception("The byte array must have at least 1 byte.");
    }
    return new String(bytes);
  }
}
