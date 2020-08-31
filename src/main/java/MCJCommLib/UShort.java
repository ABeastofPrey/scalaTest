/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MCJCommLib;

/**
 *
 * @author user
 */
public class UShort {
  int value = 0;
  
  public UShort (int aValue) {
    value = aValue;
  }
  
  public int RShift (int count) {
    value = ((value << 16) >>> count)>>>16;
    return value;
  }
  
  public int LShift (int count) {
    value = (value << count) & 0xff;
    return value;
  }
  
  public int Val() {
    return value;
  }
}
