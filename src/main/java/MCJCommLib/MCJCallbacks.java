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
public interface MCJCallbacks {
    // received new buffer
    void OnNewBufferCallback(byte[] buffer, int filledSize);
    // delegate used to export incoming data
    void OnNewMessageCAllback (String newMsg);
    // general purpose delegate for incoming data
    void OnMultiPurpDataCallback(GenDefs.FrameTypeEnum frameType, String msg, int id);
    // delegate used to export connection status
    void OnConnectionStateChangeCallback (boolean connected);
    
}
