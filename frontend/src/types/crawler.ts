export interface NormalizedUrlResult {
  rawUrl: string;
  scheme: string;
  host: string;
  port: number | null;
  path: string;
  query: string | null;
  fragmentRemoved: string | null;
  normalizedUrl: string;
}

export interface CrawlPolicyConfig {
  allowedHost: string;
  allowedPathPrefix: string;
  maxPages: number;
}

export interface FetchPolicyConfig {
  connectTimeoutMs: number;
  readTimeoutMs: number;
  userAgent: string;
  maxResponseSizeBytes: number;
  maxRedirects: number;
  acceptedContentTypes: string[];
}

export interface RobotsPolicyConfig {
  respectRobotsTxt: boolean;
  crawlDelaySeconds: number;
  userAgent: string;
}

export interface ScopeEvaluationResult {
  url: string;
  normalizedUrl?: string;
  isAllowed: boolean;
  hostMatches: boolean;
  pathMatches: boolean;
  reason: string;
}

export enum CrawlErrorType {
  TIMEOUT = 'TIMEOUT',
  HTTP_4XX = 'HTTP_4XX',
  HTTP_5XX = 'HTTP_5XX',
  DISALLOWED_BY_ROBOTS = 'DISALLOWED_BY_ROBOTS',
  OUT_OF_SCOPE = 'OUT_OF_SCOPE',
  PAYLOAD_TOO_LARGE = 'PAYLOAD_TOO_LARGE',
  INVALID_CONTENT_TYPE = 'INVALID_CONTENT_TYPE',
  CONNECTION_REFUSED = 'CONNECTION_REFUSED',
  SSL_HANDSHAKE_ERROR = 'SSL_HANDSHAKE_ERROR',
  UNKNOWN = 'UNKNOWN',
}
