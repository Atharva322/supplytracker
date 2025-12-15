import React from 'react';

export default function Homepage({ onGetStarted }) {
  return (
    <div className="relative">
      {/* Navigation Bar */}
      <nav className="fixed top-0 z-50 w-full border-b border-emerald-200 bg-white/80 backdrop-blur-md shadow-lg">
        <div className="mx-auto max-w-7xl px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-br from-emerald-500 to-green-600 shadow-lg transform hover:scale-110 transition-transform">
                <span className="text-2xl">🌾</span>
              </div>
              <div>
                <h1 className="text-xl font-bold text-emerald-900">AgriTrack</h1>
                <p className="text-xs text-emerald-600">Farm to Table Transparency</p>
              </div>
            </div>
            <div className="flex items-center gap-4">
              <button
                onClick={onGetStarted}
                className="rounded-lg border-2 border-emerald-600 bg-white px-6 py-2 text-sm font-semibold text-emerald-700 hover:bg-emerald-50 transition-all transform hover:scale-105"
              >
                Sign In
              </button>
            </div>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <div className="relative overflow-hidden pt-20">
        <div className="absolute inset-0 bg-gradient-to-br from-emerald-100/50 via-green-100/30 to-lime-100/50" />
        <div className="relative mx-auto max-w-7xl px-6 py-24">
          <div className="grid gap-12 lg:grid-cols-2 lg:gap-16 items-center">
            <div className="space-y-8">
              <div className="inline-block rounded-full bg-emerald-500/10 px-4 py-2 text-sm font-semibold text-emerald-700 border border-emerald-300">
                🌱 Next-Gen Supply Chain
              </div>
              <h1 className="text-5xl font-bold leading-tight text-emerald-900 lg:text-6xl">
                Track Your Agricultural
                <span className="bg-gradient-to-r from-emerald-600 to-green-600 bg-clip-text text-transparent"> Supply Chain</span>
              </h1>
              <p className="text-lg text-slate-600 leading-relaxed">
                From farm to table, ensure complete transparency and traceability of your agricultural products. 
                Monitor every stage of your supply chain with role-based access control.
              </p>
              <div className="flex flex-wrap gap-4">
                <button
                  onClick={onGetStarted}
                  className="rounded-xl bg-gradient-to-r from-emerald-600 to-green-600 px-8 py-4 text-base font-bold text-white shadow-xl hover:shadow-2xl transition-all transform hover:scale-105"
                >
                  Get Started →
                </button>
                <button
                  onClick={() => document.getElementById('features').scrollIntoView({ behavior: 'smooth' })}
                  className="rounded-xl border-2 border-emerald-600 bg-white px-8 py-4 text-base font-semibold text-emerald-700 hover:bg-emerald-50 transition-all"
                >
                  Learn More
                </button>
              </div>
              <div className="grid grid-cols-3 gap-4 pt-8">
                <div className="rounded-xl bg-white p-4 shadow-lg border border-emerald-100 transform hover:scale-105 transition-transform">
                  <div className="text-3xl font-bold text-emerald-600">100%</div>
                  <div className="text-xs text-slate-600">Transparency</div>
                </div>
                <div className="rounded-xl bg-white p-4 shadow-lg border border-green-100 transform hover:scale-105 transition-transform">
                  <div className="text-3xl font-bold text-green-600">Real-time</div>
                  <div className="text-xs text-slate-600">Tracking</div>
                </div>
                <div className="rounded-xl bg-white p-4 shadow-lg border border-lime-100 transform hover:scale-105 transition-transform">
                  <div className="text-3xl font-bold text-lime-600">6 Roles</div>
                  <div className="text-xs text-slate-600">Stage Control</div>
                </div>
              </div>
            </div>
            <div className="relative">
              <div className="absolute inset-0 bg-gradient-to-br from-emerald-400/20 to-green-400/20 blur-3xl" />
              <div className="relative rounded-3xl bg-white p-8 shadow-2xl border border-emerald-200 transform hover:rotate-1 transition-transform">
                <div className="space-y-6">
                  <div className="flex items-center gap-4 rounded-xl bg-gradient-to-r from-emerald-500 to-green-500 p-4 text-white shadow-lg">
                    <span className="text-3xl">🌾</span>
                    <div>
                      <div className="text-sm font-semibold">Farm Stage</div>
                      <div className="text-xs opacity-90">Origin tracking</div>
                    </div>
                  </div>
                  <div className="flex items-center gap-4 rounded-xl bg-gradient-to-r from-amber-500 to-orange-500 p-4 text-white shadow-lg transform translate-x-4">
                    <span className="text-3xl">⚙️</span>
                    <div>
                      <div className="text-sm font-semibold">Processing</div>
                      <div className="text-xs opacity-90">Quality control</div>
                    </div>
                  </div>
                  <div className="flex items-center gap-4 rounded-xl bg-gradient-to-r from-blue-500 to-cyan-500 p-4 text-white shadow-lg transform translate-x-8">
                    <span className="text-3xl">📦</span>
                    <div>
                      <div className="text-sm font-semibold">Warehouse</div>
                      <div className="text-xs opacity-90">Storage management</div>
                    </div>
                  </div>
                  <div className="flex items-center gap-4 rounded-xl bg-gradient-to-r from-purple-500 to-pink-500 p-4 text-white shadow-lg transform translate-x-12">
                    <span className="text-3xl">🚚</span>
                    <div>
                      <div className="text-sm font-semibold">Distribution</div>
                      <div className="text-xs opacity-90">Logistics tracking</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Features Section */}
      <div id="features" className="relative py-24 bg-white">
        <div className="mx-auto max-w-7xl px-6">
          <div className="text-center mb-16">
            <h2 className="text-4xl font-bold text-emerald-900 mb-4">Complete Supply Chain Visibility</h2>
            <p className="text-lg text-slate-600">Track every stage from farm to consumer with role-based access</p>
          </div>
          <div className="grid gap-8 md:grid-cols-2 lg:grid-cols-3">
            <div className="group rounded-2xl bg-gradient-to-br from-emerald-50 to-green-50 p-8 shadow-lg border border-emerald-200 hover:shadow-2xl transition-all transform hover:-translate-y-2">
              <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-emerald-500 to-green-600 text-3xl shadow-lg group-hover:scale-110 transition-transform">
                🌾
              </div>
              <h3 className="mb-3 text-xl font-bold text-emerald-900">Farm Management</h3>
              <p className="text-slate-600 leading-relaxed">
                Register farms, track harvest dates, and manage product origins. Farmers can add initial tracking stages for their products.
              </p>
            </div>
            <div className="group rounded-2xl bg-gradient-to-br from-blue-50 to-cyan-50 p-8 shadow-lg border border-blue-200 hover:shadow-2xl transition-all transform hover:-translate-y-2">
              <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-blue-500 to-cyan-600 text-3xl shadow-lg group-hover:scale-110 transition-transform">
                📊
              </div>
              <h3 className="mb-3 text-xl font-bold text-blue-900">Real-time Dashboard</h3>
              <p className="text-slate-600 leading-relaxed">
                Monitor your inventory with live statistics, product type distribution charts, and recent activity tracking.
              </p>
            </div>
            <div className="group rounded-2xl bg-gradient-to-br from-purple-50 to-pink-50 p-8 shadow-lg border border-purple-200 hover:shadow-2xl transition-all transform hover:-translate-y-2">
              <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-purple-500 to-pink-600 text-3xl shadow-lg group-hover:scale-110 transition-transform">
                🔐
              </div>
              <h3 className="mb-3 text-xl font-bold text-purple-900">Role-Based Access</h3>
              <p className="text-slate-600 leading-relaxed">
                Six specialized roles (Admin, Farmer, Processor, Warehouse Manager, Distributor, Retailer) with stage-specific permissions.
              </p>
            </div>
            <div className="group rounded-2xl bg-gradient-to-br from-amber-50 to-orange-50 p-8 shadow-lg border border-amber-200 hover:shadow-2xl transition-all transform hover:-translate-y-2">
              <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-amber-500 to-orange-600 text-3xl shadow-lg group-hover:scale-110 transition-transform">
                📍
              </div>
              <h3 className="mb-3 text-xl font-bold text-amber-900">Supply Chain Tracking</h3>
              <p className="text-slate-600 leading-relaxed">
                Complete journey visualization with timeline view, location tracking, and handler information at each stage.
              </p>
            </div>
            <div className="group rounded-2xl bg-gradient-to-br from-green-50 to-lime-50 p-8 shadow-lg border border-green-200 hover:shadow-2xl transition-all transform hover:-translate-y-2">
              <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-green-500 to-lime-600 text-3xl shadow-lg group-hover:scale-110 transition-transform">
                ⚡
              </div>
              <h3 className="mb-3 text-xl font-bold text-green-900">Instant Updates</h3>
              <p className="text-slate-600 leading-relaxed">
                Real-time synchronization across all stages. Changes reflect immediately for all authorized users.
              </p>
            </div>
            <div className="group rounded-2xl bg-gradient-to-br from-rose-50 to-red-50 p-8 shadow-lg border border-rose-200 hover:shadow-2xl transition-all transform hover:-translate-y-2">
              <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-rose-500 to-red-600 text-3xl shadow-lg group-hover:scale-110 transition-transform">
                📈
              </div>
              <h3 className="mb-3 text-xl font-bold text-rose-900">Analytics & Reports</h3>
              <p className="text-slate-600 leading-relaxed">
                Export data as CSV, filter by date ranges, search products, and generate comprehensive reports.
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* CTA Section */}
      <div className="relative overflow-hidden bg-gradient-to-r from-emerald-600 to-green-600 py-20">
        <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNjAiIHZpZXdCb3g9IjAgMCA2MCA2MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZyBmaWxsPSJub25lIiBmaWxsLXJ1bGU9ImV2ZW5vZGQiPjxwYXRoIGQ9Ik0zNiAxOGMzLjMxNCAwIDYgMi42ODYgNiA2cy0yLjY4NiA2LTYgNi02LTIuNjg2LTYtNiAyLjY4Ni02IDYtNnptMCAzNmMzLjMxNCAwIDYgMi42ODYgNiA2cy0yLjY4NiA2LTYgNi02LTIuNjg2LTYtNiAyLjY4Ni02IDYtNnpNNiAzNmMzLjMxNCAwIDYgMi42ODYgNiA2cy0yLjY4NiA2LTYgNi02LTIuNjg2LTYtNiAyLjY4Ni02IDYtNnoiIGZpbGw9IiNmZmYiIG9wYWNpdHk9Ii4wNSIvPjwvZz48L3N2Zz4=')] opacity-20" />
        <div className="relative mx-auto max-w-4xl px-6 text-center">
          <h2 className="text-4xl font-bold text-white mb-6">
            Ready to Transform Your Supply Chain?
          </h2>
          <p className="text-xl text-emerald-100 mb-8">
            Join modern farms and distributors tracking their products with transparency and efficiency.
          </p>
          <button
            onClick={onGetStarted}
            className="rounded-xl bg-white px-10 py-4 text-lg font-bold text-emerald-700 shadow-2xl hover:shadow-3xl transition-all transform hover:scale-105"
          >
            Start Tracking Now →
          </button>
        </div>
      </div>

      {/* Footer */}
      <footer className="bg-slate-900 py-12">
        <div className="mx-auto max-w-7xl px-6">
          <div className="grid gap-8 md:grid-cols-3">
            <div>
              <div className="flex items-center gap-3 mb-4">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-gradient-to-br from-emerald-500 to-green-600">
                  <span className="text-xl">🌾</span>
                </div>
                <h3 className="text-lg font-bold text-white">AgriTrack</h3>
              </div>
              <p className="text-sm text-slate-400">
                Complete supply chain transparency for agricultural products.
              </p>
            </div>
            <div>
              <h4 className="text-sm font-semibold text-white mb-3">Features</h4>
              <ul className="space-y-2 text-sm text-slate-400">
                <li>Farm Management</li>
                <li>Supply Chain Tracking</li>
                <li>Role-Based Access</li>
                <li>Real-time Dashboard</li>
              </ul>
            </div>
            <div>
              <h4 className="text-sm font-semibold text-white mb-3">Contact</h4>
              <p className="text-sm text-slate-400">
                Built for modern agriculture<br />
                © 2025 AgriTrack
              </p>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}
