package com.github.davidmoten.rx.web;

import java.util.List;
import java.util.Map;

public class Request {

	// private static Pattern requestLine = Pattern.compile("\\w+ (\\S+) ")
	Map<String, String> parameters;
	Map<String, String> headers;
	String path;
	Method method;

	public Request(List<String> lines) {
		this.method = getMethod(lines.get(0));
	}

	private static Method getMethod(String line) {
		for (Method method : Method.values()) {
			if (line.startsWith(method.toString()))
				return method;
		}
		throw new RuntimeException("method not found in " + line);
	}
}
