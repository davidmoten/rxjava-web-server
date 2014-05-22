package com.github.davidmoten.rx.web;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import rx.Observable;

public class ByteObservableTest {

	@Test
	public void testFirstAcrossTwo() {
		List<byte[]> list = Observable
				.from(new byte[] { 'a', 'b', 'c' },
						new byte[] { 'd', 'e', 'f' })
				.lift(ByteObservable.first(4)).toList().toBlockingObservable()
				.single();
		assertEquals(1, list.size());
		assertEquals("abcd", new String(list.get(0)));
	}

	@Test
	public void testFirstWithinOne() {
		List<byte[]> list = Observable
				.from(new byte[] { 'a', 'b', 'c' },
						new byte[] { 'd', 'e', 'f' })
				.lift(ByteObservable.first(2)).toList().toBlockingObservable()
				.single();
		assertEquals(1, list.size());
		assertEquals("ab", new String(list.get(0)));
	}

	@Test
	public void testFirstZeroBytes() {
		List<byte[]> list = Observable
				.from(new byte[] { 'a', 'b', 'c' },
						new byte[] { 'd', 'e', 'f' })
				.lift(ByteObservable.first(0)).toList().toBlockingObservable()
				.single();
		assertEquals(0, list.size());
	}
}
