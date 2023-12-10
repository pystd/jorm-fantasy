package utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;


//对象管理器
public class Manager{
	private Class<? extends Model> model;    	  //当前模型类
	private Constructor<Model> constructor;  	  //无参构造方法
	private String table;	  					  //表名
	private String pk;		  					  //主键名
	private Method pk_setter;  					  //主键的setter方法;
	private Map<String, Method> setter_map;  	  //有参数的setter方法
	private Map<String, Method> auto_setter_map;  //无参数的setter方法
	private Set<String> columns;  				  //所有字段名（数据库）
	
	Manager(
		Class<? extends Model> model,
		Constructor<Model> constructor,
		String table,
		String pk,
		Method pk_setter,
		Map<String, Method> setter_map,
		Map<String, Method> auto_setter_map,
		Set<String> columns
	) {
		this.model = model;
		this.constructor = constructor;
		this.table = table;
		this.pk = pk;
		this.pk_setter = pk_setter;
		this.setter_map = setter_map;
		this.auto_setter_map = auto_setter_map;
		this.columns = columns;
	}
	
	//转换值，让数据库系统能识别
	static String to_sql(Object value) {
		if(value instanceof String) {
			return "'" + value + "'";  //字符串用单引号括起来
		}else {
			return String.valueOf(value);
		}
	}
	
	//检查属性是不是数据库表的字段
	static boolean is_db_field(Field field) {
		return field.isAnnotationPresent(DBField.class);  //是否应用了自定义注解
	}
	
	//通过无参构造方法创建模型类的对象
	@SuppressWarnings("unchecked")
	private <T extends Model> T new_model_instance() {
		T instance = null;
		
		//调用无参构造方法，创建对象
		try {
			instance = (T)this.constructor.newInstance();
		} catch (InstantiationException
				| IllegalAccessException
				| IllegalArgumentException
				| InvocationTargetException
				| SecurityException e) {
			logger.error("模型类 <" + this.model + ">的对象创建失败\n");
			e.printStackTrace();
		}
		return instance;
	}
	
