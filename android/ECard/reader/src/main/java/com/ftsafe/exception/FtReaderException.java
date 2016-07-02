package com.ftsafe.exception;

public class FtReaderException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public FtReaderException() {   
		super();   
	}   
	public FtReaderException(String msg) {  
        super(msg);  
    } 
	public FtReaderException(Throwable throwable) {
		    super(throwable);
	}

	public FtReaderException(String detailMessage, Throwable throwable) {
		    super(detailMessage, throwable);
	}
}
