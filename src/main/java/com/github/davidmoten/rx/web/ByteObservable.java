package com.github.davidmoten.rx.web;

import java.io.ByteArrayOutputStream;

import rx.Observable.Operator;
import rx.Subscriber;

public class ByteObservable {

	public static Operator<byte[], byte[]> first(final int n) {
		return new Operator<byte[], byte[]>() {

			private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

			@Override
			public Subscriber<? super byte[]> call(
					Subscriber<? super byte[]> subscriber) {

			}
		};

	}
}
