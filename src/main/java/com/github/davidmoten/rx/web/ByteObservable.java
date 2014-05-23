package com.github.davidmoten.rx.web;

import java.io.ByteArrayOutputStream;

import rx.Observable.Operator;
import rx.Subscriber;
import rx.subscriptions.CompositeSubscription;

public class ByteObservable {

	public static Operator<byte[], byte[]> split(final int n) {
		if (n <= 0)
			throw new IllegalArgumentException("n must be positive");
		return new Operator<byte[], byte[]>() {

			@Override
			public Subscriber<? super byte[]> call(
					final Subscriber<? super byte[]> child) {

				final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				CompositeSubscription parent = new CompositeSubscription();
				if (n == 0) {
					child.onCompleted();
					parent.unsubscribe();
				}
				child.add(parent);
				return new Subscriber<byte[]>(parent) {

					@Override
					public void onCompleted() {
						if (!isUnsubscribed() && buffer.size() > 0) {
							child.onNext(buffer.toByteArray());
							buffer.reset();
						}
						child.onCompleted();
					}

					@Override
					public void onError(Throwable e) {
						child.onError(e);
					}

					@Override
					public void onNext(byte[] b) {
						if (!isUnsubscribed()) {
							int num = Math.min(b.length, n - buffer.size());
							if (num > 0)
								buffer.write(b, 0, num);
							if (buffer.size() == n) {
								child.onNext(buffer.toByteArray());
								buffer.reset();
							}
						}

					}
				};
			}
		};

	}
}
