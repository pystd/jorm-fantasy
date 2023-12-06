package utils;


public class FormException extends RuntimeException {
	/**
	 * 表单验证异常
	 */
	private static final long serialVersionUID = 1L;

	public FormException() {
		
	}
	
	public FormException(String message) {
		super(message);
	}
}
