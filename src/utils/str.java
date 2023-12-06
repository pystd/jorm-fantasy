package utils;


//字符串工具类
public class str {
	//首字母大写
	public static String toUpperCase(String s) {
		char[] ch = s.toCharArray();
		//如果该字符是a~z的范围内，转为大写
		if(ch[0] >= 97 && ch[0] <= 122) {
			ch[0] ^= 32;
		}
		return String.valueOf(ch);
	}
}
