package com.github.davidmoten.rx.web;

import static rx.observables.StringObservable.split;

import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.functions.Func1;
import rx.functions.Functions;
import rx.observables.GroupedObservable;

public class Request {

    private static final String CONTENT_TYPE = "Content-Type";

    private static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";

    private static final Object CONTENT_LENGTH = "Content-Length";

    private final Observable<GroupedObservable<String, Header>> map;

    private final Observable<byte[]> messageBody;

    public Request(Observable<byte[]> header, Observable<byte[]> messageBody) {

        this.messageBody = messageBody;
        map = split(header.map(asString), "\r\n")
        // skip empty lines
                .filter(nonBlankLines)
                // parse header name and value
                .map(toHeader())
                // group by header name
                .groupBy(toHeaderName)
                // cache result for resuse
                .cache();

    }

    public Observable<byte[]> getMessageBody() {
        return messageBody;
    }

    public Observable<MethodLine> getMethodLine() {
        return getHeader("MethodLine")
        // to MethodLine object
                .map(toMethodLine);
    }

    public Observable<String> getHeader(String key) {
        return map
        // select MethodLine line
                .filter(byKey("MethodLine"))
                // flatten
                .flatMap(Functions.<Observable<Header>> identity())
                // to value
                .map(toValue)
                // cache result
                .cache();
    }

    private final Func1<String, MethodLine> toMethodLine = new Func1<String, MethodLine>() {

        @Override
        public MethodLine call(String line) {
            return MethodLine.parse(line);
        }
    };

    private static final Func1<Header, String> toValue = new Func1<Header, String>() {

        @Override
        public String call(Header h) {
            return h.value;
        }
    };

    private static final Func1<GroupedObservable<String, Header>, Boolean> byKey(final String key) {
        return new Func1<GroupedObservable<String, Header>, Boolean>() {

            @Override
            public Boolean call(GroupedObservable<String, Header> group) {
                return key.equals(group.getKey());
            }
        };
    }

    private static final Func1<byte[], String> asString = new Func1<byte[], String>() {

        @Override
        public String call(byte[] bytes) {
            return new String(bytes, Charset.forName("US-ASCII"));
        }
    };

    private static final Func1<String, Header> toHeader() {
        return new Func1<String, Header>() {

            private final AtomicBoolean first = new AtomicBoolean(true);

            @Override
            public Header call(String line) {
                if (first.getAndSet(false))
                    return new Header("MethodLine", line);
                else {
                    int index = line.indexOf(':');
                    // for value skip ':' and ' '
                    return new Header(line.substring(0, index), line.substring(index + 2).trim());
                }
            }
        };
    }

    private static final Func1<Header, String> toHeaderName = new Func1<Header, String>() {

        @Override
        public String call(Header header) {
            return header.name;
        }
    };

    private static final Func1<String, Boolean> nonBlankLines = new Func1<String, Boolean>() {

        @Override
        public Boolean call(String line) {
            return !line.trim().isEmpty();
        }

    };

    private static class Header {
        String name;
        String value;

        Header(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

}
