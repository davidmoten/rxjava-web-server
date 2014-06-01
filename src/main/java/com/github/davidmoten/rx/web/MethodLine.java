package com.github.davidmoten.rx.web;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MethodLine {
    private static final Pattern firstLinePattern = Pattern.compile("^(\\S+) (\\S+) (\\S+)\\s*$");

    private final Method method;
    private final String path;
    private final String version;

    public MethodLine(Method method, String path, String version) {
        this.method = method;
        this.path = path;
        this.version = version;
    }

    public Method getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getVersion() {
        return version;
    }

    public static MethodLine parse(String line) {
        Matcher matcher = firstLinePattern.matcher(line);
        if (matcher.matches()) {
            Method method = Method.valueOf(matcher.group(1));
            String path = matcher.group(2);
            String version = matcher.group(3);
            return new MethodLine(method, path, version);
        } else
            throw new RuntimeException("first line does not match!:'" + line + "'");
    }

}
