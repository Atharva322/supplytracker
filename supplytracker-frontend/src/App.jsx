import { useEffect, useState } from "react";
import { getProducts, createProduct, updateProduct, deleteProduct, login, register } from "./api";

function App() {
  // Auth states
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [currentUser, setCurrentUser] = useState("");
  const [userRoles, setUserRoles] = useState([]);
  const [showAuthForm, setShowAuthForm] = useState(true);
  const [isLogin, setIsLogin] = useState(true);
  const [authData, setAuthData] = useState({
    username: "",
    email: "",
    password: ""
  });

  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);
  const [formData, setFormData] = useState({
    name: "",
    type: "",
    batchId: "",
    harvestDate: "",
    originFarmId: ""
  });
  
  // Filter states
  const [searchTerm, setSearchTerm] = useState("");
  const [filterType, setFilterType] = useState("");
  const [filterDateFrom, setFilterDateFrom] = useState("");
  const [filterDateTo, setFilterDateTo] = useState("");

  // Pagination states
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalItems, setTotalItems] = useState(0);
  const [pageSize] = useState(10);

  useEffect(() => {
    // Check for OAuth2 redirect with token in URL (including hash)
    const urlParams = new URLSearchParams(window.location.search);
    const hashParams = window.location.hash ? new URLSearchParams(window.location.hash.substring(1)) : null;
    
    const urlToken = urlParams.get('token') || (hashParams && hashParams.get('token'));
    const urlUsername = urlParams.get('username') || (hashParams && hashParams.get('username'));
    const urlRoles = urlParams.get('roles') || (hashParams && hashParams.get('roles'));

    if (urlToken && urlUsername) {
      // OAuth2 login successful
      localStorage.setItem("token", urlToken);
      localStorage.setItem("username", urlUsername);
      const rolesArray = urlRoles ? urlRoles.split(',') : ['ROLE_USER'];
      localStorage.setItem("roles", JSON.stringify(rolesArray));
      setIsAuthenticated(true);
      setCurrentUser(urlUsername);
      setUserRoles(rolesArray);
      setShowAuthForm(false);
      
      // Clean URL
      window.history.replaceState({}, document.title, "/");
      return;
    }

    // Check if user is already logged in
    const token = localStorage.getItem("token");
    const username = localStorage.getItem("username");
    const roles = localStorage.getItem("roles");
    if (token && username) {
      setIsAuthenticated(true);
      setCurrentUser(username);
      setUserRoles(roles ? JSON.parse(roles) : []);
      setShowAuthForm(false);
    }
  }, []);

  useEffect(() => {
    if (isAuthenticated) {
      fetchProducts();
    }
  }, [isAuthenticated]);

  const handleAuthSubmit = async (e) => {
    e.preventDefault();
    try {
      let response;
      if (isLogin) {
        response = await login(authData.username, authData.password);
      } else {
        response = await register(authData.username, authData.email, authData.password);
      }

      if (response.token) {
        localStorage.setItem("token", response.token);
        localStorage.setItem("username", response.username);
        localStorage.setItem("roles", JSON.stringify(response.roles || []));
        setIsAuthenticated(true);
        setCurrentUser(response.username);
        setUserRoles(response.roles || []);
        setShowAuthForm(false);
        setError("");
      } else {
        setError(response.message || "Authentication failed");
      }
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || "Authentication failed");
    }
  };

  const handleGoogleLogin = () => {
    // Full redirect to Google OAuth
    window.location.href = "http://localhost:8080/oauth2/authorization/google";
  };

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("username");
    localStorage.removeItem("roles");
    setIsAuthenticated(false);
    setCurrentUser("");
    setUserRoles([]);
    setShowAuthForm(true);
    setProducts([]);
  };

  const fetchProducts = async (page = currentPage) => {
    try {
      setLoading(true);
      setError("");
      const data = await getProducts(page, pageSize);
      setProducts(data.products || []);
      setCurrentPage(data.currentPage || 0);
      setTotalPages(data.totalPages || 0);
      setTotalItems(data.totalItems || 0);
    } catch (err) {
      console.error(err);
      setError("Failed to load products. Is the Spring Boot server running?");
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingProduct) {
        await updateProduct(editingProduct.id, formData);
      } else {
        await createProduct(formData);
      }
      await fetchProducts();
      resetForm();
    } catch (err) {
      console.error(err);
      setError(`Failed to ${editingProduct ? "update" : "create"} product`);
    }
  };

  const handleEdit = (product) => {
    setEditingProduct(product);
    setFormData({
      name: product.name,
      type: product.type,
      batchId: product.batchId,
      harvestDate: product.harvestDate,
      originFarmId: product.originFarmId
    });
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (!confirm("Are you sure you want to delete this product?")) return;
    try {
      await deleteProduct(id);
      await fetchProducts();
    } catch (err) {
      console.error(err);
      setError("Failed to delete product");
    }
  };

  const resetForm = () => {
    setFormData({
      name: "",
      type: "",
      batchId: "",
      harvestDate: "",
      originFarmId: ""
    });
    setEditingProduct(null);
    setShowForm(false);
  };

  const handleImportCSV = (event) => {
    const file = event.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = async (e) => {
      try {
        const text = e.target.result;
        const rows = text.split('\n').map(row => row.trim()).filter(row => row);
        
        if (rows.length < 2) {
          setError("CSV file is empty or invalid");
          return;
        }

        // Parse header
        const header = rows[0].split(',').map(h => h.replace(/"/g, '').trim().toLowerCase());
        
        // Validate required columns
        const requiredColumns = ['name', 'type', 'batchid', 'harvestdate', 'originfarmid'];
        const hasAllColumns = requiredColumns.every(col => 
          header.some(h => h === col)
        );

        if (!hasAllColumns) {
          setError("CSV must have columns: name, type, batchId, harvestDate, originFarmId");
          return;
        }

        // Parse products
        const products = [];
        for (let i = 1; i < rows.length; i++) {
          const values = rows[i].split(',').map(v => v.replace(/"/g, '').trim());
          
          if (values.length === header.length) {
            const product = {};
            header.forEach((col, index) => {
              if (col === 'name') product.name = values[index];
              if (col === 'type') product.type = values[index];
              if (col === 'batchid') product.batchId = values[index];
              if (col === 'harvestdate') product.harvestDate = values[index];
              if (col === 'originfarmid') product.originFarmId = values[index];
            });
            
            if (product.name && product.type && product.batchId) {
              products.push(product);
            }
          }
        }

        if (products.length === 0) {
          setError("No valid products found in CSV");
          return;
        }

        // Import products
        setLoading(true);
        let successCount = 0;
        let failCount = 0;

        for (const product of products) {
          try {
            await createProduct(product);
            successCount++;
          } catch (err) {
            failCount++;
            console.error(`Failed to import product: ${product.name}`, err);
          }
        }

        setError("");
        await fetchProducts();
        alert(`Import complete!\n✅ Success: ${successCount}\n❌ Failed: ${failCount}`);
      } catch (err) {
        console.error(err);
        setError("Failed to parse CSV file");
      } finally {
        setLoading(false);
        event.target.value = ''; // Reset file input
      }
    };

    reader.readAsText(file);
  };

  // Filter and search logic
  const filteredProducts = products.filter(product => {
    // Search by name
    const matchesSearch = product.name.toLowerCase().includes(searchTerm.toLowerCase());
    
    // Filter by type
    const matchesType = !filterType || product.type === filterType;
    
    // Filter by date range
    const matchesDate = (!filterDateFrom || product.harvestDate >= filterDateFrom) &&
                        (!filterDateTo || product.harvestDate <= filterDateTo);
    
    return matchesSearch && matchesType && matchesDate;
  });

  // Get unique types for filter dropdown
  const productTypes = [...new Set(products.map(p => p.type))].filter(Boolean);

  const clearFilters = () => {
    setSearchTerm("");
    setFilterType("");
    setFilterDateFrom("");
    setFilterDateTo("");
  };

  const exportToCSV = () => {
    if (filteredProducts.length === 0) {
      alert("No products to export");
      return;
    }

    // Define CSV headers
    const headers = ["ID", "Name", "Type", "Batch ID", "Harvest Date", "Origin Farm ID"];
    
    // Convert products to CSV rows
    const csvRows = [
      headers.join(","), // Header row
      ...filteredProducts.map(p => 
        [p.id, p.name, p.type, p.batchId, p.harvestDate, p.originFarmId]
          .map(field => `"${field}"`) // Quote fields to handle commas
          .join(",")
      )
    ];

    // Create CSV content
    const csvContent = csvRows.join("\n");
    
    // Create blob and download
    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const link = document.createElement("a");
    const url = URL.createObjectURL(blob);
    
    link.setAttribute("href", url);
    link.setAttribute("download", `agri-products-${new Date().toISOString().split('T')[0]}.csv`);
    link.style.visibility = "hidden";
    
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

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

          {isAuthenticated ? (
            <div className="flex items-center gap-3">
              <div className="flex flex-col items-end">
                <span className="text-sm text-slate-300">
                  Welcome, <span className="font-semibold text-emerald-400">{currentUser}</span>
                </span>
                <span className="text-[10px] text-slate-500">
                  {userRoles.includes("ROLE_ADMIN") ? "👑 Administrator" : "👤 User"}
                </span>
              </div>
              <button
                onClick={handleLogout}
                className="rounded-lg border border-slate-700 bg-slate-800 px-3 py-1.5 text-xs font-medium text-slate-200 hover:border-slate-500 hover:bg-slate-700 transition-colors"
              >
                Logout
              </button>
            </div>
          ) : (
            <span className="rounded-full border border-amber-500/40 bg-amber-500/10 px-3 py-1 text-xs font-medium text-amber-300">
              Not authenticated
            </span>
          )}
        </div>
      </header>

      {/* Main content */}
      <main className="mx-auto max-w-5xl px-4 py-8">
        {/* Login/Register Form */}
        {showAuthForm && !isAuthenticated && (
          <div className="mx-auto max-w-md">
            <div className="rounded-2xl border border-slate-800 bg-slate-900/60 shadow-xl shadow-black/40">
              <div className="border-b border-slate-800 px-6 py-4">
                <h2 className="text-lg font-semibold tracking-tight">
                  {isLogin ? "Login" : "Register"}
                </h2>
                <p className="text-xs text-slate-400">
                  {isLogin ? "Sign in to access the supply tracker" : "Create a new account"}
                </p>
              </div>

              <div className="px-6 py-4">
                {error && (
                  <div className="mb-3 rounded-lg border border-red-500/40 bg-red-500/10 px-4 py-2 text-xs text-red-200">
                    {error}
                  </div>
                )}

                <form onSubmit={handleAuthSubmit} className="space-y-3">
                  <div>
                    <label className="mb-1 block text-xs font-medium text-slate-400">
                      Username *
                    </label>
                    <input
                      type="text"
                      value={authData.username}
                      onChange={(e) => setAuthData({ ...authData, username: e.target.value })}
                      required
                      className="w-full rounded-lg border border-slate-600 bg-slate-900 px-3 py-1.5 text-sm text-slate-100 placeholder-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                      placeholder="Enter username"
                    />
                  </div>

                  {!isLogin && (
                    <div>
                      <label className="mb-1 block text-xs font-medium text-slate-400">
                        Email *
                      </label>
                      <input
                        type="email"
                        value={authData.email}
                        onChange={(e) => setAuthData({ ...authData, email: e.target.value })}
                        required
                        className="w-full rounded-lg border border-slate-600 bg-slate-900 px-3 py-1.5 text-sm text-slate-100 placeholder-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                        placeholder="Enter email"
                      />
                    </div>
                  )}

                  <div>
                    <label className="mb-1 block text-xs font-medium text-slate-400">
                      Password *
                    </label>
                    <input
                      type="password"
                      value={authData.password}
                      onChange={(e) => setAuthData({ ...authData, password: e.target.value })}
                      required
                      className="w-full rounded-lg border border-slate-600 bg-slate-900 px-3 py-1.5 text-sm text-slate-100 placeholder-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                      placeholder="Enter password"
                    />
                  </div>

                  <button
                    type="submit"
                    className="w-full rounded-lg bg-emerald-500 px-4 py-2 text-sm font-semibold text-emerald-950 shadow-sm hover:bg-emerald-400 transition-colors"
                  >
                    {isLogin ? "Login" : "Register"}
                  </button>
                </form>

                <div className="relative my-4">
                  <div className="absolute inset-0 flex items-center">
                    <div className="w-full border-t border-slate-700"></div>
                  </div>
                  <div className="relative flex justify-center text-xs">
                    <span className="bg-slate-900 px-2 text-slate-500">Or continue with</span>
                  </div>
                </div>

                <button
                  onClick={handleGoogleLogin}
                  type="button"
                  className="w-full flex items-center justify-center gap-2 rounded-lg border border-slate-700 bg-slate-800 px-4 py-2 text-sm font-medium text-slate-200 hover:border-slate-500 hover:bg-slate-700 transition-colors"
                >
                  <svg className="h-5 w-5" viewBox="0 0 24 24">
                    <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                    <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                    <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                    <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                  </svg>
                  Sign in with Google
                </button>

                <div className="mt-4 text-center text-xs text-slate-400">
                  {isLogin ? "Don't have an account? " : "Already have an account? "}
                  <button
                    onClick={() => {
                      setIsLogin(!isLogin);
                      setError("");
                    }}
                    className="font-medium text-emerald-400 hover:text-emerald-300"
                  >
                    {isLogin ? "Register" : "Login"}
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Products Card */}
        {isAuthenticated && (
          <>
        <div className="rounded-2xl border border-slate-800 bg-slate-900/60 shadow-xl shadow-black/40">
          <div className="flex items-center justify-between border-b border-slate-800 px-6 py-4">
            <div>
              <h2 className="text-lg font-semibold tracking-tight">
                Products
              </h2>
              <p className="text-xs text-slate-400">
                Showing {filteredProducts.length} of {products.length} items from the supply chain
              </p>
            </div>

            <div className="flex gap-2">
              <button
                type="button"
                onClick={exportToCSV}
                className="rounded-lg border border-slate-700 bg-slate-800 px-3 py-1.5 text-xs font-medium text-slate-200 hover:border-slate-500 hover:bg-slate-700 transition-colors"
                title="Export filtered products to CSV"
              >
                📥 Export CSV
              </button>
              {userRoles.includes("ROLE_ADMIN") && (
                <>
                  <label className="rounded-lg border border-slate-700 bg-slate-800 px-3 py-1.5 text-xs font-medium text-slate-200 hover:border-slate-500 hover:bg-slate-700 transition-colors cursor-pointer">
                    📤 Import CSV
                    <input
                      type="file"
                      accept=".csv"
                      onChange={handleImportCSV}
                      className="hidden"
                    />
                  </label>
                </>
              )}
              <button
                type="button"
                onClick={fetchProducts}
                className="rounded-lg border border-slate-700 bg-slate-800 px-3 py-1.5 text-xs font-medium text-slate-200 hover:border-slate-500 hover:bg-slate-700 transition-colors"
              >
                Refresh
              </button>
              {userRoles.includes("ROLE_ADMIN") && (
                <button
                  type="button"
                  onClick={() => setShowForm(!showForm)}
                  className="rounded-lg bg-emerald-500 px-3 py-1.5 text-xs font-semibold text-emerald-950 shadow-sm hover:bg-emerald-400 transition-colors"
                >
                  {showForm ? "Cancel" : "+ Add product"}
                </button>
              )}
            </div>
          </div>

          {/* Search & Filter Section */}
          <div className="border-b border-slate-800 bg-slate-900/40 px-6 py-4">
            <div className="grid grid-cols-1 gap-3 md:grid-cols-4">
              {/* Search */}
              <div className="md:col-span-2">
                <label className="mb-1 block text-xs font-medium text-slate-400">
                  Search by name
                </label>
                <input
                  type="text"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  placeholder="Search products..."
                  className="w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-slate-100 placeholder-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                />
              </div>

              {/* Type Filter */}
              <div>
                <label className="mb-1 block text-xs font-medium text-slate-400">
                  Filter by type
                </label>
                <select
                  value={filterType}
                  onChange={(e) => setFilterType(e.target.value)}
                  className="w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-slate-100 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                >
                  <option value="">All types</option>
                  {productTypes.map(type => (
                    <option key={type} value={type}>{type}</option>
                  ))}
                </select>
              </div>

              {/* Clear Filters */}
              <div className="flex items-end">
                <button
                  type="button"
                  onClick={clearFilters}
                  className="w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-1.5 text-xs font-medium text-slate-300 hover:border-slate-500 hover:bg-slate-700 transition-colors"
                >
                  Clear filters
                </button>
              </div>
            </div>

            {/* Date Range Filter */}
            <div className="mt-3 grid grid-cols-1 gap-3 md:grid-cols-2">
              <div>
                <label className="mb-1 block text-xs font-medium text-slate-400">
                  Harvest date from
                </label>
                <input
                  type="date"
                  value={filterDateFrom}
                  onChange={(e) => setFilterDateFrom(e.target.value)}
                  className="w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-slate-100 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                />
              </div>
              <div>
                <label className="mb-1 block text-xs font-medium text-slate-400">
                  Harvest date to
                </label>
                <input
                  type="date"
                  value={filterDateTo}
                  onChange={(e) => setFilterDateTo(e.target.value)}
                  className="w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-slate-100 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                />
              </div>
            </div>
          </div>

          {/* Body */}
          <div className="px-6 py-4">
            {/* Add/Edit Form */}
            {showForm && (
              <div className="mb-6 rounded-lg border border-slate-700 bg-slate-800/50 p-4">
                <h3 className="mb-4 text-sm font-semibold text-slate-200">
                  {editingProduct ? "Edit Product" : "Add New Product"}
                </h3>
                <form onSubmit={handleSubmit} className="space-y-3">
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="mb-1 block text-xs font-medium text-slate-400">
                        Name *
                      </label>
                      <input
                        type="text"
                        name="name"
                        value={formData.name}
                        onChange={handleInputChange}
                        required
                        className="w-full rounded-lg border border-slate-600 bg-slate-900 px-3 py-1.5 text-sm text-slate-100 placeholder-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                        placeholder="e.g., Tomatoes"
                      />
                    </div>
                    <div>
                      <label className="mb-1 block text-xs font-medium text-slate-400">
                        Type *
                      </label>
                      <input
                        type="text"
                        name="type"
                        value={formData.type}
                        onChange={handleInputChange}
                        required
                        className="w-full rounded-lg border border-slate-600 bg-slate-900 px-3 py-1.5 text-sm text-slate-100 placeholder-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                        placeholder="e.g., Vegetable"
                      />
                    </div>
                    <div>
                      <label className="mb-1 block text-xs font-medium text-slate-400">
                        Batch ID *
                      </label>
                      <input
                        type="text"
                        name="batchId"
                        value={formData.batchId}
                        onChange={handleInputChange}
                        required
                        className="w-full rounded-lg border border-slate-600 bg-slate-900 px-3 py-1.5 text-sm text-slate-100 placeholder-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                        placeholder="e.g., BATCH-001"
                      />
                    </div>
                    <div>
                      <label className="mb-1 block text-xs font-medium text-slate-400">
                        Harvest Date *
                      </label>
                      <input
                        type="date"
                        name="harvestDate"
                        value={formData.harvestDate}
                        onChange={handleInputChange}
                        required
                        className="w-full rounded-lg border border-slate-600 bg-slate-900 px-3 py-1.5 text-sm text-slate-100 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                      />
                    </div>
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-medium text-slate-400">
                      Origin Farm ID *
                    </label>
                    <input
                      type="text"
                      name="originFarmId"
                      value={formData.originFarmId}
                      onChange={handleInputChange}
                      required
                      className="w-full rounded-lg border border-slate-600 bg-slate-900 px-3 py-1.5 text-sm text-slate-100 placeholder-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                      placeholder="e.g., FARM-123"
                    />
                  </div>
                  <div className="flex gap-2 pt-2">
                    <button
                      type="submit"
                      className="rounded-lg bg-emerald-500 px-4 py-1.5 text-xs font-semibold text-emerald-950 shadow-sm hover:bg-emerald-400 transition-colors"
                    >
                      {editingProduct ? "Update Product" : "Create Product"}
                    </button>
                    <button
                      type="button"
                      onClick={resetForm}
                      className="rounded-lg border border-slate-600 bg-slate-800 px-4 py-1.5 text-xs font-medium text-slate-300 hover:border-slate-500 hover:bg-slate-700 transition-colors"
                    >
                      Cancel
                    </button>
                  </div>
                </form>
              </div>
            )}

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
                No products found. Use the form to add some records.
              </div>
            )}

            {!loading && !error && products.length > 0 && filteredProducts.length === 0 && (
              <div className="rounded-lg border border-dashed border-slate-700 bg-slate-900/70 px-4 py-6 text-center text-sm text-slate-400">
                No products match your search criteria. Try adjusting your filters.
              </div>
            )}

            {!loading && !error && filteredProducts.length > 0 && (
              <div className="-mx-4 overflow-x-auto">
                <table className="min-w-full table-fixed border-collapse text-sm">
                  <thead>
                    <tr className="border-b border-slate-800 bg-slate-900/80">
                      <th className="w-[18%] px-4 py-2 text-left text-xs font-semibold uppercase tracking-wide text-slate-400">
                        Name
                      </th>
                      <th className="w-[15%] px-4 py-2 text-left text-xs font-semibold uppercase tracking-wide text-slate-400">
                        Type
                      </th>
                      <th className="w-[15%] px-4 py-2 text-left text-xs font-semibold uppercase tracking-wide text-slate-400">
                        Batch ID
                      </th>
                      <th className="w-[15%] px-4 py-2 text-left text-xs font-semibold uppercase tracking-wide text-slate-400">
                        Harvest Date
                      </th>
                      <th className="w-[15%] px-4 py-2 text-left text-xs font-semibold uppercase tracking-wide text-slate-400">
                        Origin Farm
                      </th>
                      <th className="w-[22%] px-4 py-2 text-right text-xs font-semibold uppercase tracking-wide text-slate-400">
                        Actions
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredProducts.map((p) => (
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
                        <td className="px-4 py-2 text-right">
                          {userRoles.includes("ROLE_ADMIN") ? (
                            <div className="flex justify-end gap-2">
                              <button
                                onClick={() => handleEdit(p)}
                                className="rounded-lg border border-slate-600 bg-slate-800 px-3 py-1 text-xs font-medium text-slate-200 hover:border-slate-500 hover:bg-slate-700 transition-colors"
                              >
                                Edit
                              </button>
                              <button
                                onClick={() => handleDelete(p.id)}
                                className="rounded-lg border border-red-500/40 bg-red-500/10 px-3 py-1 text-xs font-medium text-red-300 hover:border-red-400 hover:bg-red-500/20 transition-colors"
                              >
                                Delete
                              </button>
                            </div>
                          ) : (
                            <span className="text-xs text-slate-500">View only</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {/* Pagination Controls */}
            {!loading && !error && totalPages > 1 && (
              <div className="mt-4 flex items-center justify-between border-t border-slate-800 pt-4">
                <div className="text-xs text-slate-400">
                  Showing {currentPage * pageSize + 1} to {Math.min((currentPage + 1) * pageSize, totalItems)} of {totalItems} products
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() => fetchProducts(currentPage - 1)}
                    disabled={currentPage === 0}
                    className="rounded-lg border border-slate-700 bg-slate-800 px-3 py-1.5 text-xs font-medium text-slate-200 hover:border-slate-500 hover:bg-slate-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Previous
                  </button>
                  <span className="flex items-center px-3 text-xs text-slate-300">
                    Page {currentPage + 1} of {totalPages}
                  </span>
                  <button
                    onClick={() => fetchProducts(currentPage + 1)}
                    disabled={currentPage >= totalPages - 1}
                    className="rounded-lg border border-slate-700 bg-slate-800 px-3 py-1.5 text-xs font-medium text-slate-200 hover:border-slate-500 hover:bg-slate-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Footer note */}
        <p className="mt-6 text-xs text-slate-500">
          ✅ Validation, Error Handling & Pagination added. Next: User Roles & Permissions.
        </p>
          </>
        )}
      </main>
    </div>
  );
}

export default App;
