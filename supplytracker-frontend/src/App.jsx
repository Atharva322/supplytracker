import { useEffect, useState } from "react";
import { getProducts } from "./api";

function App() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchProducts = async () => {
      try {
        setLoading(true);
        setError("");
        const data = await getProducts();
        setProducts(data);
      } catch (err) {
        console.error(err);
        setError("Failed to load products. Is the Spring Boot server running?");
      } finally {
        setLoading(false);
      }
    };

    fetchProducts();
  }, []);

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      {/* Top bar */}
      <header className="border-b border-slate-800 bg-slate-900/70 backdrop-blur">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-4">
          <div className="flex items-center gap-3">
            <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-amber-400/10 text-2xl">
              🧺
            </span>
            <div>
              <h1 className="text-xl font-semibold tracking-tight">
                Agri Supply Tracker
              </h1>
              <p className="text-xs text-slate-400">
                React + Tailwind · Spring Boot · MongoDB
              </p>
            </div>
          </div>

          <span className="rounded-full border border-emerald-500/40 bg-emerald-500/10 px-3 py-1 text-xs font-medium text-emerald-300">
            API connected
          </span>
        </div>
      </header>

      {/* Main content */}
      <main className="mx-auto max-w-5xl px-4 py-8">
        {/* Card */}
        <div className="rounded-2xl border border-slate-800 bg-slate-900/60 shadow-xl shadow-black/40">
          <div className="flex items-center justify-between border-b border-slate-800 px-6 py-4">
            <div>
              <h2 className="text-lg font-semibold tracking-tight">
                Products
              </h2>
              <p className="text-xs text-slate-400">
                Showing {products.length} items from the supply chain
              </p>
            </div>

            <div className="flex gap-2">
              <button
                type="button"
                className="rounded-lg border border-slate-700 bg-slate-800 px-3 py-1.5 text-xs font-medium text-slate-200 hover:border-slate-500 hover:bg-slate-700 transition-colors"
              >
                Refresh
              </button>
              <button
                type="button"
                className="rounded-lg bg-emerald-500 px-3 py-1.5 text-xs font-semibold text-emerald-950 shadow-sm hover:bg-emerald-400 transition-colors"
              >
                + Add product (later)
              </button>
            </div>
          </div>

          {/* Body */}
          <div className="px-6 py-4">
            {loading && (
              <div className="flex items-center gap-2 text-sm text-slate-400">
                <span className="h-2 w-2 animate-ping rounded-full bg-emerald-400" />
                Loading products…
              </div>
            )}

            {error && (
              <div className="mb-3 rounded-lg border border-red-500/40 bg-red-500/10 px-4 py-2 text-xs text-red-200">
                {error}
              </div>
            )}

            {!loading && !error && products.length === 0 && (
              <div className="rounded-lg border border-dashed border-slate-700 bg-slate-900/70 px-4 py-6 text-center text-sm text-slate-400">
                No products found. Use Postman or the future form to add some
                records.
              </div>
            )}

            {!loading && !error && products.length > 0 && (
              <div className="-mx-4 overflow-x-auto">
                <table className="min-w-full table-fixed border-collapse text-sm">
                  <thead>
                    <tr className="border-b border-slate-800 bg-slate-900/80">
                      <th className="w-1/5 px-4 py-2 text-left text-xs font-semibold uppercase tracking-wide text-slate-400">
                        Name
                      </th>
                      <th className="w-1/6 px-4 py-2 text-left text-xs font-semibold uppercase tracking-wide text-slate-400">
                        Type
                      </th>
                      <th className="w-1/6 px-4 py-2 text-left text-xs font-semibold uppercase tracking-wide text-slate-400">
                        Batch ID
                      </th>
                      <th className="w-1/6 px-4 py-2 text-left text-xs font-semibold uppercase tracking-wide text-slate-400">
                        Harvest Date
                      </th>
                      <th className="w-1/6 px-4 py-2 text-left text-xs font-semibold uppercase tracking-wide text-slate-400">
                        Origin Farm
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {products.map((p) => (
                      <tr
                        key={p.id}
                        className="border-b border-slate-800/80 hover:bg-slate-800/60 transition-colors"
                      >
                        <td className="px-4 py-2 font-medium text-slate-100">
                          {p.name}
                        </td>
                        <td className="px-4 py-2 text-slate-300">{p.type}</td>
                        <td className="px-4 py-2 text-slate-300">
                          <span className="rounded-full bg-slate-800 px-2 py-0.5 text-[11px] font-mono text-slate-300">
                            {p.batchId}
                          </span>
                        </td>
                        <td className="px-4 py-2 text-slate-300">
                          {p.harvestDate}
                        </td>
                        <td className="px-4 py-2 text-slate-300">
                          {p.originFarmId}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>

        {/* Footer note */}
        <p className="mt-6 text-xs text-slate-500">
          Next steps: we’ll add create / edit / delete, search & filters, CSV
          export, auth with Spring Security + JWT, and later your YOLOv3 sorting
          model.
        </p>
      </main>
    </div>
  );
}

export default App;
