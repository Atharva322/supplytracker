import { Navigate } from 'react-router-dom';

/**
 * Protected Route Component
 * Redirects to homepage if user is not authenticated
 */
const ProtectedRoute = ({ children, isAuthenticated, requiredRoles = [] }) => {
  if (!isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  // Check for required roles if specified
  if (requiredRoles.length > 0) {
    const userRoles = JSON.parse(localStorage.getItem('roles') || '[]');
    const hasRequiredRole = requiredRoles.some(role => userRoles.includes(role));
    
    if (!hasRequiredRole) {
      return (
        <div className="min-h-screen bg-gradient-to-br from-red-50 to-orange-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl p-8 max-w-md text-center">
            <div className="text-6xl mb-4">🔒</div>
            <h2 className="text-2xl font-bold text-red-600 mb-4">Access Denied</h2>
            <p className="text-gray-600 mb-6">
              You don't have permission to access this resource.
            </p>
            <button
              onClick={() => window.history.back()}
              className="bg-gradient-to-r from-emerald-600 to-green-600 text-white px-6 py-2 rounded-xl font-semibold hover:from-emerald-700 hover:to-green-700 transition-all"
            >
              Go Back
            </button>
          </div>
        </div>
      );
    }
  }

  return children;
};

export default ProtectedRoute;
