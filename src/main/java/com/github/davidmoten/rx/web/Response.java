package com.github.davidmoten.rx.web;

import rx.Observable;

public class Response {
	Observable<Integer> code;
	Observable<Header> headers;
	Observable<byte[]> body;
}
