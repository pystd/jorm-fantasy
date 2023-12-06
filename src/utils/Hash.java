package utils;

import java.math.BigInteger;
import java.security.MessageDigest;


//哈希算法类
public class Hash {
	//md5
	public static String md5(String key) {
		String result = null;
		
		try {
			MessageDigest m = MessageDigest.getInstance("MD5");
			byte[] digit = m.digest(key.getBytes());
			result = new BigInteger(1, digit).toString(16);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
}
