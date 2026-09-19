package com.webscout.crawler;

public final class HttpStatusClassifier {

    private HttpStatusClassifier() {
    }

    public static CrawlErrorType classify(int statusCode) {

        validateStatusCode(statusCode);

        if (statusCode >= 200 && statusCode < 300) {
            return null;
        }

        if (statusCode == 429) {
            return CrawlErrorType.HTTP_RATE_LIMITED;
        }

        if (statusCode >= 400 && statusCode < 500) {
            return CrawlErrorType.HTTP_CLIENT_ERROR;
        }

        if (statusCode >= 500 && statusCode < 600) {
            return CrawlErrorType.HTTP_SERVER_ERROR;
        }

        if (statusCode >= 300 && statusCode < 400) {
            return CrawlErrorType.HTTP_REDIRECTION_ERROR;
        }

        return CrawlErrorType.UNKNOWN;
    }

    public static boolean isSuccessful(int statusCode) {

        validateStatusCode(statusCode);

        return statusCode >= 200
                && statusCode < 300;
    }

    private static void validateStatusCode(int statusCode) {

        if (statusCode < 100 || statusCode > 599) {
            throw new IllegalArgumentException(
                    "Invalid HTTP status code"
            );
        }
    }
}