package utils;


import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;


//数据库操作类
public class DB {
	//主机地址
	private static final String HOST = "localhost";
	//端口
	private static final int PORT = 3306;
	//数据库名
	private static final String DB = "router";
	//账号
	private static final String USER = "root";  
	//密码
	private static final String PASS = "123456";
	//jdbc的URL
	private static final String URL = String.format("jdbc:mysql://%s:%d/%s", HOST, PORT, DB);
		
	private static Connection conn = null;
	
	//连接数据库
	public static Connection connect() {
		try {
			if(conn == null || conn.isClosed()) {
				conn = DriverManager.getConnection(URL, USER, PASS);
			}
		}catch(SQLException e) {
			logger.error("数据库连接失败\n");
			e.printStackTrace();
		}
		return conn;
	}

	//关闭数据库连接
	public static void closeDB() {
		try {
			if(conn != null && !conn.isClosed()) {
				conn.close();
			}
		}catch(SQLException e) {
			logger.error("关闭数据库失败\n");
			e.printStackTrace();
		}
	}
	
	//关闭语句
	public static void closeStatement(Statement smt) {
		try {
			if(smt != null && !smt.isClosed()) {
				smt.close();
			}
		}catch(SQLException e) {
			logger.error("关闭语句失败\n");
			e.printStackTrace();
		}
	}
	
	//关闭结果集
	public static void closeResultSet(ResultSet rs) {
		try {
			if(rs != null && !rs.isClosed()) {
				rs.close();
			}
		}catch(SQLException e) {
			logger.error("关闭结果集失败\n");
			e.printStackTrace();
		}
	}

	//执行查询操作
	public static Set<Map<String, Object>> executeQuery(String sql) {
		Statement smt = null;
		ResultSet rs = null;
		Set<Map<String, Object>> set = new HashSet<>();
		
		try {
			connect();
			smt = conn.createStatement();
			rs = smt.executeQuery(sql);
			
			//构造每行数据
			while(rs.next()) {
				ResultSetMetaData metadata = rs.getMetaData();
				int field_count = metadata.getColumnCount();  //获取字段个数
				Map<String, Object> map = new HashMap<>();
				
				//构造每列数据
				for(int i=1; i<=field_count; i++) {
					String name = metadata.getColumnName(i);  //获取字段名
					Object value = rs.getObject(name);		  //获取字段值
					map.put(name, value);  //保存本列数据
				}
				
				set.add(map);  		   //保存本行数据
			}
			
		}catch(Exception e) {
			logger.error("SQL语句执行失败: " + sql + "\n");
			e.printStackTrace();
		}finally {
			closeResultSet(rs);
			closeStatement(smt);
			closeDB();
		}
		
		return set;
	}
	
	//执行更新操作（增、删、改）
	public static int executeUpdate(String sql) {
		Statement smt = null;
		int count = 0;
		
		try {
			connect();
			smt = conn.createStatement();
			count = smt.executeUpdate(sql);
		}catch(SQLException e) {
			logger.error("SQL语句执行失败: " + sql + "\n");
			e.printStackTrace();
		}finally {
			closeStatement(smt);
			closeDB();
		}
		
		return count;
	}
}
