package com.github.davidmoten.rx.web;

import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class Main {
	public static void main(String[] args) throws InterruptedException {
		ServerObservable.requests(8080).observeOn(Schedulers.io())
				.subscribe(new Action1<Request>() {
					public void call(Request request) {
						System.out.println(request);
					}
				});
	}
}
