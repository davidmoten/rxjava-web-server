package com.github.davidmoten.rx.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import rx.Observable;

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
	public void test() {
		InputStream is = ServerObservableTest.class
				.getResourceAsStream("/request-post.txt");
		assertNotNull(is);
		Conversation o = ServerObservable.requests(null, is).first()
				.toBlockingObservable().single();
		System.out.println(o);
	}

	@Test
	public void test2() throws IOException {
		InputStream resource = ServerObservableTest.class
				.getResourceAsStream("/request-post.txt");
		byte[] bytes = IOUtils.toByteArray(resource);

		ByteArrayInputStream is = new ByteArrayInputStream(Arrays.copyOf(bytes,
				100000));

		Conversation o = ServerObservable.requests(null, is).first()
				.toBlockingObservable().single();
		System.out.println(new String(o.request().getMessageBody().toList()
				.toBlockingObservable().single().get(0)));
	}

}
