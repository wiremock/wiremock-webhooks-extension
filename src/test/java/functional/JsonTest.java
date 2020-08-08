package functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.wiremock.webhooks.WebhookDefinition;

import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class JsonTest {

    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        objectMapper = new ObjectMapper();
    }

    @Test
    public void webhookWithNoDelay() throws IOException {
        WebhookDefinition webhookDefinition = objectMapper.readValue("{\n" +
                "      \"headers\" : {\n" +
                "        \"Content-Type\" : \"application/json\"\n" +
                "      },\n" +
                "      \"method\" : \"POST\",\n" +
                "      \"body\" : \"{ \\\"result\\\": \\\"SUCCESS\\\" }\",\n" +
                "      \"url\" : \"http://localhost:56299/callback\"\n" +
                "    }", WebhookDefinition.class);
        assertThat("delay is defaulted to 0", webhookDefinition.getDelay(), is(0L));
    }

    @Test
    public void webhookWithDelay() throws IOException {
        WebhookDefinition webhookDefinition = objectMapper.readValue("{\n" +
                "      \"headers\" : {\n" +
                "        \"Content-Type\" : \"application/json\"\n" +
                "      },\n" +
                "      \"delay\": 1000,\n" +
                "      \"method\" : \"POST\",\n" +
                "      \"body\" : \"{ \\\"result\\\": \\\"SUCCESS\\\" }\",\n" +
                "      \"url\" : \"http://localhost:56299/callback\"\n" +
                "    }", WebhookDefinition.class);
        assertThat("delay can be optionally set", webhookDefinition.getDelay(), is(1_000L));
    }
}
