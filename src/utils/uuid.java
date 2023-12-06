package utils;

import java.util.UUID;


public class uuid {
	//生成长度32位的随机字符
	public static String uuid4() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
