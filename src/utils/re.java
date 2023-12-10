package utils;

import java.util.regex.Pattern;


public class re {
	private static Pattern pattern_ipv4 = Pattern.compile(
		"^((25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)\\.){3}" + 
	    "(25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)$"
	);
	
	//匹配ipv4
	public static boolean match_ipv4(String s) {
		if (s == null) {
			return false;
		}
		return pattern_ipv4.matcher(s).find();  //是否匹配成功
	}
}
