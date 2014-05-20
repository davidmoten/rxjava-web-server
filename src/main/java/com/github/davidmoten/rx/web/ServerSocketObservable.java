package com.github.davidmoten.rx.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.StringObservable;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

public class ServerSocketObservable {

	public static Observable<Socket> from(final int port) {
		return Observable.create(new OnSubscribe<Socket>() {

			public void call(Subscriber<? super Socket> subscriber) {
				try {
					final ServerSocket serverSocket = new ServerSocket(port);

					Subscription closeServerSocket = Subscriptions
							.create(new Action0() {
								public void call() {
									try {
										serverSocket.close();
									} catch (IOException e) {
										throw new RuntimeException(e);
									}
								}
							});

					subscriber.add(closeServerSocket);

					while (!subscriber.isUnsubscribed()) {
						try {
							Socket socket = serverSocket.accept();
							subscriber.onNext(socket);
						} catch (SocketTimeoutException e) {
							// ignore, just wait again
						} catch (IOException e) {
							subscriber.onError(e);
						} catch (SecurityException e) {
							subscriber.onError(e);
						} catch (IllegalBlockingModeException e) {
							subscriber.onError(e);
						}
					}

				} catch (IOException e) {
					subscriber.onError(e);
				}
			}
		});
	}

	public static Observable<String> lines(final int port) {
		return from(port).observeOn(Schedulers.io()).flatMap(
				new Func1<Socket, Observable<String>>() {

					public Observable<String> call(final Socket socket) {
						try {
							Observable<byte[]> bytes = StringObservable
									.from(socket.getInputStream());
							Observable<String> decoded = StringObservable
									.decode(bytes, StandardCharsets.UTF_8);
							return StringObservable.split(decoded, "\n")
									.takeWhile(NON_BLANK)
									.doOnCompleted(new Action0() {

										public void call() {
											try {
												PrintWriter out = new PrintWriter(
														socket.getOutputStream());
												out.print("HTTP/1.1 200 OK\r\n");
												out.print("Content-Type: text/plain\r\n");
												out.print("\r\n");
												out.print("Got the message "
														+ new Date());
												out.close();
												socket.close();
												System.out
														.println("-- closed socket");
											} catch (IOException e) {
												throw new RuntimeException(e);
											}
										}
									});
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

	public static void main(String[] args) throws InterruptedException {
		lines(8080).subscribe(new Action1<String>() {
			public void call(String line) {
				System.out.println(line.trim());
			}
		});
	}
}
