import React, { lazy, Suspense } from "react";
import ReactDOM from "react-dom/client";
import { QueryClientProvider } from '@tanstack/react-query';
import { queryClient } from './utils/queryClient';
import ErrorBoundary from './components/ErrorBoundary';
import "./index.css"; 

// Lazy load the App component for code splitting
const App = lazy(() => import("./App.jsx"));

// Loading fallback component
const loadingFallback = (
  <div className="min-h-screen bg-gradient-to-br from-emerald-50 via-green-50 to-lime-50 flex items-center justify-center">
    <div className="text-center">
      <div className="animate-spin text-6xl mb-4">🌾</div>
      <h2 className="text-2xl font-bold text-emerald-800">Loading AgriTrack...</h2>
      <p className="text-emerald-600 mt-2">Please wait</p>
    </div>
  </div>
);

const root = document.getElementById("root");

if (!root) {
  throw new Error("Root element #root was not found");
}

ReactDOM.createRoot(root).render(
  <React.StrictMode>
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <Suspense fallback={loadingFallback}>
          <App />
        </Suspense>
      </QueryClientProvider>
    </ErrorBoundary>
  </React.StrictMode>
);
