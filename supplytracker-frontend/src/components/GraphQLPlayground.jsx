import { useState, useRef } from 'react';

/**
 * GraphQL Playground Component
 * Provides an interface to test GraphQL queries, mutations, and subscriptions
 */
export default function GraphQLPlayground() {
  const eventSourceRef = useRef(null);
  const [query, setQuery] = useState(`# Example Queries

# Get all products
query GetAllProducts {
  products {
    id
    name
    type
    status
    currentLocation
  }
}

# Get product by ID
query GetProduct {
  product(id: "YOUR_PRODUCT_ID") {
    id
    name
    type
    batchId
    harvestDate
    originFarmName
    trackingHistory {
      stage
      location
      timestamp
    }
  }
}

# Search products
query SearchProducts {
  searchProducts(keyword: "apple") {
    id
    name
    type
    status
  }
}

# Get products by status
query ProductsByStatus {
  productsByStatus(status: "IN_TRANSIT") {
    id
    name
    currentLocation
    destination
  }
}`);

  const [mutation, setMutation] = useState(`# Example Mutations

# Create a new product
mutation CreateProduct {
  createProduct(input: {
    name: "Fresh Apples"
    type: "Fruit"
    batchId: "BATCH-2024-001"
    harvestDate: "2024-12-15"
    originFarmId: "farm123"
    originFarmName: "Green Valley Farm"
    currentLocation: "Farm Storage"
    status: "AT_FARM"
  }) {
    id
    name
    status
  }
}

# Update product status
mutation UpdateStatus {
  updateProductStatus(
    id: "YOUR_PRODUCT_ID"
    status: "IN_TRANSIT"
    location: "Highway 101"
  ) {
    id
    status
    currentLocation
  }
}

# Add tracking stage
mutation AddTracking {
  addTrackingStage(
    productId: "YOUR_PRODUCT_ID"
    stage: {
      stage: "Loaded onto Truck"
      location: "Distribution Center"
      timestamp: "2024-12-15T10:30:00"
      description: "Loaded 50 boxes"
      handledBy: "John Doe"
    }
  ) {
    id
    trackingHistory {
      stage
      location
      timestamp
    }
  }
}`);

  const [subscription, setSubscription] = useState(`# Example Subscriptions

# Subscribe to product updates
subscription OnProductUpdate {
  productUpdated {
    id
    name
    status
    currentLocation
  }
}

# Subscribe to new products
subscription OnProductCreated {
  productCreated {
    id
    name
    type
    status
  }
}

# Subscribe to status changes
subscription OnStatusChange {
  productStatusChanged(productId: "YOUR_PRODUCT_ID") {
    productId
    oldStatus
    newStatus
    location
    timestamp
  }
}`);

  const [result, setResult] = useState('');
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('query');
  const [isSubscribed, setIsSubscribed] = useState(false);
  const [subscriptionMessages, setSubscriptionMessages] = useState([]);

  const executeGraphQL = async (graphqlQuery) => {
    setLoading(true);
    setResult('');

    try {
      const response = await fetch('http://localhost:8080/graphql', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify({ query: graphqlQuery })
      });

      const data = await response.json();
      setResult(JSON.stringify(data, null, 2));
    } catch (error) {
      setResult(`Error: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleExecute = () => {
    const currentQuery = activeTab === 'query' ? query : mutation;
    executeGraphQL(currentQuery);
  };

  const handleSubscribe = () => {
    if (isSubscribed) {
      // Unsubscribe - close EventSource
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
      setIsSubscribed(false);
      setSubscriptionMessages([]);
      setResult('Subscription stopped.');
      return;
    }

    // Subscribe using Server-Sent Events (SSE)
    setLoading(true);
    setSubscriptionMessages([]);
    setResult('Connecting to SSE stream...\n');

    try {
      // Create EventSource for SSE
      const token = localStorage.getItem('token');
      const url = `http://localhost:8080/api/products/stream${token ? `?token=${token}` : ''}`;
      const eventSource = new EventSource(url);

      eventSource.onopen = () => {
        setLoading(false);
        setIsSubscribed(true);
        setResult('✅ Connected! Listening for product updates...\n');
      };

      eventSource.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          const message = {
            timestamp: new Date().toLocaleTimeString(),
            data: data
          };
          setSubscriptionMessages(prev => [...prev, message]);
          setResult(prevResult => 
            prevResult + `\n[${message.timestamp}] ${JSON.stringify(data, null, 2)}\n`
          );
        } catch (error) {
          console.error('Error parsing SSE data:', error);
        }
      };

      eventSource.onerror = (error) => {
        console.error('SSE error:', error);
        setResult(prevResult => 
          prevResult + `\n❌ Connection error. Make sure the backend is running on http://localhost:8080\n`
        );
        eventSource.close();
        setIsSubscribed(false);
        setLoading(false);
      };

      eventSourceRef.current = eventSource;
    } catch (error) {
      setResult(`❌ Error: ${error.message}`);
      setIsSubscribed(false);
      setLoading(false);
    }
  };

  const openGraphiQL = () => {
    window.open('http://localhost:8080/graphiql', '_blank');
  };

  return (
    <div className="p-6">
      <div className="mb-6">
        <h2 className="text-3xl font-bold mb-2">GraphQL API</h2>
        <p className="text-gray-600">
          Test GraphQL queries, mutations, and subscriptions for the Supply Tracker API
        </p>
        <button
          onClick={openGraphiQL}
          className="mt-3 px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors"
        >
          🚀 Open GraphiQL Playground
        </button>
      </div>

      {/* Tabs */}
      <div className="flex space-x-2 mb-4 border-b">
        <button
          onClick={() => {
            setActiveTab('query');
            setResult('');
          }}
          className={`px-4 py-2 font-medium ${
            activeTab === 'query'
              ? 'text-blue-600 border-b-2 border-blue-600'
              : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          📊 Queries
        </button>
        <button
          onClick={() => {
            setActiveTab('mutation');
            setResult('');
          }}
          className={`px-4 py-2 font-medium ${
            activeTab === 'mutation'
              ? 'text-green-600 border-b-2 border-green-600'
              : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          ✏️ Mutations
        </button>
        <button
          onClick={() => {
            setActiveTab('subscription');
            if (!isSubscribed) {
              setResult('');
            }
          }}
          className={`px-4 py-2 font-medium ${
            activeTab === 'subscription'
              ? 'text-purple-600 border-b-2 border-purple-600'
              : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          🔔 Subscriptions
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Input Panel */}
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium mb-2">
              {activeTab === 'query' ? 'GraphQL Query' : 
               activeTab === 'mutation' ? 'GraphQL Mutation' : 
               'GraphQL Subscription'}
            </label>
            <textarea
              value={activeTab === 'query' ? query : activeTab === 'mutation' ? mutation : subscription}
              onChange={(e) => {
                if (activeTab === 'query') setQuery(e.target.value);
                else if (activeTab === 'mutation') setMutation(e.target.value);
                else if (activeTab === 'subscription') setSubscription(e.target.value);
              }}
              className="w-full h-96 p-4 border rounded-lg font-mono text-sm"
              placeholder="Enter your GraphQL subscription here..."
            />
          </div>

          {activeTab === 'subscription' ? (
            <div className="space-y-3">
              <button
                onClick={handleSubscribe}
                disabled={loading}
                className={`w-full px-6 py-3 text-white rounded-lg transition-colors font-medium ${
                  isSubscribed 
                    ? 'bg-red-600 hover:bg-red-700' 
                    : 'bg-purple-600 hover:bg-purple-700'
                } disabled:bg-gray-400`}
              >
                {loading ? '⏳ Connecting...' : isSubscribed ? '⏹️ Stop Subscription' : '🔔 Start Subscription'}
              </button>
              <div className="p-4 bg-purple-50 border border-purple-200 rounded-lg">
                <p className="text-sm text-purple-800">
                  <strong>💡 Tip:</strong> Click "Start Subscription" to listen for real-time product updates via Server-Sent Events (SSE). 
                  Create or update products to see live updates appear here!
                </p>
              </div>
            </div>
          ) : (
            <button
              onClick={handleExecute}
              disabled={loading}
              className="w-full px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 transition-colors font-medium"
            >
              {loading ? '⏳ Executing...' : '▶️ Execute'}
            </button>
          )}
        </div>

        {/* Result Panel */}
        <div>
          <label className="block text-sm font-medium mb-2">Response</label>
          <pre className="w-full h-96 p-4 border rounded-lg bg-gray-50 overflow-auto font-mono text-sm">
            {result || 'Execute a query or mutation to see results here...'}
          </pre>
        </div>
      </div>

      {/* Documentation */}
      <div className="mt-8 p-6 bg-blue-50 border border-blue-200 rounded-lg">
        <h3 className="text-xl font-bold mb-3">📚 Quick Guide</h3>
        
        <div className="space-y-4">
          <div>
            <h4 className="font-semibold text-blue-900">Queries (Read Data)</h4>
            <ul className="list-disc list-inside text-sm text-gray-700 mt-1 space-y-1">
              <li><code>products</code> - Get all products</li>
              <li><code>product(id: ID!)</code> - Get product by ID</li>
              <li><code>searchProducts(keyword: String!)</code> - Search products</li>
              <li><code>productsByStatus(status: String!)</code> - Filter by status</li>
              <li><code>productsByFarm(farmId: String!)</code> - Filter by farm</li>
            </ul>
          </div>

          <div>
            <h4 className="font-semibold text-green-900">Mutations (Modify Data)</h4>
            <ul className="list-disc list-inside text-sm text-gray-700 mt-1 space-y-1">
              <li><code>createProduct(input: ProductInput!)</code> - Create new product</li>
              <li><code>updateProduct(id: ID!, input: ProductInput!)</code> - Update product</li>
              <li><code>deleteProduct(id: ID!)</code> - Delete product (Admin only)</li>
              <li><code>addTrackingStage(productId: ID!, stage: TrackingStageInput!)</code> - Add tracking</li>
              <li><code>updateProductStatus(id: ID!, status: String!, location: String)</code> - Update status</li>
            </ul>
          </div>

          <div>
            <h4 className="font-semibold text-purple-900">Subscriptions (Real-time Updates via SSE)</h4>
            <ul className="list-disc list-inside text-sm text-gray-700 mt-1 space-y-1">
              <li>Click "Start Subscription" to receive real-time product updates</li>
              <li>Uses Server-Sent Events (SSE) - no WebSocket needed!</li>
              <li>Automatically receives updates when products are created or modified</li>
            </ul>
          </div>
        </div>

        <div className="mt-4 p-3 bg-white rounded border border-blue-300">
          <p className="text-sm text-gray-700">
            <strong>💡 Tip:</strong> Use the GraphiQL playground (button above) for a better development 
            experience with auto-completion, schema documentation, and subscription testing!
          </p>
        </div>
      </div>
    </div>
  );
}
