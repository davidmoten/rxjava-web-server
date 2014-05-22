package com.github.davidmoten.rx.web;

import java.io.ByteArrayOutputStream;

import rx.Observable.Operator;
import rx.Observer;
import rx.Subscriber;
import rx.observers.Subscribers;

public class ByteObservable {

	public static Operator<byte[], byte[]> first(final int n) {
		return new Operator<byte[], byte[]>() {

			private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

			@Override
			public Subscriber<? super byte[]> call(
					final Subscriber<? super byte[]> subscriber) {
				Subscriber<byte[]> sub = Subscribers
						.from(new Observer<byte[]>() {

							@Override
							public void onCompleted() {
								subscriber.onCompleted();
							}

							@Override
							public void onError(Throwable e) {
								subscriber.onError(e);
							}

							@Override
							public void onNext(byte[] b) {
								int num = Math.min(b.length, n - buffer.size());
								if (num > 0)
									buffer.write(b, 0, num);
								if (buffer.size() == n) {
									subscriber.onNext(buffer.toByteArray());
									buffer.reset();
									subscriber.onCompleted();
								}
							}
						});
				subscriber.add(sub);
				return sub;
			}
		};

	}
}
