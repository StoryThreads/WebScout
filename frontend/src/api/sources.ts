import { apiClient } from './client';
import { SourceResponse, CreateSourceRequest, UpdateSourceRequest } from '../types/source';

export const sourcesApi = {
  getAll: async (): Promise<SourceResponse[]> => {
    const response = await apiClient.get<SourceResponse[]>('/sources');
    return response.data;
  },

  getById: async (sourceId: number): Promise<SourceResponse> => {
    const response = await apiClient.get<SourceResponse>(`/sources/${sourceId}`);
    return response.data;
  },

  create: async (payload: CreateSourceRequest): Promise<SourceResponse> => {
    const response = await apiClient.post<SourceResponse>('/sources', payload);
    return response.data;
  },

  update: async (sourceId: number, payload: UpdateSourceRequest): Promise<SourceResponse> => {
    const response = await apiClient.patch<SourceResponse>(`/sources/${sourceId}`, payload);
    return response.data;
  },

  delete: async (sourceId: number): Promise<void> => {
    await apiClient.delete(`/sources/${sourceId}`);
  },
};
