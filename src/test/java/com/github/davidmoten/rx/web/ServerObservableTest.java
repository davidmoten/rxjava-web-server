package com.github.davidmoten.rx.web;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import rx.Observable;
import rx.subjects.PublishSubject;

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
		PublishSubject<Integer> subject = PublishSubject.create();

	}
}
