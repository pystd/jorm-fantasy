package utils;

import java.util.Collections;

//日志类
public abstract class logger {
	private static void log(Object obj, boolean is_error) {
		String msg = String.format("[%s]%s", Time.formatTime(), String.valueOf(obj));
		
		if (is_error) {
			System.err.printf(msg);
		}else {
			System.out.printf(msg);
		}
	}
	
	//打印普通信息
	public static void info(Object obj) {
		log(obj, false);
	}
	
	//打印错误信息
	public static void error(Object obj) {
		log(obj, true);
	}
	
	//打印字符串若干次
	public static void repeat(String str, int count) {
		System.out.println(String.join("", Collections.nCopies(count, str)));
	}
}
