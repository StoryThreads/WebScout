import React, { useState, useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { SourceResponse } from '../../types/source';
import { sourcesApi } from '../../api/sources';
import { SourceModal } from './SourceModal';
import { TriggerCrawlModal } from '../crawls/TriggerCrawlModal';
import {
  Globe,
  Plus,
  Search,
  ExternalLink,
  Edit2,
  Trash2,
  Clock,
  Layers,
  ShieldAlert,
  AlertCircle,
  PlayCircle,
  RefreshCw,
  ArrowRight,
  Cpu,
  CheckCircle2,
} from 'lucide-react';

export const SourcesPage: React.FC = () => {
  const [sources, setSources] = useState<SourceResponse[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Filter & Search states
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'DISABLED'>('ALL');

  // Modal states
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedSource, setSelectedSource] = useState<SourceResponse | null>(null);

  // Crawl modal state
  const [isTriggerModalOpen, setIsTriggerModalOpen] = useState(false);
  const [crawlSourceId, setCrawlSourceId] = useState<number | undefined>(undefined);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Delete confirmation modal state
  const [sourceToDelete, setSourceToDelete] = useState<SourceResponse | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const fetchSources = async () => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const data = await sourcesApi.getAll();
      setSources(data);
    } catch {
      setErrorMessage('Failed to load sources from backend server.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchSources();
  }, []);

  const handleCreateNew = () => {
    setSelectedSource(null);
    setIsModalOpen(true);
  };

  const handleEdit = (source: SourceResponse) => {
    setSelectedSource(source);
    setIsModalOpen(true);
  };

  const handleToggleEnabled = async (source: SourceResponse) => {
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
      setSources((prev) => prev.map((s) => (s.id === updated.id ? updated : s)));
    } catch {
      alert('Failed to update source status.');
    }
  };

  const handleDeleteConfirm = async () => {
    if (!sourceToDelete) return;
    setIsDeleting(true);
    try {
      await sourcesApi.delete(sourceToDelete.id);
      setSources((prev) => prev.filter((s) => s.id !== sourceToDelete.id));
      setSourceToDelete(null);
    } catch {
      alert('Cannot delete source. It may have existing crawl dependencies.');
    } finally {
      setIsDeleting(false);
    }
  };

  const handleModalSuccess = (saved: SourceResponse) => {
    setSources((prev) => {
      const exists = prev.some((s) => s.id === saved.id);
      if (exists) {
        return prev.map((s) => (s.id === saved.id ? saved : s));
      }
      return [saved, ...prev];
    });
  };

  // Filtered source list
  const filteredSources = useMemo(() => {
    return sources.filter((s) => {
      const matchesSearch =
        s.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        s.baseUrl.toLowerCase().includes(searchQuery.toLowerCase());

      if (!matchesSearch) return false;

      if (statusFilter === 'ACTIVE') return s.enabled;
      if (statusFilter === 'DISABLED') return !s.enabled;
      return true;
    });
  }, [sources, searchQuery, statusFilter]);

  const activeCount = sources.filter((s) => s.enabled).length;
  const disabledCount = sources.filter((s) => !s.enabled).length;

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
            <h1 className="text-2xl font-bold tracking-tight text-white">
              Source Management
            </h1>
            <span className="rounded-full bg-emerald-500/10 px-2.5 py-0.5 text-xs font-semibold text-emerald-400 border border-emerald-500/20">
              Phase 4 & 5 Live
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Register and manage trusted web sources, politeness limits, and crawl scopes.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={fetchSources}
            className="flex items-center gap-1.5 rounded-xl border border-slate-800 bg-slate-900 px-3.5 py-2 text-xs font-semibold text-slate-300 hover:bg-slate-800 hover:text-white transition-all"
            title="Refresh Sources"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`} />
            <span>Refresh</span>
          </button>
          <button
            onClick={handleCreateNew}
            className="flex items-center gap-2 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 px-4 py-2 text-xs font-semibold text-slate-950 shadow-lg shadow-emerald-500/20 hover:from-emerald-400 hover:to-teal-400 transition-all"
          >
            <Plus className="h-4 w-4" />
            <span>Add Web Source</span>
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
            placeholder="Search sources by name or URL..."
            className="w-full rounded-xl border border-slate-800 bg-slate-950/80 pl-10 pr-4 py-2 text-xs text-white placeholder-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500 transition-all"
          />
        </div>

        {/* Filter Tabs */}
        <div className="flex items-center gap-1 rounded-xl bg-slate-950/80 p-1 border border-slate-800">
          <button
            onClick={() => setStatusFilter('ALL')}
            className={`rounded-lg px-3 py-1.5 text-xs font-medium transition-all ${
              statusFilter === 'ALL'
                ? 'bg-slate-800 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            All ({sources.length})
          </button>
          <button
            onClick={() => setStatusFilter('ACTIVE')}
            className={`rounded-lg px-3 py-1.5 text-xs font-medium transition-all ${
              statusFilter === 'ACTIVE'
                ? 'bg-emerald-500/10 text-emerald-400 shadow-sm border border-emerald-500/20'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Active ({activeCount})
          </button>
          <button
            onClick={() => setStatusFilter('DISABLED')}
            className={`rounded-lg px-3 py-1.5 text-xs font-medium transition-all ${
              statusFilter === 'DISABLED'
                ? 'bg-slate-800 text-slate-300 shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Disabled ({disabledCount})
          </button>
        </div>
      </div>

      {/* Sources Grid / List */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {[1, 2, 3].map((i) => (
            <div
              key={i}
              className="h-56 rounded-2xl border border-slate-800/80 bg-slate-900/40 p-6 animate-pulse space-y-4"
            >
              <div className="flex justify-between items-center">
                <div className="h-6 w-32 bg-slate-800 rounded-lg" />
                <div className="h-5 w-16 bg-slate-800 rounded-full" />
              </div>
              <div className="h-4 w-48 bg-slate-800/60 rounded" />
              <div className="h-16 w-full bg-slate-950/40 rounded-xl" />
            </div>
          ))}
        </div>
      ) : filteredSources.length === 0 ? (
        /* Empty State */
        <div className="rounded-2xl border border-slate-800/80 bg-slate-900/40 p-12 text-center space-y-4">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-800/80 text-slate-400 border border-slate-700/60">
            <Globe className="h-7 w-7" />
          </div>
          <div className="space-y-1">
            <h3 className="text-base font-bold text-white">No Sources Found</h3>
            <p className="text-xs text-slate-400 max-w-sm mx-auto">
              {searchQuery
                ? 'No web sources match your search query.'
                : 'No web sources have been registered yet. Add your first trusted domain to begin collecting intelligence.'}
            </p>
          </div>
          <button
            onClick={handleCreateNew}
            className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 px-5 py-2.5 text-xs font-semibold text-slate-950 shadow-lg shadow-emerald-500/20 hover:from-emerald-400 hover:to-teal-400 transition-all"
          >
            <Plus className="h-4 w-4" />
            <span>Add First Source</span>
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {filteredSources.map((source) => (
            <div
              key={source.id}
              className={`rounded-2xl border transition-all duration-200 p-5 flex flex-col justify-between space-y-4 ${
                source.enabled
                  ? 'border-slate-800 bg-slate-900/80 hover:border-slate-700 shadow-lg'
                  : 'border-slate-800/50 bg-slate-950/40 opacity-75'
              }`}
            >
              {/* Card Header */}
              <div className="space-y-2">
                <div className="flex items-start justify-between gap-3">
                  <Link
                    to={`/sources/${source.id}`}
                    className="text-sm font-bold text-white truncate flex-1 hover:text-emerald-400 transition-colors group flex items-center gap-1.5"
                    title={`View ${source.name} Details & Crawler Policy`}
                  >
                    <span className="truncate">{source.name}</span>
                    <ArrowRight className="h-3 w-3 opacity-0 group-hover:opacity-100 group-hover:translate-x-0.5 transition-all text-emerald-400 shrink-0" />
                  </Link>
                  <button
                    onClick={() => handleToggleEnabled(source)}
                    className={`shrink-0 text-[10px] font-semibold px-2 py-0.5 rounded-full border transition-all ${
                      source.enabled
                        ? 'bg-emerald-950/80 text-emerald-400 border-emerald-800/60 hover:bg-emerald-900'
                        : 'bg-slate-800 text-slate-400 border-slate-700 hover:bg-slate-700'
                    }`}
                  >
                    {source.enabled ? 'ACTIVE' : 'DISABLED'}
                  </button>
                </div>

                <a
                  href={source.baseUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="group inline-flex items-center gap-1.5 text-xs text-slate-400 hover:text-emerald-400 transition-colors font-mono truncate max-w-full"
                  title={source.baseUrl}
                >
                  <Globe className="h-3.5 w-3.5 shrink-0 text-slate-500 group-hover:text-emerald-400" />
                  <span className="truncate">{source.baseUrl}</span>
                  <ExternalLink className="h-3 w-3 shrink-0 opacity-0 group-hover:opacity-100 transition-opacity" />
                </a>
              </div>

              {/* Source Spec Sheet */}
              <div className="rounded-xl border border-slate-800/80 bg-slate-950/80 p-3 text-[11px] text-slate-400 grid grid-cols-2 gap-2">
                <div className="space-y-0.5">
                  <span className="text-slate-500 text-[10px] uppercase font-semibold">Max Pages</span>
                  <div className="font-mono text-slate-200 font-medium">{source.maxPages} pages</div>
                </div>
                <div className="space-y-0.5">
                  <span className="text-slate-500 text-[10px] uppercase font-semibold">Crawl Delay</span>
                  <div className="font-mono text-slate-200 font-medium flex items-center gap-1">
                    <Clock className="h-3 w-3 text-slate-500" />
                    {source.crawlDelaySeconds}s
                  </div>
                </div>
                <div className="space-y-0.5">
                  <span className="text-slate-500 text-[10px] uppercase font-semibold">Timeout</span>
                  <div className="font-mono text-slate-200 font-medium">{source.requestTimeoutMs}ms</div>
                </div>
                <div className="space-y-0.5">
                  <span className="text-slate-500 text-[10px] uppercase font-semibold">Scope Prefix</span>
                  <div
                    className="font-mono text-slate-200 font-medium truncate"
                    title={source.allowedPathPrefix || 'Whole Domain'}
                  >
                    {source.allowedPathPrefix ? (
                      <span className="flex items-center gap-1">
                        <Layers className="h-3 w-3 text-teal-400" />
                        {source.allowedPathPrefix}
                      </span>
                    ) : (
                      'Whole Domain'
                    )}
                  </div>
                </div>
              </div>

              {/* Card Footer Actions */}
              <div className="flex items-center justify-between border-t border-slate-800/80 pt-3 text-xs">
                <div className="flex items-center gap-2">
                  <Link
                    to={`/sources/${source.id}`}
                    className="flex items-center gap-1 rounded-lg p-1.5 text-teal-400 hover:bg-teal-500/10 transition-all font-medium"
                    title="View Crawler Foundations Policy"
                  >
                    <Cpu className="h-3.5 w-3.5" />
                    <span className="text-[11px]">Crawler Policy</span>
                  </Link>
                  <button
                    onClick={() => handleEdit(source)}
                    className="flex items-center gap-1 rounded-lg p-1.5 text-slate-400 hover:bg-slate-800 hover:text-white transition-all"
                    title="Edit Source Configuration"
                  >
                    <Edit2 className="h-3.5 w-3.5" />
                    <span className="text-[11px]">Edit</span>
                  </button>
                  <button
                    onClick={() => setSourceToDelete(source)}
                    className="flex items-center gap-1 rounded-lg p-1.5 text-slate-400 hover:bg-red-500/10 hover:text-red-400 transition-all"
                    title="Delete Source"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                    <span className="text-[11px]">Delete</span>
                  </button>
                </div>

                <button
                  onClick={() => {
                    if (!source.enabled) return;
                    setCrawlSourceId(source.id);
                    setIsTriggerModalOpen(true);
                  }}
                  disabled={!source.enabled}
                  className={`flex items-center gap-1.5 text-[11px] font-semibold px-3 py-1 rounded-lg border transition-all ${
                    source.enabled
                      ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-400 hover:bg-emerald-500/20 hover:border-emerald-500/50 shadow-sm'
                      : 'border-slate-800 bg-slate-950 text-slate-600 cursor-not-allowed'
                  }`}
                  title={source.enabled ? 'Trigger Asynchronous Crawl (Phase 9)' : 'Enable source to run crawl'}
                >
                  <PlayCircle className="h-3.5 w-3.5" />
                  <span>Run Crawl</span>
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Edit / Create Modal */}
      <SourceModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSuccess={handleModalSuccess}
        sourceToEdit={selectedSource}
      />

      {/* Trigger Crawl Modal */}
      <TriggerCrawlModal
        isOpen={isTriggerModalOpen}
        onClose={() => {
          setIsTriggerModalOpen(false);
          setCrawlSourceId(undefined);
        }}
        onSuccess={(res, src) => {
          setToastMessage(`Crawl #${res.crawlId} successfully initiated for "${src.name}"!`);
          setTimeout(() => setToastMessage(null), 5000);
        }}
        sources={sources}
        preselectedSourceId={crawlSourceId}
      />

      {/* Delete Confirmation Modal */}
      {sourceToDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/80 p-4 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-2xl border border-red-500/30 bg-slate-900 p-6 shadow-2xl space-y-4">
            <div className="flex items-center gap-3 text-red-400">
              <div className="p-2 rounded-xl bg-red-500/10 border border-red-500/20">
                <ShieldAlert className="h-6 w-6" />
              </div>
              <h3 className="text-base font-bold text-white">Delete Web Source</h3>
            </div>
            <p className="text-xs text-slate-300 leading-relaxed">
              Are you sure you want to delete <strong className="text-white">"{sourceToDelete.name}"</strong>?
              Historical crawl logs and discovered pages are protected by database RESTRICT invariants and will not cascade.
            </p>
            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                onClick={() => setSourceToDelete(null)}
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
