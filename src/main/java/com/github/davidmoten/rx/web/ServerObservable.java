package com.github.davidmoten.rx.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.observables.GroupedObservable;
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

	public static Observable<Conversation> requests(final int port) {
		return from(port)
		// to RequestResponse
				.flatMap(new Func1<Socket, Observable<Conversation>>() {

					@Override
					public Observable<Conversation> call(final Socket socket) {
						System.out.println("\nreading request from " + socket);
						try {
							return requests(socket, socket.getInputStream());
						} catch (IOException e) {
							return Observable.error(e);
						}
					}

				});
	}

	private static byte[] headerTerminator = new byte[] { '\r', '\n', '\r',
			'\n' };

	static Observable<Conversation> requests(Socket socket, InputStream is) {

		Observable<GroupedObservable<Boolean, byte[]>> grouped = from(is, 8192)
		// aggregate the bytes of the header
				.lift(new OperatorAggregateToPattern(headerTerminator))
				// group the items after the header into its own Observable
				.groupBy(isHeader)
				//can cache this with very little cost because we are caching two references to Observables only
				.cache();
		Observable<byte[]> header = grouped.flatMap(toHeader(true)).first();
		Observable<byte[]> rest = grouped.flatMap(toHeader(true));

	}

	private static Func1<byte[], Boolean> isHeader = new Func1<byte[], Boolean>() {

		@Override
		public Boolean call(byte[] arg0) {
			// TODO Auto-generated method stub
			return null;
		}
	};

	private static Func1<GroupedObservable<Boolean, byte[]>, Observable<byte[]>> toHeader(
			final boolean value) {
		return new Func1<GroupedObservable<Boolean, byte[]>, Observable<byte[]>>() {
			@Override
			public Observable<byte[]> call(
					GroupedObservable<Boolean, byte[]> group) {
				if (group.getKey().equals(value))
					return group;
				else
					return Observable.empty();
			}

		};
	}

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
