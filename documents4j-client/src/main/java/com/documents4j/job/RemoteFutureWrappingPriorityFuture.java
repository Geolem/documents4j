package com.documents4j.job;

import com.documents4j.api.DocumentType;
import com.documents4j.api.IInputStreamConsumer;
import com.documents4j.api.IInputStreamSource;
import com.documents4j.ws.ConverterNetworkProtocol;
import com.google.common.base.MoreObjects;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

class RemoteFutureWrappingPriorityFuture extends AbstractFutureWrappingPriorityFuture<InputStream, RemoteConversionContext> {

    private final WebTarget webTarget;

    private final IInputStreamSource source;
    private final DocumentType sourceFormat;

    private final IInputStreamConsumer consumer;
    private final DocumentType targetFormat;

    private final AtomicBoolean consumptionMark;

    RemoteFutureWrappingPriorityFuture(WebTarget webTarget,
                                       IInputStreamSource source,
                                       DocumentType sourceFormat,
                                       IInputStreamConsumer consumer,
                                       DocumentType targetFormat,
                                       int priority) {
        super(priority);
        this.webTarget = webTarget;
        this.source = source;
        this.sourceFormat = sourceFormat;
        this.consumer = consumer;
        this.targetFormat = targetFormat;
        this.consumptionMark = new AtomicBoolean(false);
    }

    @Override
    protected InputStream fetchSource() {
        return source.getInputStream();
    }

    @Override
    protected void onSourceConsumed(InputStream fetchedSource) {
        if (consumptionMark.compareAndSet(false, true)) {
            source.onConsumed(fetchedSource);
        }
    }

    @Override
    protected RemoteConversionContext startConversion(InputStream fetchedSource) {
        return new RemoteConversionContext(webTarget
                .path(ConverterNetworkProtocol.RESOURCE_PATH)
                .request(targetFormat.toString())
                .header(ConverterNetworkProtocol.HEADER_JOB_PRIORITY, getPriority().getValue())
                .async()
                .post(Entity.entity(new ConsumeOnCloseInputStream(this, fetchedSource), sourceFormat.toString())));
    }

    @Override
    protected void onConversionFinished(RemoteConversionContext conversionContext) throws Exception {
        // We do not need to check the status again, this callback will only be triggered on a successful conversion.
        try (Response response = conversionContext.getWebResponse().get()) {
            consumer.onComplete(response.readEntity(InputStream.class));
        }
    }

    @Override
    protected void onConversionFailed(RuntimeException e) {
        consumer.onException(e);
    }

    @Override
    protected void onConversionCancelled() {
        consumer.onCancel();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper("RemoteConversion")
                .add("pending", !(getPendingCondition().getCount() == 0L))
                .add("cancelled", isCancelled())
                .add("done", isDone())
                .add("priority", getPriority())
                .add("web-target", webTarget.getUri())
                .toString();
    }
}
