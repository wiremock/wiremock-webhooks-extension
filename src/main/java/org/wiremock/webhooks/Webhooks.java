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
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;
import static com.github.tomakehurst.wiremock.common.LocalNotifier.notifier;
import static com.github.tomakehurst.wiremock.http.HttpClientFactory.getHttpRequestFor;

public class Webhooks extends PostServeAction {

    private final ScheduledExecutorService scheduler;
    private final HttpClient httpClient;
    private final List<WebhookTransformer> transformers;

    private Webhooks(
            ScheduledExecutorService scheduler,
            HttpClient httpClient,
            List<WebhookTransformer> transformers) {
      this.scheduler = scheduler;
      this.httpClient = httpClient;
      this.transformers = transformers;
    }

    public Webhooks() {
      this(Executors.newScheduledThreadPool(10), HttpClientFactory.createClient(), new ArrayList<WebhookTransformer>());
    }

    public Webhooks(WebhookTransformer... transformers) {
      this(Executors.newScheduledThreadPool(10), HttpClientFactory.createClient(), Arrays.asList(transformers));
    }

    @Override
    public String getName() {
        return "webhook";
    }

    @Override
    public void doAction(final ServeEvent serveEvent, final Admin admin, final Parameters parameters) {
        final Notifier notifier = notifier();
        final WebhookDefinition[] definitions = {parameters.as(WebhookDefinition.class)};
        WebhookDefinition definition = definitions[0];
        scheduler.schedule(
            new Runnable() {
                @Override
                public void run() {
                    for (WebhookTransformer transformer: transformers) {
                        definitions[0] = transformer.transform(serveEvent, definitions[0]);
                    }
                    HttpUriRequest request = buildRequest(definitions[0]);

                    try {
                        HttpResponse response = httpClient.execute(request);
                        notifier.info(
                            String.format("Webhook %s request to %s returned status %s\n\n%s",
                                definitions[0].getMethod(),
                                definitions[0].getUrl(),
                                response.getStatusLine(),
                                EntityUtils.toString(response.getEntity())
                            )
                        );
                    } catch (IOException e) {
                        throwUnchecked(e);
                    }
                }
            },
            definition.getDelay(),
            TimeUnit.MILLISECONDS
        );
    }

    private static HttpUriRequest buildRequest(WebhookDefinition definition) {
        HttpUriRequest request = getHttpRequestFor(
                definition.getMethod(),
                definition.getUrl().toString()
        );

        for (HttpHeader header: definition.getHeaders().all()) {
            request.addHeader(header.key(), header.firstValue());
        }

        if (definition.getMethod().hasEntity()) {
            HttpEntityEnclosingRequestBase entityRequest = (HttpEntityEnclosingRequestBase) request;
            entityRequest.setEntity(new ByteArrayEntity(definition.getBinaryBody()));
        }

        return request;
    }

    public static WebhookDefinition webhook() {
        return new WebhookDefinition();
    }
}
