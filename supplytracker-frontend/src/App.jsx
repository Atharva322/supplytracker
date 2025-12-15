import { useEffect, useState } from "react";
import { getProducts, createProduct, updateProduct, deleteProduct, login, register, addTrackingStage, getDashboardStats, getFarms, createFarm, updateFarm, deleteFarm } from "./api";
import Homepage from "./Homepage";

function App() {
  // View states
  const [currentView, setCurrentView] = useState("homepage"); // homepage, auth, main
  
  // Auth states
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [currentUser, setCurrentUser] = useState("");
  const [userRoles, setUserRoles] = useState([]);
  const [userStageProfile, setUserStageProfile] = useState("");
  const [userLocation, setUserLocation] = useState("");
  const [userFarmId, setUserFarmId] = useState("");
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
    originFarmId: "",
    destination: "",
    status: "AT_FARM"
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

  // Tracking history states
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [showTrackingModal, setShowTrackingModal] = useState(false);
  const [showAddStageForm, setShowAddStageForm] = useState(false);
  const [trackingFormData, setTrackingFormData] = useState({
    stage: "",
    location: "",
    handler: "",
    notes: ""
  });

  // Dashboard states  
  const [activeTab, setActiveTab] = useState("products"); // "dashboard", "products", or "farms"
  const [dashboardStats, setDashboardStats] = useState(null);
  const [dashboardLoading, setDashboardLoading] = useState(false);

  // Farm states
  const [farms, setFarms] = useState([]);
  const [farmsLoading, setFarmsLoading] = useState(false);
  const [showFarmForm, setShowFarmForm] = useState(false);
  const [editingFarm, setEditingFarm] = useState(null);
  const [farmFormData, setFarmFormData] = useState({
    name: "",
    location: "",
    owner: "",
    contactInfo: "",
    description: ""
  });

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
      setCurrentView("main"); // Go to main app after OAuth login
      
      // Clean URL
      window.history.replaceState({}, document.title, "/");
      return;
    }

    // Check if user is already logged in
    const token = localStorage.getItem("token");
    const username = localStorage.getItem("username");
    const roles = localStorage.getItem("roles");
    const stageProfile = localStorage.getItem("stageProfile");
    const location = localStorage.getItem("location");
    const farmId = localStorage.getItem("farmId");
    if (token && username) {
      setIsAuthenticated(true);
      setCurrentUser(username);
      setUserRoles(roles ? JSON.parse(roles) : []);
      setUserStageProfile(stageProfile || "");
      setUserLocation(location || "");
      setUserFarmId(farmId || "");
      setShowAuthForm(false);
      setCurrentView("main"); // Go to main app if authenticated
    }
  }, []);

  useEffect(() => {
    if (isAuthenticated) {
      fetchProducts();
      fetchDashboardStats();
      fetchFarms();
    }
  }, [isAuthenticated]);

  const fetchDashboardStats = async () => {
    try {
      setDashboardLoading(true);
      const data = await getDashboardStats();
      setDashboardStats(data);
    } catch (err) {
      console.error("Failed to fetch dashboard stats:", err);
    } finally {
      setDashboardLoading(false);
    }
  };

  const fetchFarms = async () => {
    try {
      setFarmsLoading(true);
      const data = await getFarms();
      setFarms(data || []);
    } catch (err) {
      console.error("Failed to fetch farms:", err);
      setError("Failed to load farms");
    } finally {
      setFarmsLoading(false);
    }
  };

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
        if (response.stageProfile) localStorage.setItem("stageProfile", response.stageProfile);
        if (response.location) localStorage.setItem("location", response.location);
        if (response.associatedFarmId) localStorage.setItem("farmId", response.associatedFarmId);
        
        setIsAuthenticated(true);
        setCurrentUser(response.username);
        setUserRoles(response.roles || []);
        setUserStageProfile(response.stageProfile || "");
        setUserLocation(response.location || "");
        setUserFarmId(response.associatedFarmId || "");
        setShowAuthForm(false);
        setCurrentView("main"); // Go to main app after login
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
    localStorage.removeItem("stageProfile");
    localStorage.removeItem("location");
    localStorage.removeItem("farmId");
    setIsAuthenticated(false);
    setCurrentUser("");
    setUserRoles([]);
    setUserStageProfile("");
    setUserLocation("");
    setUserFarmId("");
    setShowAuthForm(true);
    setProducts([]);
    setCurrentView("homepage");
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

  const handleViewTracking = (product) => {
    setSelectedProduct(product);
    setShowTrackingModal(true);
    setShowAddStageForm(false);
  };

  const handleAddStage = async (e) => {
    e.preventDefault();
    try {
      await addTrackingStage(selectedProduct.id, trackingFormData);
      // Refresh products to get updated tracking history
      await fetchProducts();
      // Update selectedProduct with new data
      const updatedProducts = await getProducts(currentPage, pageSize);
      const updatedProduct = updatedProducts.products.find(p => p.id === selectedProduct.id);
      if (updatedProduct) {
        setSelectedProduct(updatedProduct);
      }
      // Reset form
      setTrackingFormData({
        stage: "",
        location: "",
        handler: "",
        notes: ""
      });
      setShowAddStageForm(false);
    } catch (err) {
      console.error(err);
      setError("Failed to add tracking stage");
    }
  };

  const closeTrackingModal = () => {
    setShowTrackingModal(false);
    setSelectedProduct(null);
    setShowAddStageForm(false);
    setTrackingFormData({
      stage: "",
      location: "",
      handler: "",
      notes: ""
    });
  };

  const handleFarmSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingFarm) {
        await updateFarm(editingFarm.id, farmFormData);
      } else {
        await createFarm(farmFormData);
      }
      await fetchFarms();
      await fetchDashboardStats(); // Update stats
      resetFarmForm();
    } catch (err) {
      console.error(err);
      setError(`Failed to ${editingFarm ? "update" : "create"} farm`);
    }
  };

  const handleEditFarm = (farm) => {
    setEditingFarm(farm);
    setFarmFormData({
      name: farm.name,
      location: farm.location,
      owner: farm.owner,
      contactInfo: farm.contactInfo || "",
      description: farm.description || ""
    });
    setShowFarmForm(true);
  };

  const handleDeleteFarm = async (id) => {
    if (!confirm("Are you sure you want to delete this farm?")) return;
    try {
      await deleteFarm(id);
      await fetchFarms();
      await fetchDashboardStats(); // Update stats
    } catch (err) {
      console.error(err);
      setError("Failed to delete farm");
    }
  };

  const resetFarmForm = () => {
    setFarmFormData({
      name: "",
      location: "",
      owner: "",
      contactInfo: "",
      description: ""
    });
    setEditingFarm(null);
    setShowFarmForm(false);
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
    <div className="min-h-screen bg-gradient-to-br from-emerald-50 via-green-50 to-lime-50">
      {/* Show Homepage if not authenticated and viewing homepage */}
      {currentView === "homepage" && !isAuthenticated && (
        <Homepage onGetStarted={() => setCurrentView("auth")} />
      )}

      {/* Auth Modal */}
      {currentView === "auth" && !isAuthenticated && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
          <div className="w-full max-w-md">
            <button
              onClick={() => setCurrentView("homepage")}
              className="mb-4 rounded-lg border-2 border-emerald-600 bg-white px-4 py-2 text-sm font-semibold text-emerald-700 hover:bg-emerald-50 transition-colors"
            >
              ← Back to Home
            </button>
            <div className="rounded-2xl border-2 border-emerald-200 bg-white p-8 shadow-2xl">
              <div className="mb-6 text-center">
                <div className="mx-auto mb-3 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-emerald-500 to-green-600 text-4xl shadow-lg">
                  🌾
                </div>
                <h2 className="text-2xl font-bold text-emerald-900">
                  {isLogin ? "Welcome Back" : "Create Account"}
                </h2>
                <p className="mt-1 text-sm text-slate-600">
                  {isLogin ? "Sign in to your account" : "Start tracking your supply chain"}
                </p>
              </div>

              {error && (
                <div className="mb-4 rounded-lg border-2 border-red-400 bg-red-50 px-4 py-3 text-sm text-red-700">
                  {error}
                </div>
              )}

              <form onSubmit={handleAuthSubmit} className="space-y-4">
                <div>
                  <label className="mb-2 block text-sm font-medium text-slate-700">
                    Username *
                  </label>
                  <input
                    type="text"
                    value={authData.username}
                    onChange={(e) => setAuthData({ ...authData, username: e.target.value })}
                    required
                    className="w-full rounded-lg border-2 border-slate-300 bg-white px-4 py-2.5 text-sm text-slate-900 placeholder-slate-400 focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-200"
                    placeholder="Enter your username"
                  />
                </div>

                {!isLogin && (
                  <div>
                    <label className="mb-2 block text-sm font-medium text-slate-700">
                      Email *
                    </label>
                    <input
                      type="email"
                      value={authData.email}
                      onChange={(e) => setAuthData({ ...authData, email: e.target.value })}
                      required
                      className="w-full rounded-lg border-2 border-slate-300 bg-white px-4 py-2.5 text-sm text-slate-900 placeholder-slate-400 focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-200"
                      placeholder="Enter your email"
                    />
                  </div>
                )}

                <div>
                  <label className="mb-2 block text-sm font-medium text-slate-700">
                    Password *
                  </label>
                  <input
                    type="password"
                    value={authData.password}
                    onChange={(e) => setAuthData({ ...authData, password: e.target.value })}
                    required
                    className="w-full rounded-lg border-2 border-slate-300 bg-white px-4 py-2.5 text-sm text-slate-900 placeholder-slate-400 focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-200"
                    placeholder="Enter your password"
                  />
                </div>

                <button
                  type="submit"
                  className="w-full rounded-xl bg-gradient-to-r from-emerald-600 to-green-600 px-4 py-3 text-base font-bold text-white shadow-lg hover:shadow-xl transition-all transform hover:scale-105"
                >
                  {isLogin ? "Sign In" : "Create Account"}
                </button>
              </form>

              <div className="relative my-6">
                <div className="absolute inset-0 flex items-center">
                  <div className="w-full border-t border-slate-300"></div>
                </div>
                <div className="relative flex justify-center text-sm">
                  <span className="bg-white px-3 text-slate-600">Or continue with</span>
                </div>
              </div>

              <button
                onClick={handleGoogleLogin}
                type="button"
                className="w-full flex items-center justify-center gap-2 rounded-xl border-2 border-slate-300 bg-white px-4 py-3 text-sm font-semibold text-slate-700 hover:bg-slate-50 transition-colors"
              >
                <svg className="h-5 w-5" viewBox="0 0 24 24">
                  <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                  <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                  <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                  <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                </svg>
                Sign in with Google
              </button>

              <div className="mt-6 text-center text-sm text-slate-600">
                {isLogin ? "Don't have an account? " : "Already have an account? "}
                <button
                  onClick={() => {
                    setIsLogin(!isLogin);
                    setError("");
                  }}
                  className="font-semibold text-emerald-600 hover:text-emerald-700"
                >
                  {isLogin ? "Sign up" : "Sign in"}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Main App - Only show when authenticated and on main view */}
      {isAuthenticated && currentView === "main" && (
        <>
          <header className="sticky top-0 z-40 border-b border-slate-800 bg-slate-900/80 backdrop-blur-sm">
            <div className="flex items-center justify-between px-6 py-4">
              <div>
                <h1 className="text-xl font-semibold tracking-tight">
                  Agri Supply Tracker
                </h1>
                <p className="text-xs text-slate-400">
                  React + Tailwind · Spring Boot · MongoDB
                </p>
              </div>

              <div className="flex items-center gap-3">
                <div className="flex flex-col items-end">
                  <span className="text-sm text-slate-300">
                    Welcome, <span className="font-semibold text-emerald-400">{currentUser}</span>
                  </span>
                  <div className="flex flex-col items-end gap-0.5">
                    <span className="text-[10px] text-slate-500">
                      {userRoles.includes("ROLE_ADMIN") ? "👑 Administrator" : "👤 User"}
                    </span>
                    {userStageProfile && (
                      <span className="text-[10px] text-blue-400">
                        🎯 {userStageProfile === "WAREHOUSE_MANAGER" ? "Warehouse Manager" : userStageProfile.charAt(0) + userStageProfile.slice(1).toLowerCase()}
                      </span>
                    )}
                    {userLocation && (
                      <span className="text-[10px] text-slate-400">
                        📍 {userLocation}
                      </span>
                    )}
                  </div>
                </div>
                <button
                  onClick={handleLogout}
                  className="rounded-lg border border-slate-700 bg-slate-800 px-3 py-1.5 text-xs font-medium text-slate-200 hover:border-slate-500 hover:bg-slate-700 transition-colors"
                >
                  Logout
                </button>
              </div>
            </div>
          </header>

          <main className="container mx-auto max-w-7xl px-6 py-8">
            <div className="mb-6 flex gap-2 rounded-xl border border-slate-800 bg-slate-900/60 p-2">
              <button
                onClick={() => setActiveTab("dashboard")}
                className={`flex-1 rounded-lg px-4 py-2.5 text-sm font-medium transition-all ${
                  activeTab === "dashboard"
                    ? "bg-emerald-500 text-emerald-950 shadow-lg shadow-emerald-500/20"
                    : "text-slate-400 hover:bg-slate-800 hover:text-slate-200"
                }`}
              >
                📊 Dashboard
              </button>
              <button
                onClick={() => setActiveTab("products")}
                className={`flex-1 rounded-lg px-4 py-2.5 text-sm font-medium transition-all ${
                  activeTab === "products"
                    ? "bg-emerald-500 text-emerald-950 shadow-lg shadow-emerald-500/20"
                    : "text-slate-400 hover:bg-slate-800 hover:text-slate-200"
                }`}
              >
                📦 Products
              </button>
              <button
                onClick={() => setActiveTab("farms")}
                className={`flex-1 rounded-lg px-4 py-2.5 text-sm font-medium transition-all ${
                  activeTab === "farms"
                    ? "bg-emerald-500 text-emerald-950 shadow-lg shadow-emerald-500/20"
                    : "text-slate-400 hover:bg-slate-800 hover:text-slate-200"
                }`}
              >
                🏞️ Farms
              </button>
            </div>

            {/* Dashboard View */}
            {activeTab === "dashboard" && (
              <div className="space-y-6">
                {dashboardLoading ? (
                  <div className="flex items-center justify-center py-12">
                    <div className="flex items-center gap-2 text-sm text-slate-400">
                      <span className="h-2 w-2 animate-ping rounded-full bg-emerald-400" />
                      Loading dashboard...
                    </div>
                  </div>
                ) : dashboardStats ? (
                  <>
                    {/* Stats Cards */}
                    <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
                      {/* Total Products */}
                      <div className="rounded-xl border border-slate-800 bg-gradient-to-br from-blue-500/10 to-slate-900/60 p-6">
                        <div className="flex items-center justify-between">
                          <div>
                            <p className="text-xs font-medium text-slate-400">Total Products</p>
                            <p className="mt-2 text-3xl font-bold text-slate-100">{dashboardStats.totalProducts}</p>
                          </div>
                          <div className="rounded-full bg-blue-500/20 p-3 text-2xl">📦</div>
                        </div>
                      </div>

                      {/* Unique Types */}
                      <div className="rounded-xl border border-slate-800 bg-gradient-to-br from-emerald-500/10 to-slate-900/60 p-6">
                        <div className="flex items-center justify-between">
                          <div>
                            <p className="text-xs font-medium text-slate-400">Product Types</p>
                            <p className="mt-2 text-3xl font-bold text-slate-100">{dashboardStats.uniqueTypes}</p>
                          </div>
                          <div className="rounded-full bg-emerald-500/20 p-3 text-2xl">🏷️</div>
                        </div>
                      </div>

                      {/* Unique Farms */}
                      <div className="rounded-xl border border-slate-800 bg-gradient-to-br from-amber-500/10 to-slate-900/60 p-6">
                        <div className="flex items-center justify-between">
                          <div>
                            <p className="text-xs font-medium text-slate-400">Origin Farms</p>
                            <p className="mt-2 text-3xl font-bold text-slate-100">{dashboardStats.uniqueFarms}</p>
                          </div>
                          <div className="rounded-full bg-amber-500/20 p-3 text-2xl">🏞️</div>
                        </div>
                      </div>

                      {/* Tracking Stages */}
                      <div className="rounded-xl border border-slate-800 bg-gradient-to-br from-purple-500/10 to-slate-900/60 p-6">
                        <div className="flex items-center justify-between">
                          <div>
                            <p className="text-xs font-medium text-slate-400">Tracking Stages</p>
                            <p className="mt-2 text-3xl font-bold text-slate-100">{dashboardStats.totalTrackingStages}</p>
                          </div>
                          <div className="rounded-full bg-purple-500/20 p-3 text-2xl">📍</div>
                        </div>
                      </div>
                    </div>

                    {/* Charts Section */}
                    <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
                      {/* Products by Type Chart */}
                      <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-6">
                        <h3 className="mb-4 text-sm font-semibold text-slate-200">Products by Type</h3>
                        <div className="space-y-3">
                          {Object.entries(dashboardStats.productsByType || {}).map(([type, count]) => {
                            const total = dashboardStats.totalProducts;
                            const percentage = total > 0 ? ((count / total) * 100).toFixed(1) : 0;
                            return (
                              <div key={type}>
                                <div className="mb-1 flex items-center justify-between text-xs">
                                  <span className="font-medium text-slate-300">{type}</span>
                                  <span className="text-slate-500">{count} ({percentage}%)</span>
                                </div>
                                <div className="h-2 overflow-hidden rounded-full bg-slate-800">
                                  <div
                                    className="h-full rounded-full bg-gradient-to-r from-emerald-500 to-emerald-400"
                                    style={{ width: `${percentage}%` }}
                                  />
                                </div>
                              </div>
                            );
                          })}
                        </div>
                      </div>

                      {/* Recent Products */}
                      <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-6">
                        <h3 className="mb-4 text-sm font-semibold text-slate-200">Recent Products</h3>
                        <div className="space-y-3">
                          {dashboardStats.recentProducts && dashboardStats.recentProducts.length > 0 ? (
                            dashboardStats.recentProducts.map((product) => (
                              <div key={product.id} className="flex items-center justify-between rounded-lg border border-slate-800 bg-slate-800/50 p-3">
                                <div>
                                  <p className="text-sm font-medium text-slate-200">{product.name}</p>
                                  <p className="text-xs text-slate-400">{product.type} • {product.batchId}</p>
                                </div>
                                <div className="text-right">
                                  <p className="text-xs text-slate-500">{product.harvestDate}</p>
                                </div>
                              </div>
                            ))
                          ) : (
                            <p className="text-sm text-slate-500">No products yet</p>
                          )}
                        </div>
                      </div>
                    </div>
                  </>
                ) : (
                  <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-12 text-center">
                    <p className="text-sm text-slate-400">Unable to load dashboard statistics</p>
                  </div>
                )}
              </div>
            )}

        {/* Products Card */}
        {activeTab === "products" && (
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
                      Origin Farm *
                    </label>
                    <select
                      name="originFarmId"
                      value={formData.originFarmId}
                      onChange={handleInputChange}
                      required
                      className="w-full rounded-lg border border-slate-600 bg-slate-900 px-3 py-1.5 text-sm text-slate-100 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                    >
                      <option value="">Select farm</option>
                      {farms.map(farm => (
                        <option key={farm.id} value={farm.id}>
                          {farm.name} - {farm.location}
                        </option>
                      ))}
                    </select>
                    {farms.length === 0 && (
                      <p className="mt-1 text-xs text-amber-400">No farms available. Please add a farm first.</p>
                    )}
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-medium text-slate-400">
                      Destination (optional)
                    </label>
                    <input
                      type="text"
                      name="destination"
                      value={formData.destination}
                      onChange={handleInputChange}
                      className="w-full rounded-lg border border-slate-600 bg-slate-900 px-3 py-1.5 text-sm text-slate-100 placeholder-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                      placeholder="e.g., Mumbai Market"
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
                      <th className="w-[30%] px-4 py-2 text-right text-xs font-semibold uppercase tracking-wide text-slate-400">
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
                          <div className="flex justify-end gap-2">
                            <button
                              onClick={() => handleViewTracking(p)}
                              className="rounded-lg border border-blue-500/40 bg-blue-500/10 px-3 py-1 text-xs font-medium text-blue-300 hover:border-blue-400 hover:bg-blue-500/20 transition-colors"
                            >
                              📍 Track
                            </button>
                            {userRoles.includes("ROLE_ADMIN") && (
                              <>
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
                              </>
                            )}
                          </div>
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
        )}

        {/* Farms View */}
        {activeTab === "farms" && (
          <div className="rounded-2xl border border-slate-800 bg-slate-900/60 shadow-xl shadow-black/40">
            <div className="flex items-center justify-between border-b border-slate-800 px-6 py-4">
              <div>
                <h2 className="text-lg font-semibold tracking-tight">Farms</h2>
                <p className="text-xs text-slate-400">
                  Showing {farms.length} farm(s)
                </p>
              </div>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={fetchFarms}
                  className="rounded-lg border border-slate-700 bg-slate-800 px-3 py-1.5 text-xs font-medium text-slate-200 hover:border-slate-500 hover:bg-slate-700 transition-colors"
                >
                  Refresh
                </button>
                {userRoles.includes("ROLE_ADMIN") && (
                  <button
                    type="button"
                    onClick={() => setShowFarmForm(!showFarmForm)}
                    className="rounded-lg bg-emerald-500 px-3 py-1.5 text-xs font-semibold text-emerald-950 shadow-sm hover:bg-emerald-400 transition-colors"
                  >
                    {showFarmForm ? "Cancel" : "+ Add Farm"}
                  </button>
                )}
              </div>
            </div>

            <div className="px-6 py-4">
              {/* Add/Edit Farm Form */}
              {showFarmForm && (
                <div className="mb-6 rounded-lg border border-slate-700 bg-slate-800/50 p-4">
                  <h3 className="mb-4 text-sm font-semibold text-slate-200">
                    {editingFarm ? "Edit Farm" : "Add New Farm"}
                  </h3>
                  <form onSubmit={handleFarmSubmit} className="space-y-3">
                    <div className="grid grid-cols-2 gap-3">
                      <div>
                        <label className="mb-1 block text-xs font-medium text-slate-400">
                          Farm Name *
                        </label>
                        <input
                          type="text"
                          value={farmFormData.name}
                          onChange={(e) => setFarmFormData(prev => ({ ...prev, name: e.target.value }))}
                          required
                          className="w-full rounded-lg border border-slate-600 bg-slate-900 px-3 py-1.5 text-sm text-slate-100 placeholder-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                          placeholder="e.g., Green Valley Farm"
                        />
                      </div>
                      <div>
                        <label className="mb-1 block text-xs font-medium text-slate-400">
                          Location *
                        </label>
                        <input
                          type="text"
                          value={farmFormData.location}
                          onChange={(e) => setFarmFormData(prev => ({ ...prev, location: e.target.value }))}
                          required
                          className="w-full rounded-lg border border-slate-600 bg-slate-900 px-3 py-1.5 text-sm text-slate-100 placeholder-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                          placeholder="e.g., Maharashtra, India"
                        />
                      </div>
                      <div>
                        <label className="mb-1 block text-xs font-medium text-slate-400">
                          Owner Name *
                        </label>
                        <input
                          type="text"
                          value={farmFormData.owner}
                          onChange={(e) => setFarmFormData(prev => ({ ...prev, owner: e.target.value }))}
                          required
                          className="w-full rounded-lg border border-slate-600 bg-slate-900 px-3 py-1.5 text-sm text-slate-100 placeholder-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                          placeholder="e.g., Rajesh Kumar"
                        />
                      </div>
                      <div>
                        <label className="mb-1 block text-xs font-medium text-slate-400">
                          Contact Info
                        </label>
                        <input
                          type="text"
                          value={farmFormData.contactInfo}
                          onChange={(e) => setFarmFormData(prev => ({ ...prev, contactInfo: e.target.value }))}
                          className="w-full rounded-lg border border-slate-600 bg-slate-900 px-3 py-1.5 text-sm text-slate-100 placeholder-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                          placeholder="Phone / Email"
                        />
                      </div>
                    </div>
                    <div>
                      <label className="mb-1 block text-xs font-medium text-slate-400">
                        Description
                      </label>
                      <textarea
                        value={farmFormData.description}
                        onChange={(e) => setFarmFormData(prev => ({ ...prev, description: e.target.value }))}
                        rows="2"
                        className="w-full rounded-lg border border-slate-600 bg-slate-900 px-3 py-1.5 text-sm text-slate-100 placeholder-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                        placeholder="Additional details about the farm..."
                      />
                    </div>
                    <div className="flex gap-2 pt-2">
                      <button
                        type="submit"
                        className="rounded-lg bg-emerald-500 px-4 py-1.5 text-xs font-semibold text-emerald-950 shadow-sm hover:bg-emerald-400 transition-colors"
                      >
                        {editingFarm ? "Update Farm" : "Create Farm"}
                      </button>
                      <button
                        type="button"
                        onClick={resetFarmForm}
                        className="rounded-lg border border-slate-600 bg-slate-800 px-4 py-1.5 text-xs font-medium text-slate-300 hover:border-slate-500 hover:bg-slate-700 transition-colors"
                      >
                        Cancel
                      </button>
                    </div>
                  </form>
                </div>
              )}

              {farmsLoading && (
                <div className="flex items-center gap-2 text-sm text-slate-400">
                  <span className="h-2 w-2 animate-ping rounded-full bg-emerald-400" />
                  Loading farms…
                </div>
              )}

              {!farmsLoading && farms.length === 0 && (
                <div className="rounded-lg border border-dashed border-slate-700 bg-slate-900/70 px-4 py-6 text-center text-sm text-slate-400">
                  No farms found. {userRoles.includes("ROLE_ADMIN") && "Use the form to add some farms."}
                </div>
              )}

              {!farmsLoading && farms.length > 0 && (
                <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
                  {farms.map((farm) => (
                    <div key={farm.id} className="rounded-lg border border-slate-700 bg-slate-800/50 p-4 transition-all hover:border-slate-600">
                      <div className="mb-3 flex items-start justify-between">
                        <div>
                          <h3 className="font-semibold text-slate-100">{farm.name}</h3>
                          <p className="mt-1 text-xs text-slate-400">📍 {farm.location}</p>
                        </div>
                        <div className="rounded-full bg-emerald-500/20 p-2 text-lg">🏞️</div>
                      </div>
                      <div className="space-y-2 text-xs">
                        <div>
                          <span className="text-slate-500">Owner:</span>
                          <span className="ml-1 text-slate-300">{farm.owner}</span>
                        </div>
                        {farm.contactInfo && (
                          <div>
                            <span className="text-slate-500">Contact:</span>
                            <span className="ml-1 text-slate-300">{farm.contactInfo}</span>
                          </div>
                        )}
                        {farm.description && (
                          <p className="mt-2 text-slate-400 italic">{farm.description}</p>
                        )}
                      </div>
                      {userRoles.includes("ROLE_ADMIN") && (
                        <div className="mt-4 flex gap-2 border-t border-slate-700 pt-3">
                          <button
                            onClick={() => handleEditFarm(farm)}
                            className="flex-1 rounded-lg border border-slate-600 bg-slate-800 px-3 py-1.5 text-xs font-medium text-slate-200 hover:border-slate-500 hover:bg-slate-700 transition-colors"
                          >
                            Edit
                          </button>
                          <button
                            onClick={() => handleDeleteFarm(farm.id)}
                            className="flex-1 rounded-lg border border-red-500/40 bg-red-500/10 px-3 py-1.5 text-xs font-medium text-red-300 hover:border-red-400 hover:bg-red-500/20 transition-colors"
                          >
                            Delete
                          </button>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {/* Tracking History Modal */}
        {showTrackingModal && selectedProduct && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
            <div className="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-xl border border-slate-700 bg-slate-900 shadow-2xl">
              {/* Modal Header */}
              <div className="sticky top-0 z-10 border-b border-slate-700 bg-slate-900 px-6 py-4">
                <div className="flex items-start justify-between">
                  <div>
                    <h2 className="text-lg font-bold text-slate-100">
                      📦 Supply Chain Tracking
                    </h2>
                    <p className="mt-1 text-sm text-slate-400">
                      {selectedProduct.name} - Batch {selectedProduct.batchId}
                    </p>
                  </div>
                  <button
                    onClick={closeTrackingModal}
                    className="rounded-lg p-2 text-slate-400 hover:bg-slate-800 hover:text-slate-200 transition-colors"
                  >
                    ✕
                  </button>
                </div>
              </div>

              {/* Modal Body */}
              <div className="px-6 py-6">
                {/* Product Info */}
                <div className="mb-6 grid grid-cols-2 gap-4 rounded-lg border border-slate-700 bg-slate-800/50 p-4">
                  <div>
                    <p className="text-xs font-medium text-slate-400">Type</p>
                    <p className="mt-1 text-sm text-slate-200">{selectedProduct.type}</p>
                  </div>
                  <div>
                    <p className="text-xs font-medium text-slate-400">Harvest Date</p>
                    <p className="mt-1 text-sm text-slate-200">{selectedProduct.harvestDate}</p>
                  </div>
                  <div>
                    <p className="text-xs font-medium text-slate-400">Origin Farm</p>
                    <p className="mt-1 text-sm text-slate-200">{selectedProduct.originFarmId}</p>
                  </div>
                  <div>
                    <p className="text-xs font-medium text-slate-400">Tracking Stages</p>
                    <p className="mt-1 text-sm text-slate-200">
                      {selectedProduct.trackingHistory?.length || 0} stage(s)
                    </p>
                  </div>
                </div>

                {/* Add Stage Button */}
                {(userRoles.includes("ROLE_ADMIN") || userRoles.includes("ROLE_FARMER") || userRoles.includes("ROLE_PROCESSOR") || userRoles.includes("ROLE_WAREHOUSE_MANAGER") || userRoles.includes("ROLE_DISTRIBUTOR") || userRoles.includes("ROLE_RETAILER")) && !showAddStageForm && (
                  <button
                    onClick={() => setShowAddStageForm(true)}
                    className="mb-6 w-full rounded-lg border-2 border-dashed border-emerald-500/50 bg-emerald-500/10 px-4 py-3 text-sm font-medium text-emerald-300 hover:border-emerald-500 hover:bg-emerald-500/20 transition-colors"
                  >
                    + Add New Tracking Stage
                  </button>
                )}

                {/* Add Stage Form */}
                {showAddStageForm && (
                  <form onSubmit={handleAddStage} className="mb-6 rounded-lg border border-emerald-500/30 bg-slate-800/50 p-4">
                    <h3 className="mb-4 text-sm font-semibold text-slate-200">Add Tracking Stage</h3>
                    <div className="space-y-3">
                      <div className="grid grid-cols-2 gap-3">
                        <div>
                          <label className="mb-1 block text-xs font-medium text-slate-400">
                            Stage * {userStageProfile && <span className="text-emerald-400 text-xs">({userStageProfile})</span>}
                          </label>
                          <select
                            value={trackingFormData.stage}
                            onChange={(e) => setTrackingFormData(prev => ({ ...prev, stage: e.target.value }))}
                            required
                            className="w-full rounded-lg border border-slate-600 bg-slate-900 px-3 py-1.5 text-sm text-slate-100 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                          >
                            <option value="">Select stage</option>
                            {(userRoles.includes("ROLE_ADMIN") || userStageProfile === "FARMER") && (
                              <option value="Farm">🌾 Farm</option>
                            )}
                            {(userRoles.includes("ROLE_ADMIN") || userStageProfile === "PROCESSOR") && (
                              <>
                                <option value="Processing">⚙️ Processing</option>
                                <option value="Quality Check">✅ Quality Check</option>
                              </>
                            )}
                            {(userRoles.includes("ROLE_ADMIN") || userStageProfile === "WAREHOUSE_MANAGER") && (
                              <option value="Warehouse">📦 Warehouse</option>
                            )}
                            {(userRoles.includes("ROLE_ADMIN") || userStageProfile === "DISTRIBUTOR") && (
                              <option value="Distribution">🚚 Distribution</option>
                            )}
                            {(userRoles.includes("ROLE_ADMIN") || userStageProfile === "RETAILER") && (
                              <option value="Retail">🏪 Retail</option>
                            )}
                          </select>
                        </div>
                        <div>
                          <label className="mb-1 block text-xs font-medium text-slate-400">
                            Location *
                          </label>
                          <input
                            type="text"
                            value={trackingFormData.location}
                            onChange={(e) => setTrackingFormData(prev => ({ ...prev, location: e.target.value }))}
                            required
                            className="w-full rounded-lg border border-slate-600 bg-slate-900 px-3 py-1.5 text-sm text-slate-100 placeholder-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                            placeholder="e.g., Mumbai Warehouse"
                          />
                        </div>
                      </div>
                      <div>
                        <label className="mb-1 block text-xs font-medium text-slate-400">
                          Handler *
                        </label>
                        <input
                          type="text"
                          value={trackingFormData.handler}
                          onChange={(e) => setTrackingFormData(prev => ({ ...prev, handler: e.target.value }))}
                          required
                          className="w-full rounded-lg border border-slate-600 bg-slate-900 px-3 py-1.5 text-sm text-slate-100 placeholder-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                          placeholder="Person or company handling this stage"
                        />
                      </div>
                      <div>
                        <label className="mb-1 block text-xs font-medium text-slate-400">
                          Notes (optional)
                        </label>
                        <textarea
                          value={trackingFormData.notes}
                          onChange={(e) => setTrackingFormData(prev => ({ ...prev, notes: e.target.value }))}
                          rows="2"
                          className="w-full rounded-lg border border-slate-600 bg-slate-900 px-3 py-1.5 text-sm text-slate-100 placeholder-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                          placeholder="Additional information..."
                        />
                      </div>
                      <div className="flex gap-2 pt-2">
                        <button
                          type="submit"
                          className="rounded-lg bg-emerald-500 px-4 py-1.5 text-xs font-semibold text-emerald-950 hover:bg-emerald-400 transition-colors"
                        >
                          Add Stage
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            setShowAddStageForm(false);
                            setTrackingFormData({ stage: "", location: "", handler: "", notes: "" });
                          }}
                          className="rounded-lg border border-slate-600 bg-slate-800 px-4 py-1.5 text-xs font-medium text-slate-300 hover:border-slate-500 hover:bg-slate-700 transition-colors"
                        >
                          Cancel
                        </button>
                      </div>
                    </div>
                  </form>
                )}

                {/* Tracking Timeline */}
                <div className="space-y-4">
                  <h3 className="text-sm font-semibold text-slate-300">
                    Supply Chain Journey
                  </h3>
                  
                  {(!selectedProduct.trackingHistory || selectedProduct.trackingHistory.length === 0) ? (
                    <div className="rounded-lg border border-dashed border-slate-700 bg-slate-800/30 px-4 py-8 text-center">
                      <p className="text-sm text-slate-400">
                        No tracking stages recorded yet.
                      </p>
                      {(userRoles.includes("ROLE_ADMIN") || userRoles.includes("ROLE_FARMER") || userRoles.includes("ROLE_PROCESSOR") || userRoles.includes("ROLE_WAREHOUSE_MANAGER") || userRoles.includes("ROLE_DISTRIBUTOR") || userRoles.includes("ROLE_RETAILER")) && (
                        <p className="mt-1 text-xs text-slate-500">
                          Click "Add New Tracking Stage" to begin tracking this product's journey.
                        </p>
                      )}
                    </div>
                  ) : (
                    <div className="relative">
                      {/* Timeline line */}
                      <div className="absolute left-4 top-0 bottom-0 w-0.5 bg-slate-700" />
                      
                      {/* Timeline items */}
                      <div className="space-y-4">
                        {selectedProduct.trackingHistory.map((stage, index) => (
                          <div key={index} className="relative pl-12">
                            {/* Timeline dot */}
                            <div className="absolute left-0 top-1 flex h-8 w-8 items-center justify-center rounded-full border-2 border-emerald-500 bg-slate-900">
                              <span className="text-sm">
                                {stage.stage === "Farm" && "🌾"}
                                {stage.stage === "Processing" && "⚙️"}
                                {stage.stage === "Quality Check" && "✅"}
                                {stage.stage === "Warehouse" && "📦"}
                                {stage.stage === "Distribution" && "🚚"}
                                {stage.stage === "Retail" && "🏪"}
                                {!["Farm", "Processing", "Quality Check", "Warehouse", "Distribution", "Retail"].includes(stage.stage) && "📍"}
                              </span>
                            </div>
                            
                            {/* Stage content */}
                            <div className="rounded-lg border border-slate-700 bg-slate-800/50 p-4">
                              <div className="flex items-start justify-between">
                                <div>
                                  <h4 className="font-semibold text-slate-100">{stage.stage}</h4>
                                  <p className="mt-1 text-sm text-slate-300">📍 {stage.location}</p>
                                  <p className="mt-0.5 text-xs text-slate-400">
                                    Handled by: {stage.handler}
                                  </p>
                                  {stage.notes && (
                                    <p className="mt-2 text-xs text-slate-400 italic">
                                      "{stage.notes}"
                                    </p>
                                  )}
                                </div>
                                <div className="text-right">
                                  <p className="text-xs text-slate-500">
                                    {stage.timestamp ? new Date(stage.timestamp).toLocaleDateString() : "N/A"}
                                  </p>
                                  <p className="text-xs text-slate-500">
                                    {stage.timestamp ? new Date(stage.timestamp).toLocaleTimeString() : ""}
                                  </p>
                                </div>
                              </div>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Footer note */}
        <p className="mt-6 text-xs text-slate-500">
          ✅ Dashboard & Tracking features added!
        </p>
          </main>
        </>
      )}
    </div>
  );
}

export default App;

