package com.webscout.crawler;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public final class JavaHttpFetcher implements HttpFetcher {

    private final HttpClient httpClient;

    public JavaHttpFetcher() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public FetchResult fetch(
            NormalizedUrl url,
            FetchPolicy policy
    ) {
        Objects.requireNonNull(
                url,
                "URL must not be null"
        );

        Objects.requireNonNull(
                policy,
                "Fetch policy must not be null"
        );

        NormalizedUrl currentUrl = url;
        int redirectsFollowed = 0;

        while (true) {

            HttpRequest request =
                    buildRequest(
                            currentUrl,
                            policy
                    );

            long startNanos =
                    System.nanoTime();

            HttpResponse<InputStream> response;

            try {
                response =
                        httpClient.send(
                                request,
                                HttpResponse.BodyHandlers.ofInputStream()
                        );

            } catch (java.net.http.HttpTimeoutException exception) {

                throw new CrawlFetchException(
                        CrawlErrorType.TIMEOUT,
                        "HTTP request timed out",
                        exception
                );

            } catch (IOException exception) {

                throw new CrawlFetchException(
                        CrawlErrorType.CONNECTION_FAILURE,
                        "HTTP request failed",
                        exception
                );

            } catch (InterruptedException exception) {

                Thread.currentThread().interrupt();

                throw new CrawlFetchException(
                        CrawlErrorType.CONNECTION_FAILURE,
                        "HTTP request was interrupted",
                        exception
                );
            }

            try (InputStream bodyStream =
                         response.body()) {

                long contentLength =
                        response.headers()
                                .firstValueAsLong(
                                        "Content-Length"
                                )
                                .orElse(-1L);

                if (contentLength >
                        policy.maxResponseBytes()) {

                    throw new CrawlFetchException(
                            CrawlErrorType.RESPONSE_TOO_LARGE,
                            "HTTP response exceeds configured size limit",
                            null
                    );
                }

                byte[] body =
                        readResponseBody(
                                bodyStream,
                                policy.maxResponseBytes()
                        );

                long latencyMillis =
                        Duration.ofNanos(
                                System.nanoTime()
                                        - startNanos
                        ).toMillis();

                int statusCode =
                        response.statusCode();

                /*
                 * Redirect responses are handled before
                 * normal HTTP status classification.
                 */
                if (isRedirectStatus(statusCode)) {

                    /*
                     * When redirects are disabled,
                     * return the redirect response as-is.
                     */
                    if (policy.maxRedirects() == 0) {

                        return toFetchResult(
                                url,
                                currentUrl,
                                response,
                                body,
                                latencyMillis
                        );
                    }

                    if (redirectsFollowed >=
                            policy.maxRedirects()) {

                        throw new CrawlFetchException(
                                CrawlErrorType.TOO_MANY_REDIRECTS,
                                "Maximum redirect limit exceeded",
                                null,
                                statusCode,
                                toHeaderMap(response)
                        );
                    }

                    String location =
                            response.headers()
                                    .firstValue("Location")
                                    .orElse(null);

                    if (location == null ||
                            location.isBlank()) {

                        throw new CrawlFetchException(
                                CrawlErrorType.HTTP_REDIRECTION_ERROR,
                                "Redirect response does not contain a Location header",
                                null,
                                statusCode,
                                toHeaderMap(response)
                        );
                    }

                    final URI redirectUri;

                    try {

                        redirectUri =
                                currentUrl.uri()
                                        .resolve(location);

                    } catch (IllegalArgumentException exception) {

                        throw new CrawlFetchException(
                                CrawlErrorType.HTTP_REDIRECTION_ERROR,
                                "Redirect Location is invalid",
                                exception,
                                statusCode,
                                toHeaderMap(response)
                        );
                    }

                    final NormalizedUrl redirectUrl;

                    try {

                        redirectUrl =
                                NormalizedUrl.parse(
                                        redirectUri.toString()
                                );

                    } catch (IllegalArgumentException exception) {

                        throw new CrawlFetchException(
                                CrawlErrorType.HTTP_REDIRECTION_ERROR,
                                "Redirect target is invalid",
                                exception,
                                statusCode,
                                toHeaderMap(response)
                        );
                    }

                    currentUrl =
                            redirectUrl;

                    redirectsFollowed++;

                    continue;
                }

                /*
                 * HTTP status classification must happen
                 * BEFORE Content-Type validation.
                 *
                 * This ensures:
                 *
                 * 404 -> HTTP_CLIENT_ERROR
                 * 429 -> HTTP_RATE_LIMITED
                 * 503 -> HTTP_SERVER_ERROR
                 *
                 * even when the response is text/plain
                 * or has another non-HTML content type.
                 */
                if (!HttpStatusClassifier.isSuccessful(
                        statusCode
                )) {

                    CrawlErrorType errorType =
                            HttpStatusClassifier.classify(
                                    statusCode
                            );

                    throw new CrawlFetchException(
                            errorType,
                            "HTTP request returned status "
                                    + statusCode,
                            null,
                            statusCode,
                            toHeaderMap(response)
                    );
                }

                /*
                 * Only successful HTTP responses are
                 * candidates for HTML crawling.
                 */
                validateContentType(response);

                return toFetchResult(
                        url,
                        currentUrl,
                        response,
                        body,
                        latencyMillis
                );

            } catch (CrawlFetchException exception) {

                throw exception;

            } catch (IOException exception) {

                throw new CrawlFetchException(
                        CrawlErrorType.CONNECTION_FAILURE,
                        "Failed while reading HTTP response",
                        exception
                );
            }
        }
    }

    private HttpRequest buildRequest(
            NormalizedUrl url,
            FetchPolicy policy
    ) {
        return HttpRequest.newBuilder()
                .uri(url.uri())
                .timeout(policy.requestTimeout())
                .header(
                        "User-Agent",
                        policy.userAgent()
                )
                .GET()
                .build();
    }

    private byte[] readResponseBody(
            InputStream inputStream,
            long maxResponseBytes
    ) throws IOException {

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        byte[] buffer =
                new byte[8192];

        long totalBytes = 0;

        int bytesRead;

        while ((bytesRead =
                inputStream.read(buffer)) != -1) {

            totalBytes += bytesRead;

            if (totalBytes >
                    maxResponseBytes) {

                throw new CrawlFetchException(
                        CrawlErrorType.RESPONSE_TOO_LARGE,
                        "HTTP response exceeds configured size limit",
                        null
                );
            }

            output.write(
                    buffer,
                    0,
                    bytesRead
            );
        }

        return output.toByteArray();
    }

    private void validateContentType(
            HttpResponse<InputStream> response
    ) {
        String contentType =
                response.headers()
                        .firstValue("Content-Type")
                        .orElse(null);

        if (contentType == null ||
                contentType.isBlank()) {

            throw new CrawlFetchException(
                    CrawlErrorType.UNSUPPORTED_CONTENT_TYPE,
                    "HTTP response does not specify a Content-Type",
                    null
            );
        }

        String normalized =
                contentType
                        .split(";", 2)[0]
                        .trim()
                        .toLowerCase();

        if (!normalized.equals("text/html")
                && !normalized.equals(
                "application/xhtml+xml"
        )) {

            throw new CrawlFetchException(
                    CrawlErrorType.UNSUPPORTED_CONTENT_TYPE,
                    "Unsupported HTTP Content-Type: "
                            + normalized,
                    null
            );
        }
    }

    private FetchResult toFetchResult(
            NormalizedUrl requestedUrl,
            NormalizedUrl finalUrl,
            HttpResponse<InputStream> response,
            byte[] body,
            long latencyMillis
    ) {
        String contentType =
                response.headers()
                        .firstValue("Content-Type")
                        .orElse(null);

        Map<String, String> headers =
                toHeaderMap(response);

        return FetchResult.success(
                requestedUrl,
                finalUrl,
                response.statusCode(),
                contentType,
                body,
                latencyMillis,
                headers
        );
    }

    private Map<String, String> toHeaderMap(
            HttpResponse<?> response
    ) {
        return response.headers()
                .map()
                .entrySet()
                .stream()
                .collect(
                        Collectors.toUnmodifiableMap(
                                Map.Entry::getKey,
                                entry -> String.join(
                                        ", ",
                                        entry.getValue()
                                )
                        )
                );
    }

    private boolean isRedirectStatus(
            int statusCode
    ) {
        return statusCode == 301
                || statusCode == 302
                || statusCode == 303
                || statusCode == 307
                || statusCode == 308;
    }
}