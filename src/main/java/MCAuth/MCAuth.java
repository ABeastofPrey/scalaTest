package MCAuth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.json.JSONException;
import org.json.JSONObject;


public class MCAuth {
    
  private static final Random RANDOM = new SecureRandom();
  private static final int ITERATIONS = 10000;
  private static final int KEY_LENGTH = 256;
  private static final int TOKEN_EXPIRE_TIME = 24 * 60 * 60 * 1000; // 24 hours

  public static byte[] getNextSalt() {
    byte[] salt = new byte[16];
    RANDOM.nextBytes(salt);
    return salt;
  }

  public static byte[] hash(char[] password, byte[] salt) {
    PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
    Arrays.fill(password, Character.MIN_VALUE);
    try {
      SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      return skf.generateSecret(spec).getEncoded();
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new AssertionError("Error while hashing a password: " + e.getMessage(), e);
    } finally {
      spec.clearPassword();
    }
  }

  public static boolean isExpectedPassword(char[] password, byte[] salt, byte[] expectedHash) {
    byte[] pwdHash = hash(password, salt);
    Arrays.fill(password, Character.MIN_VALUE);
    if (pwdHash.length != expectedHash.length)
      return false;
    for (int i = 0; i < pwdHash.length; i++) {
      if (pwdHash[i] != expectedHash[i])
        return false;
    }
    return true;
  }   

  /* 
    THIS TOKEN IS USED TO AUTHENTICATE A USER FOR SOME OPERATIONS
    THIS TOKEN EXPIRES IN TOKEN_EXPIRE_TIME MILLISECONDS.
  */
  public static String generateToken(String username, int permission) {
    JSONObject data = new JSONObject();
    data.put("username", username);
    data.put("permission",permission);
    long nowMillis = System.currentTimeMillis();
    try {
      Algorithm algorithm = Algorithm.HMAC256("Servotronix");
      String token = JWT.create()
        .withIssuer("auth0")
        .withExpiresAt(new Date(nowMillis + TOKEN_EXPIRE_TIME))
        .withSubject(data.toString())
        .sign(algorithm);
      return token;
    } catch (UnsupportedEncodingException exception){
      exception.printStackTrace();
      return null;
    } catch (JWTCreationException exception){
      exception.printStackTrace();
      return null;
    }
  }

  /*
    THIS FUNCTION RECEIVES A TOKEN
    AND RETURNS -1 IF IT'S INVALID, OR THE USER'S PERMISSION IF IT'S A VALID TOKEN
  */
  public static int verifyToken(String token) {
    if (token == null)
      return -1;
    try {
      Algorithm algorithm = Algorithm.HMAC256("Servotronix");
      JWTVerifier verifier = JWT.require(algorithm)
        .withIssuer("auth0")
        .build(); //Reusable verifier instance
      DecodedJWT jwt = verifier.verify(token);
      JSONObject data = new JSONObject(jwt.getSubject());
      int permission = data.getInt("permission");
      if (jwt.getExpiresAt().getTime() > System.currentTimeMillis())
        return permission;
    } catch (UnsupportedEncodingException exception){
        return -1;
    } catch (JWTVerificationException exception){
        return -1;
    } catch (JSONException e) { // invalid token
      return -1;
    }
    return -1;
  }
  
  /*
    THIS FUNCTION RECEIVES A TOKEN
    AND RETURNS -1 IF IT'S INVALID, OR THE USER'S PERMISSION IF IT'S A VALID TOKEN
  */
  public String getUsernameFromToken(String token) {
    if (token == null)
      return null;
    try {
      Algorithm algorithm = Algorithm.HMAC256("Servotronix");
      JWTVerifier verifier = JWT.require(algorithm)
        .withIssuer("auth0")
        .build(); //Reusable verifier instance
      DecodedJWT jwt = verifier.verify(token);
      JSONObject data = new JSONObject(jwt.getSubject());
      if (jwt.getExpiresAt().getTime() > System.currentTimeMillis())
        return data.getString("username");
    } catch (UnsupportedEncodingException exception){
        return null;
    } catch (JWTVerificationException exception){
        return null;
    } catch (JSONException e) { // invalid token
      return null;
    }
    return null;
  }
    
}


