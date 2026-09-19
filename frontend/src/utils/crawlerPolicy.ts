import {
  NormalizedUrlResult,
  CrawlPolicyConfig,
  FetchPolicyConfig,
  RobotsPolicyConfig,
  ScopeEvaluationResult,
} from '../types/crawler';
import { SourceResponse } from '../types/source';

/**
 * Normalizes a URL adhering strictly to WebScout Phase 5 NormalizedUrl contracts:
 * - Scheme: lowercase (http/https only)
 * - Host: lowercase, default ports (80, 443) stripped
 * - Path: defaulted to '/', relative resolution supported
 * - Fragment: discarded (#...)
 */
export function normalizeUrl(rawUrl: string, baseUrl?: string): NormalizedUrlResult {
  if (!rawUrl || !rawUrl.trim()) {
    throw new Error('URL must not be empty');
  }

  const trimmed = rawUrl.trim();
  let parsed: URL;

  try {
    if (baseUrl) {
      parsed = new URL(trimmed, baseUrl);
    } else {
      parsed = new URL(trimmed);
    }
  } catch {
    throw new Error(`Invalid URL format: "${trimmed}"`);
  }

  const scheme = parsed.protocol.replace(':', '').toLowerCase();
  if (scheme !== 'http' && scheme !== 'https') {
    throw new Error(`URL scheme must be HTTP or HTTPS, received: ${scheme}`);
  }

  const host = parsed.hostname.toLowerCase();
  if (!host) {
    throw new Error('URL must contain a valid host');
  }

  // Strip default ports
  let port: number | null = parsed.port ? parseInt(parsed.port, 10) : null;
  if ((scheme === 'http' && port === 80) || (scheme === 'https' && port === 443)) {
    port = null;
  }

  const fragmentRemoved = parsed.hash ? parsed.hash : null;

  // Path defaults to '/'
  let path = parsed.pathname || '/';
  if (!path.startsWith('/')) {
    path = '/' + path;
  }

  const query = parsed.search ? parsed.search : null;

  // Reconstruct normalized string without fragment or default port
  const portPart = port ? `:${port}` : '';
  const queryPart = query || '';
  const normalizedUrl = `${scheme}://${host}${portPart}${path}${queryPart}`;

  return {
    rawUrl: trimmed,
    scheme,
    host,
    port,
    path,
    query,
    fragmentRemoved,
    normalizedUrl,
  };
}

/**
 * Normalizes the allowed path prefix matching CrawlPolicy.normalizePathPrefix
 */
export function normalizePathPrefix(path?: string | null): string {
  if (!path || !path.trim()) {
    return '/';
  }

  let normalized = path.trim();

  if (!normalized.startsWith('/')) {
    normalized = '/' + normalized;
  }

  if (normalized.includes('#')) {
    throw new Error('Allowed path prefix must not contain a fragment (#)');
  }

  // Strip trailing slash if length > 1 (e.g., "/blog/" -> "/blog")
  if (normalized.length > 1 && normalized.endsWith('/')) {
    normalized = normalized.substring(0, normalized.length - 1);
  }

  return normalized;
}

/**
 * Evaluates whether a target URL is in scope for a given source configuration
 * based on Phase 5 CrawlPolicy rules.
 */
export function evaluateCrawlScope(
  candidateUrl: string,
  source: { baseUrl: string; allowedPathPrefix?: string | null; maxPages: number }
): ScopeEvaluationResult {
  try {
    const baseNorm = normalizeUrl(source.baseUrl);
    const targetNorm = normalizeUrl(candidateUrl, baseNorm.normalizedUrl);

    const allowedHost = baseNorm.host;
    const allowedPathPrefix = normalizePathPrefix(source.allowedPathPrefix);

    const hostMatches = targetNorm.host === allowedHost;

    let pathMatches = false;
    if (allowedPathPrefix === '/') {
      pathMatches = true;
    } else {
      pathMatches =
        targetNorm.path === allowedPathPrefix ||
        targetNorm.path.startsWith(allowedPathPrefix + '/');
    }

    const isAllowed = hostMatches && pathMatches;

    let reason = '';
    if (!hostMatches) {
      reason = `Host mismatch: target host "${targetNorm.host}" differs from allowed host "${allowedHost}"`;
    } else if (!pathMatches) {
      reason = `Path outside scope: path "${targetNorm.path}" does not match allowed prefix "${allowedPathPrefix}"`;
    } else {
      reason = `In scope: host "${allowedHost}" matches and path "${targetNorm.path}" is within prefix "${allowedPathPrefix}"`;
    }

    return {
      url: candidateUrl,
      normalizedUrl: targetNorm.normalizedUrl,
      isAllowed,
      hostMatches,
      pathMatches,
      reason,
    };
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : 'Invalid URL';
    return {
      url: candidateUrl,
      isAllowed: false,
      hostMatches: false,
      pathMatches: false,
      reason: `Evaluation failed: ${msg}`,
    };
  }
}

/**
 * Extracts Phase 5 CrawlPolicy parameters from a registered Source
 */
export function getDerivedCrawlPolicy(source: SourceResponse): CrawlPolicyConfig {
  let allowedHost = '';
  try {
    const parsed = new URL(source.baseUrl);
    allowedHost = parsed.hostname.toLowerCase();
  } catch {
    allowedHost = source.baseUrl;
  }

  return {
    allowedHost,
    allowedPathPrefix: normalizePathPrefix(source.allowedPathPrefix),
    maxPages: source.maxPages,
  };
}

/**
 * Extracts Phase 5 FetchPolicy parameters from a registered Source
 */
export function getDerivedFetchPolicy(source: SourceResponse): FetchPolicyConfig {
  return {
    connectTimeoutMs: Math.min(source.requestTimeoutMs, 10000),
    readTimeoutMs: source.requestTimeoutMs,
    userAgent: source.userAgent,
    maxResponseSizeBytes: 10 * 1024 * 1024, // 10MB per locked spec
    maxRedirects: 5,
    acceptedContentTypes: ['text/html', 'application/xhtml+xml'],
  };
}

/**
 * Extracts Phase 5 RobotsPolicy configuration from a registered Source
 */
export function getDerivedRobotsPolicy(source: SourceResponse): RobotsPolicyConfig {
  return {
    respectRobotsTxt: true,
    crawlDelaySeconds: source.crawlDelaySeconds,
    userAgent: source.userAgent,
  };
}
