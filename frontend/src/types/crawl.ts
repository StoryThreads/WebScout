export type CrawlJobStatus = 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

export type CrawlTriggerType = 'MANUAL' | 'SCHEDULED';

export interface CrawlJobResponse {
  crawlId: number;
  status: CrawlJobStatus;
}

export interface CrawlJobDetailResponse {
  crawlId: number;
  status: CrawlJobStatus;
  triggerType: CrawlTriggerType;
  startedAt: string | null;
  finishedAt: string | null;
  pagesDiscovered: number;
  pagesProcessed: number;
  pagesSucceeded: number;
  pagesSkipped: number;
  pagesFailed: number;
  errorCode: string | null;
  errorMessage: string | null;
  createdAt: string;
}

export interface CrawlJobWithSource extends CrawlJobDetailResponse {
  sourceId: number;
  sourceName: string;
  sourceBaseUrl: string;
}
