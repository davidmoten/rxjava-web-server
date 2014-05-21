package com.github.davidmoten.rx.web;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rx.Observable;

public class Request {

	private static Pattern firstLinePattern = Pattern
			.compile("^(\\S+) (\\S+) (\\S+)\\s*$");

	Map<String, String> parameters;
	Map<String, String> headers;
	String path;
	Method method;
	String version;

	private final Observable<byte[]> messageBody;

	public Request(List<String> lines, Observable<byte[]> messageBody) {

		this.messageBody = messageBody;
		String firstLine = lines.get(0);
		Matcher matcher = firstLinePattern.matcher(firstLine);
		if (matcher.matches()) {
			method = Method.valueOf(matcher.group(1));
			path = matcher.group(2);
			version = matcher.group(3);
		} else
			throw new RuntimeException("first line does not match!:'"
					+ firstLine + "'");
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Request [parameters=");
		builder.append(parameters);
		builder.append(", headers=");
		builder.append(headers);
		builder.append(", path=");
		builder.append(path);
		builder.append(", method=");
		builder.append(method);
		builder.append(", version=");
		builder.append(version);
		builder.append("]");
		return builder.toString();
	}

}
