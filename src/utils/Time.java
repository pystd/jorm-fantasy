package utils;

import java.text.SimpleDateFormat;
import java.util.Date;


//时间类
public abstract class Time {
	private static final String DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss";  
	
	//默认格式的当前时间
	public static String formatTime() {
		return new SimpleDateFormat(DEFAULT_FORMAT).format(new Date());
	}
	
	//指定格式的当前时间
	public static String formatTime(String f) {
		return new SimpleDateFormat(f).format(new Date());
	}
}
