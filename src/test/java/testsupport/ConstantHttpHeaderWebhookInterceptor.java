package testsupport;


import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.wiremock.webhooks.WebhookDefinition;
import org.wiremock.webhooks.interceptors.WebhookInterceptor;

public class ConstantHttpHeaderWebhookInterceptor implements WebhookInterceptor {

  public static final String key = "X-customer-header";
  public static final String value = "foo";

  @Override
  public WebhookDefinition intercept(ServeEvent serveEvent, WebhookDefinition webhookDefinition) {
    return webhookDefinition.withHeader(key, value);
  }
}