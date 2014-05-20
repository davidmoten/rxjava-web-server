package com.github.davidmoten.rx.web;

import java.io.PrintWriter;
import java.util.Date;

import rx.Observer;
import rx.schedulers.Schedulers;

public class Main {

	public static void main(String[] args) throws InterruptedException {

		ServerObservable.requests(8080).observeOn(Schedulers.io())
				.subscribe(new Observer<RequestResponse>() {

					public void onCompleted() {

					}

					public void onError(Throwable e) {
						e.printStackTrace();
					}

					public void onNext(RequestResponse r) {
						System.out.println(r.request());
						try {
							PrintWriter out = new PrintWriter(r.response()
									.getOutputStream());
							out.print("HTTP/1.1 200 OK\r\n");
							out.print("Content-Type: text/plain\r\n");
							out.print("\r\n");
							out.print("Got the message " + new Date());
							out.close();
							r.response().close();
						} catch (RuntimeException e) {
							e.printStackTrace();
						}
					}
				});
	}
}
