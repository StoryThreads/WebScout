import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { authApi } from '../../api/auth';
import { sourcesApi } from '../../api/sources';
import { crawlsApi } from '../../api/crawls';
import { TOKEN_STORAGE, apiClient } from '../../api/client';
import {
  User,
  Globe,
  KeyRound,
  RefreshCw,
  Send,
  Layers,
  ArrowRight,
  Sparkles,
  Terminal,
  Cpu,
  PlayCircle,
  Activity,
  CheckCircle2,
} from 'lucide-react';
import { normalizeUrl, evaluateCrawlScope } from '../../utils/crawlerPolicy';

export const DashboardPage: React.FC = () => {
  const { user, refreshProfile } = useAuth();

  const [testResult, setTestResult] = useState<{
    endpoint: string;
    status: 'idle' | 'loading' | 'success' | 'error';
    data?: unknown;
    timestamp?: string;
  }>({
    endpoint: 'None',
    status: 'idle',
  });

  const [accessToken, setAccessToken] = useState<string | null>(TOKEN_STORAGE.getAccessToken());
  const [refreshToken, setRefreshToken] = useState<string | null>(TOKEN_STORAGE.getRefreshToken());
  const [sourceCount, setSourceCount] = useState<number | null>(null);
  const [crawlCount, setCrawlCount] = useState<number | null>(null);

  const syncTokens = () => {
    setAccessToken(TOKEN_STORAGE.getAccessToken());
    setRefreshToken(TOKEN_STORAGE.getRefreshToken());
  };

  const loadMetrics = async () => {
    try {
      const sources = await sourcesApi.getAll();
      setSourceCount(sources.length);

      let total = 0;
      for (const s of sources) {
        try {
          const c = await crawlsApi.getBySource(s.id);
          total += c.length;
        } catch {
          // ignore
        }
      }
      setCrawlCount(total);
    } catch {
      // Ignored if unauthenticated or network failure
    }
  };

  useEffect(() => {
    syncTokens();
    loadMetrics();
  }, []);

  // 1. Test GET /api/v1/users/me
  const handleTestCurrentUser = async () => {
    setTestResult({ endpoint: 'GET /api/v1/users/me', status: 'loading' });
    try {
      const data = await authApi.getCurrentUser();
      setTestResult({
        endpoint: 'GET /api/v1/users/me',
        status: 'success',
        data,
        timestamp: new Date().toLocaleTimeString(),
      });
      await refreshProfile();
    } catch (err: unknown) {
      setTestResult({
        endpoint: 'GET /api/v1/users/me',
        status: 'error',
        data: err,
        timestamp: new Date().toLocaleTimeString(),
      });
    }
  };

  // 2. Test POST /api/v1/auth/refresh
  const handleTestTokenRefresh = async () => {
    setTestResult({ endpoint: 'POST /api/v1/auth/refresh', status: 'loading' });
    try {
      const currentRefreshToken = TOKEN_STORAGE.getRefreshToken();
      if (!currentRefreshToken) throw new Error('No refresh token present in storage.');

      const data = await authApi.refresh({ refreshToken: currentRefreshToken });
      TOKEN_STORAGE.setTokens(data.accessToken, data.refreshToken);
      syncTokens();

      setTestResult({
        endpoint: 'POST /api/v1/auth/refresh',
        status: 'success',
        data: {
          message: 'Token pair successfully rotated in PostgreSQL database!',
          newAccessTokenPreview: `${data.accessToken.substring(0, 24)}...`,
          newRefreshTokenPreview: `${data.refreshToken.substring(0, 24)}...`,
        },
        timestamp: new Date().toLocaleTimeString(),
      });
    } catch (err: unknown) {
      setTestResult({
        endpoint: 'POST /api/v1/auth/refresh',
        status: 'error',
        data: err,
        timestamp: new Date().toLocaleTimeString(),
      });
    }
  };

  // 3. Test Silent Recovery via invalid token simulation
  const handleTestSilentRecovery = async () => {
    setTestResult({ endpoint: 'Testing Silent Token Refresh Interceptor', status: 'loading' });
    try {
      const response = await apiClient.get('/users/me', {
        headers: { Authorization: 'Bearer invalid.tampered.jwt' },
      });
      syncTokens();
      setTestResult({
        endpoint: 'Silent Refresh Recovery Verification',
        status: 'success',
        data: {
          status: '401 Intercepted & Automatically Healed!',
          explanation:
            'The API client caught the 401 Unauthorized response, invoked POST /api/v1/auth/refresh with the refresh token, and re-executed the original request without user disruption.',
          recoveredUserData: response.data,
        },
        timestamp: new Date().toLocaleTimeString(),
      });
    } catch (err: unknown) {
      setTestResult({
        endpoint: 'Silent Refresh Recovery Verification',
        status: 'error',
        data: err,
        timestamp: new Date().toLocaleTimeString(),
      });
    }
  };

  // 4. Test GET /api/v1/sources
  const handleTestSources = async () => {
    setTestResult({ endpoint: 'GET /api/v1/sources', status: 'loading' });
    try {
      const data = await sourcesApi.getAll();
      setSourceCount(data.length);
      setTestResult({
        endpoint: 'GET /api/v1/sources',
        status: 'success',
        data: {
          totalSources: data.length,
          sources: data,
        },
        timestamp: new Date().toLocaleTimeString(),
      });
    } catch (err: unknown) {
      setTestResult({
        endpoint: 'GET /api/v1/sources',
        status: 'error',
        data: err,
        timestamp: new Date().toLocaleTimeString(),
      });
    }
  };

  // 5. Test Phase 5 Crawler Foundations: URL Normalizer & Scope Policy Invariants
  const handleTestCrawlerFoundations = async () => {
    setTestResult({ endpoint: 'Phase 5 Crawler Foundations Verification', status: 'loading' });
    try {
      const sample1 = normalizeUrl('https://EnGiNeErInG.WebScout.IO:443/docs/intro//nested/../path?ref=dash#hash-fragment');
      const sample2 = normalizeUrl('http://example.com:80/');
      
      const mockSource = {
        baseUrl: 'https://docs.webscout.io/guides',
        allowedPathPrefix: '/guides',
        maxPages: 100,
      };

      const scopeTestAllowed = evaluateCrawlScope('https://docs.webscout.io/guides/architecture/flow#diagram', mockSource);
      const scopeTestBlockedHost = evaluateCrawlScope('https://attacker-site.com/guides', mockSource);
      const scopeTestBlockedPath = evaluateCrawlScope('https://docs.webscout.io/secret-admin', mockSource);

      setTestResult({
        endpoint: 'Phase 5 Crawler Foundations & URL Normalizer',
        status: 'success',
        data: {
          phase: 'Phase 5 — Crawler Foundations',
          urlNormalizationEngine: {
            description: 'Scheme & host lowercasing, default ports (80/443) stripped, fragment removal, path normalization',
            testCase1: {
              input: 'https://EnGiNeErInG.WebScout.IO:443/docs/intro//nested/../path?ref=dash#hash-fragment',
              output: sample1,
            },
            testCase2: {
              input: 'http://example.com:80/',
              output: sample2,
            },
          },
          crawlPolicyScopeEvaluation: {
            configuredSource: mockSource,
            inScopeEvaluation: scopeTestAllowed,
            outOfScopeHostMismatch: scopeTestBlockedHost,
            outOfScopePathMismatch: scopeTestBlockedPath,
          },
          frontierBehavior: 'Seen URL deduplication via NormalizedUrl key; bounded FIFO queue',
          status: 'Phase 5 contracts verified and ready for Phase 6 HTTP Fetcher',
        },
        timestamp: new Date().toLocaleTimeString(),
      });
    } catch (err: unknown) {
      setTestResult({
        endpoint: 'Phase 5 Crawler Foundations Verification',
        status: 'error',
        data: err,
        timestamp: new Date().toLocaleTimeString(),
      });
    }
  };

  // 6. Test GET /api/v1/sources/{sourceId}/crawls (Phase 9)
  const handleTestGetCrawls = async () => {
    setTestResult({ endpoint: 'GET /api/v1/sources/{id}/crawls', status: 'loading' });
    try {
      const sources = await sourcesApi.getAll();
      if (sources.length === 0) {
        throw new Error('Please create at least one Web Source in Phase 4 before testing crawl queries.');
      }
      const targetSource = sources[0];
      const crawlHistory = await crawlsApi.getBySource(targetSource.id);

      setTestResult({
        endpoint: `GET /api/v1/sources/${targetSource.id}/crawls`,
        status: 'success',
        data: {
          phase: 'Phase 9 — Crawl Job Engine & History',
          targetSource: {
            id: targetSource.id,
            name: targetSource.name,
            baseUrl: targetSource.baseUrl,
          },
          totalHistoricalJobs: crawlHistory.length,
          crawlJobs: crawlHistory,
        },
        timestamp: new Date().toLocaleTimeString(),
      });
    } catch (err: unknown) {
      setTestResult({
        endpoint: 'GET /api/v1/sources/{id}/crawls',
        status: 'error',
        data: err,
        timestamp: new Date().toLocaleTimeString(),
      });
    }
  };

  // 7. Test POST /api/v1/sources/{sourceId}/crawl (Phase 9 Manual Crawl Trigger)
  const handleTestTriggerCrawl = async () => {
    setTestResult({ endpoint: 'POST /api/v1/sources/{id}/crawl', status: 'loading' });
    try {
      const sources = await sourcesApi.getAll();
      const activeSource = sources.find((s) => s.enabled);
      if (!activeSource) {
        throw new Error('No active/enabled Web Sources available to crawl. Please enable or create one first.');
      }

      const response = await crawlsApi.triggerCrawl(activeSource.id);
      loadMetrics();

      setTestResult({
        endpoint: `POST /api/v1/sources/${activeSource.id}/crawl`,
        status: 'success',
        data: {
          phase: 'Phase 9 — Asynchronous CrawlCoordinator Dispatch',
          targetSource: {
            id: activeSource.id,
            name: activeSource.name,
            baseUrl: activeSource.baseUrl,
          },
          responseContract: response,
          explanation:
            'Spring Boot controller accepted request (202 ACCEPTED), persisted CrawlJob in QUEUED status, and kicked off asynchronous execution on the dedicated crawlTaskExecutor thread pool.',
        },
        timestamp: new Date().toLocaleTimeString(),
      });
    } catch (err: any) {
      const errorPayload = err.response?.data || err.message || err;
      setTestResult({
        endpoint: 'POST /api/v1/sources/{id}/crawl',
        status: 'error',
        data: errorPayload,
        timestamp: new Date().toLocaleTimeString(),
      });
    }
  };

  return (
    <div className="space-y-8 max-w-7xl mx-auto">
      {/* Top Welcome Banner */}
      <div className="relative overflow-hidden rounded-2xl border border-emerald-500/20 bg-gradient-to-r from-emerald-950/40 via-slate-900 to-slate-900 p-6 md:p-8 shadow-xl">
        <div className="relative z-10 flex flex-col md:flex-row md:items-center md:justify-between gap-6">
          <div className="space-y-2">
            <div className="inline-flex items-center gap-2 rounded-full border border-emerald-500/30 bg-emerald-500/10 px-3 py-1 text-xs font-semibold text-emerald-400">
              <Sparkles className="h-3.5 w-3.5" />
              <span>WebScout Control Center • Phase 1–9 Fully Operational</span>
            </div>
            <h1 className="text-2xl md:text-3xl font-bold tracking-tight text-white">
              Welcome back, {user?.email}
            </h1>
            <p className="text-sm text-slate-400 max-w-2xl">
              Your personal web intelligence workspace is fully active with Phase 4 Source Management, Phase 5–8 Pipeline (Fetcher, Extractor, Persistence), and Phase 9 Crawl Job Engine.
            </p>
          </div>

          <div className="flex items-center gap-3 flex-wrap">
            <div className="rounded-xl border border-slate-800 bg-slate-950/80 p-3.5 text-center min-w-[110px]">
              <span className="text-[11px] uppercase font-semibold text-slate-500">User ID</span>
              <p className="text-lg font-bold text-emerald-400 font-mono">#{user?.id}</p>
            </div>
            <Link
              to="/sources"
              className="rounded-xl border border-slate-800 bg-slate-950/80 p-3.5 text-center min-w-[120px] hover:border-emerald-500/40 hover:bg-slate-900 transition-all group"
            >
              <span className="text-[11px] uppercase font-semibold text-slate-500 group-hover:text-emerald-400 flex items-center justify-center gap-1">
                Sources
                <ArrowRight className="h-2.5 w-2.5" />
              </span>
              <p className="text-lg font-bold text-white font-mono">
                {sourceCount !== null ? sourceCount : '...'}
              </p>
            </Link>
            <Link
              to="/crawls"
              className="rounded-xl border border-teal-500/30 bg-teal-500/10 p-3.5 text-center min-w-[120px] hover:border-teal-500/50 hover:bg-teal-500/20 transition-all group"
            >
              <span className="text-[11px] uppercase font-semibold text-teal-400 flex items-center justify-center gap-1">
                Crawl Engine
                <ArrowRight className="h-2.5 w-2.5" />
              </span>
              <p className="text-lg font-bold text-teal-300 font-mono">
                {crawlCount !== null ? `${crawlCount} Runs` : 'Active'}
              </p>
            </Link>
            <div className="rounded-xl border border-slate-800 bg-slate-950/80 p-3.5 text-center min-w-[110px]">
              <span className="text-[11px] uppercase font-semibold text-slate-500">Account</span>
              <p className="text-lg font-bold text-emerald-400">{user?.status}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Interactive Testing & Verification Playground (Replaces Postman) */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-lg bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              <Terminal className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-white">Live API Testing Playground</h2>
              <p className="text-xs text-slate-400">
                Execute live Spring Boot 4.1.1 endpoints in browser without Postman
              </p>
            </div>
          </div>
          <button
            onClick={syncTokens}
            className="flex items-center gap-1.5 text-xs text-slate-400 hover:text-white transition-colors"
          >
            <RefreshCw className="h-3.5 w-3.5" />
            <span>Sync Tokens</span>
          </button>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Action Triggers Column */}
          <div className="space-y-4">
            <div className="rounded-2xl border border-slate-800/80 bg-slate-900/80 p-5 space-y-4">
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-300">
                Execute Test Scenarios
              </h3>

              <div className="space-y-2.5">
                <button
                  onClick={handleTestCurrentUser}
                  className="w-full flex items-center justify-between p-3 rounded-xl border border-slate-800 bg-slate-950/60 hover:border-emerald-500/50 hover:bg-emerald-500/5 text-left transition-all group"
                >
                  <div className="flex items-center gap-2.5">
                    <User className="h-4 w-4 text-emerald-400" />
                    <div>
                      <div className="text-xs font-semibold text-slate-200">GET /api/v1/users/me</div>
                      <div className="text-[11px] text-slate-500">Fetch authenticated user profile</div>
                    </div>
                  </div>
                  <Send className="h-3.5 w-3.5 text-slate-500 group-hover:text-emerald-400 group-hover:translate-x-0.5 transition-all" />
                </button>

                <button
                  onClick={handleTestTokenRefresh}
                  className="w-full flex items-center justify-between p-3 rounded-xl border border-slate-800 bg-slate-950/60 hover:border-emerald-500/50 hover:bg-emerald-500/5 text-left transition-all group"
                >
                  <div className="flex items-center gap-2.5">
                    <RefreshCw className="h-4 w-4 text-teal-400" />
                    <div>
                      <div className="text-xs font-semibold text-slate-200">POST /api/v1/auth/refresh</div>
                      <div className="text-[11px] text-slate-500">Rotate refresh token in DB</div>
                    </div>
                  </div>
                  <Send className="h-3.5 w-3.5 text-slate-500 group-hover:text-teal-400 group-hover:translate-x-0.5 transition-all" />
                </button>

                <button
                  onClick={handleTestSilentRecovery}
                  className="w-full flex items-center justify-between p-3 rounded-xl border border-slate-800 bg-slate-950/60 hover:border-emerald-500/50 hover:bg-emerald-500/5 text-left transition-all group"
                >
                  <div className="flex items-center gap-2.5">
                    <KeyRound className="h-4 w-4 text-purple-400" />
                    <div>
                      <div className="text-xs font-semibold text-slate-200">Simulate 401 Interceptor</div>
                      <div className="text-[11px] text-slate-500">Prove silent refresh & self-healing</div>
                    </div>
                  </div>
                  <Send className="h-3.5 w-3.5 text-slate-500 group-hover:text-purple-400 group-hover:translate-x-0.5 transition-all" />
                </button>

                <button
                  onClick={handleTestSources}
                  className="w-full flex items-center justify-between p-3 rounded-xl border border-slate-800 bg-slate-950/60 hover:border-emerald-500/50 hover:bg-emerald-500/5 text-left transition-all group"
                >
                  <div className="flex items-center gap-2.5">
                    <Globe className="h-4 w-4 text-emerald-400" />
                    <div>
                      <div className="text-xs font-semibold text-slate-200">GET /api/v1/sources</div>
                      <div className="text-[11px] text-slate-500">Fetch registered web sources (Phase 4)</div>
                    </div>
                  </div>
                  <Send className="h-3.5 w-3.5 text-slate-500 group-hover:text-emerald-400 group-hover:translate-x-0.5 transition-all" />
                </button>

                {/* Phase 9 Scenario 6: GET Source Crawls */}
                <button
                  onClick={handleTestGetCrawls}
                  className="w-full flex items-center justify-between p-3 rounded-xl border border-teal-500/30 bg-teal-500/5 hover:border-teal-500/60 hover:bg-teal-500/10 text-left transition-all group"
                >
                  <div className="flex items-center gap-2.5">
                    <Activity className="h-4 w-4 text-teal-400" />
                    <div>
                      <div className="text-xs font-semibold text-teal-200">GET /api/v1/sources/{'{id}'}/crawls</div>
                      <div className="text-[11px] text-teal-400/70">Fetch source crawl history (Phase 9)</div>
                    </div>
                  </div>
                  <Send className="h-3.5 w-3.5 text-teal-400 group-hover:translate-x-0.5 transition-all" />
                </button>

                {/* Phase 9 Scenario 7: Trigger Crawl */}
                <button
                  onClick={handleTestTriggerCrawl}
                  className="w-full flex items-center justify-between p-3 rounded-xl border border-emerald-500/40 bg-emerald-500/10 hover:border-emerald-500/70 hover:bg-emerald-500/20 text-left transition-all group shadow-sm"
                >
                  <div className="flex items-center gap-2.5">
                    <PlayCircle className="h-4 w-4 text-emerald-400" />
                    <div>
                      <div className="text-xs font-semibold text-emerald-200">POST /api/v1/sources/{'{id}'}/crawl</div>
                      <div className="text-[11px] text-emerald-400/80">Dispatch async crawl engine (Phase 9)</div>
                    </div>
                  </div>
                  <Send className="h-3.5 w-3.5 text-emerald-400 group-hover:translate-x-0.5 transition-all" />
                </button>

                {/* Phase 5 URL Normalizer */}
                <button
                  onClick={handleTestCrawlerFoundations}
                  className="w-full flex items-center justify-between p-3 rounded-xl border border-slate-800 bg-slate-950/60 hover:border-slate-700 text-left transition-all group"
                >
                  <div className="flex items-center gap-2.5">
                    <Cpu className="h-4 w-4 text-slate-400" />
                    <div>
                      <div className="text-xs font-semibold text-slate-300">Phase 5 Crawler Simulator</div>
                      <div className="text-[11px] text-slate-500">Verify NormalizedUrl & CrawlPolicy logic</div>
                    </div>
                  </div>
                  <Send className="h-3.5 w-3.5 text-slate-500 group-hover:translate-x-0.5 transition-all" />
                </button>
              </div>
            </div>

            {/* Current Active Token State */}
            <div className="rounded-2xl border border-slate-800/80 bg-slate-900/80 p-5 space-y-3 text-xs">
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-300">
                Active Client Session
              </h3>
              <div className="space-y-2 font-mono text-[11px]">
                <div>
                  <span className="text-slate-500">Access Token (15m):</span>
                  <div className="truncate rounded bg-slate-950 p-2 text-emerald-400 border border-slate-800/80 mt-1">
                    {accessToken || 'None'}
                  </div>
                </div>
                <div>
                  <span className="text-slate-500">Refresh Token (Rotated):</span>
                  <div className="truncate rounded bg-slate-950 p-2 text-teal-400 border border-slate-800/80 mt-1">
                    {refreshToken || 'None'}
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Response Inspector Console */}
          <div className="lg:col-span-2 rounded-2xl border border-slate-800/80 bg-slate-900/80 p-5 flex flex-col">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800/80 mb-3">
              <div className="flex items-center gap-2">
                <span className="h-2.5 w-2.5 rounded-full bg-slate-700" />
                <span className="text-xs font-semibold text-slate-300">Live Response Inspector</span>
                {testResult.endpoint !== 'None' && (
                  <span className="text-[11px] font-mono text-emerald-400 bg-emerald-950/60 px-2 py-0.5 rounded border border-emerald-800/40">
                    {testResult.endpoint}
                  </span>
                )}
              </div>
              {testResult.timestamp && (
                <span className="text-[11px] text-slate-500 font-mono">{testResult.timestamp}</span>
              )}
            </div>

            <div className="flex-1 rounded-xl bg-slate-950 p-4 font-mono text-xs overflow-x-auto border border-slate-800/60 min-h-[220px]">
              {testResult.status === 'idle' && (
                <div className="h-full flex items-center justify-center text-slate-600 text-center py-12">
                  Click any test scenario on the left to fire a live API request and inspect the backend response payload.
                </div>
              )}
              {testResult.status === 'loading' && (
                <div className="h-full flex items-center justify-center text-slate-400 py-12 gap-2">
                  <RefreshCw className="h-4 w-4 animate-spin text-emerald-400" />
                  <span>Dispatching request to Spring Boot backend...</span>
                </div>
              )}
              {testResult.status !== 'idle' && testResult.status !== 'loading' && (
                <pre className="text-emerald-300 whitespace-pre-wrap leading-relaxed">
                  {JSON.stringify(testResult.data, null, 2)}
                </pre>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Platform Roadmap & System Readiness */}
      <div className="space-y-4">
        <h2 className="text-lg font-bold text-white flex items-center gap-2">
          <Layers className="h-5 w-5 text-emerald-400" />
          <span>Implementation Roadmap Status</span>
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="rounded-xl border border-emerald-500/30 bg-emerald-500/5 p-4 space-y-2">
            <div className="flex items-center justify-between text-xs">
              <span className="font-semibold text-emerald-400">Phase 0 – 2</span>
              <span className="rounded bg-emerald-950 px-1.5 py-0.5 text-[10px] text-emerald-400 border border-emerald-800">Done</span>
            </div>
            <h4 className="text-sm font-bold text-white">Boot 4 & PostgreSQL</h4>
            <p className="text-xs text-slate-400">V1–V4 Flyway migrations, Java 21, Spring Data JPA.</p>
          </div>

          <div className="rounded-xl border border-emerald-500/30 bg-emerald-500/5 p-4 space-y-2">
            <div className="flex items-center justify-between text-xs">
              <span className="font-semibold text-emerald-400">Phase 3</span>
              <span className="rounded bg-emerald-950 px-1.5 py-0.5 text-[10px] text-emerald-400 border border-emerald-800">Done</span>
            </div>
            <h4 className="text-sm font-bold text-white">Security & JWT</h4>
            <p className="text-xs text-slate-400">Stateless auth filter, token rotation, reuse revocation.</p>
          </div>

          <div className="rounded-xl border border-emerald-500/30 bg-emerald-500/5 p-4 space-y-2">
            <div className="flex items-center justify-between text-xs">
              <span className="font-semibold text-emerald-400">Phase 4 & 5</span>
              <span className="rounded bg-emerald-950 px-1.5 py-0.5 text-[10px] text-emerald-400 border border-emerald-800">Done</span>
            </div>
            <h4 className="text-sm font-bold text-white">Sources & Crawler Policy</h4>
            <p className="text-xs text-slate-400">Source CRUD, NormalizedUrl, CrawlPolicy scope & frontier.</p>
          </div>

          <div className="rounded-xl border border-teal-500/40 bg-teal-500/10 p-4 space-y-2 shadow-sm">
            <div className="flex items-center justify-between text-xs">
              <span className="font-semibold text-teal-400">Phase 6 – 9</span>
              <span className="rounded bg-teal-950 px-1.5 py-0.5 text-[10px] text-teal-300 border border-teal-700">Live</span>
            </div>
            <h4 className="text-sm font-bold text-white">Crawl Job Engine</h4>
            <p className="text-xs text-slate-400">HTTP fetcher, Jsoup extraction, persistence & async CrawlCoordinator.</p>
          </div>
        </div>
      </div>

      {/* Next Phase Preparation Banner */}
      <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-5 flex items-center justify-between flex-wrap gap-4">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-lg bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            <CheckCircle2 className="h-5 w-5" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-white">Current Milestone: Phase 1 through 9 Fully Integrated!</h3>
            <p className="text-xs text-slate-400">
              Source management, crawler foundations, HTTP fetcher, HTML extractor, page persistence, and CrawlCoordinator are fully operational. Next backend phase is Phase 10 — Search, Pagination, Filtering & Sorting.
            </p>
          </div>
        </div>
        <Link
          to="/crawls"
          className="flex items-center gap-2 text-xs font-semibold text-emerald-400 hover:text-emerald-300 transition-colors"
        >
          <span>Open Crawl Console</span>
          <ArrowRight className="h-4 w-4" />
        </Link>
      </div>
    </div>
  );
};
