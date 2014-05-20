package com.github.davidmoten.rx.web;

public enum RequestHeader {

	ACCEPT("Accept");

	private String name;

	private RequestHeader(String name) {
		this.name = name;
	}

	public String key() {
		return name;
	}

}
