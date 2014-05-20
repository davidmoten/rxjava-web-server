package com.github.davidmoten.rx.web;

import static rx.Observable.using;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
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

	public static Observable<Request> requests(final int port) {
		return from(port).flatMap(new Func1<Socket, Observable<Request>>() {

			public Observable<Request> call(final Socket socket) {
				try {
					Observable<byte[]> bytes = StringObservable.from(socket
							.getInputStream());
					final Observable<String> decoded = StringObservable.decode(
							bytes, StandardCharsets.UTF_8);
					Func1<SocketSubscription, Observable<Request>> factory = requestFactory(
							socket, decoded);
					SocketSubscriptionFactory subscriptionFactory = new SocketSubscriptionFactory(
							socket);
					return using(subscriptionFactory, factory);
				} catch (IOException e) {
					return Observable.error(e);
				}
			}

		});
	}

	private static Func1<SocketSubscription, Observable<Request>> requestFactory(
			final Socket socket, final Observable<String> decoded) {
		return new Func1<SocketSubscription, Observable<Request>>() {

			public Observable<Request> call(SocketSubscription t1) {
				return StringObservable
				// split by line feed
						.split(decoded, "\n")
						// stop when encounter blank line
						.takeWhile(NON_BLANK)
						// aggregate lines as list
						.toList()
						// parse the lines as a request
						.map(TO_REQUEST)
						// process the request
						.doOnNext(process(socket));
			}
		};
	}

	private static class SocketSubscriptionFactory implements
			Func0<SocketSubscription> {

		private final SocketSubscription sub;

		SocketSubscriptionFactory(Socket socket) {
			this.sub = new SocketSubscription(socket);
		}

		public SocketSubscription call() {
			return sub;
		}

	}

	private static class SocketSubscription implements Subscription {

		private final Subscription inner;

		SocketSubscription(final Socket socket) {
			inner = Subscriptions.create(new Action0() {

				public void call() {
					try {
						socket.close();
					} catch (IOException e) {
						// ignore
					}
				}
			});
		}

		public void unsubscribe() {
			inner.unsubscribe();
		}

		public boolean isUnsubscribed() {
			return inner.isUnsubscribed();
		}

	}

	private static Action1<Request> process(final Socket socket) {
		return new Action1<Request>() {

			public void call(Request request) {
				try {
					PrintWriter out = new PrintWriter(socket.getOutputStream());
					out.print("HTTP/1.1 200 OK\r\n");
					out.print("Content-Type: text/plain\r\n");
					out.print("\r\n");
					out.print("Got the message " + new Date());
					out.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	private static Func1<String, Boolean> NON_BLANK = new Func1<String, Boolean>() {

		public Boolean call(String line) {
			return line.trim().length() > 0;
		}
	};

	private static Func1<List<String>, Request> TO_REQUEST = new Func1<List<String>, Request>() {

		public Request call(List<String> lines) {
			return new Request(lines);
		}
	};

}
