package com.github.davidmoten.rx.web;

import java.net.Socket;
import java.nio.charset.Charset;
import java.util.List;

import rx.Observable;
import rx.Observable.Operator;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Func1;
import rx.observables.StringObservable;
import rx.observers.Subscribers;
import rx.subjects.PublishSubject;

class OperatorRequest implements Operator<RequestResponse, byte[]> {

	private final Socket socket;

	OperatorRequest(Socket socket) {
		this.socket = socket;
	}

	@Override
	public Subscriber<? super byte[]> call(
			final Subscriber<? super RequestResponse> child) {
		Subscriber<byte[]> parent = Subscribers.from(new Observer<byte[]>() {

			boolean first = true;
			PublishSubject<byte[]> messageBody = PublishSubject.create();

			@Override
			public void onCompleted() {
				child.onCompleted();
			}

			@Override
			public void onError(Throwable e) {
				child.onError(e);
			}

			@Override
			public void onNext(byte[] bytes) {
				if (first) {
					final Observable<String> header = StringObservable.decode(
							Observable.just(bytes), Charset.forName("US-ASCII"));

					RequestResponse result = StringObservable
							// split by line feed
							.split(header, "\r\n")
							// log line
							// .doOnNext(LOG)
							// aggregate lines as list
							.toList()
							// parse the lines as a request
							.map(toRequestResponse(socket, messageBody))
							.toBlockingObservable().single();
					if (result.request().method.equals(Method.GET)) {
						child.onNext(result);
						messageBody.onCompleted();
						// unsub
					} else if (result.request().method.equals(Method.POST)) {
						child.onNext(result);
					}
				} else {
					messageBody.onNext(bytes);
				}
				first = false;
			}
		});
		child.add(parent);
		return parent;
	}

	private static Func1<List<String>, RequestResponse> toRequestResponse(
			final Socket socket, final Observable<byte[]> messageBody) {
		return new Func1<List<String>, RequestResponse>() {

			@Override
			public RequestResponse call(List<String> lines) {
				return new RequestResponse(new Request(lines, messageBody),
						new Response(socket));
			}
		};
	}
}