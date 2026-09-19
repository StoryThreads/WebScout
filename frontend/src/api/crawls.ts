import { apiClient } from './client';
import { CrawlJobResponse, CrawlJobDetailResponse } from '../types/crawl';

export const crawlsApi = {
  triggerCrawl: async (sourceId: number): Promise<CrawlJobResponse> => {
    const response = await apiClient.post<CrawlJobResponse>(`/sources/${sourceId}/crawl`);
    return response.data;
  },

  getById: async (crawlId: number): Promise<CrawlJobDetailResponse> => {
    const response = await apiClient.get<CrawlJobDetailResponse>(`/crawls/${crawlId}`);
    return response.data;
  },

  getBySource: async (sourceId: number): Promise<CrawlJobDetailResponse[]> => {
    const response = await apiClient.get<CrawlJobDetailResponse[]>(`/sources/${sourceId}/crawls`);
    return response.data;
  },
};
