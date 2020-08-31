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

public class GenDefs {
  // Message types - these types are set by the MC and returned in the message header - first byte (INVALID_FRAME is local definition for bad transmission)
  public enum FrameTypeEnum { INVALID_FRAME, ACKNOWLEDGE_FRAME, DATA_FRAME, BINARY_FRAME, CONTROL_FRAME, PROMPT_FRAME, ERROR_FRAME, ASYNC_FRAME, ASYNC2_FRAME, ASYNC_OTHER };
  // local message types - used to transfer data from the connector to the application
  //public enum UserFrameTypes { Info, Error, Data, Prompt, Ack }

  
}
