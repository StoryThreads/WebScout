import React, { useState, useEffect, useMemo, useRef, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { crawlsApi } from '../../api/crawls';
import { sourcesApi } from '../../api/sources';
import { SourceResponse } from '../../types/source';
import { CrawlJobDetailResponse, CrawlJobStatus } from '../../types/crawl';
import { TriggerCrawlModal } from './TriggerCrawlModal';
import {
  PlayCircle,
  RefreshCw,
  Clock,
  Layers,
  CheckCircle2,
  XCircle,
  AlertCircle,
  ExternalLink,
  ChevronRight,
  Filter,
  Eye,
  Activity,
  Calendar,
  Search,
  Info,
  Timer,
  FileText,
  TrendingUp,
} from 'lucide-react';

interface CrawlWithSourceMetadata extends CrawlJobDetailResponse {
  sourceName: string;
  sourceBaseUrl: string;
  sourceMaxPages: number;
}

export const CrawlsPage: React.FC = () => {
  const [sources, setSources] = useState<SourceResponse[]>([]);
  const [crawls, setCrawls] = useState<CrawlWithSourceMetadata[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Filter states
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [sourceFilter, setSourceFilter] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  // Modals
  const [isTriggerModalOpen, setIsTriggerModalOpen] = useState(false);
  const [preselectedSourceId, setPreselectedSourceId] = useState<number | undefined>(undefined);
  const [inspectJob, setInspectJob] = useState<CrawlWithSourceMetadata | null>(null);

  // Success toast message
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Ref to track polling interval
  const pollIntervalRef = useRef<number | null>(null);

  const fetchAllData = useCallback(async () => {
    try {
      setErrorMessage(null);
      const sourcesList = await sourcesApi.getAll();
      setSources(sourcesList);

      // Fetch crawls for each source in parallel
      const crawlPromises = sourcesList.map(async (src) => {
        try {
          const list = await crawlsApi.getBySource(src.id);
          return list.map((job) => ({
            ...job,
            sourceName: src.name,
            sourceBaseUrl: src.baseUrl,
            sourceMaxPages: src.maxPages,
          }));
        } catch {
          return [];
        }
      });

      const results = await Promise.all(crawlPromises);
      const flattened = results.flat();
      // Sort newest first
      flattened.sort(
        (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      );
      setCrawls(flattened);
    } catch {
      setErrorMessage('Failed to load crawl data from backend.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAllData();
  }, [fetchAllData]);

  // Check if any job is currently active (QUEUED or RUNNING)
  const hasActiveJobs = useMemo(() => {
    return crawls.some((c) => c.status === 'QUEUED' || c.status === 'RUNNING');
  }, [crawls]);

  // Live polling: automatically poll when active jobs exist
  useEffect(() => {
    if (hasActiveJobs) {
      if (!pollIntervalRef.current) {
        pollIntervalRef.current = window.setInterval(() => {
          fetchAllData();
        }, 2500);
      }
    } else {
      if (pollIntervalRef.current) {
        clearInterval(pollIntervalRef.current);
        pollIntervalRef.current = null;
      }
    }

    return () => {
      if (pollIntervalRef.current) {
        clearInterval(pollIntervalRef.current);
        pollIntervalRef.current = null;
      }
    };
  }, [hasActiveJobs, fetchAllData]);

  const handleTriggerSuccess = (res: { crawlId: number; status: string }, source: SourceResponse) => {
    setToastMessage(`Crawl #${res.crawlId} successfully initiated for "${source.name}"!`);
    setTimeout(() => setToastMessage(null), 5000);
    fetchAllData();
  };

  const handleOpenTriggerForSource = (sourceId?: number) => {
    setPreselectedSourceId(sourceId);
    setIsTriggerModalOpen(true);
  };

  // Calculations
  const totalCrawls = crawls.length;
  const activeCrawls = crawls.filter((c) => c.status === 'QUEUED' || c.status === 'RUNNING');
  const completedCrawls = crawls.filter((c) => c.status === 'COMPLETED').length;
  const failedCrawls = crawls.filter((c) => c.status === 'FAILED').length;
  const totalPagesProcessed = crawls.reduce((acc, c) => acc + (c.pagesProcessed || 0), 0);
  const totalPagesDiscovered = crawls.reduce((acc, c) => acc + (c.pagesDiscovered || 0), 0);

  // Filtered crawls list
  const filteredCrawls = useMemo(() => {
    return crawls.filter((c) => {
      if (statusFilter !== 'ALL' && c.status !== statusFilter) return false;
      if (sourceFilter !== 'ALL' && c.sourceName !== sourceFilter) return false;
      if (searchQuery) {
        const q = searchQuery.toLowerCase();
        const matchesId = c.crawlId.toString().includes(q);
        const matchesName = c.sourceName.toLowerCase().includes(q);
        const matchesUrl = c.sourceBaseUrl.toLowerCase().includes(q);
        const matchesError = c.errorCode?.toLowerCase().includes(q) || c.errorMessage?.toLowerCase().includes(q);
        if (!matchesId && !matchesName && !matchesUrl && !matchesError) return false;
      }
      return true;
    });
  }, [crawls, statusFilter, sourceFilter, searchQuery]);

  const formatDuration = (startedAt: string | null, finishedAt: string | null) => {
    if (!startedAt) return '—';
    const start = new Date(startedAt).getTime();
    const end = finishedAt ? new Date(finishedAt).getTime() : Date.now();
    const diffSeconds = Math.max(0, Math.floor((end - start) / 1000));
    if (diffSeconds < 60) return `${diffSeconds}s`;
    const mins = Math.floor(diffSeconds / 60);
    const secs = diffSeconds % 60;
    return `${mins}m ${secs}s`;
  };

  const getStatusBadge = (status: CrawlJobStatus) => {
    switch (status) {
      case 'RUNNING':
        return (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-teal-500/10 px-2.5 py-0.5 text-xs font-semibold text-teal-400 border border-teal-500/30 animate-pulse">
            <span className="h-1.5 w-1.5 rounded-full bg-teal-400 animate-ping" />
            RUNNING
          </span>
        );
      case 'QUEUED':
        return (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-amber-500/10 px-2.5 py-0.5 text-xs font-semibold text-amber-400 border border-amber-500/30">
            <Clock className="h-3 w-3" />
            QUEUED
          </span>
        );
      case 'COMPLETED':
        return (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-500/10 px-2.5 py-0.5 text-xs font-semibold text-emerald-400 border border-emerald-500/30">
            <CheckCircle2 className="h-3 w-3" />
            COMPLETED
          </span>
        );
      case 'FAILED':
        return (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-red-500/10 px-2.5 py-0.5 text-xs font-semibold text-red-400 border border-red-500/30">
            <XCircle className="h-3 w-3" />
            FAILED
          </span>
        );
      case 'CANCELLED':
        return (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-800 px-2.5 py-0.5 text-xs font-semibold text-slate-400 border border-slate-700">
            CANCELLED
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center rounded-full bg-slate-800 px-2.5 py-0.5 text-xs font-semibold text-slate-400">
            {status}
          </span>
        );
    }
  };

  return (
    <div className="space-y-8 max-w-7xl mx-auto">
      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed top-20 right-8 z-50 rounded-xl border border-emerald-500/30 bg-slate-900/95 p-4 shadow-2xl backdrop-blur-md flex items-center gap-3 text-xs text-emerald-300 animate-in slide-in-from-top-4 duration-200">
          <CheckCircle2 className="h-5 w-5 text-emerald-400 shrink-0" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
              <span>Crawl Engine Console</span>
              <span className="rounded-full bg-gradient-to-r from-emerald-500/10 to-teal-500/10 px-2.5 py-0.5 text-xs font-semibold text-emerald-400 border border-emerald-500/20">
                Phase 9 Live
              </span>
            </h1>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Real-time crawler execution monitoring, frontier progress, and page discovery metrics.
          </p>
        </div>

        <div className="flex items-center gap-3">
          {hasActiveJobs && (
            <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-teal-500/30 bg-teal-500/10 text-xs text-teal-300 font-mono animate-pulse">
              <Activity className="h-3.5 w-3.5" />
              <span>Auto-polling (2.5s)</span>
            </div>
          )}
          <button
            onClick={() => fetchAllData()}
            className="flex items-center gap-1.5 rounded-xl border border-slate-800 bg-slate-900 px-3.5 py-2 text-xs font-semibold text-slate-300 hover:bg-slate-800 hover:text-white transition-all"
            title="Refresh All Crawls"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`} />
            <span>Refresh</span>
          </button>
          <button
            onClick={() => handleOpenTriggerForSource()}
            className="flex items-center gap-2 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 px-4 py-2 text-xs font-semibold text-slate-950 shadow-lg shadow-emerald-500/20 hover:from-emerald-400 hover:to-teal-400 transition-all"
          >
            <PlayCircle className="h-4 w-4" />
            <span>Start New Crawl</span>
          </button>
        </div>
      </div>

      {/* Error Callout */}
      {errorMessage && (
        <div className="flex items-center gap-3 rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-xs text-red-400">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{errorMessage}</span>
        </div>
      )}

      {/* Overview Metric Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-6 gap-4">
        {/* Total Crawls */}
        <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-4 space-y-2 backdrop-blur-sm">
          <div className="flex items-center justify-between text-slate-400 text-xs">
            <span className="uppercase font-semibold text-[10px] text-slate-500">Total Crawls</span>
            <Layers className="h-4 w-4 text-slate-400" />
          </div>
          <div className="text-2xl font-bold font-mono text-white">{totalCrawls}</div>
          <div className="text-[11px] text-slate-500">All registered jobs</div>
        </div>

        {/* Active Jobs */}
        <div className="rounded-2xl border border-teal-500/30 bg-teal-500/5 p-4 space-y-2 backdrop-blur-sm">
          <div className="flex items-center justify-between text-teal-400 text-xs">
            <span className="uppercase font-semibold text-[10px] text-teal-400/80">Active Engine</span>
            <Activity className="h-4 w-4 text-teal-400" />
          </div>
          <div className="text-2xl font-bold font-mono text-teal-300 flex items-center gap-2">
            <span>{activeCrawls.length}</span>
            {activeCrawls.length > 0 && (
              <span className="h-2 w-2 rounded-full bg-teal-400 animate-ping" />
            )}
          </div>
          <div className="text-[11px] text-teal-400/70">
            {activeCrawls.length > 0 ? 'Executing frontier...' : 'Engine idle'}
          </div>
        </div>

        {/* Completed */}
        <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-4 space-y-2 backdrop-blur-sm">
          <div className="flex items-center justify-between text-emerald-400 text-xs">
            <span className="uppercase font-semibold text-[10px] text-emerald-500">Completed</span>
            <CheckCircle2 className="h-4 w-4 text-emerald-400" />
          </div>
          <div className="text-2xl font-bold font-mono text-emerald-400">{completedCrawls}</div>
          <div className="text-[11px] text-slate-500">Successful runs</div>
        </div>

        {/* Failed */}
        <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-4 space-y-2 backdrop-blur-sm">
          <div className="flex items-center justify-between text-red-400 text-xs">
            <span className="uppercase font-semibold text-[10px] text-red-500">Failed</span>
            <XCircle className="h-4 w-4 text-red-400" />
          </div>
          <div className="text-2xl font-bold font-mono text-red-400">{failedCrawls}</div>
          <div className="text-[11px] text-slate-500">Unreached limits/errors</div>
        </div>

        {/* Pages Discovered */}
        <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-4 space-y-2 backdrop-blur-sm">
          <div className="flex items-center justify-between text-slate-400 text-xs">
            <span className="uppercase font-semibold text-[10px] text-slate-500">Discovered</span>
            <TrendingUp className="h-4 w-4 text-purple-400" />
          </div>
          <div className="text-2xl font-bold font-mono text-purple-300">{totalPagesDiscovered}</div>
          <div className="text-[11px] text-slate-500">Frontier candidate URLs</div>
        </div>

        {/* Pages Processed */}
        <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-4 space-y-2 backdrop-blur-sm">
          <div className="flex items-center justify-between text-slate-400 text-xs">
            <span className="uppercase font-semibold text-[10px] text-slate-500">Processed</span>
            <FileText className="h-4 w-4 text-emerald-400" />
          </div>
          <div className="text-2xl font-bold font-mono text-emerald-300">{totalPagesProcessed}</div>
          <div className="text-[11px] text-slate-500">HTTP fetch & extracted</div>
        </div>
      </div>

      {/* Active Crawls Banner (if any is active) */}
      {activeCrawls.length > 0 && (
        <div className="space-y-3">
          <h3 className="text-xs font-bold uppercase tracking-wider text-teal-400 flex items-center gap-2">
            <Activity className="h-4 w-4 animate-spin" />
            <span>Currently Executing Crawl Jobs</span>
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {activeCrawls.map((active) => {
              const progressPct = Math.min(
                100,
                active.sourceMaxPages > 0
                  ? Math.round((active.pagesProcessed / active.sourceMaxPages) * 100)
                  : 0
              );
              return (
                <div
                  key={active.crawlId}
                  className="rounded-2xl border border-teal-500/40 bg-slate-900/90 p-5 space-y-4 shadow-xl backdrop-blur-md relative overflow-hidden"
                >
                  <div className="absolute top-0 left-0 right-0 h-1 bg-slate-800">
                    <div
                      className="h-full bg-gradient-to-r from-teal-400 to-emerald-400 transition-all duration-500"
                      style={{ width: `${Math.max(5, progressPct)}%` }}
                    />
                  </div>

                  <div className="flex items-start justify-between gap-3 pt-1">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-mono text-teal-400 font-bold">
                          Job #{active.crawlId}
                        </span>
                        {getStatusBadge(active.status)}
                      </div>
                      <h4 className="text-sm font-bold text-white mt-1">{active.sourceName}</h4>
                      <p className="text-xs text-slate-400 font-mono truncate max-w-sm">
                        {active.sourceBaseUrl}
                      </p>
                    </div>
                    <div className="text-right">
                      <div className="text-xs text-slate-400 font-mono flex items-center gap-1 justify-end">
                        <Timer className="h-3 w-3 text-teal-400" />
                        <span>{formatDuration(active.startedAt, active.finishedAt)}</span>
                      </div>
                      <span className="text-[10px] text-slate-500 uppercase">{active.triggerType}</span>
                    </div>
                  </div>

                  {/* Progress Bar & Counters */}
                  <div className="space-y-2">
                    <div className="flex justify-between text-xs font-mono">
                      <span className="text-slate-400">
                        Processed {active.pagesProcessed} / {active.sourceMaxPages} max pages
                      </span>
                      <span className="text-teal-400 font-bold">{progressPct}%</span>
                    </div>
                    <div className="w-full bg-slate-950 rounded-full h-2 overflow-hidden border border-slate-800">
                      <div
                        className="bg-teal-400 h-full rounded-full transition-all duration-500"
                        style={{ width: `${progressPct}%` }}
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-4 gap-2 text-center text-[11px] font-mono bg-slate-950/80 p-2.5 rounded-xl border border-slate-800/80">
                    <div>
                      <span className="text-slate-500 block text-[10px]">Discovered</span>
                      <span className="text-purple-300 font-bold">{active.pagesDiscovered}</span>
                    </div>
                    <div>
                      <span className="text-slate-500 block text-[10px]">Succeeded</span>
                      <span className="text-emerald-400 font-bold">{active.pagesSucceeded}</span>
                    </div>
                    <div>
                      <span className="text-slate-500 block text-[10px]">Skipped</span>
                      <span className="text-amber-400 font-bold">{active.pagesSkipped}</span>
                    </div>
                    <div>
                      <span className="text-slate-500 block text-[10px]">Failed</span>
                      <span className="text-red-400 font-bold">{active.pagesFailed}</span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Search & Filter Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 rounded-2xl border border-slate-800/80 bg-slate-900/60 p-4 backdrop-blur-md">
        {/* Search */}
        <div className="relative flex-1 max-w-md">
          <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3.5 text-slate-500">
            <Search className="h-4 w-4" />
          </div>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search by job ID, source name, or error code..."
            className="w-full rounded-xl border border-slate-800 bg-slate-950/80 pl-10 pr-4 py-2 text-xs text-white placeholder-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500 transition-all"
          />
        </div>

        {/* Source Dropdown Filter */}
        <div className="flex items-center gap-2">
          <Filter className="h-3.5 w-3.5 text-slate-500" />
          <select
            value={sourceFilter}
            onChange={(e) => setSourceFilter(e.target.value)}
            className="rounded-xl border border-slate-800 bg-slate-950/80 px-3 py-2 text-xs text-slate-300 focus:border-emerald-500 focus:outline-none"
          >
            <option value="ALL">All Sources ({crawls.length})</option>
            {sources.map((src) => (
              <option key={src.id} value={src.name}>
                {src.name}
              </option>
            ))}
          </select>

          {/* Status Tabs */}
          <div className="flex items-center gap-1 rounded-xl bg-slate-950/80 p-1 border border-slate-800">
            {['ALL', 'QUEUED', 'RUNNING', 'COMPLETED', 'FAILED'].map((st) => (
              <button
                key={st}
                onClick={() => setStatusFilter(st)}
                className={`rounded-lg px-2.5 py-1 text-xs font-medium transition-all ${
                  statusFilter === st
                    ? 'bg-slate-800 text-white shadow-sm'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                {st}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Crawls Table / Feed */}
      {isLoading ? (
        <div className="rounded-2xl border border-slate-800/80 bg-slate-900/40 p-12 text-center">
          <RefreshCw className="h-8 w-8 text-emerald-400 animate-spin mx-auto mb-3" />
          <p className="text-xs text-slate-400">Loading crawl jobs from PostgreSQL backend...</p>
        </div>
      ) : filteredCrawls.length === 0 ? (
        <div className="rounded-2xl border border-slate-800/80 bg-slate-900/40 p-12 text-center space-y-4">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-800/80 text-slate-400 border border-slate-700/60">
            <PlayCircle className="h-7 w-7" />
          </div>
          <div className="space-y-1">
            <h3 className="text-base font-bold text-white">No Crawl Jobs Found</h3>
            <p className="text-xs text-slate-400 max-w-sm mx-auto">
              {searchQuery || statusFilter !== 'ALL' || sourceFilter !== 'ALL'
                ? 'No crawl jobs match the selected filter criteria.'
                : 'No crawls have been triggered yet. Select a web source and start your first asynchronous crawl job.'}
            </p>
          </div>
          <button
            onClick={() => handleOpenTriggerForSource()}
            className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 px-5 py-2.5 text-xs font-semibold text-slate-950 shadow-lg shadow-emerald-500/20 hover:from-emerald-400 hover:to-teal-400 transition-all"
          >
            <PlayCircle className="h-4 w-4" />
            <span>Start First Crawl</span>
          </button>
        </div>
      ) : (
        <div className="space-y-3">
          {filteredCrawls.map((job) => (
            <div
              key={job.crawlId}
              className="rounded-2xl border border-slate-800/80 bg-slate-900/70 hover:border-slate-700/80 p-5 transition-all space-y-4 backdrop-blur-sm"
            >
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
                <div className="flex items-center gap-3">
                  <span className="text-xs font-mono font-bold text-emerald-400 bg-emerald-950/60 border border-emerald-800/40 px-2 py-0.5 rounded">
                    #{job.crawlId}
                  </span>
                  <div>
                    <div className="flex items-center gap-2">
                      <Link
                        to={`/sources/${sources.find((s) => s.name === job.sourceName)?.id || ''}`}
                        className="text-sm font-bold text-white hover:text-emerald-400 transition-colors flex items-center gap-1 group"
                      >
                        <span>{job.sourceName}</span>
                        <ChevronRight className="h-3 w-3 text-slate-500 group-hover:text-emerald-400 transition-colors" />
                      </Link>
                      {getStatusBadge(job.status)}
                      <span className="text-[10px] font-mono text-slate-500 bg-slate-950 px-2 py-0.5 rounded border border-slate-800">
                        {job.triggerType}
                      </span>
                    </div>
                    <a
                      href={job.sourceBaseUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-xs text-slate-400 font-mono hover:underline inline-flex items-center gap-1 mt-0.5"
                    >
                      <span className="truncate max-w-sm">{job.sourceBaseUrl}</span>
                      <ExternalLink className="h-2.5 w-2.5" />
                    </a>
                  </div>
                </div>

                <div className="flex items-center gap-4 text-xs font-mono text-slate-400 self-end sm:self-center">
                  <div className="flex items-center gap-1 text-slate-400" title="Execution Duration">
                    <Timer className="h-3.5 w-3.5 text-slate-500" />
                    <span>{formatDuration(job.startedAt, job.finishedAt)}</span>
                  </div>
                  <div className="flex items-center gap-1 text-slate-500" title="Created Timestamp">
                    <Calendar className="h-3.5 w-3.5" />
                    <span>{new Date(job.createdAt).toLocaleString()}</span>
                  </div>
                  <button
                    onClick={() => setInspectJob(job)}
                    className="flex items-center gap-1 rounded-lg border border-slate-800 bg-slate-950 px-2.5 py-1.5 text-xs text-slate-300 hover:text-white hover:bg-slate-800 transition-all font-sans"
                    title="Inspect Job Metadata"
                  >
                    <Eye className="h-3 w-3" />
                    <span>Inspect</span>
                  </button>
                  <button
                    onClick={() => {
                      const src = sources.find((s) => s.name === job.sourceName);
                      if (src) handleOpenTriggerForSource(src.id);
                    }}
                    className="flex items-center gap-1 rounded-lg border border-emerald-500/20 bg-emerald-500/10 px-2.5 py-1.5 text-xs text-emerald-400 hover:bg-emerald-500/20 transition-all font-sans font-semibold"
                    title="Re-run crawl for this source"
                  >
                    <PlayCircle className="h-3 w-3" />
                    <span>Re-run</span>
                  </button>
                </div>
              </div>

              {/* Error callout if failed */}
              {job.status === 'FAILED' && (job.errorCode || job.errorMessage) && (
                <div className="rounded-xl border border-red-500/30 bg-red-500/10 p-3 text-xs text-red-300 space-y-1">
                  <div className="font-bold flex items-center gap-1.5 text-red-400">
                    <AlertCircle className="h-4 w-4" />
                    <span>Error Code: {job.errorCode || 'UNKNOWN'}</span>
                  </div>
                  <p className="text-[11px] font-mono leading-relaxed opacity-90">{job.errorMessage}</p>
                </div>
              )}

              {/* Stats Strip */}
              <div className="grid grid-cols-2 sm:grid-cols-5 gap-2 text-xs font-mono bg-slate-950/70 p-3 rounded-xl border border-slate-800/60">
                <div className="space-y-0.5">
                  <span className="text-[10px] text-slate-500 uppercase">Discovered</span>
                  <div className="font-bold text-purple-300">{job.pagesDiscovered} pages</div>
                </div>
                <div className="space-y-0.5">
                  <span className="text-[10px] text-slate-500 uppercase">Processed</span>
                  <div className="font-bold text-slate-200">{job.pagesProcessed} pages</div>
                </div>
                <div className="space-y-0.5">
                  <span className="text-[10px] text-slate-500 uppercase">Succeeded</span>
                  <div className="font-bold text-emerald-400">{job.pagesSucceeded} ok</div>
                </div>
                <div className="space-y-0.5">
                  <span className="text-[10px] text-slate-500 uppercase">Skipped</span>
                  <div className="font-bold text-amber-400">{job.pagesSkipped} skipped</div>
                </div>
                <div className="space-y-0.5">
                  <span className="text-[10px] text-slate-500 uppercase">Failed</span>
                  <div className="font-bold text-red-400">{job.pagesFailed} failed</div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Inspect Modal */}
      {inspectJob && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/80 p-4 backdrop-blur-sm">
          <div className="w-full max-w-2xl rounded-2xl border border-slate-800 bg-slate-900 p-6 shadow-2xl space-y-5">
            <div className="flex items-center justify-between border-b border-slate-800/80 pb-4">
              <div className="flex items-center gap-3">
                <div className="p-2 rounded-xl bg-slate-800 text-slate-300">
                  <Info className="h-5 w-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-white">
                    Crawl Job #{inspectJob.crawlId} Diagnostics
                  </h3>
                  <p className="text-xs text-slate-400">{inspectJob.sourceName}</p>
                </div>
              </div>
              <button
                onClick={() => setInspectJob(null)}
                className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-800 hover:text-white"
              >
                ✕
              </button>
            </div>

            <div className="space-y-4 text-xs font-mono max-h-[420px] overflow-y-auto">
              <div className="grid grid-cols-2 gap-3 text-slate-300">
                <div className="p-3 rounded-xl bg-slate-950 border border-slate-800">
                  <span className="text-slate-500 text-[10px] uppercase block">Current Status</span>
                  <div className="mt-1">{getStatusBadge(inspectJob.status)}</div>
                </div>
                <div className="p-3 rounded-xl bg-slate-950 border border-slate-800">
                  <span className="text-slate-500 text-[10px] uppercase block">Trigger Type</span>
                  <div className="mt-1 text-white font-bold">{inspectJob.triggerType}</div>
                </div>
                <div className="p-3 rounded-xl bg-slate-950 border border-slate-800">
                  <span className="text-slate-500 text-[10px] uppercase block">Started At</span>
                  <div className="mt-1 text-slate-300">
                    {inspectJob.startedAt ? new Date(inspectJob.startedAt).toLocaleString() : 'Not started'}
                  </div>
                </div>
                <div className="p-3 rounded-xl bg-slate-950 border border-slate-800">
                  <span className="text-slate-500 text-[10px] uppercase block">Finished At</span>
                  <div className="mt-1 text-slate-300">
                    {inspectJob.finishedAt ? new Date(inspectJob.finishedAt).toLocaleString() : 'Running / Active'}
                  </div>
                </div>
              </div>

              {/* Detailed JSON breakdown */}
              <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 space-y-1">
                <span className="text-slate-500 text-[10px] uppercase block font-sans font-bold">
                  Raw API Payload (Backend Contract)
                </span>
                <pre className="text-emerald-400 text-[11px] whitespace-pre-wrap overflow-x-auto">
                  {JSON.stringify(inspectJob, null, 2)}
                </pre>
              </div>
            </div>

            <div className="flex items-center justify-end pt-2 border-t border-slate-800/80">
              <button
                onClick={() => setInspectJob(null)}
                className="rounded-xl border border-slate-800 bg-slate-950 px-4 py-2 text-xs font-semibold text-slate-300 hover:bg-slate-800"
              >
                Close Inspector
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Trigger Crawl Modal */}
      <TriggerCrawlModal
        isOpen={isTriggerModalOpen}
        onClose={() => {
          setIsTriggerModalOpen(false);
          setPreselectedSourceId(undefined);
        }}
        onSuccess={handleTriggerSuccess}
        sources={sources}
        preselectedSourceId={preselectedSourceId}
      />
    </div>
  );
};
