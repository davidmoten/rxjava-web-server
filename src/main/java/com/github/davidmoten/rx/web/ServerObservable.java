package com.github.davidmoten.rx.web;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.observables.StringObservable;
import rx.subscriptions.Subscriptions;

public final class ServerObservable {

	public static Observable<Socket> from(final int port) {
		return Observable.create(new OnSubscribe<Socket>() {

			public void call(Subscriber<? super Socket> subscriber) {
				try {
					final ServerSocket serverSocket = new ServerSocket(port);
					Subscription closeServerSocket = closingSubscription(serverSocket);
					subscriber.add(closeServerSocket);
					System.out.println("listening on port " + port);
					while (!subscriber.isUnsubscribed()) {
						try {
							Socket socket = serverSocket.accept();
							subscriber.onNext(socket);
						} catch (SocketTimeoutException e) {
							// ignore, just wait again
						} catch (Exception e) {
							subscriber.onError(e);
						}
					}
				} catch (IOException e) {
					subscriber.onError(e);
				}
			}

		});
	}

	private static Subscription closingSubscription(
			final ServerSocket serverSocket) {
		return Subscriptions.create(new Action0() {
			public void call() {
				try {
					serverSocket.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	public static Observable<RequestResponse> requests(final int port) {
		return from(port).flatMap(
				new Func1<Socket, Observable<RequestResponse>>() {

					public Observable<RequestResponse> call(final Socket socket) {
						try {
							Observable<byte[]> bytes = StringObservable
									.from(socket.getInputStream());
							final Observable<String> decoded = StringObservable
									.decode(bytes, StandardCharsets.UTF_8);

							return StringObservable
							// split by line feed
									.split(decoded, "\n")
									// stop when encounter blank line
									.takeWhile(NON_BLANK)
									// aggregate lines as list
									.toList()
									// parse the lines as a request
									.map(toRequestResponse(socket));
						} catch (IOException e) {
							return Observable.error(e);
						}
					}
				});
	}

	private static Func1<String, Boolean> NON_BLANK = new Func1<String, Boolean>() {

		public Boolean call(String line) {
			return line.trim().length() > 0;
		}
	};

	private static Func1<List<String>, RequestResponse> toRequestResponse(
			final Socket socket) {
		return new Func1<List<String>, RequestResponse>() {

			public RequestResponse call(List<String> lines) {
				return new RequestResponse(new Request(lines), new Response(
						socket));
			}
		};
	}

}
