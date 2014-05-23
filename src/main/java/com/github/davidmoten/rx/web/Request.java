package com.github.davidmoten.rx.web;

import static com.github.davidmoten.rx.web.ByteObservable.split;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rx.Observable;
import rx.observables.StringObservable;

public class Request {

	private static final String CONTENT_TYPE = "Content-Type";

	private static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";

	private static final Object CONTENT_LENGTH = "Content-Length";

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
		System.out.println("mbody="
				+ new String(messageBody.first().toBlockingObservable()
						.single()));

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

		if (headers.get(CONTENT_LENGTH) != null
				&& APPLICATION_X_WWW_FORM_URLENCODED.equals(headers
						.get(CONTENT_TYPE))) {
			int length = Integer.parseInt(headers.get(CONTENT_LENGTH));
			System.out.println("using length!!!!!!!!!!!!!!!!!");
			this.messageBody = messageBody.lift(split(length)).first();
			List<String> list = StringObservable
					.split(StringObservable.decode(messageBody,
							Charset.forName("UTF-8")), "\n").toList()
					.toBlockingObservable().single();
			System.out.println("messageBody=" + list);
		} else
			this.messageBody = Observable.empty();
	}

	private static Map<String, String> getHeaders(List<String> lines) {
		Map<String, String> map = new HashMap<String, String>();
		for (int i = 1; i < lines.size(); i++) {
			String line = lines.get(i);
			int index = line.indexOf(':');
			if (index != -1 && index < line.length() - 2) {
				// for value skip ':' and ' '
				map.put(line.substring(0, index), line.substring(index + 2)
						.trim());
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
