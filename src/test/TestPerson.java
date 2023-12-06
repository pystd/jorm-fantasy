package test;
 
import java.util.Map;
import java.util.Set;
import models.Person;
 
public class Test {
 
	public static void main(String[] args) {
		//查询所有数据
		Set<Person> objs = Person.objects.all();
		
		for(Person obj: objs) {
			System.out.println("编号:" + obj.getId());
			System.out.println("姓名:" + obj.getName());
			System.out.println("年龄:" + obj.getAge());
			System.out.println("----------");
		}
		
		//添加一条数据
		int row = Person.objects.create(Map.of("id", "1", "name", "张三", "age", 18));
		if(row == 1) {
			System.out.println("添加成功");
		}else {
			System.out.println("添加失败");
		}
		
		//修改一条数据
		row = Person.objects.update("1", Map.of("age", 19));
		if(row == 1) {
			System.out.println("修改成功");
		}else {
			System.out.println("修改失败");
		}
		
		//删除一条数据
		row = Person.objects.delete("1");
		if(row == 1) {
			System.out.println("删除成功");
		}else {
			System.out.println("删除失败");
		}
	}
}