package io.nightbeam.studio.xauctions.util;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * Checks Modrinth for a newer release of xAuctions and logs a console message when an update is available.
 */
public final class ModrinthUpdateChecker {

    private static final String PROJECT_ID = "jY2BjuRn";
    private static final String VERSIONS_ENDPOINT =
            "https://api.modrinth.com/v2/project/" + PROJECT_ID + "/version?version_type=release&limit=20";
    private static final String DOWNLOAD_URL =
            "https://modrinth.com/plugin/xauctions-advanced-auctions-house";

    private final XAuctionsPlugin plugin;

    public ModrinthUpdateChecker(XAuctionsPlugin plugin) {
        this.plugin = plugin;
    }

    public void checkAsync() {
        if (!plugin.getConfig().getBoolean("update-checker.enabled", true)) {
            return;
        }

        plugin.getPlatformAdapter().runAsync(() -> {
            try {
                String latest = fetchLatestReleaseVersion();
                if (latest == null || latest.isBlank()) {
                    return;
                }
                String local = plugin.getDescription().getVersion();
                if (isNewer(latest, local)) {
                    if (plugin.getConfig().getBoolean("update-checker.notify-console", true)) {
                        plugin.getLogger().info("Update available: " + latest + " (you have " + local + ")");
                        plugin.getLogger().info("Download: " + DOWNLOAD_URL);
                    }
                } else if (plugin.getConfig().getBoolean("update-checker.notify-console", true)) {
                    plugin.getLogger().info("Running latest version (" + local + ").");
                }
            } catch (Exception ex) {
                plugin.getLogger().log(Level.FINE, "Update check failed (non-critical): " + ex.getMessage());
            }
        });
    }

    private String fetchLatestReleaseVersion() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(VERSIONS_ENDPOINT).openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "xAuctions/" + plugin.getDescription().getVersion());
        int code = conn.getResponseCode();
        if (code != 200) {
            return null;
        }
        StringBuilder body = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                body.append(line);
            }
        }
        return parseLatestReleaseVersion(body.toString());
    }

    static String parseLatestReleaseVersion(String jsonBody) {
        if (jsonBody == null || jsonBody.isEmpty()) {
            return null;
        }
        String bestRelease = null;
        int searchFrom = 0;
        while (true) {
            int versionKey = jsonBody.indexOf("\"version_number\"", searchFrom);
            if (versionKey < 0) {
                break;
            }
            String version = readJsonStringValue(jsonBody, versionKey);
            int nextVersionKey = jsonBody.indexOf("\"version_number\"", versionKey + 16);
            int typeKey = jsonBody.indexOf("\"version_type\"", versionKey);
            searchFrom = versionKey + 16;
            if (version == null) {
                continue;
            }
            if (typeKey < 0 || (nextVersionKey >= 0 && typeKey > nextVersionKey)) {
                continue;
            }
            String type = readJsonStringValue(jsonBody, typeKey);
            if (type == null || !"release".equalsIgnoreCase(type)) {
                continue;
            }
            if (bestRelease == null || isNewer(version, bestRelease)) {
                bestRelease = version;
            }
        }
        return bestRelease;
    }

    private static String readJsonStringValue(String json, int keyIndex) {
        int colon = json.indexOf(':', keyIndex);
        if (colon < 0) {
            return null;
        }
        int quote = json.indexOf('"', colon + 1);
        if (quote < 0) {
            return null;
        }
        int end = json.indexOf('"', quote + 1);
        if (end < 0) {
            return null;
        }
        return json.substring(quote + 1, end);
    }

    static boolean isNewer(String latest, String current) {
        try {
            int[] latestParts = parseVersion(latest);
            int[] currentParts = parseVersion(current);
            for (int i = 0; i < Math.max(latestParts.length, currentParts.length); i++) {
                int l = i < latestParts.length ? latestParts[i] : 0;
                int c = i < currentParts.length ? currentParts[i] : 0;
                if (l > c) {
                    return true;
                }
                if (l < c) {
                    return false;
                }
            }
            return false;
        } catch (Exception exception) {
            return false;
        }
    }

    private static int[] parseVersion(String version) {
        String cleaned = version.replaceAll("[^0-9.]", "");
        String[] parts = cleaned.split("\\.");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Integer.parseInt(parts[i].isEmpty() ? "0" : parts[i]);
        }
        return result;
    }
}
