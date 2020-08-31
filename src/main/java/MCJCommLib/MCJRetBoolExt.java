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
public class MCJRetBoolExt {
    public boolean b = true;
    public String msgStr = "";
    
    public MCJRetBoolExt (boolean res) {
        b = res;
    }

    public MCJRetBoolExt (boolean res, String s) {
        b = res;
        msgStr=s;
    }
}
