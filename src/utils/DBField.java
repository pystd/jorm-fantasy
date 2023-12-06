package utils;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;


/**
 * 数据库字段
 */
@Retention(RUNTIME)  //生命周期设为运行期
@Target(FIELD) 			//仅应用于属性
public @interface DBField{
	boolean auto() default false;  	  //是否自动生成值
	boolean pk() default false;    //是否为主键
}
