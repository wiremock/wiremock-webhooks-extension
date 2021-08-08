package functional;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestListener;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.http.entity.StringEntity;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.wiremock.webhooks.Webhooks;
import org.wiremock.webhooks.WebhooksExtension;
import testsupport.ConstantHttpHeaderWebhookTransformer;
import testsupport.TestNotifier;
import testsupport.WireMockTestClient;

import java.util.concurrent.CountDownLatch;

import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.http.RequestMethod.GET;
import static com.github.tomakehurst.wiremock.http.RequestMethod.POST;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.wiremock.webhooks.Webhooks.webhook;
import static org.wiremock.webhooks.WebhooksExtension.aWebhook;
import static org.wiremock.webhooks.WebhooksExtension.webhooks;

public class WebhooksAcceptanceTest {

    @Rule
    public WireMockRule targetServer = new WireMockRule(options().dynamicPort());

    CountDownLatch latch;

    Webhooks webhook = new Webhooks(
            new ConstantHttpHeaderWebhookTransformer()
    );
    WebhooksExtension newWebhooks = new WebhooksExtension(
            new ConstantHttpHeaderWebhookTransformer()
    );

    TestNotifier notifier = new TestNotifier();
    WireMockTestClient client;

    @Rule
    public WireMockRule rule = new WireMockRule(
            options()
                    .dynamicPort()
                    .notifier(notifier)
                    .extensions(webhook, newWebhooks));

    @Before
    public void init() {
        targetServer.addMockServiceRequestListener(new RequestListener() {
            @Override
            public void requestReceived(Request request, Response response) {
                if (request.getUrl().startsWith("/callback")) {
                    latch.countDown();
                }
            }
        });
        reset();
        notifier.reset();
        targetServer.stubFor(any(anyUrl())
            .willReturn(aResponse().withStatus(200)));
        latch = new CountDownLatch(1);
        client = new WireMockTestClient(rule.port());
        WireMock.configureFor(targetServer.port());

        System.out.println("Target server port: " + targetServer.port());
        System.out.println("Under test server port: " + rule.port());
    }

    @Test
    public void firesASingleWebhookWhenRequested() throws Exception {
        rule.stubFor(post(urlPathEqualTo("/something-async"))
            .willReturn(aResponse().withStatus(200))
            .withPostServeAction("webhook", webhook()
                .withMethod(POST)
                .withUrl("http://localhost:" + targetServer.port() + "/callback")
                .withHeader("Content-Type", "application/json")
                .withBody("{ \"result\": \"SUCCESS\" }"))
        );

        verify(0, postRequestedFor(anyUrl()));

        client.post("/something-async", new StringEntity("", TEXT_PLAIN));

        waitForRequestToTargetServer();

        targetServer.verify(1, postRequestedFor(urlEqualTo("/callback"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(equalToJson("{ \"result\": \"SUCCESS\" }"))
        );

        assertThat(notifier.getInfoMessages(), hasItem(allOf(
            containsString("Webhook POST request to"),
            containsString("/callback returned status"),
            containsString("200")
        )));
    }

    @Test
    public void firesMinimalWebhookWithTransformerApplied() throws Exception {
        rule.stubFor(post(urlPathEqualTo("/something-async"))
            .willReturn(aResponse().withStatus(200))
            .withPostServeAction("webhook", webhook()
                .withMethod(GET)
                .withUrl("http://localhost:" + targetServer.port() + "/callback"))
        );

        verify(0, postRequestedFor(anyUrl()));

        client.post("/something-async", new StringEntity("", TEXT_PLAIN));

        waitForRequestToTargetServer();

        verify(1, getRequestedFor(urlEqualTo("/callback"))
            .withHeader(ConstantHttpHeaderWebhookTransformer.key,
                equalTo(ConstantHttpHeaderWebhookTransformer.value)));
    }

    @Test
    public void firesMinimalSingletonWebhook() throws Exception {
        rule.stubFor(post(urlPathEqualTo("/something-async"))
                .willReturn(aResponse().withStatus(200))
                .withPostServeAction("webhooks", webhooks()
                        .addWebhook(aWebhook()
                                .withMethod(GET)
                                .withUrl("http://localhost:" + targetServer.port() + "/callback")
                        )
                        .build()
                )
        );

        verify(0, postRequestedFor(anyUrl()));

        client.post("/something-async", new StringEntity("", TEXT_PLAIN));

        waitForRequestToTargetServer();

        verify(1, getRequestedFor(urlEqualTo("/callback"))
                .withHeader(ConstantHttpHeaderWebhookTransformer.key,
                        equalTo(ConstantHttpHeaderWebhookTransformer.value)));
    }

    @Test
    public void firesManyWebhooks() throws Exception {
        rule.stubFor(post(urlPathEqualTo("/something-async"))
                .willReturn(aResponse().withStatus(200))
                .withPostServeAction("webhooks", webhooks()
                        .addWebhook(aWebhook()
                                .withMethod(GET)
                                .withUrl("http://localhost:" + targetServer.port() + "/callback")
                        )
                        .addWebhook(aWebhook()
                                .withMethod(GET)
                                .withUrl("http://localhost:" + targetServer.port() + "/callback")
                        )
                        .build()
                )
        );

        verify(0, postRequestedFor(anyUrl()));

        client.post("/something-async", new StringEntity("", TEXT_PLAIN));

        waitForRequestToTargetServer();

        verify(2, getRequestedFor(urlEqualTo("/callback"))
                .withHeader(ConstantHttpHeaderWebhookTransformer.key,
                        equalTo(ConstantHttpHeaderWebhookTransformer.value)));
    }

    @Test
    public void firesNoWebhooks() throws Exception {
        rule.stubFor(post(urlPathEqualTo("/something-async"))
                .willReturn(aResponse().withStatus(200))
                .withPostServeAction("webhooks", webhooks().build())
        );

        verify(0, postRequestedFor(anyUrl()));

        client.post("/something-async", new StringEntity("", TEXT_PLAIN));

        Thread.sleep(500);

        verify(0, getRequestedFor(urlEqualTo("/callback"))
                .withHeader(ConstantHttpHeaderWebhookTransformer.key,
                        equalTo(ConstantHttpHeaderWebhookTransformer.value)));
    }

    private void waitForRequestToTargetServer() throws Exception {
        latch.await(2, SECONDS);
        assertThat("Timed out waiting for target server to receive a request",
            latch.getCount(), is(0L));
    }

}
