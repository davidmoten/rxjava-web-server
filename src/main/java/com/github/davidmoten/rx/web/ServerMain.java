package com.github.davidmoten.rx.web;

import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;

import rx.Observer;
import rx.observables.StringObservable;
import rx.schedulers.Schedulers;

public class ServerMain {

	public static void main(String[] args) throws InterruptedException {

		ServerObservable.requests(8080).observeOn(Schedulers.io())
				.subscribe(new Observer<RequestResponse>() {

					@Override
					public void onNext(RequestResponse r) {
						System.out.println(r.request());
						System.out.println("--body--");
						List<String> list = StringObservable
								.decode(r.request().getMessageBody(),
										Charset.forName("US-ASCII")).toList()
								.toBlockingObservable().single();
						for (String s : list)
							System.out.print(s);
						System.out.println("--end--");
						try {
							PrintWriter out = r.response().createWriter();
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

					@Override
					public void onError(Throwable e) {
						e.printStackTrace();
					}

					@Override
					public void onCompleted() {

					}
				});
	}
}
