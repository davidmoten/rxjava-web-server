package com.github.davidmoten.rx.web;

import java.util.HashMap;
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
	Long contentLength;

	private final Observable<byte[]> messageBody;

	public Request(List<String> lines, Observable<byte[]> messageBody) {

		String firstLine = lines.get(0);
		Matcher matcher = firstLinePattern.matcher(firstLine);
		if (matcher.matches()) {
			method = Method.valueOf(matcher.group(1));
			path = matcher.group(2);
			version = matcher.group(3);
		} else
			throw new RuntimeException("first line does not match!:'"
					+ firstLine + "'");
		headers = getHeaders(lines);

		if ("application/x-www-form-urlencoded".equals(headers
				.get("Content-Type")))
			this.messageBody = ServerObservable.aggregateHeader(messageBody,
					new byte[] { '\n' }).first();
		else
			this.messageBody = Observable.empty();
	}

	private static Map<String, String> getHeaders(List<String> lines) {
		Map<String, String> map = new HashMap<String, String>();
		for (int i = 1; i < lines.size(); i++) {
			String line = lines.get(i);
			int index = line.indexOf(':');
			if (index != -1 && index < line.length() - 1) {
				map.put(line.substring(0, index), line.substring(index + 1));
			}
		}
		return map;
	}

	public Observable<byte[]> getMessageBody() {
		return messageBody;
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
