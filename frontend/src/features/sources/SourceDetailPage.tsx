import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { SourceResponse } from '../../types/source';
import { sourcesApi } from '../../api/sources';
import { crawlsApi } from '../../api/crawls';
import { CrawlJobDetailResponse, CrawlJobStatus } from '../../types/crawl';
import { SourceModal } from './SourceModal';
import {
  getDerivedCrawlPolicy,
  getDerivedFetchPolicy,
  getDerivedRobotsPolicy,
  evaluateCrawlScope,
  normalizeUrl,
} from '../../utils/crawlerPolicy';
import { ScopeEvaluationResult, NormalizedUrlResult } from '../../types/crawler';
import {
  ArrowLeft,
  Edit2,
  Trash2,
  ExternalLink,
  Clock,
  Layers,
  Bot,
  CheckCircle2,
  XCircle,
  AlertCircle,
  RefreshCw,
  Search,
  Sparkles,
  Cpu,
  ShieldAlert,
  PlayCircle,
  Activity,
  Timer,
  Calendar,
  Eye,
  Info,
  Loader2,
} from 'lucide-react';

export const SourceDetailPage: React.FC = () => {
  const { sourceId } = useParams<{ sourceId: string }>();
  const navigate = useNavigate();

  const [source, setSource] = useState<SourceResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Edit modal
  const [isEditModalOpen, setIsEditModalOpen] = useState<boolean>(false);

  // Delete modal
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState<boolean>(false);
  const [isDeleting, setIsDeleting] = useState<boolean>(false);

  // Status toggle loading
  const [isToggling, setIsToggling] = useState<boolean>(false);

  // Phase 5 Scope Simulator State
  const [candidateUrlInput, setCandidateUrlInput] = useState<string>('');
  const [scopeResult, setScopeResult] = useState<ScopeEvaluationResult | null>(null);
  const [normalizedResult, setNormalizedResult] = useState<NormalizedUrlResult | null>(null);

  // Phase 9 Crawl Job Engine State
  const [crawls, setCrawls] = useState<CrawlJobDetailResponse[]>([]);
  const [isLoadingCrawls, setIsLoadingCrawls] = useState<boolean>(false);
  const [isTriggeringCrawl, setIsTriggeringCrawl] = useState<boolean>(false);
  const [crawlActionError, setCrawlActionError] = useState<string | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [inspectCrawl, setInspectCrawl] = useState<CrawlJobDetailResponse | null>(null);

  const pollIntervalRef = useRef<number | null>(null);

  const fetchSourceDetail = async () => {
    if (!sourceId) return;
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const data = await sourcesApi.getById(Number(sourceId));
      setSource(data);
      setCandidateUrlInput(data.baseUrl);
    } catch {
      setErrorMessage('Failed to load source details from server or source does not exist.');
    } finally {
      setIsLoading(false);
    }
  };

  const fetchCrawls = useCallback(async () => {
    if (!sourceId) return;
    setIsLoadingCrawls(true);
    try {
      const list = await crawlsApi.getBySource(Number(sourceId));
      setCrawls(list);
    } catch {
      // Ignore if not found
    } finally {
      setIsLoadingCrawls(false);
    }
  }, [sourceId]);

  useEffect(() => {
    fetchSourceDetail();
    fetchCrawls();
  }, [sourceId, fetchCrawls]);

  // Check if any crawl is active for this source
  const hasActiveCrawl = crawls.some((c) => c.status === 'QUEUED' || c.status === 'RUNNING');

  // Live polling effect for active crawls
  useEffect(() => {
    if (hasActiveCrawl) {
      if (!pollIntervalRef.current) {
        pollIntervalRef.current = window.setInterval(() => {
          fetchCrawls();
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
  }, [hasActiveCrawl, fetchCrawls]);

  const handleToggleStatus = async () => {
    if (!source) return;
    setIsToggling(true);
    try {
      const updated = await sourcesApi.update(source.id, {
        name: source.name,
        baseUrl: source.baseUrl,
        enabled: !source.enabled,
        crawlDelaySeconds: source.crawlDelaySeconds,
        requestTimeoutMs: source.requestTimeoutMs,
        maxPages: source.maxPages,
        allowedPathPrefix: source.allowedPathPrefix,
        userAgent: source.userAgent,
      });
      setSource(updated);
    } catch {
      alert('Failed to toggle source status.');
    } finally {
      setIsToggling(false);
    }
  };

  const handleDeleteConfirm = async () => {
    if (!source) return;
    setIsDeleting(true);
    try {
      await sourcesApi.delete(source.id);
      navigate('/sources');
    } catch {
      alert('Failed to delete source. Crawl dependencies may restrict deletion.');
      setIsDeleting(false);
    }
  };

  // Phase 5 Scope & Normalization Test
  const handleTestScope = (urlToTest?: string) => {
    if (!source) return;
    const testTarget = urlToTest !== undefined ? urlToTest : candidateUrlInput;
    if (!testTarget.trim()) return;

    try {
      const norm = normalizeUrl(testTarget, source.baseUrl);
      setNormalizedResult(norm);
    } catch {
      setNormalizedResult(null);
    }

    const result = evaluateCrawlScope(testTarget, source);
    setScopeResult(result);
  };

  // Phase 9 Trigger Manual Crawl
  const handleTriggerCrawl = async () => {
    if (!source) return;
    if (!source.enabled) {
      setCrawlActionError('This source is disabled. Please enable it before triggering a crawl.');
      return;
    }

    setIsTriggeringCrawl(true);
    setCrawlActionError(null);

    try {
      const res = await crawlsApi.triggerCrawl(source.id);
      setToastMessage(`Crawl #${res.crawlId} successfully queued! Starting crawler engine...`);
      setTimeout(() => setToastMessage(null), 5000);
      await fetchCrawls();
    } catch (err: any) {
      if (err.response?.status === 409) {
        setCrawlActionError(
          'An active crawl is already QUEUED or RUNNING for this source. Please wait for it to complete.'
        );
      } else {
        const msg =
          err.response?.data?.message ||
          err.message ||
          'Failed to dispatch crawl job. Please check backend server.';
        setCrawlActionError(msg);
      }
    } finally {
      setIsTriggeringCrawl(false);
    }
  };

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
      default:
        return (
          <span className="inline-flex items-center rounded-full bg-slate-800 px-2.5 py-0.5 text-xs font-semibold text-slate-400">
            {status}
          </span>
        );
    }
  };

  if (isLoading) {
    return (
      <div className="max-w-6xl mx-auto space-y-6">
        <div className="flex items-center gap-3">
          <div className="h-8 w-8 bg-slate-800 rounded-lg animate-pulse" />
          <div className="h-6 w-48 bg-slate-800 rounded animate-pulse" />
        </div>
        <div className="h-64 rounded-2xl border border-slate-800 bg-slate-900/40 animate-pulse" />
      </div>
    );
  }

  if (errorMessage || !source) {
    return (
      <div className="max-w-4xl mx-auto space-y-6 text-center py-16">
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-red-500/10 text-red-400 border border-red-500/20">
          <AlertCircle className="h-8 w-8" />
        </div>
        <div className="space-y-2">
          <h2 className="text-xl font-bold text-white">Source Not Found</h2>
          <p className="text-sm text-slate-400 max-w-md mx-auto">
            {errorMessage || 'The requested source could not be found.'}
          </p>
        </div>
        <Link
          to="/sources"
          className="inline-flex items-center gap-2 rounded-xl border border-slate-800 bg-slate-900 px-4 py-2 text-xs font-semibold text-slate-300 hover:bg-slate-800 transition-all"
        >
          <ArrowLeft className="h-4 w-4" />
          <span>Back to Sources List</span>
        </Link>
      </div>
    );
  }

  const crawlPolicy = getDerivedCrawlPolicy(source);
  const fetchPolicy = getDerivedFetchPolicy(source);
  const robotsPolicy = getDerivedRobotsPolicy(source);
  const activeJob = crawls.find((c) => c.status === 'QUEUED' || c.status === 'RUNNING');

  return (
    <div className="space-y-8 max-w-6xl mx-auto">
      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed top-20 right-8 z-50 rounded-xl border border-emerald-500/30 bg-slate-900/95 p-4 shadow-2xl backdrop-blur-md flex items-center gap-3 text-xs text-emerald-300 animate-in slide-in-from-top-4 duration-200">
          <CheckCircle2 className="h-5 w-5 text-emerald-400 shrink-0" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* Top Breadcrumb & Actions Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/sources')}
            className="flex h-9 w-9 items-center justify-center rounded-xl border border-slate-800 bg-slate-900 text-slate-400 hover:bg-slate-800 hover:text-white transition-colors"
            title="Back to Sources"
          >
            <ArrowLeft className="h-4 w-4" />
          </button>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-2xl font-bold text-white tracking-tight">{source.name}</h1>
              <span
                className={`rounded-full px-2.5 py-0.5 text-[11px] font-semibold border ${
                  source.enabled
                    ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                    : 'bg-slate-800 text-slate-400 border-slate-700'
                }`}
              >
                {source.enabled ? 'ACTIVE' : 'DISABLED'}
              </span>
              <span className="rounded-full bg-gradient-to-r from-emerald-500/10 to-teal-500/10 px-2 py-0.5 text-[10px] font-semibold text-emerald-400 border border-emerald-500/20">
                Phase 4 – 9 Integrated
              </span>
            </div>
            <div className="flex items-center gap-3 text-xs text-slate-400 mt-1">
              <a
                href={source.baseUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1 text-emerald-400 hover:underline font-mono"
              >
                <span>{source.baseUrl}</span>
                <ExternalLink className="h-3 w-3" />
              </a>
              <span>•</span>
              <span>Source ID: #{source.id}</span>
              <span>•</span>
              <span>Updated: {new Date(source.updatedAt).toLocaleDateString()}</span>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2 flex-wrap">
          {/* Phase 9 Run Crawl Button */}
          <button
            onClick={handleTriggerCrawl}
            disabled={isTriggeringCrawl || !source.enabled || hasActiveCrawl}
            className={`flex items-center gap-2 rounded-xl px-4 py-2 text-xs font-semibold transition-all shadow-lg ${
              source.enabled && !hasActiveCrawl
                ? 'bg-gradient-to-r from-emerald-500 to-teal-500 text-slate-950 shadow-emerald-500/20 hover:from-emerald-400 hover:to-teal-400'
                : 'bg-slate-800 text-slate-500 border border-slate-700 cursor-not-allowed'
            }`}
            title={
              !source.enabled
                ? 'Enable source to trigger crawl'
                : hasActiveCrawl
                ? 'An active crawl is currently running for this source'
                : 'Trigger Asynchronous Crawl Job (Phase 9)'
            }
          >
            {isTriggeringCrawl ? (
              <>
                <Loader2 className="h-3.5 w-3.5 animate-spin" />
                <span>Queuing Job...</span>
              </>
            ) : hasActiveCrawl ? (
              <>
                <Activity className="h-3.5 w-3.5 animate-spin text-teal-300" />
                <span>Crawl Executing</span>
              </>
            ) : (
              <>
                <PlayCircle className="h-3.5 w-3.5" />
                <span>Run Crawl</span>
              </>
            )}
          </button>

          <button
            onClick={handleToggleStatus}
            disabled={isToggling}
            className={`flex items-center gap-1.5 rounded-xl border px-3.5 py-2 text-xs font-semibold transition-all ${
              source.enabled
                ? 'border-slate-800 bg-slate-900 text-slate-300 hover:bg-slate-800'
                : 'border-emerald-500/30 bg-emerald-500/10 text-emerald-400 hover:bg-emerald-500/20'
            }`}
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isToggling ? 'animate-spin' : ''}`} />
            <span>{source.enabled ? 'Disable Source' : 'Enable Source'}</span>
          </button>

          <button
            onClick={() => setIsEditModalOpen(true)}
            className="flex items-center gap-1.5 rounded-xl border border-slate-800 bg-slate-900 px-3.5 py-2 text-xs font-semibold text-slate-300 hover:bg-slate-800 hover:text-white transition-all"
          >
            <Edit2 className="h-3.5 w-3.5" />
            <span>Edit</span>
          </button>

          <button
            onClick={() => setIsDeleteModalOpen(true)}
            className="flex items-center gap-1.5 rounded-xl border border-red-500/20 bg-red-500/10 px-3.5 py-2 text-xs font-semibold text-red-400 hover:bg-red-500/20 transition-all"
          >
            <Trash2 className="h-3.5 w-3.5" />
            <span>Delete</span>
          </button>
        </div>
      </div>

      {/* Crawl Action Error Banner */}
      {crawlActionError && (
        <div className="rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-xs text-red-300 flex items-start gap-3">
          <AlertCircle className="h-4 w-4 shrink-0 text-red-400 mt-0.5" />
          <div className="leading-relaxed">{crawlActionError}</div>
        </div>
      )}

      {/* Active Crawl Live Execution Banner */}
      {activeJob && (
        <div className="rounded-2xl border border-teal-500/40 bg-slate-900/90 p-5 space-y-4 shadow-xl backdrop-blur-md relative overflow-hidden">
          <div className="flex items-center justify-between border-b border-slate-800/80 pb-3">
            <div className="flex items-center gap-2">
              <span className="h-2 w-2 rounded-full bg-teal-400 animate-ping" />
              <h3 className="text-sm font-bold text-white flex items-center gap-2">
                <span>Active Crawl Execution In Progress</span>
                <span className="font-mono text-xs text-teal-400">#Job {activeJob.crawlId}</span>
              </h3>
              {getStatusBadge(activeJob.status)}
            </div>
            <div className="text-xs font-mono text-teal-300 flex items-center gap-1.5">
              <Timer className="h-3.5 w-3.5" />
              <span>Elapsed: {formatDuration(activeJob.startedAt, activeJob.finishedAt)}</span>
            </div>
          </div>

          {/* Progress bar */}
          <div className="space-y-1.5">
            <div className="flex justify-between text-xs font-mono">
              <span className="text-slate-400">
                Frontier Progress: {activeJob.pagesProcessed} of {source.maxPages} max pages
              </span>
              <span className="text-teal-300 font-bold">
                {Math.min(100, Math.round((activeJob.pagesProcessed / source.maxPages) * 100))}%
              </span>
            </div>
            <div className="w-full bg-slate-950 rounded-full h-2.5 overflow-hidden border border-slate-800">
              <div
                className="bg-gradient-to-r from-teal-400 to-emerald-400 h-full rounded-full transition-all duration-500"
                style={{
                  width: `${Math.max(
                    5,
                    Math.min(100, Math.round((activeJob.pagesProcessed / source.maxPages) * 100))
                  )}%`,
                }}
              />
            </div>
          </div>

          <div className="grid grid-cols-4 gap-2 text-center text-xs font-mono bg-slate-950/80 p-3 rounded-xl border border-slate-800/80">
            <div>
              <span className="text-slate-500 text-[10px] block uppercase">Discovered</span>
              <span className="text-purple-300 font-bold">{activeJob.pagesDiscovered}</span>
            </div>
            <div>
              <span className="text-slate-500 text-[10px] block uppercase">Succeeded</span>
              <span className="text-emerald-400 font-bold">{activeJob.pagesSucceeded}</span>
            </div>
            <div>
              <span className="text-slate-500 text-[10px] block uppercase">Skipped</span>
              <span className="text-amber-400 font-bold">{activeJob.pagesSkipped}</span>
            </div>
            <div>
              <span className="text-slate-500 text-[10px] block uppercase">Failed</span>
              <span className="text-red-400 font-bold">{activeJob.pagesFailed}</span>
            </div>
          </div>
        </div>
      )}

      {/* Grid: Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        {/* Card 1: Crawl Budget & Politeness */}
        <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-5 space-y-4 backdrop-blur-sm">
          <div className="flex items-center justify-between">
            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-2">
              <Clock className="h-4 w-4 text-emerald-400" />
              <span>Politeness & Budget</span>
            </h3>
            <span className="text-[10px] bg-slate-800 text-slate-300 px-2 py-0.5 rounded">Phase 4</span>
          </div>
          <div className="space-y-3 font-mono text-xs">
            <div className="flex justify-between items-center py-1 border-b border-slate-800/60">
              <span className="text-slate-400">Max Pages Cap</span>
              <span className="text-white font-semibold">{source.maxPages} pages</span>
            </div>
            <div className="flex justify-between items-center py-1 border-b border-slate-800/60">
              <span className="text-slate-400">Crawl Delay</span>
              <span className="text-white font-semibold">{source.crawlDelaySeconds}s delay</span>
            </div>
            <div className="flex justify-between items-center py-1 border-b border-slate-800/60">
              <span className="text-slate-400">Request Timeout</span>
              <span className="text-white font-semibold">{source.requestTimeoutMs}ms</span>
            </div>
            <div className="flex justify-between items-center py-1">
              <span className="text-slate-400">Status</span>
              <span className={source.enabled ? 'text-emerald-400 font-semibold' : 'text-slate-500'}>
                {source.enabled ? 'Active / Eligible' : 'Disabled'}
              </span>
            </div>
          </div>
        </div>

        {/* Card 2: Scope & Boundaries */}
        <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-5 space-y-4 backdrop-blur-sm">
          <div className="flex items-center justify-between">
            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-2">
              <Layers className="h-4 w-4 text-teal-400" />
              <span>Scope Boundaries</span>
            </h3>
            <span className="text-[10px] bg-slate-800 text-slate-300 px-2 py-0.5 rounded">Phase 5 Scope</span>
          </div>
          <div className="space-y-3 font-mono text-xs">
            <div className="flex justify-between items-center py-1 border-b border-slate-800/60">
              <span className="text-slate-400">Allowed Host</span>
              <span className="text-teal-400 font-semibold truncate max-w-[160px]" title={crawlPolicy.allowedHost}>
                {crawlPolicy.allowedHost}
              </span>
            </div>
            <div className="flex justify-between items-center py-1 border-b border-slate-800/60">
              <span className="text-slate-400">Scope Prefix</span>
              <span
                className="text-white font-semibold truncate max-w-[160px]"
                title={crawlPolicy.allowedPathPrefix}
              >
                {crawlPolicy.allowedPathPrefix === '/' ? '/ (Whole Domain)' : crawlPolicy.allowedPathPrefix}
              </span>
            </div>
            <div className="flex justify-between items-center py-1 border-b border-slate-800/60">
              <span className="text-slate-400">Subdomains</span>
              <span className="text-slate-400">Restricted to Host</span>
            </div>
            <div className="flex justify-between items-center py-1">
              <span className="text-slate-400">JavaScript</span>
              <span className="text-slate-400">Disabled (V1 HTML)</span>
            </div>
          </div>
        </div>

        {/* Card 3: Fetch & Robots Policy */}
        <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-5 space-y-4 backdrop-blur-sm">
          <div className="flex items-center justify-between">
            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-2">
              <Bot className="h-4 w-4 text-purple-400" />
              <span>Fetch & Compliance</span>
            </h3>
            <span className="text-[10px] bg-slate-800 text-slate-300 px-2 py-0.5 rounded">Phase 5 Fetch</span>
          </div>
          <div className="space-y-3 font-mono text-xs">
            <div className="flex justify-between items-center py-1 border-b border-slate-800/60">
              <span className="text-slate-400">Robots.txt</span>
              <span className="text-emerald-400 font-semibold flex items-center gap-1">
                <CheckCircle2 className="h-3 w-3" />
                {robotsPolicy.respectRobotsTxt ? 'Respected' : 'Bypassed'}
              </span>
            </div>
            <div className="flex justify-between items-center py-1 border-b border-slate-800/60">
              <span className="text-slate-400">Max Size Limit</span>
              <span className="text-white font-semibold">
                {fetchPolicy.maxResponseSizeBytes / (1024 * 1024)} MB Payload
              </span>
            </div>
            <div className="flex justify-between items-center py-1 border-b border-slate-800/60">
              <span className="text-slate-400">Max Redirects</span>
              <span className="text-white font-semibold">{fetchPolicy.maxRedirects} Hops</span>
            </div>
            <div className="flex justify-between items-center py-1">
              <span className="text-slate-400">Content-Types</span>
              <span
                className="text-slate-400 truncate max-w-[160px]"
                title={fetchPolicy.acceptedContentTypes.join(', ')}
              >
                {fetchPolicy.acceptedContentTypes.join(', ')}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* User Agent Banner */}
      <div className="rounded-xl border border-slate-800 bg-slate-900/40 p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
        <div className="flex items-center gap-2.5">
          <Bot className="h-4 w-4 text-emerald-400 shrink-0" />
          <span className="text-slate-400 font-semibold">Configured User-Agent Header:</span>
          <code className="text-emerald-300 font-mono bg-slate-950 px-2.5 py-1 rounded border border-slate-800 truncate max-w-xl">
            {source.userAgent}
          </code>
        </div>
        <div className="text-[11px] text-slate-500 shrink-0">
          Enforced by Phase 6 HTTP Fetcher & Phase 9 Crawler
        </div>
      </div>

      {/* Phase 9 Crawl History & Execution Feed */}
      <div className="rounded-2xl border border-slate-800/80 bg-slate-900/80 p-6 md:p-8 space-y-6 shadow-xl">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800/80 pb-6">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-gradient-to-tr from-emerald-500/20 to-teal-500/20 text-emerald-400 border border-emerald-500/30">
              <PlayCircle className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-white flex items-center gap-2">
                <span>Phase 9 Crawl Execution Engine & History</span>
                <span className="text-xs font-mono text-emerald-400 bg-emerald-950/80 border border-emerald-800/60 px-2 py-0.5 rounded">
                  {crawls.length} Total Runs
                </span>
              </h2>
              <p className="text-xs text-slate-400 mt-0.5">
                Asynchronous background crawls, page discovery, and HTTP status outcomes for this source.
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => fetchCrawls()}
              className="flex items-center gap-1.5 rounded-xl border border-slate-800 bg-slate-950 px-3 py-1.5 text-xs text-slate-300 hover:text-white hover:bg-slate-800 transition-all font-medium"
            >
              <RefreshCw className={`h-3 w-3 ${isLoadingCrawls ? 'animate-spin' : ''}`} />
              <span>Refresh Runs</span>
            </button>
            <Link
              to="/crawls"
              className="flex items-center gap-1 rounded-xl border border-teal-500/30 bg-teal-500/10 px-3 py-1.5 text-xs text-teal-300 hover:bg-teal-500/20 transition-all font-medium"
            >
              <span>View Global Crawl Console</span>
              <ExternalLink className="h-3 w-3" />
            </Link>
          </div>
        </div>

        {/* Crawls List */}
        {isLoadingCrawls && crawls.length === 0 ? (
          <div className="py-12 text-center text-slate-400 text-xs">
            <RefreshCw className="h-6 w-6 animate-spin mx-auto text-emerald-400 mb-2" />
            Loading crawl history...
          </div>
        ) : crawls.length === 0 ? (
          <div className="rounded-xl border border-slate-800 bg-slate-950/60 p-8 text-center space-y-3">
            <PlayCircle className="h-8 w-8 text-slate-500 mx-auto" />
            <div className="space-y-1">
              <h4 className="text-sm font-bold text-white">No Crawls Executed Yet</h4>
              <p className="text-xs text-slate-400 max-w-sm mx-auto">
                This source has never been crawled. Click "Run Crawl" above to initiate your first asynchronous intelligence harvest.
              </p>
            </div>
          </div>
        ) : (
          <div className="space-y-3">
            {crawls.map((crawl) => (
              <div
                key={crawl.crawlId}
                className="rounded-xl border border-slate-800/80 bg-slate-950/80 p-4 space-y-3 hover:border-slate-700 transition-all"
              >
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                  <div className="flex items-center gap-2.5">
                    <span className="font-mono text-xs font-bold text-emerald-400 bg-emerald-950/60 border border-emerald-800/40 px-2 py-0.5 rounded">
                      #{crawl.crawlId}
                    </span>
                    {getStatusBadge(crawl.status)}
                    <span className="text-[10px] font-mono text-slate-500 bg-slate-900 px-2 py-0.5 rounded border border-slate-800">
                      {crawl.triggerType}
                    </span>
                  </div>

                  <div className="flex items-center gap-4 text-xs font-mono text-slate-400 self-end sm:self-center">
                    <div className="flex items-center gap-1" title="Duration">
                      <Timer className="h-3 w-3 text-slate-500" />
                      <span>{formatDuration(crawl.startedAt, crawl.finishedAt)}</span>
                    </div>
                    <div className="flex items-center gap-1 text-slate-500" title="Created At">
                      <Calendar className="h-3 w-3" />
                      <span>{new Date(crawl.createdAt).toLocaleString()}</span>
                    </div>
                    <button
                      onClick={() => setInspectCrawl(crawl)}
                      className="flex items-center gap-1 text-[11px] text-slate-400 hover:text-white rounded border border-slate-800 bg-slate-900 px-2 py-1 transition-all"
                    >
                      <Eye className="h-3 w-3" />
                      <span>Details</span>
                    </button>
                  </div>
                </div>

                {/* Error diagnostics if failed */}
                {crawl.status === 'FAILED' && (crawl.errorCode || crawl.errorMessage) && (
                  <div className="rounded-lg border border-red-500/30 bg-red-500/10 p-2.5 text-xs text-red-300">
                    <div className="font-bold flex items-center gap-1 text-red-400">
                      <AlertCircle className="h-3.5 w-3.5" />
                      <span>Error: {crawl.errorCode || 'UNKNOWN'}</span>
                    </div>
                    <p className="text-[11px] font-mono mt-0.5 opacity-90">{crawl.errorMessage}</p>
                  </div>
                )}

                {/* Outcome Stats */}
                <div className="grid grid-cols-2 sm:grid-cols-5 gap-2 text-xs font-mono bg-slate-900/60 p-2.5 rounded-lg border border-slate-800/60">
                  <div>
                    <span className="text-[10px] text-slate-500 uppercase">Discovered:</span>{' '}
                    <span className="text-purple-300 font-bold">{crawl.pagesDiscovered}</span>
                  </div>
                  <div>
                    <span className="text-[10px] text-slate-500 uppercase">Processed:</span>{' '}
                    <span className="text-white font-bold">{crawl.pagesProcessed}</span>
                  </div>
                  <div>
                    <span className="text-[10px] text-slate-500 uppercase">Succeeded:</span>{' '}
                    <span className="text-emerald-400 font-bold">{crawl.pagesSucceeded}</span>
                  </div>
                  <div>
                    <span className="text-[10px] text-slate-500 uppercase">Skipped:</span>{' '}
                    <span className="text-amber-400 font-bold">{crawl.pagesSkipped}</span>
                  </div>
                  <div>
                    <span className="text-[10px] text-slate-500 uppercase">Failed:</span>{' '}
                    <span className="text-red-400 font-bold">{crawl.pagesFailed}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Phase 5 Crawler Foundations: Interactive Scope & Normalization Simulator */}
      <div className="rounded-2xl border border-slate-800/80 bg-slate-900/80 p-6 md:p-8 space-y-6 shadow-xl">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-800/80 pb-6">
          <div>
            <div className="flex items-center gap-2">
              <div className="p-2 rounded-xl bg-gradient-to-tr from-teal-500/20 to-emerald-500/20 text-teal-400 border border-teal-500/30">
                <Cpu className="h-5 w-5" />
              </div>
              <h2 className="text-lg font-bold text-white">
                Phase 5 Crawler Foundation: Scope & URL Normalizer Simulator
              </h2>
            </div>
            <p className="text-xs text-slate-400 mt-1">
              Test candidate URLs against this source's <code>CrawlPolicy</code> and evaluate{' '}
              <code>NormalizedUrl</code> resolution, default port stripping, and path scope rules.
            </p>
          </div>

          {/* Quick preset tests */}
          <div className="flex items-center gap-1.5 flex-wrap">
            <span className="text-[11px] text-slate-500 font-semibold uppercase mr-1">Presets:</span>
            <button
              onClick={() => {
                const url = source.baseUrl;
                setCandidateUrlInput(url);
                handleTestScope(url);
              }}
              className="rounded-lg border border-slate-800 bg-slate-950 px-2.5 py-1 text-[11px] text-slate-300 hover:text-emerald-400 hover:border-emerald-500/40 transition-colors font-mono"
            >
              Root Seed
            </button>
            <button
              onClick={() => {
                const prefix = source.allowedPathPrefix && source.allowedPathPrefix !== '/' ? source.allowedPathPrefix : '/articles';
                const url = `${source.baseUrl}${prefix}/example-item#heading`;
                setCandidateUrlInput(url);
                handleTestScope(url);
              }}
              className="rounded-lg border border-slate-800 bg-slate-950 px-2.5 py-1 text-[11px] text-slate-300 hover:text-teal-400 hover:border-teal-500/40 transition-colors font-mono"
            >
              Scoped + Fragment (#)
            </button>
            <button
              onClick={() => {
                const url = 'https://malicious-external-site.com/exploit';
                setCandidateUrlInput(url);
                handleTestScope(url);
              }}
              className="rounded-lg border border-slate-800 bg-slate-950 px-2.5 py-1 text-[11px] text-slate-300 hover:text-red-400 hover:border-red-500/40 transition-colors font-mono"
            >
              External Disallowed
            </button>
          </div>
        </div>

        {/* Input bar */}
        <div className="flex flex-col sm:flex-row items-stretch gap-3">
          <div className="relative flex-1">
            <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3.5 text-slate-500">
              <Search className="h-4 w-4" />
            </div>
            <input
              type="text"
              value={candidateUrlInput}
              onChange={(e) => setCandidateUrlInput(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleTestScope()}
              placeholder="Enter candidate URL to evaluate (e.g. https://domain.com/path#fragment or /relative/page)"
              className="w-full rounded-xl border border-slate-800 bg-slate-950 pl-10 pr-4 py-2.5 text-xs text-white placeholder-slate-500 focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500 font-mono transition-all"
            />
          </div>
          <button
            onClick={() => handleTestScope()}
            className="rounded-xl bg-gradient-to-r from-teal-500 to-emerald-500 px-5 py-2.5 text-xs font-bold text-slate-950 hover:from-teal-400 hover:to-emerald-400 transition-all shadow-lg shadow-teal-500/20 shrink-0"
          >
            Evaluate Scope
          </button>
        </div>

        {/* Evaluation Output Section */}
        {scopeResult && (
          <div className="space-y-4 pt-2">
            {/* Verdict Header */}
            <div
              className={`rounded-xl border p-4 flex items-center justify-between gap-4 ${
                scopeResult.isAllowed
                  ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300'
                  : 'border-red-500/30 bg-red-500/10 text-red-300'
              }`}
            >
              <div className="flex items-center gap-3">
                {scopeResult.isAllowed ? (
                  <CheckCircle2 className="h-6 w-6 text-emerald-400 shrink-0" />
                ) : (
                  <XCircle className="h-6 w-6 text-red-400 shrink-0" />
                )}
                <div>
                  <div className="font-bold text-sm">
                    {scopeResult.isAllowed
                      ? 'IN SCOPE — Permitted by CrawlPolicy'
                      : 'OUT OF SCOPE — Disallowed by CrawlPolicy'}
                  </div>
                  <div className="text-xs opacity-90 mt-0.5">{scopeResult.reason}</div>
                </div>
              </div>

              <div className="hidden sm:flex items-center gap-2 text-xs font-mono">
                <span
                  className={`px-2.5 py-1 rounded-full border ${
                    scopeResult.hostMatches
                      ? 'border-emerald-500/40 bg-emerald-500/20 text-emerald-300'
                      : 'border-red-500/40 bg-red-500/20 text-red-300'
                  }`}
                >
                  Host: {scopeResult.hostMatches ? 'MATCH' : 'MISMATCH'}
                </span>
                <span
                  className={`px-2.5 py-1 rounded-full border ${
                    scopeResult.pathMatches
                      ? 'border-emerald-500/40 bg-emerald-500/20 text-emerald-300'
                      : 'border-red-500/40 bg-red-500/20 text-red-300'
                  }`}
                >
                  Path: {scopeResult.pathMatches ? 'ALLOWED' : 'BLOCKED'}
                </span>
              </div>
            </div>

            {/* Normalization Breakdown Grid */}
            {normalizedResult && (
              <div className="rounded-xl border border-slate-800 bg-slate-950 p-4 space-y-3 text-xs font-mono">
                <div className="text-[11px] font-sans font-bold uppercase tracking-wider text-slate-400 flex items-center gap-2">
                  <Sparkles className="h-3.5 w-3.5 text-teal-400" />
                  <span>Phase 5 NormalizedUrl Properties</span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-slate-300">
                  <div className="space-y-1">
                    <span className="text-slate-500 text-[10px] uppercase">Normalized URL String</span>
                    <div className="p-2 rounded bg-slate-900 border border-slate-800 text-teal-300 break-all">
                      {normalizedResult.normalizedUrl}
                    </div>
                  </div>
                  <div className="space-y-1">
                    <span className="text-slate-500 text-[10px] uppercase">Normalized Host</span>
                    <div className="p-2 rounded bg-slate-900 border border-slate-800 text-slate-200">
                      {normalizedResult.host}
                    </div>
                  </div>
                  <div className="space-y-1">
                    <span className="text-slate-500 text-[10px] uppercase">Normalized Path</span>
                    <div className="p-2 rounded bg-slate-900 border border-slate-800 text-slate-200">
                      {normalizedResult.path}
                    </div>
                  </div>
                  <div className="space-y-1">
                    <span className="text-slate-500 text-[10px] uppercase">Fragment Stripped (#)</span>
                    <div className="p-2 rounded bg-slate-900 border border-slate-800 text-slate-400">
                      {normalizedResult.fragmentRemoved || 'None (Clean)'}
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Crawl Inspect Modal */}
      {inspectCrawl && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/80 p-4 backdrop-blur-sm">
          <div className="w-full max-w-xl rounded-2xl border border-slate-800 bg-slate-900 p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-slate-800/80 pb-3">
              <div className="flex items-center gap-2.5">
                <Info className="h-5 w-5 text-emerald-400" />
                <h3 className="text-base font-bold text-white">
                  Crawl Job #{inspectCrawl.crawlId} Metadata
                </h3>
              </div>
              <button
                onClick={() => setInspectCrawl(null)}
                className="rounded-lg p-1 text-slate-400 hover:text-white"
              >
                ✕
              </button>
            </div>
            <pre className="p-4 rounded-xl bg-slate-950 border border-slate-800 text-emerald-400 font-mono text-xs overflow-x-auto max-h-[350px]">
              {JSON.stringify(inspectCrawl, null, 2)}
            </pre>
            <div className="flex justify-end pt-2">
              <button
                onClick={() => setInspectCrawl(null)}
                className="rounded-xl border border-slate-800 bg-slate-950 px-4 py-2 text-xs font-semibold text-slate-300 hover:bg-slate-800"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Edit Modal */}
      <SourceModal
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        onSuccess={(updated) => setSource(updated)}
        sourceToEdit={source}
      />

      {/* Delete Confirmation Modal */}
      {isDeleteModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/80 p-4 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-2xl border border-red-500/30 bg-slate-900 p-6 shadow-2xl space-y-4">
            <div className="flex items-center gap-3 text-red-400">
              <div className="p-2 rounded-xl bg-red-500/10 border border-red-500/20">
                <ShieldAlert className="h-6 w-6" />
              </div>
              <h3 className="text-base font-bold text-white">Delete Web Source</h3>
            </div>
            <p className="text-xs text-slate-300 leading-relaxed">
              Are you sure you want to delete <strong className="text-white">"{source.name}"</strong>?
              Crawl logs and discovered pages are protected by database RESTRICT invariants and will not cascade.
            </p>
            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                onClick={() => setIsDeleteModalOpen(false)}
                disabled={isDeleting}
                className="rounded-xl border border-slate-800 bg-slate-950 px-4 py-2 text-xs font-semibold text-slate-300 hover:bg-slate-800 transition-all"
              >
                Cancel
              </button>
              <button
                onClick={handleDeleteConfirm}
                disabled={isDeleting}
                className="rounded-xl bg-red-500 px-4 py-2 text-xs font-semibold text-white shadow-lg shadow-red-500/20 hover:bg-red-600 transition-all disabled:opacity-50"
              >
                {isDeleting ? 'Deleting...' : 'Delete Source'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
