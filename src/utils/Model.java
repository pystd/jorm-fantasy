package utils;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Field;


public abstract class Model{
	/**
	 * 基础模型类
	 * 
	 * 
	 * 子类需要满足以下要求：
	 *   1.位于models包下
	 *   2.定义静态的objects属性，不可初始化
	 *   3.定义无参构造方法
	 *   4.所有字段应用注解器 @DBField    (参数说明  pk:是否为主键    auto:是否自动生成值)
	 *   5.所有字段的名称和数据库中保持一致
	 *   6.定义主键
	 *   7.定义所有字段对应的setter方法，且方法名称为：set+字段首字母大写形式  (例如：字段name，方法名为setName)
	 *   
	 *   
	 * 管理器对象的可用方法:
	 *   all         查询所有数据
	 *   filter      查询指定数据
	 *   order_by    按指定顺序查询
	 *   get         查询一条数据   
	 *   create      添加一条数据
	 *   update      更新一条数据
	 *   delete      删除一条数据
	 **/
	
	private static final String ATTR_OBJECTS = "objects";  	//模型管理器
	private static final String ATTR_TABLE = "table";		//表名
	private static boolean initialized = false;   			//是否已初始化
	
	static {
		//如果未初始化，则执行初始化
		if(!initialized) {
			init();
		}
	}
	
	public static void init() {
		if(initialized) {
			return;
		}
		
		logger.info("系统初始化...\n");
		
		//查找所有模型类
		Set<Class<Model>> classes = ModelFinder.findAll();
		
		//设置模型类的属性值
		for(Class<Model> cls: classes) {
			//检查该类是否继承自Model类
			if(!Model.class.isAssignableFrom(cls)) {
				logger.error(String.format("模型类 <%s> 不是 <%s> 的子类，请检查", cls, Model.class));
				System.exit(1);
			}
			
			//检查该类是否定义了无参构造方法
			Constructor<Model> constructor = null;
			try {
				constructor = cls.getConstructor();
			} catch (NoSuchMethodException | SecurityException e) {
				logger.error("模型类 <" + cls + "> 未定义无参构造方法，请检查\n");
				e.printStackTrace();
				System.exit(1);
			}
			
			//设置表名
			String table_name = null;
			try {
				Field field = cls.getField(ATTR_TABLE);
				table_name = String.valueOf(field.get(cls));  //获取表名
			}catch(Exception e) {
				table_name = cls.getSimpleName().toLowerCase();  //默认值：类名的小写形式
			}
			
			//设置所有字段名和setter方法对象
			String pk_name = null;
			Set<String> columns = new HashSet<>();
			Method pk_setter = null;
			Map<String, Method> setter_map = new HashMap<>();  //有参setter方法
			Map<String, Method> auto_setter_map = new HashMap<>();  //无参setter方法

			for(Field field: cls.getDeclaredFields()) {
				//跳过非表中的字段
				if(!Manager.is_db_field(field)) {
					continue;
				}
				
				//获取属性名
				String field_name = field.getName();
				//保存属性名
				columns.add(field_name);
				//获取注解对象
				DBField anno = field.getAnnotation(DBField.class);
				
				//如果是主键，保存该值
				if(anno.pk()) {
					pk_name = field_name;
					pk_setter = getSetterMethod(cls, field_name, null);  //主键的setter方法必须无参数
					if(pk_setter == null) {
						logger.error(String.format(
							"模型类 <%s> 未获取到主键 %s 的无参setter方法，请检查",
							cls,
							field_name
						));
						System.exit(1);
					}
				}
				
				//保存无参数的setter方法
				else if(anno.auto()) {
					Method method = getSetterMethod(cls, field_name, null);
					if(method == null) {
						logger.error(String.format(
							"模型类 <%s> 未设置属性 %s 对应的无参setter方法，请检查",
							cls,
							field_name
						));
						System.exit(1);
					}
					auto_setter_map.put(field_name, method);
				}
				
				//保存有参数的setter方法
				else {
					Method method = getSetterMethod(cls, field_name, field.getType());
					if(method == null) {
						logger.error(String.format(
							"模型类 <%s> 未设置属性 %s 对应的有参setter方法，请检查",
							cls,
							field_name
						));
						System.exit(1);
					}
					setter_map.put(field_name, method);
				}
			}
			
			if(pk_name == null) {
				logger.error("模型类 <" + cls + "> 未设置主键，请检查");
				System.exit(1);
			}
			
			//创建管理器对象
			try {
				Field objects = cls.getField(ATTR_OBJECTS);
				Manager manager = new Manager(
					cls,
					constructor,
					table_name,
					pk_name,
					pk_setter,
					setter_map,
					auto_setter_map,
					columns
				);
				objects.set(null, manager);
			} catch (Exception e) {
				logger.error("模型类 <" + cls.getName() + "> 创建Manager对象失败，请检查\n");
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		//初始化完毕，修改标识
		logger.info("初始化完毕\n");
		initialized = true;
	}
	
	//根据属性名获取对应的set方法
	static Method getSetterMethod(Class<Model> cls, String name, Class<?> type) {
		String fun_name = "set" + str.toUpperCase(name);
		
		try {
			if(type == null) {
				return cls.getMethod(fun_name);		   //获取无参数的方法
			}else {
				return cls.getMethod(fun_name, type);  //获取指定参数类型的方法
			}
		} catch (NoSuchMethodException | SecurityException e) {
			logger.error(String.format("模型类 <%s> 获取方法 %s 失败\n", cls, fun_name));
			e.printStackTrace();
			return null;
		}
	}
}


//模型类查找器
class ModelFinder {
	private static final String PKG_NAME = "models";  //模型类所在的包
	
	//查找所有模型类
	public static Set<Class<Model>> findAll() {
		//检查设置的包名
		if(PKG_NAME.endsWith(".")) {
			logger.error("包名不合法，请检查：" + PKG_NAME);
			System.exit(1);
		}
		
		String path = PKG_NAME.replaceAll("[.]", "/");
		InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(path);

		//包下无模型类
		if(stream == null) {
			logger.error("未找到可用的模型类，请检查\n");
			return new HashSet<>();
		}

		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		return reader.lines()
				.filter(line -> line.endsWith(".class"))
				.map(line -> getClass(line))
	      		.collect(Collectors.toSet());
	}
	
	//查找指定模型类
	@SuppressWarnings("unchecked")
	private static <T extends Model> Class<T> getClass(String className) {
		try {
			return (Class<T>)Class.forName(
					PKG_NAME + "." + className.substring(0, className.lastIndexOf('.'))
					);
		} catch (ClassNotFoundException e) {
			logger.error("模型类不存在\n" + e);
			
		} catch (Exception e) {
			logger.error("查找模型类失败\n");
			e.printStackTrace();
		}
		
		return null;
	}
}