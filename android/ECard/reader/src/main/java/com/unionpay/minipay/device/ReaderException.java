package com.unionpay.minipay.device;

public class ReaderException extends Exception {

	private static final long serialVersionUID = 1;

	public ReaderException() {
	}

	public ReaderException(String detailMessage) {
		super(detailMessage);
	}

	public ReaderException(Throwable throwable) {
		super(throwable);
	}

	public ReaderException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}
}
