import React, { useState, useEffect } from 'react';
import { SourceResponse } from '../../types/source';
import { crawlsApi } from '../../api/crawls';
import { CrawlJobResponse } from '../../types/crawl';
import {
  PlayCircle,
  X,
  AlertCircle,
  CheckCircle2,
  Clock,
  Layers,
  Bot,
  Globe,
  Loader2,
} from 'lucide-react';

interface TriggerCrawlModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (response: CrawlJobResponse, source: SourceResponse) => void;
  sources: SourceResponse[];
  preselectedSourceId?: number;
}

export const TriggerCrawlModal: React.FC<TriggerCrawlModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  sources,
  preselectedSourceId,
}) => {
  const [selectedSourceId, setSelectedSourceId] = useState<number | ''>('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      setErrorMessage(null);
      if (preselectedSourceId) {
        setSelectedSourceId(preselectedSourceId);
      } else {
        const activeFirst = sources.find((s) => s.enabled);
        setSelectedSourceId(activeFirst ? activeFirst.id : '');
      }
    }
  }, [isOpen, preselectedSourceId, sources]);

  if (!isOpen) return null;

  const selectedSource = sources.find((s) => s.id === Number(selectedSourceId));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedSource) return;

    if (!selectedSource.enabled) {
      setErrorMessage('This source is disabled. Please enable it first in Source Management.');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      const result = await crawlsApi.triggerCrawl(selectedSource.id);
      onSuccess(result, selectedSource);
      onClose();
    } catch (err: any) {
      if (err.response?.status === 409) {
        setErrorMessage(
          'An active crawl is already QUEUED or RUNNING for this source. Please wait for it to complete.'
        );
      } else {
        const msg =
          err.response?.data?.message ||
          err.message ||
          'Failed to trigger crawl job. Please check backend status.';
        setErrorMessage(msg);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/80 p-4 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="w-full max-w-lg rounded-2xl border border-slate-800 bg-slate-900 shadow-2xl overflow-hidden">
        {/* Modal Header */}
        <div className="flex items-center justify-between border-b border-slate-800/80 px-6 py-4 bg-slate-900/90">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              <PlayCircle className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-white">Trigger Crawl Job</h2>
              <p className="text-xs text-slate-400">
                Phase 9 Asynchronous CrawlCoordinator Engine
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

        {/* Modal Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          {/* Error Message */}
          {errorMessage && (
            <div className="rounded-xl border border-red-500/30 bg-red-500/10 p-3.5 flex items-start gap-3 text-xs text-red-300">
              <AlertCircle className="h-4 w-4 shrink-0 mt-0.5 text-red-400" />
              <div className="leading-relaxed">{errorMessage}</div>
            </div>
          )}

          {/* Source Selection */}
          <div className="space-y-2">
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
              Target Web Source
            </label>
            <select
              value={selectedSourceId}
              onChange={(e) => setSelectedSourceId(e.target.value ? Number(e.target.value) : '')}
              className="w-full rounded-xl border border-slate-800 bg-slate-950 px-3.5 py-2.5 text-xs text-white focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
              required
            >
              <option value="" disabled>
                -- Select a Source to Crawl --
              </option>
              {sources.map((src) => (
                <option key={src.id} value={src.id}>
                  {src.name} ({src.baseUrl}) {src.enabled ? '' : '• [DISABLED]'}
                </option>
              ))}
            </select>
          </div>

          {/* Selected Source Summary Specs */}
          {selectedSource && (
            <div className="rounded-xl border border-slate-800 bg-slate-950/60 p-4 space-y-3">
              <div className="flex items-center justify-between border-b border-slate-800/80 pb-2.5 text-xs">
                <span className="text-slate-400 flex items-center gap-1.5">
                  <Globe className="h-3.5 w-3.5 text-slate-500" />
                  <span className="font-mono truncate max-w-[220px]">{selectedSource.baseUrl}</span>
                </span>
                <span
                  className={`text-[10px] font-semibold px-2 py-0.5 rounded-full border ${
                    selectedSource.enabled
                      ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                      : 'bg-red-500/10 text-red-400 border-red-500/20'
                  }`}
                >
                  {selectedSource.enabled ? 'ACTIVE & ELIGIBLE' : 'DISABLED'}
                </span>
              </div>

              <div className="grid grid-cols-2 gap-2 text-[11px] font-mono">
                <div className="bg-slate-900/80 p-2 rounded-lg border border-slate-800/60 flex items-center justify-between">
                  <span className="text-slate-500 flex items-center gap-1">
                    <Layers className="h-3 w-3 text-teal-400" /> Max Pages:
                  </span>
                  <span className="text-white font-semibold">{selectedSource.maxPages}</span>
                </div>
                <div className="bg-slate-900/80 p-2 rounded-lg border border-slate-800/60 flex items-center justify-between">
                  <span className="text-slate-500 flex items-center gap-1">
                    <Clock className="h-3 w-3 text-emerald-400" /> Delay:
                  </span>
                  <span className="text-white font-semibold">{selectedSource.crawlDelaySeconds}s</span>
                </div>
                <div className="bg-slate-900/80 p-2 rounded-lg border border-slate-800/60 flex items-center justify-between">
                  <span className="text-slate-500">Timeout:</span>
                  <span className="text-white font-semibold">{selectedSource.requestTimeoutMs}ms</span>
                </div>
                <div className="bg-slate-900/80 p-2 rounded-lg border border-slate-800/60 flex items-center justify-between">
                  <span className="text-slate-500">Path Scope:</span>
                  <span className="text-teal-300 font-semibold truncate max-w-[100px]" title={selectedSource.allowedPathPrefix || '/'}>
                    {selectedSource.allowedPathPrefix || '/'}
                  </span>
                </div>
              </div>

              <div className="flex items-center gap-1.5 text-[10px] text-slate-500 pt-1">
                <Bot className="h-3 w-3 text-slate-400" />
                <span className="truncate">UA: {selectedSource.userAgent}</span>
              </div>
            </div>
          )}

          {/* Engine Notice */}
          <div className="rounded-xl border border-emerald-500/20 bg-emerald-500/5 p-3 text-xs text-slate-300 flex items-start gap-2.5">
            <CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0 mt-0.5" />
            <div className="text-[11px] leading-relaxed text-slate-400">
              Once dispatched, the backend creates a job in <code className="text-emerald-300">QUEUED</code> status and executes the crawl frontier asynchronously on a dedicated thread pool.
            </div>
          </div>

          {/* Modal Footer */}
          <div className="flex items-center justify-end gap-3 pt-2 border-t border-slate-800/80">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="rounded-xl border border-slate-800 bg-slate-950 px-4 py-2 text-xs font-semibold text-slate-300 hover:bg-slate-800 transition-all"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting || !selectedSource || !selectedSource.enabled}
              className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 px-5 py-2 text-xs font-semibold text-slate-950 shadow-lg shadow-emerald-500/20 hover:from-emerald-400 hover:to-teal-400 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="h-3.5 w-3.5 animate-spin" />
                  <span>Dispatching...</span>
                </>
              ) : (
                <>
                  <PlayCircle className="h-3.5 w-3.5" />
                  <span>Dispatch Crawl Job</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
