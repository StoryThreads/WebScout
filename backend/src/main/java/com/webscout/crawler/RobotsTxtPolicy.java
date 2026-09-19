package com.webscout.crawler;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class RobotsTxtPolicy implements RobotsPolicy {

    private final Duration crawlDelay;
    private final List<Rule> rules = new ArrayList<>();

    public RobotsTxtPolicy(
            NormalizedUrl seedUrl,
            Duration timeout,
            String userAgent,
            Duration configuredDelay
    ) {
        if (seedUrl == null) {
            throw new IllegalArgumentException("seedUrl must not be null");
        }

        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }

        if (userAgent == null || userAgent.isBlank()) {
            throw new IllegalArgumentException("userAgent must not be blank");
        }

        if (configuredDelay == null || configuredDelay.isNegative()) {
            throw new IllegalArgumentException(
                    "configuredDelay must not be negative"
            );
        }

        this.crawlDelay = configuredDelay;

        loadRobotsTxt(seedUrl, timeout, userAgent);
    }

    @Override
    public boolean isAllowed(NormalizedUrl url) {
        if (url == null) {
            return false;
        }

        String path = url.uri().getPath();

        if (path == null || path.isBlank()) {
            path = "/";
        }

        Rule bestMatch = null;

        for (Rule rule : rules) {
            if (!rule.matches(path)) {
                continue;
            }

            if (bestMatch == null
                    || rule.path().length() > bestMatch.path().length()) {
                bestMatch = rule;
            }
        }

        return bestMatch == null || bestMatch.allowed();
    }

    @Override
    public Duration crawlDelay() {
        return crawlDelay;
    }

    private void loadRobotsTxt(
            NormalizedUrl seedUrl,
            Duration timeout,
            String userAgent
    ) {
        URI robotsUri = URI.create(
                seedUrl.uri().getScheme()
                        + "://"
                        + seedUrl.uri().getAuthority()
                        + "/robots.txt"
        );

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(robotsUri)
                .timeout(timeout)
                .header("User-Agent", userAgent)
                .GET()
                .build();

        try {
            HttpResponse<String> response =
                    client.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            int status = response.statusCode();

            /*
             * If robots.txt does not exist or cannot be retrieved,
             * allow the crawl to continue.
             */
            if (status == 404 || status >= 500) {
                return;
            }

            if (status < 200 || status >= 300) {
                return;
            }

            parseRobotsTxt(response.body(), userAgent);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Interrupted while fetching robots.txt",
                    e
            );

        } catch (Exception e) {
            /*
             * robots.txt retrieval failure should not prevent
             * the entire crawl from running.
             */
        }
    }

    private void parseRobotsTxt(
            String content,
            String userAgent
    ) {
        if (content == null || content.isBlank()) {
            return;
        }

        String[] lines = content.split("\\R");

        boolean currentGroupMatches = false;
        boolean foundMatchingGroup = false;

        List<Rule> parsedRules = new ArrayList<>();

        for (String rawLine : lines) {

            String line = rawLine;

            int commentIndex = line.indexOf('#');

            if (commentIndex >= 0) {
                line = line.substring(0, commentIndex);
            }

            line = line.trim();

            if (line.isEmpty()) {
                currentGroupMatches = false;
                continue;
            }

            int separatorIndex = line.indexOf(':');

            if (separatorIndex < 0) {
                continue;
            }

            String directive =
                    line.substring(0, separatorIndex)
                            .trim()
                            .toLowerCase(Locale.ROOT);

            String value =
                    line.substring(separatorIndex + 1)
                            .trim();

            if ("user-agent".equals(directive)) {

                String requestedAgent =
                        userAgent.toLowerCase(Locale.ROOT);

                String robotsAgent =
                        value.toLowerCase(Locale.ROOT);

                currentGroupMatches =
                        "*".equals(robotsAgent)
                                || requestedAgent.contains(robotsAgent);

                if (currentGroupMatches) {
                    foundMatchingGroup = true;
                }

                continue;
            }

            if (!currentGroupMatches) {
                continue;
            }

            if ("disallow".equals(directive)) {

                if (!value.isBlank()) {
                    parsedRules.add(
                            new Rule(value, false)
                    );
                }

            } else if ("allow".equals(directive)) {

                if (!value.isBlank()) {
                    parsedRules.add(
                            new Rule(value, true)
                    );
                }
            }
        }

        if (foundMatchingGroup) {
            rules.addAll(parsedRules);
        }
    }

    private record Rule(
            String path,
            boolean allowed
    ) {

        boolean matches(String requestPath) {

            if (path.equals("/")) {
                return true;
            }

            String regex =
                    Pattern.quote(path)
                            .replace("\\*", ".*");

            return requestPath.matches(
                    "^" + regex + ".*$"
            );
        }
    }
}