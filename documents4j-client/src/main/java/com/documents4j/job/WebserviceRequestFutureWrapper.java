package com.documents4j.job;

import com.documents4j.throwables.ConverterException;
import com.documents4j.ws.ConverterNetworkProtocol;
import com.google.common.base.MoreObjects;

import jakarta.ws.rs.core.Response;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class WebserviceRequestFutureWrapper implements Future<Boolean> {

    private final Future<Response> futureResponse;

    public WebserviceRequestFutureWrapper(Future<Response> futureResponse) {
        this.futureResponse = futureResponse;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return futureResponse.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return futureResponse.isCancelled();
    }

    @Override
    public boolean isDone() {
        return futureResponse.isDone();
    }

    @Override
    public Boolean get() throws InterruptedException, ExecutionException {
        return handle(futureResponse.get());
    }

    @Override
    public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return handle(futureResponse.get(timeout, unit));
    }

    private static boolean handle(Response response) throws ExecutionException {
        try {
            return ConverterNetworkProtocol.Status.from(response.getStatus()).resolve();
        } catch (ConverterException e) {
            throw new ExecutionException("The conversion resulted in an error", e);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(WebserviceRequestFutureWrapper.class)
                .add("futureResponse", futureResponse)
                .toString();
    }
}
