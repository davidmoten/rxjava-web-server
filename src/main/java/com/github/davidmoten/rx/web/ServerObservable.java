package com.github.davidmoten.rx.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.StringObservable;
import rx.subscriptions.CompositeSubscription;
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
		Observable<byte[]> bytes = ServerObservable.from(is, 8192).doOnNext(
				logBytes);
		Observable<byte[]> requestHeaderAndMessageBody = aggregateHeader(bytes,
				requestTerminator);

		final Observable<String> header = StringObservable.decode(
				requestHeaderAndMessageBody.first(),
				Charset.forName("US-ASCII")).doOnNext(LOG);

		Observable<RequestResponse> result = StringObservable
		// split by line feed
				.split(header, "\r\n")
				// log line
				.doOnNext(LOG)
				// aggregate lines as list
				.toList()
				// parse the lines as a request
				.map(toRequestResponse(socket,
						requestHeaderAndMessageBody.skip(1)));
		return result;
	}

	private static byte[] requestTerminator = new byte[] { '\r', '\n', '\r',
			'\n' };

	/**
	 * All bytes up to the occurrence of pattern are part of the first item
	 * emitted and following that are the remaining bytes over possibly multiple
	 * emissions.
	 * 
	 * @param bytes
	 * @return
	 */
	public static Observable<byte[]> aggregateHeader(Observable<byte[]> source,
			final byte[] pattern) {

		return source.lift(new OperatorAggregateToPattern(pattern));
	}

	public static class OperatorAggregateToPattern implements
			Operator<byte[], byte[]> {

		private final byte[] pattern;

		OperatorAggregateToPattern(byte[] pattern) {
			this.pattern = pattern;
		}

		@Override
		public Subscriber<? super byte[]> call(
				final Subscriber<? super byte[]> child) {
			CompositeSubscription parent = new CompositeSubscription();
			child.add(parent);
			Subscriber<byte[]> sub = new Subscriber<byte[]>(parent) {

				final ByteMatcher splitMatcher = new ByteMatcher(pattern);
				final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				AtomicBoolean found = new AtomicBoolean(false);

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
					doOnNext(child, bytes, found, buffer, splitMatcher);
				}

			};
			return sub;
		}
	}

	private static void doOnNext(final Subscriber<? super byte[]> child,
			byte[] bytes, AtomicBoolean found, ByteArrayOutputStream buffer,
			ByteMatcher splitMatcher) {
		if (found.get()) {
			child.onNext(bytes);
		} else {
			try {
				buffer.write(bytes);
				byte[] array = buffer.toByteArray();
				int index = splitMatcher.search(array);
				if (index != -1) {
					found.set(true);
					byte[] header = Arrays.copyOfRange(array, 0, index);
					if (index + splitMatcher.patternLength() == array.length) {
						child.onNext(header);
					} else {
						byte[] rest = Arrays.copyOfRange(array, index
								+ splitMatcher.patternLength(), array.length);
						child.onNext(header);
						child.onNext(rest);
					}
					// TODO release memory held by buffer (could be large)
				}
			} catch (IOException e) {
				child.onError(e);
			}
		}
	}

	private static Action1<String> LOG = log("");

	private static Action1<String> log(final String prefix) {
		return new Action1<String>() {

			@Override
			public void call(String line) {
				System.out.println(prefix + " " + line);
			}
		};
	}

	private static final Action1<byte[]> logBytes = new Action1<byte[]>() {

		@Override
		public void call(byte[] bytes) {
			System.out.println("bytes=\n-----------------");
			System.out.println(new String(bytes));
			System.out.println("-----------------");
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

	/**
	 * Reads from the bytes from a source {@link InputStream} and outputs
	 * {@link Observable} of {@code byte[]}s
	 * 
	 * @param i
	 *            Source {@link InputStream}
	 * @param size
	 *            internal buffer size
	 * @return the Observable containing read byte arrays from the input
	 */
	public static Observable<byte[]> from(final InputStream i, final int size) {
		return Observable.create(new OnSubscribe<byte[]>() {
			@Override
			public void call(Subscriber<? super byte[]> o) {
				byte[] buffer = new byte[size];
				try {
					if (o.isUnsubscribed())
						return;
					int n = 0;
					n = i.read(buffer);
					while (n != -1 && !o.isUnsubscribed()) {
						o.onNext(Arrays.copyOf(buffer, n));
						if (!o.isUnsubscribed())
							n = i.read(buffer);
					}
				} catch (IOException e) {
					o.onError(e);
				}
				if (o.isUnsubscribed())
					return;
				o.onCompleted();
			}
		});
	}

}
