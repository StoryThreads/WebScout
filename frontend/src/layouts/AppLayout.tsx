import React, { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  LayoutDashboard,
  Globe,
  PlayCircle,
  FileText,
  Hash,
  Search,
  LogOut,
  Radio,
  Menu,
  X,
  User,
  ShieldCheck,
} from 'lucide-react';

export const AppLayout: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  const handleLogout = async () => {
    setIsLoggingOut(true);
    await logout();
    navigate('/login');
  };

  const navItems = [
    { name: 'Dashboard', path: '/', icon: LayoutDashboard, status: 'Ready' },
    { name: 'Sources', path: '/sources', icon: Globe, status: 'Ready' },
    { name: 'Crawl Engine', path: '/crawls', icon: PlayCircle, status: 'Ready' },
    { name: 'Indexed Pages', path: '/pages', icon: FileText, status: 'Phase 8' },
    { name: 'Topics & Scoring', path: '/topics', icon: Hash, status: 'Phase 11' },
    { name: 'Search & Filters', path: '/search', icon: Search, status: 'Phase 10' },
  ];

  return (
    <div className="flex min-h-screen bg-slate-950 text-slate-100 antialiased">
      {/* Mobile Drawer Overlay */}
      {mobileMenuOpen && (
        <div
          className="fixed inset-0 z-40 bg-slate-950/80 backdrop-blur-sm lg:hidden"
          onClick={() => setMobileMenuOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside
        className={`fixed inset-y-0 left-0 z-50 flex w-72 flex-col border-r border-slate-800/80 bg-slate-900/95 backdrop-blur-xl transition-transform duration-300 ease-in-out lg:static lg:translate-x-0 ${
          mobileMenuOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {/* Brand Header */}
        <div className="flex h-16 items-center justify-between border-b border-slate-800/80 px-6">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-tr from-emerald-600 to-teal-400 text-slate-950 shadow-lg shadow-emerald-500/20">
              <Radio className="h-5 w-5 animate-pulse" />
            </div>
            <div>
              <span className="text-lg font-bold tracking-tight text-white flex items-center gap-1.5">
                WebScout
                <span className="text-[10px] uppercase font-semibold px-1.5 py-0.5 rounded bg-emerald-950/80 text-emerald-400 border border-emerald-800/50">
                  v1.0
                </span>
              </span>
              <p className="text-[11px] text-slate-400">Personal Web Intelligence</p>
            </div>
          </div>
          <button
            onClick={() => setMobileMenuOpen(false)}
            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-800 hover:text-white lg:hidden"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Navigation Items */}
        <nav className="flex-1 space-y-1.5 overflow-y-auto px-4 py-6">
          <div className="px-3 pb-2 text-[10px] font-semibold uppercase tracking-wider text-slate-500">
            Platform Modules
          </div>
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.name}
                to={item.path}
                onClick={() => setMobileMenuOpen(false)}
                className={({ isActive }) =>
                  `group flex items-center justify-between rounded-xl px-3.5 py-2.5 text-sm font-medium transition-all ${
                    isActive
                      ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 shadow-sm'
                      : 'text-slate-400 hover:bg-slate-800/60 hover:text-slate-200'
                  }`
                }
              >
                <div className="flex items-center gap-3">
                  <Icon className="h-4 w-4" />
                  <span>{item.name}</span>
                </div>
                {item.status !== 'Ready' ? (
                  <span className="text-[10px] font-medium text-slate-500 group-hover:text-slate-400 bg-slate-800/80 px-2 py-0.5 rounded-full border border-slate-700/50">
                    {item.status}
                  </span>
                ) : (
                  <span className="h-1.5 w-1.5 rounded-full bg-emerald-500"></span>
                )}
              </NavLink>
            );
          })}
        </nav>

        {/* System & Architecture Status Footer */}
        <div className="border-t border-slate-800/80 p-4">
          <div className="rounded-xl border border-slate-800 bg-slate-950/60 p-3.5 text-xs text-slate-400 space-y-2">
            <div className="flex items-center justify-between font-medium text-slate-300">
              <span className="flex items-center gap-2">
                <ShieldCheck className="h-4 w-4 text-emerald-400" />
                Crawler Pipeline
              </span>
              <span className="flex items-center gap-1.5 text-emerald-400 text-[11px]">
                <span className="h-2 w-2 rounded-full bg-emerald-400 animate-ping" />
                Phase 1–9 Live
              </span>
            </div>
            <p className="text-[11px] leading-relaxed text-slate-500">
              Crawl Job Engine, HTTP Fetcher & Page Persistence operational.
            </p>
          </div>
        </div>
      </aside>

      {/* Main Content Viewport */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {/* Top Navigation Bar */}
        <header className="flex h-16 items-center justify-between border-b border-slate-800/80 bg-slate-900/60 px-6 backdrop-blur-lg">
          <div className="flex items-center gap-4">
            <button
              onClick={() => setMobileMenuOpen(true)}
              className="rounded-lg p-2 text-slate-400 hover:bg-slate-800 hover:text-white lg:hidden"
            >
              <Menu className="h-5 w-5" />
            </button>
            <div className="hidden sm:flex items-center gap-2 text-xs text-slate-400">
              <span className="h-2 w-2 rounded-full bg-emerald-400" />
              <span>Spring Boot 4.1.1 API Connected</span>
            </div>
          </div>

          {/* User Account & Actions */}
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2.5 rounded-full border border-slate-800 bg-slate-900/90 px-3.5 py-1.5 shadow-sm">
              <div className="flex h-6 w-6 items-center justify-center rounded-full bg-slate-800 text-slate-300">
                <User className="h-3.5 w-3.5" />
              </div>
              <span className="text-xs font-medium text-slate-200">{user?.email}</span>
              <span className="rounded bg-emerald-950/80 px-1.5 py-0.5 text-[10px] font-semibold text-emerald-400 border border-emerald-800/40">
                {user?.status}
              </span>
            </div>

            <button
              onClick={handleLogout}
              disabled={isLoggingOut}
              className="flex items-center gap-1.5 rounded-lg border border-slate-700/60 bg-slate-800/80 px-3 py-1.5 text-xs font-medium text-slate-300 hover:bg-red-500/10 hover:border-red-500/30 hover:text-red-400 transition-all disabled:opacity-50"
              title="Logout from WebScout"
            >
              <LogOut className="h-3.5 w-3.5" />
              <span className="hidden sm:inline">
                {isLoggingOut ? 'Logging out...' : 'Sign Out'}
              </span>
            </button>
          </div>
        </header>

        {/* Scrollable Content */}
        <main className="flex-1 overflow-y-auto p-6 md:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
};
