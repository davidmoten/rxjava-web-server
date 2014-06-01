package com.github.davidmoten.rx.web;

import java.io.PrintWriter;
import java.util.Date;

import rx.Observer;
import rx.schedulers.Schedulers;

public class ServerMain {

    public static void main(String[] args) throws InterruptedException {

        ServerObservable
        // listen for requests on port 8080
                .requests(8080)
                // serve requests concurrently
                .observeOn(Schedulers.io())
                // subscribe
                .subscribe(new Observer<Conversation>() {

                    @Override
                    public void onNext(Conversation r) {
                        System.out.println("request=" + r.request());
                        System.out.println(r.request().getMethodLine().toBlockingObservable().single().getMethod());
                        try {
                            PrintWriter out = r.response().createWriter();
                            out.print("HTTP/1.1 200 OK\r\n");
                            out.print("Content-Type: text/plain\r\n");
                            out.print("\r\n");
                            out.print("Got the message " + new Date());
                            out.close();
                            r.response().close();
                        } catch (RuntimeException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onCompleted() {

                    }
                });
    }
}
