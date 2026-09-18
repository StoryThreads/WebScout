import React, { useState, useEffect } from 'react';
import { SourceResponse, CreateSourceRequest, UpdateSourceRequest } from '../../types/source';
import { sourcesApi } from '../../api/sources';
import { X, Globe, Shield, Clock, Layers, Bot, AlertCircle, Check } from 'lucide-react';
import { AxiosError } from 'axios';
import { ApiErrorResponse } from '../../types/auth';

interface SourceModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (source: SourceResponse) => void;
  sourceToEdit?: SourceResponse | null;
}

export const SourceModal: React.FC<SourceModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  sourceToEdit,
}) => {
  const isEditing = !!sourceToEdit;

  const [name, setName] = useState('');
  const [baseUrl, setBaseUrl] = useState('');
  const [enabled, setEnabled] = useState(true);
  const [crawlDelaySeconds, setCrawlDelaySeconds] = useState(2);
  const [requestTimeoutMs, setRequestTimeoutMs] = useState(5000);
  const [maxPages, setMaxPages] = useState(50);
  const [allowedPathPrefix, setAllowedPathPrefix] = useState('');
  const [userAgent, setUserAgent] = useState('WebScout/1.0 (+https://webscout.io/bot)');

  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (sourceToEdit) {
      setName(sourceToEdit.name);
      setBaseUrl(sourceToEdit.baseUrl);
      setEnabled(sourceToEdit.enabled);
      setCrawlDelaySeconds(sourceToEdit.crawlDelaySeconds);
      setRequestTimeoutMs(sourceToEdit.requestTimeoutMs);
      setMaxPages(sourceToEdit.maxPages);
      setAllowedPathPrefix(sourceToEdit.allowedPathPrefix || '');
      setUserAgent(sourceToEdit.userAgent);
    } else {
      // Reset defaults for creation
      setName('');
      setBaseUrl('');
      setEnabled(true);
      setCrawlDelaySeconds(2);
      setRequestTimeoutMs(5000);
      setMaxPages(50);
      setAllowedPathPrefix('');
      setUserAgent('WebScout/1.0 (+https://webscout.io/bot)');
    }
    setErrorMessage(null);
  }, [sourceToEdit, isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    // Basic client validation
    if (!name.trim()) {
      setErrorMessage('Source name is required.');
      return;
    }

    if (!baseUrl.trim()) {
      setErrorMessage('Base URL is required.');
      return;
    }

    if (!baseUrl.startsWith('http://') && !baseUrl.startsWith('https://')) {
      setErrorMessage('Base URL must begin with http:// or https://');
      return;
    }

    if (crawlDelaySeconds < 0) {
      setErrorMessage('Crawl delay must be 0 or greater.');
      return;
    }

    if (requestTimeoutMs < 1) {
      setErrorMessage('Request timeout must be at least 1 ms.');
      return;
    }

    if (maxPages < 1) {
      setErrorMessage('Max pages must be at least 1.');
      return;
    }

    const payload: CreateSourceRequest | UpdateSourceRequest = {
      name: name.trim(),
      baseUrl: baseUrl.trim(),
      enabled,
      crawlDelaySeconds: Number(crawlDelaySeconds),
      requestTimeoutMs: Number(requestTimeoutMs),
      maxPages: Number(maxPages),
      allowedPathPrefix: allowedPathPrefix.trim() ? allowedPathPrefix.trim() : null,
      userAgent: userAgent.trim(),
    };

    setIsSubmitting(true);
    try {
      let saved: SourceResponse;
      if (isEditing && sourceToEdit) {
        saved = await sourcesApi.update(sourceToEdit.id, payload);
      } else {
        saved = await sourcesApi.create(payload as CreateSourceRequest);
      }
      onSuccess(saved);
      onClose();
    } catch (err: unknown) {
      const axiosError = err as AxiosError<ApiErrorResponse>;
      if (axiosError.response?.data) {
        const errorData = axiosError.response.data;
        if (errorData.code === 'SOURCE_NAME_ALREADY_EXISTS') {
          setErrorMessage(`A source named "${name}" already exists in your workspace.`);
        } else if (errorData.message) {
          setErrorMessage(errorData.message);
        } else {
          setErrorMessage('Failed to save source. Please check the inputs.');
        }
      } else {
        setErrorMessage('Failed to connect to backend server.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/80 p-4 backdrop-blur-sm overflow-y-auto">
      <div className="relative w-full max-w-2xl rounded-2xl border border-slate-800 bg-slate-900 p-6 md:p-8 shadow-2xl my-8">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-800/80 pb-5">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              <Globe className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-white">
                {isEditing ? 'Edit Web Source' : 'Register New Web Source'}
              </h2>
              <p className="text-xs text-slate-400">
                Configure target domain, politeness delay, and crawler boundaries
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-800 hover:text-white transition-colors"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Error Callout */}
        {errorMessage && (
          <div className="mt-5 flex items-start gap-3 rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-xs text-red-400">
            <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
            <div className="flex-1 font-medium">{errorMessage}</div>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="mt-6 space-y-5">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            {/* Source Name */}
            <div className="space-y-1.5 md:col-span-2">
              <label className="text-xs font-semibold uppercase tracking-wider text-slate-300">
                Source Name <span className="text-emerald-400">*</span>
              </label>
              <input
                type="text"
                required
                maxLength={255}
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Spring Boot Official Docs"
                className="w-full rounded-xl border border-slate-800 bg-slate-950/80 px-4 py-2.5 text-sm text-white placeholder-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500 transition-all"
              />
              <span className="text-[11px] text-slate-500">
                Must be unique within your personal workspace.
              </span>
            </div>

            {/* Base URL */}
            <div className="space-y-1.5 md:col-span-2">
              <label className="text-xs font-semibold uppercase tracking-wider text-slate-300">
                Base URL <span className="text-emerald-400">*</span>
              </label>
              <div className="relative">
                <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3.5 text-slate-500">
                  <Globe className="h-4 w-4" />
                </div>
                <input
                  type="url"
                  required
                  maxLength={2048}
                  value={baseUrl}
                  onChange={(e) => setBaseUrl(e.target.value)}
                  placeholder="https://docs.spring.io/spring-boot"
                  className="w-full rounded-xl border border-slate-800 bg-slate-950/80 pl-10 pr-4 py-2.5 text-sm text-white placeholder-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500 transition-all font-mono"
                />
              </div>
              <span className="text-[11px] text-slate-500">
                Crawler root endpoint. Only HTTP and HTTPS targets are permitted.
              </span>
            </div>

            {/* Allowed Path Prefix */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold uppercase tracking-wider text-slate-300">
                Allowed Path Prefix (Optional)
              </label>
              <div className="relative">
                <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3.5 text-slate-500">
                  <Layers className="h-4 w-4" />
                </div>
                <input
                  type="text"
                  maxLength={2048}
                  value={allowedPathPrefix}
                  onChange={(e) => setAllowedPathPrefix(e.target.value)}
                  placeholder="/reference/html/"
                  className="w-full rounded-xl border border-slate-800 bg-slate-950/80 pl-10 pr-4 py-2.5 text-sm text-white placeholder-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500 transition-all font-mono"
                />
              </div>
              <span className="text-[11px] text-slate-500">
                Constrains discovery strictly to URLs starting with this path.
              </span>
            </div>

            {/* Max Pages */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold uppercase tracking-wider text-slate-300">
                Max Pages Cap <span className="text-emerald-400">*</span>
              </label>
              <input
                type="number"
                min={1}
                required
                value={maxPages}
                onChange={(e) => setMaxPages(Math.max(1, parseInt(e.target.value) || 1))}
                className="w-full rounded-xl border border-slate-800 bg-slate-950/80 px-4 py-2.5 text-sm text-white placeholder-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500 transition-all"
              />
              <span className="text-[11px] text-slate-500">
                Maximum pages crawled per job execution.
              </span>
            </div>

            {/* Crawl Delay */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold uppercase tracking-wider text-slate-300">
                Crawl Delay (Seconds) <span className="text-emerald-400">*</span>
              </label>
              <div className="relative">
                <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3.5 text-slate-500">
                  <Clock className="h-4 w-4" />
                </div>
                <input
                  type="number"
                  min={0}
                  required
                  value={crawlDelaySeconds}
                  onChange={(e) => setCrawlDelaySeconds(Math.max(0, parseInt(e.target.value) || 0))}
                  className="w-full rounded-xl border border-slate-800 bg-slate-950/80 pl-10 pr-4 py-2.5 text-sm text-white placeholder-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500 transition-all"
                />
              </div>
              <span className="text-[11px] text-slate-500">
                Politeness pause between requests to target host.
              </span>
            </div>

            {/* Request Timeout */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold uppercase tracking-wider text-slate-300">
                Request Timeout (ms) <span className="text-emerald-400">*</span>
              </label>
              <input
                type="number"
                min={100}
                required
                value={requestTimeoutMs}
                onChange={(e) => setRequestTimeoutMs(Math.max(100, parseInt(e.target.value) || 100))}
                className="w-full rounded-xl border border-slate-800 bg-slate-950/80 px-4 py-2.5 text-sm text-white placeholder-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500 transition-all"
              />
              <span className="text-[11px] text-slate-500">
                HTTP connection & read timeout (e.g. 5000ms = 5s).
              </span>
            </div>

            {/* Custom User Agent */}
            <div className="space-y-1.5 md:col-span-2">
              <label className="text-xs font-semibold uppercase tracking-wider text-slate-300">
                User-Agent Header <span className="text-emerald-400">*</span>
              </label>
              <div className="relative">
                <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3.5 text-slate-500">
                  <Bot className="h-4 w-4" />
                </div>
                <input
                  type="text"
                  required
                  maxLength={512}
                  value={userAgent}
                  onChange={(e) => setUserAgent(e.target.value)}
                  className="w-full rounded-xl border border-slate-800 bg-slate-950/80 pl-10 pr-4 py-2.5 text-sm text-white placeholder-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500 transition-all font-mono"
                />
              </div>
            </div>

            {/* Status Toggle */}
            <div className="md:col-span-2 flex items-center justify-between rounded-xl border border-slate-800 bg-slate-950/60 p-4">
              <div className="space-y-0.5">
                <div className="text-xs font-semibold text-slate-200 flex items-center gap-2">
                  <Shield className="h-4 w-4 text-emerald-400" />
                  Enable Source for Crawling
                </div>
                <p className="text-[11px] text-slate-500">
                  Disabled sources cannot be crawled or scheduled.
                </p>
              </div>
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  checked={enabled}
                  onChange={(e) => setEnabled(e.target.checked)}
                  className="sr-only peer"
                />
                <div className="w-11 h-6 bg-slate-800 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-emerald-500"></div>
              </label>
            </div>
          </div>

          {/* Footer Actions */}
          <div className="flex items-center justify-end gap-3 border-t border-slate-800/80 pt-5">
            <button
              type="button"
              onClick={onClose}
              className="rounded-xl border border-slate-800 bg-slate-950 px-5 py-2.5 text-xs font-semibold text-slate-300 hover:bg-slate-800 hover:text-white transition-all"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="flex items-center gap-2 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 px-6 py-2.5 text-xs font-semibold text-slate-950 shadow-lg shadow-emerald-500/20 hover:from-emerald-400 hover:to-teal-400 transition-all disabled:opacity-50"
            >
              <Check className="h-4 w-4" />
              <span>{isSubmitting ? 'Saving...' : isEditing ? 'Update Source' : 'Register Source'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
