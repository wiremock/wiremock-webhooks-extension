package org.wiremock.webhooks;

import com.github.tomakehurst.wiremock.common.Notifier;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.PostServeAction;
import com.github.tomakehurst.wiremock.http.HttpClientFactory;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;
import org.wiremock.webhooks.interceptors.WebhookTransformer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;
import static com.github.tomakehurst.wiremock.common.LocalNotifier.notifier;
import static com.github.tomakehurst.wiremock.http.HttpClientFactory.getHttpRequestFor;
import static java.util.concurrent.TimeUnit.SECONDS;

public class WebhooksExtension extends PostServeAction {

    private final ScheduledExecutorService scheduler;
    private final HttpClient httpClient;
    private final List<WebhookTransformer> transformers;

    private WebhooksExtension(ScheduledExecutorService scheduler, HttpClient httpClient, List<WebhookTransformer> transformers) {
        this.scheduler = scheduler;
        this.httpClient = httpClient;
        this.transformers = transformers;
    }

    public WebhooksExtension() {
        this(Executors.newScheduledThreadPool(10), HttpClientFactory.createClient(), new ArrayList<WebhookTransformer>());
    }

    public WebhooksExtension(WebhookTransformer... globalTransformers) {
        this(Executors.newScheduledThreadPool(10), HttpClientFactory.createClient(), Arrays.asList(globalTransformers));
    }

    @Override
    public String getName() {
        return "webhooks";
    }

    @Override
    public void doAction(final ServeEvent serveEvent, final Admin admin, final Parameters parameters) {
        final Notifier notifier = notifier();

        scheduler.schedule(
                new Runnable() {
                    @Override
                    public void run() {
                        WebhookDefinitionContainer hooks = parameters.as(WebhookDefinitionContainer.class);
                        for (WebhookDefinition definition : hooks.getWebhooks()) {
                            for (WebhookTransformer transformer : transformers) {
                                definition = transformer.transform(serveEvent, definition);
                            }
                            final WebhookDefinition finalDefinition = definition;
                            HttpUriRequest request = buildRequest(finalDefinition);
                            try {
                                HttpResponse response = httpClient.execute(request);
                                notifier.info(
                                        String.format("Webhook %s request to %s returned status %s\n\n%s",
                                                finalDefinition.getMethod(),
                                                finalDefinition.getUrl(),
                                                response.getStatusLine(),
                                                EntityUtils.toString(response.getEntity())
                                        )
                                );
                            } catch (IOException e) {
                                throwUnchecked(e);
                            }
                        }
                    }
                },
                0L,
                SECONDS
        );
    }

    private static HttpUriRequest buildRequest(WebhookDefinition definition) {
        HttpUriRequest request = getHttpRequestFor(
                definition.getMethod(),
                definition.getUrl().toString()
        );

        for (HttpHeader header : definition.getHeaders().all()) {
            request.addHeader(header.key(), header.firstValue());
        }

        if (definition.getMethod().hasEntity()) {
            HttpEntityEnclosingRequestBase entityRequest = (HttpEntityEnclosingRequestBase) request;
            entityRequest.setEntity(new ByteArrayEntity(definition.getBinaryBody()));
        }

        return request;
    }

    public static WebhookDefinition aWebhook() {
        return new WebhookDefinition();
    }

    public static WebhookDefinitionBuilder webhooks() {
        return new WebhookDefinitionBuilder();
    }
}
