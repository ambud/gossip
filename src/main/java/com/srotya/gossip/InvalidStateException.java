package com.srotya.gossip;

public class InvalidStateException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public InvalidStateException(String errorMessage) {
		super(errorMessage);
	}

}
