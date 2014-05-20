package com.github.davidmoten.rx.web;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import rx.Observable;

public class Response {
	private final Socket socket;

	public Response(Socket socket) {
		this.socket = socket;
	}

	public OutputStream getOutputStream() {
		try {
			return socket.getOutputStream();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void close() {
		try {
			socket.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	Observable<Integer> code;
	Observable<Header> headers;
	Observable<byte[]> body;
}
