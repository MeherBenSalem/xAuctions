package io.nightbeam.studio.xauctions.addons;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordWebhook {

    private final String url;

    public DiscordWebhook(String url) {
        this.url = url;
    }

    public void send(String content) {
        if (url == null || url.isEmpty() || url.equals("YOUR_WEBHOOK_URL"))
            return;

        // Run async
        new Thread(() -> {
            try {
                URL obj = new URL(url);
                HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setDoOutput(true);

                // Simple JSON payload
                String jsonInputString = "{\"content\": \"" + content + "\"}";

                try (OutputStream os = con.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                con.getResponseCode(); // Trigger request
                con.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void sendEmbed(String title, String description, int color) {
        if (url == null || url.isEmpty() || url.equals("YOUR_WEBHOOK_URL"))
            return;

        new Thread(() -> {
            try {
                URL obj = new URL(url);
                HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setDoOutput(true);

                // JSON payload for embed
                String json = "{"
                        + "\"embeds\": [{"
                        + "\"title\": \"" + title + "\","
                        + "\"description\": \"" + description + "\","
                        + "\"color\": " + color
                        + "}]"
                        + "}";

                try (OutputStream os = con.getOutputStream()) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                con.getResponseCode();
                con.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
