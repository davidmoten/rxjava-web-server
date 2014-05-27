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
				.lift(ByteObservable.split(4)).toList().toBlockingObservable()
				.single();
		assertEquals(2, list.size());
		assertEquals("abcd", new String(list.get(0)));
		assertEquals("ef", new String(list.get(1)));
	}

	@Test
	public void testFirstWithinOne() {
		List<byte[]> list = Observable
				.from(new byte[] { 'a', 'b', 'c' },
						new byte[] { 'd', 'e', 'f' })
				.lift(ByteObservable.split(2)).toList().toBlockingObservable()
				.single();
		assertEquals(3, list.size());
		assertEquals("ab", new String(list.get(0)));
		assertEquals("cd", new String(list.get(1)));
		assertEquals("ef", new String(list.get(2)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFirstZeroBytes() {
		Observable.from(new byte[] { 'a', 'b', 'c' }).lift(
				ByteObservable.split(0));
	}

	@Test
	public void testAggregateHeader() {
		List<byte[]> list = Observable
				.from(new byte[] { 'a', 'b', 'c' },
						new byte[] { 'd', 'e', 'f' })
				.lift(ByteObservable.split(4)).toList().toBlockingObservable()
				.single();
		assertEquals(2, list.size());
		assertEquals("abcd", new String(list.get(0)));
		assertEquals("ef", new String(list.get(1)));
	}

}
