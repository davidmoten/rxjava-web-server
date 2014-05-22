package com.github.davidmoten.rx.web;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class ServerObservableTest {

	@Test
	public void testCanFindPatternSplitAcrossItems() {
		List<byte[]> list = ServerObservable
				.aggregateHeader(
						Observable.from(new byte[] { 'a', 'b', 'c' },
								new byte[] { 'd', 'e', 'f' }),
						new byte[] { 'c', 'd' }).toList()
				.toBlockingObservable().single();
		assertEquals("ab", new String(list.get(0)));
	}

	@Test
	public void testWorksIfPatternAtEnd() {
		List<byte[]> list = ServerObservable
				.aggregateHeader(
						Observable.from(new byte[] { 'a', 'b', 'c', 'd' }),
						new byte[] { 'c', 'd' }).toList()
				.toBlockingObservable().single();
		assertEquals("ab", new String(list.get(0)));
		assertEquals(1, list.size());
	}

	@Test
	public void testTakeConcurrentSafe() {
		int count = Observable.from(1, 2, 3).observeOn(Schedulers.newThread())
				.take(1).doOnNext(new Action1<Integer>() {

					@Override
					public void call(Integer n) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// do nothing
						}
					}
				}).count().toBlockingObservable().single();
		assertEquals(1, count);
	}
}
