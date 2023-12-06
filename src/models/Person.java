package models;
 
import utils.Model;          //模型超类
import utils.Manager;        //模型管理器
import utils.DBField;        //字段注解
import utils.FormException;  //表单验证异常
 
 
public class Person extends Model {
	public static Manager objects;  //模型管理器对象
	//public static String table = "XXX";   表名，不设置则默认使用类名的小写，即:person
	
	@DBField(pk=true)
	private String id;	  //编号
	
	@DBField
	private String name;  //姓名
	
	@DBField
	private int age;	  //年龄
	
    public Person() {}
 
	public String getId() {
		return id;
	}
 
	public void setId(String id) {
		this.id = id;
	}
 
	public String getName() {
		return name;
	}
 
	public void setName(String name) {
		this.name = name;
	}
 
	public int getAge() {
		return age;
	}
 
	public void setAge(int age) {
		if(age < 0) {
			throw new FormException("年龄不能小于0");
		}
		this.age = age;
	}
}