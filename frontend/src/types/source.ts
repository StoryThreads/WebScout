export interface SourceResponse {
  id: number;
  userId: number;
  name: string;
  baseUrl: string;
  enabled: boolean;
  crawlDelaySeconds: number;
  requestTimeoutMs: number;
  maxPages: number;
  allowedPathPrefix?: string | null;
  userAgent: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSourceRequest {
  name: string;
  baseUrl: string;
  enabled: boolean;
  crawlDelaySeconds: number;
  requestTimeoutMs: number;
  maxPages: number;
  allowedPathPrefix?: string | null;
  userAgent: string;
}

export interface UpdateSourceRequest {
  name: string;
  baseUrl: string;
  enabled: boolean;
  crawlDelaySeconds: number;
  requestTimeoutMs: number;
  maxPages: number;
  allowedPathPrefix?: string | null;
  userAgent: string;
}
