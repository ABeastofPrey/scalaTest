package com.servotronix.mcwebserver;

import java.io.File;

public class MCLicenseManager {
    
    public static boolean hasTPLicense(MCConnection conn) {
        String MCKey = conn.getMngr().QueryMC("?sys.SerialNumber",0);
        try {
            File encryptedFile = new File("/var/home/mc/FFS0/MCTP");
            String decrypted = MCEncryptor.decrypt(encryptedFile);
            if (!decrypted.equals(MCKey)) {
                System.out.println("Invalid License File");
                return false;
            }
        } catch (Exception ex) {
            System.out.println("Invalid License File from exception: " + ex.getMessage());
            return false;
        }
        return true;
    }
    
}
