package com.github.davidmoten.rx.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.StringObservable;
import rx.subscriptions.Subscriptions;

public final class ServerObservable {

	public static Observable<Socket> from(final int port) {
		return Observable.create(new OnSubscribe<Socket>() {

			@Override
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
			@Override
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

					@Override
					public Observable<RequestResponse> call(final Socket socket) {
						System.out.println("\nreading request from " + socket);
						try {
							InputStream is = socket.getInputStream();
							return toRequestResponse(socket, is);

						} catch (IOException e) {
							return Observable.error(e);
						}
					}

				});
	}

	static Observable<RequestResponse> toRequestResponse(final Socket socket,
			InputStream is) {
		Observable<byte[]> bytes = StringObservable.from(is);
		Observable<byte[]> requestHeaderAndMessageBody = aggregateHeader(bytes,
				requestTerminator);

		Observable<byte[]> cache = requestHeaderAndMessageBody.cache();

		final Observable<String> header = StringObservable.decode(
				cache.first(), Charset.forName("US-ASCII"));

		Observable<RequestResponse> result = StringObservable
		// split by line feed
				.split(header, "\n")
				// log line
				.doOnNext(LOG)
				// stop when encounter second blank line
				.takeWhile(lessThanTwoEmptyLines())
				// aggregate lines as list
				.toList()
				// parse the lines as a request
				.map(toRequestResponse(socket, cache.skip(1)));
		return result;
	}

	private static byte[] requestTerminator = new byte[] { '\r', '\n', '\r',
			'\n' };

	/**
	 * All bytes up to the occurrence of \r\n\r\n are part of the request and
	 * following that is the message body. The first byte array emitted is the
	 * complete bytes of the request headers and then follows a sequence of
	 * message body segments.
	 * 
	 * @param bytes
	 * @return
	 */
	public static Observable<byte[]> aggregateHeader(Observable<byte[]> source,
			final byte[] pattern) {
		return source.concatMap(new Func1<byte[], Observable<byte[]>>() {

			final ByteMatcher splitMatcher = new ByteMatcher(pattern);
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			AtomicBoolean found = new AtomicBoolean(false);

			@Override
			public Observable<byte[]> call(byte[] bytes) {
				System.out.println("bytes: " + new String(bytes));
				if (found.get())
					return Observable.just(bytes);
				else {
					try {
						buffer.write(bytes);
						byte[] array = buffer.toByteArray();
						int index = splitMatcher.search(array);
						if (index != -1) {
							found.set(true);
							byte[] requestHeader = Arrays.copyOfRange(array, 0,
									index);
							if (index + splitMatcher.patternLength() == array.length) {
								// release some memory
								buffer.reset();
								return Observable.just(requestHeader);
							} else {
								byte[] rest = Arrays.copyOfRange(array, index
										+ splitMatcher.patternLength(),
										array.length);
								// release some memory
								buffer.reset();
								return Observable.from(requestHeader, rest);
							}
						} else
							return Observable.empty();
					} catch (IOException e) {
						return Observable.error(e);
					}
				}
			}
		});
	}

	private static Action1<String> LOG = new Action1<String>() {

		@Override
		public void call(String line) {
			System.out.println(line);
		}
	};

	private static Func1<String, Boolean> lessThanTwoEmptyLines() {
		return new Func1<String, Boolean>() {
			AtomicInteger count = new AtomicInteger();

			@Override
			public Boolean call(String line) {
				if (line.trim().length() == 0)
					count.incrementAndGet();
				return count.get() < 1;
			}
		};
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
