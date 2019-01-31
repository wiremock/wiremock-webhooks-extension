package org.wiremock.webhooks;

import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;
import static com.github.tomakehurst.wiremock.common.LocalNotifier.notifier;
import static com.github.tomakehurst.wiremock.http.HttpClientFactory.getHttpRequestFor;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.github.tomakehurst.wiremock.common.Notifier;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.PostServeAction;
import com.github.tomakehurst.wiremock.http.HttpClientFactory;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;
import org.wiremock.webhooks.interceptors.WebhookTransformer;

public class Webhooks extends PostServeAction {

    private final ScheduledExecutorService scheduler;
    private final HttpClient httpClient;
    private List<WebhookTransformer> interceptors = new ArrayList<>();

    private Webhooks(ScheduledExecutorService scheduler, HttpClient httpClient,
        List<WebhookTransformer> interceptors) {
      this.scheduler = scheduler;
      this.httpClient = httpClient;
      this.interceptors = interceptors;
    }

    public Webhooks() {
      this(Executors.newScheduledThreadPool(10), HttpClientFactory.createClient(),
          new ArrayList<WebhookTransformer>());
    }

    public Webhooks(WebhookTransformer... interceptors) {
      this(Executors.newScheduledThreadPool(10), HttpClientFactory.createClient(),
          Arrays.asList(interceptors));
    }

    @Override
    public String getName() {
        return "webhook";
    }

    @Override
    public void doAction(ServeEvent serveEvent, Admin admin, Parameters parameters) {
        final WebhookDefinition initialDefinition = parameters.as(WebhookDefinition.class);
        final Notifier notifier = notifier();
        final List<WebhookTransformer> runnableInterceptors = ImmutableList.copyOf(interceptors);
        final ServeEvent servedEvent = serveEvent;

        scheduler.schedule(
            new Runnable() {
                @Override
                public void run() {
                    WebhookDefinition definition = initialDefinition;
                    for(WebhookTransformer interceptor : runnableInterceptors) {
                        definition = interceptor.intercept(servedEvent, definition);
                    }
                    HttpUriRequest request = buildRequest(definition);

                    try {
                        HttpResponse response = httpClient.execute(request);
                        notifier.info(
                            String.format("Webhook %s request to %s returned status %s\n\n%s",
                                definition.getMethod(),
                                definition.getUrl(),
                                response.getStatusLine(),
                                EntityUtils.toString(response.getEntity())
                            )
                        );
                    } catch (IOException e) {
                        throwUnchecked(e);
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
