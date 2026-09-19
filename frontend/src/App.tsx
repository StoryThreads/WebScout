import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/guards/ProtectedRoute';
import { PublicRoute } from './components/guards/PublicRoute';
import { AppLayout } from './layouts/AppLayout';
import { LoginPage } from './features/auth/LoginPage';
import { RegisterPage } from './features/auth/RegisterPage';
import { DashboardPage } from './features/dashboard/DashboardPage';
import { SourcesPage } from './features/sources/SourcesPage';
import { SourceDetailPage } from './features/sources/SourceDetailPage';
import { CrawlsPage } from './features/crawls/CrawlsPage';

export const App: React.FC = () => {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          {/* Public Auth Routes */}
          <Route
            path="/login"
            element={
              <PublicRoute>
                <LoginPage />
              </PublicRoute>
            }
          />
          <Route
            path="/register"
            element={
              <PublicRoute>
                <RegisterPage />
              </PublicRoute>
            }
          />

          {/* Authenticated Application Shell */}
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <AppLayout />
              </ProtectedRoute>
            }
          >
            <Route index element={<DashboardPage />} />
            <Route path="sources" element={<SourcesPage />} />
            <Route path="sources/:sourceId" element={<SourceDetailPage />} />
            <Route path="crawls" element={<CrawlsPage />} />
            <Route path="pages" element={<DashboardPage />} />
            <Route path="topics" element={<DashboardPage />} />
            <Route path="search" element={<DashboardPage />} />
          </Route>

          {/* Catch-all route */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
};

export default App;
