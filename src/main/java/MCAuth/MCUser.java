package MCAuth;

import java.io.Serializable;

public class MCUser implements Serializable{
    
    private String username;
    private String token;
    private int permission;

    public MCUser(String username, String token, int permission) {
        this.username = username;
        this.token = token;
        this.permission = permission;
    }

    public String getUsername() {
        return username;
    }

    public String getToken() {
        return token;
    }

    public int getPermission() {
        return permission;
    }

}