	//完善对象，用于查询操作
	private <T extends Model> T getObjBySelect(Map<String, Object> params) {
		T obj = new_model_instance();
		if(obj == null) {
			return null;
		}
		
		//调用指定属性对应的有参setter方法
		for(Map.Entry<String, Object> entry: params.entrySet()) {
			String name = entry.getKey();
			Object value = entry.getValue();
			
			try {
				//该属性存在无参setter方法，或该属性是主键，使用查询到的值
				Method m = this.auto_setter_map.get(name);
				if(m != null || name.equals(this.pk)) {
					//直接设置属性的值
					Field field = this.model.getDeclaredField(name);
					field.setAccessible(true);
					field.set(obj, value);		
				}
				
				//该属性存在有参setter方法，调用该方法
				else {
					m = this.setter_map.get(name);
					if(m != null) {
						m.invoke(obj, value);
					}else {
						logger.error("属性 " + name + " 既不存在无参setter方法，也不存在有参setter方法，请检查\n");
						return null;
					}
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				logger.error("调用属性 " + name + " 的有参setter方法失败\n");
				e.printStackTrace();
				return null;
			} catch (FormException e) {  //表单验证失败
				logger.error(e + "\n");
				return null;
			} catch (Exception e) {
				logger.error("属性设置失败\n");
				e.printStackTrace();
				return null;
			}
		}
		return obj;
	}
	
	//创建对象，并更新参数
	private Map<String, Object> update_params(Map<String, Object> params, boolean is_create){
		Model obj = new_model_instance();
		if(obj == null) {
			return null;
		}
		Map<String, Object> new_params = new HashMap<>();
		
		//调用指定属性对应的有参setter方法
		for(Map.Entry<String, Object> entry: params.entrySet()) {
			String name = entry.getKey();
			
			try {
				//该属性存在无参setter方法，或该属性是主键，跳过
				Method m = this.auto_setter_map.get(name);
				if(m != null || name.equals(this.pk)) {
					continue;
				}
				
				//该属性存在有参setter方法，调用该方法
				else {
					m = this.setter_map.get(name);
					if(m != null) {
						m.invoke(obj, entry.getValue());
						new_params.put(name, entry.getValue());
					}else {
						logger.error("属性 " + name + " 既不存在无参setter方法，也不存在有参setter方法，请检查\n");
						return null;
					}
				}
			} catch (InvocationTargetException e) {
				logger.error(e.getCause().getMessage() + "\n");  //获取setter方法中产生的异常信息
				return null;
			} catch (IllegalAccessException | IllegalArgumentException e) {
				logger.error("调用属性 " + name + " 的有参setter方法失败\n");
				e.printStackTrace();
				return null;
			} catch (Exception e) {
				logger.error("属性设置失败\n");
				e.printStackTrace();
				return null;
			}
		}
		
		//调用所有无参setter方法
		for(Map.Entry<String, Method> entry: this.auto_setter_map.entrySet()) {
			String name = entry.getKey();
			Method m = entry.getValue();
			
			try {
				m.invoke(obj);
				//获取该属性的值
				Field field = this.model.getDeclaredField(name);
				field.setAccessible(true);
				new_params.put(name, field.get(obj));
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				logger.error("调用无参setter方法" + m.getName() + "失败\n");
				e.printStackTrace();
				return null;
			} catch (Exception e) {
				logger.error("属性的值获取失败\n");
				e.printStackTrace();
				return null;
			}
		}
		
		//如果是创建操作，调用主键的无参setter方法，生成主键的值
		if(is_create) {
			try {
				this.pk_setter.invoke(obj);
				//获取该属性的值
				Field field = this.model.getDeclaredField(this.pk);
				field.setAccessible(true);
				new_params.put(this.pk, field.get(obj));
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				logger.error("调用主键 " + this.pk + " 的无参setter方法失败\n");
				e.printStackTrace();
				return null;
			} catch (Exception e) {
				logger.error("主键" + this.pk + "的值获取失败\n");
				e.printStackTrace();
				return null;
			}
		}
		
		return new_params;
	}
	
	//查询所有数据
	public <T extends Model> Set<T> all() {
		String sql = String.format(
			"select %s from %s;",
			String.join(",", this.columns),
			this.table
		);
		//执行SQL语句
		Set<Map<String, Object>> data = DB.executeQuery(sql);
		
		Set<T> objs= new HashSet<>();
		for(Map<String, Object> row: data) {
			T obj = getObjBySelect(row);
			if(obj != null) {
				objs.add(obj);
			}
		}
		return objs;
	}
	
	//查询指定数据
	public <T extends Model> Set<T> filter(Map<String, Object> params) {
		String[] where_params = new String[params.size()];
		int i = 0;
		
		for(Map.Entry<String, Object> m: params.entrySet()) {
			where_params[i] = m.getKey() + "=" + to_sql(m.getValue());
			i++;
		}
		
		String sql = String.format(
			"select %s from %s where %s;",
			String.join(",", this.columns),
			this.table,
			String.join(",", where_params)
		);
		//执行SQL语句
		Set<Map<String, Object>> data = DB.executeQuery(sql);
		
		Set<T> objs= new HashSet<>();
		for(Map<String, Object> row: data) {
			T obj = getObjBySelect(row);
			if(obj != null) {
				objs.add(obj);
			}
		}
		return objs;
	}
	
	//查询所有数据，并排序
	public <T extends Model> Set<T> order_by(String field, boolean reverse) {
		String sql = String.format(
			"select %s from %s order by %s %s;",
			String.join(",", this.columns),
			this.table,
			field,
			reverse ? "desc" : "asc"
		);
		//执行SQL语句
		Set<Map<String, Object>> data = DB.executeQuery(sql);
		
		Set<T> objs= new HashSet<>();
		for(Map<String, Object> row: data) {
			T obj = getObjBySelect(row);
			if(obj != null) {
				objs.add(obj);
			}
		}
		return objs;
	}

	//按主键查询一条数据
	public <T extends Model> T get(Object pk) {
		String sql = String.format(
			"select %s from %s where %s=%s;",
			String.join(",", this.columns),
			this.table,
			this.pk,
			to_sql(pk)
		);
		
		T obj = null;
		Set<Map<String, Object>> data = DB.executeQuery(sql);
		for(Map<String, Object> row: data) {
			obj = getObjBySelect(row);
			break;  //只取第一条数据
		}
		return obj;
	}
	
	//添加一条数据
	public int create(Map<String, Object> params) {
		params = update_params(params, true);
		if(params == null) {
			return 0;
		}
		
		String[] fields = new String[params.size()];
		String[] values = new String[params.size()];
		int i = 0;
		
		for(Map.Entry<String, Object> m: params.entrySet()) {
			fields[i] = m.getKey();
			values[i] = to_sql(m.getValue());
			i++;
		}
		
		String sql = String.format(
			"insert into %s (%s) values (%s);",
			this.table,
			String.join(",", fields),
			String.join(",", values)
		);
		//执行SQL语句
		return DB.executeUpdate(sql);
	}
	
	//按主键修改一条数据
	public int update(Object pk, Map<String, Object> params) {
		//创建对象，调用set方法，做表单验证
		params = update_params(params, false);
		if(params == null) {
			return 0;
		}
		
		String[] set_params = new String[params.size()];
		int i = 0;
		for(Map.Entry<String, Object> m: params.entrySet()) {
			set_params[i] = m.getKey() + "=" + to_sql(m.getValue());
			i++;
		}
		
		String sql = String.format(
			"update %s set %s where %s=%s;",
			this.table,
			String.join(",", set_params),
			this.pk,
			to_sql(pk)
		);
		//执行SQL语句
		return DB.executeUpdate(sql);
	}
	
	//按主键删除一条数据
	public int delete(Object pk) {
		String sql = String.format(
			"delete from %s where %s=%s;",
			this.table,
			this.pk,
			to_sql(pk)
		);
		//执行SQL语句
		return DB.executeUpdate(sql);
	}
}