import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { authApi } from '../../api/auth';
import { sourcesApi } from '../../api/sources';
import { TOKEN_STORAGE, apiClient } from '../../api/client';
import {
  User,
  Globe,
  KeyRound,
  RefreshCw,
  Send,
  Database,
  Layers,
  ArrowRight,
  Sparkles,
  Terminal,
} from 'lucide-react';

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

  const syncTokens = () => {
    setAccessToken(TOKEN_STORAGE.getAccessToken());
    setRefreshToken(TOKEN_STORAGE.getRefreshToken());
  };

  const loadSourceCount = async () => {
    try {
      const data = await sourcesApi.getAll();
      setSourceCount(data.length);
    } catch {
      // Ignored if unauthenticated or network failure
    }
  };

  useEffect(() => {
    syncTokens();
    loadSourceCount();
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
      // Temporarily set an invalid access token in memory header to trigger 401
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

  return (
    <div className="space-y-8 max-w-7xl mx-auto">
      {/* Top Welcome Banner */}
      <div className="relative overflow-hidden rounded-2xl border border-emerald-500/20 bg-gradient-to-r from-emerald-950/40 via-slate-900 to-slate-900 p-6 md:p-8 shadow-xl">
        <div className="relative z-10 flex flex-col md:flex-row md:items-center md:justify-between gap-6">
          <div className="space-y-2">
            <div className="inline-flex items-center gap-2 rounded-full border border-emerald-500/30 bg-emerald-500/10 px-3 py-1 text-xs font-semibold text-emerald-400">
              <Sparkles className="h-3.5 w-3.5" />
              <span>WebScout Control Center • Phase 4 Live</span>
            </div>
            <h1 className="text-2xl md:text-3xl font-bold tracking-tight text-white">
              Welcome back, {user?.email}
            </h1>
            <p className="text-sm text-slate-400 max-w-2xl">
              Your personal web intelligence workspace is authenticated with Phase 4 Source Management active.
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
                Verify backend contracts & token rotation live in browser instead of Postman
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
            <p className="text-xs text-slate-400">V1–V3 Flyway migrations, Java 21, Spring Data JPA.</p>
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
              <span className="font-semibold text-emerald-400">Phase 4</span>
              <span className="rounded bg-emerald-950 px-1.5 py-0.5 text-[10px] text-emerald-400 border border-emerald-800">Live</span>
            </div>
            <h4 className="text-sm font-bold text-white">Source Management</h4>
            <p className="text-xs text-slate-400">Source CRUD, unique names, delays, timeouts, limits.</p>
          </div>

          <div className="rounded-xl border border-slate-700/60 bg-slate-900/60 p-4 space-y-2 hover:border-slate-600 transition-colors">
            <div className="flex items-center justify-between text-xs">
              <span className="font-semibold text-slate-300">Phase 5</span>
              <span className="rounded bg-slate-800 px-1.5 py-0.5 text-[10px] text-slate-300 border border-slate-700">Next Up</span>
            </div>
            <h4 className="text-sm font-bold text-white">Crawler Engine</h4>
            <p className="text-xs text-slate-400">URL normalization, robots.txt, politeness & fetcher.</p>
          </div>
        </div>
      </div>

      {/* Next Phase Preparation Banner */}
      <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-5 flex items-center justify-between flex-wrap gap-4">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-lg bg-slate-800 text-slate-300">
            <Database className="h-5 w-5" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-white">Next Step: Phase 4 — Source Management</h3>
            <p className="text-xs text-slate-400">
              When ready, we can implement the Source entities and CRUD APIs, then plug in the Source UI directly into this shell.
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2 text-xs font-semibold text-emerald-400">
          <span>Frontend Shell Ready</span>
          <ArrowRight className="h-4 w-4" />
        </div>
      </div>
    </div>
  );
};
