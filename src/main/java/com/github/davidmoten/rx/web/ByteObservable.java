package com.github.davidmoten.rx.web;

import java.io.ByteArrayOutputStream;

import rx.Observable.Operator;
import rx.Subscriber;
import rx.subscriptions.CompositeSubscription;

public class ByteObservable {

	public static Operator<byte[], byte[]> first(final int n) {
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

					boolean completed = false;

					@Override
					public void onCompleted() {
						if (!completed) {
							child.onCompleted();
						}
					}

					@Override
					public void onError(Throwable e) {
						if (!completed) {
							child.onError(e);
						}
					}

					@Override
					public void onNext(byte[] b) {
						if (!isUnsubscribed()) {
							synchronized (this) {
								int num = Math.min(b.length, n - buffer.size());
								if (num > 0)
									buffer.write(b, 0, num);
							}
							if (buffer.size() == n) {
								completed = true;
								child.onNext(buffer.toByteArray());
								buffer.reset();
								child.onCompleted();
								unsubscribe();
							}
						}
					}

				};
			}
		};

	}
}
