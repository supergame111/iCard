package ftsafe.utils;

public class ErrMessage extends Exception{

	private static final long serialVersionUID = 1L;
	
	public ErrMessage() {
		super();
	}   
	public ErrMessage(String msg) {  
        super(msg);  
    } 
}
