package com.effectiveosgi.rt.nanoweb;

public class NanoServletException extends Exception {

	private static final long serialVersionUID = 1L;

	private final int code;

	public NanoServletException(int code, String message) {
		super(message);
		this.code = code;
	}
	
	public int getCode() {
		return code;
	}

}
