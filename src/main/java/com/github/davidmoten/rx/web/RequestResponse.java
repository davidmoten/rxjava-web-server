package com.github.davidmoten.rx.web;

public class RequestResponse {
	private final Request request;
	private final Response response;

	public RequestResponse(Request request, Response response) {
		this.request = request;
		this.response = response;
	}

	public Request request() {
		return this.request;
	}

	public Response response() {
		return response;
	}

}
