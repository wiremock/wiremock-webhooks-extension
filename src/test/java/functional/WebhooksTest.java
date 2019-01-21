package functional;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.wiremock.webhooks.Webhooks;
import org.wiremock.webhooks.interceptors.WebhookInterceptor;
import testsupport.ConstantHttpHeaderWebhookInterceptor;

public class WebhooksTest {

  @Test
  public void immutableOnSet() {
    Webhooks webhooks = new Webhooks();
    List<WebhookInterceptor> interceptors = new ArrayList<>();
    interceptors.add(new ConstantHttpHeaderWebhookInterceptor());
    webhooks.setInterceptors(interceptors);
    interceptors.add(new ConstantHttpHeaderWebhookInterceptor());
    assertEquals(1, webhooks.getInterceptors().size());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void immutableOnGet(){
    Webhooks webhooks = new Webhooks();
    List<WebhookInterceptor> interceptors = new ArrayList<>();
    interceptors.add(new ConstantHttpHeaderWebhookInterceptor());
    webhooks.setInterceptors(interceptors);
    interceptors = webhooks.getInterceptors();
    interceptors.add(new ConstantHttpHeaderWebhookInterceptor());
  }
}
