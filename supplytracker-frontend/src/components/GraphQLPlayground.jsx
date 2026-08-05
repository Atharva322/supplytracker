import { useState } from 'react';

const examples = {
  query: `query GetAllProducts {
  products { id name type status currentLocation }
}`,
  mutation: `mutation CreateProduct {
  createProduct(input: {
    name: "Fresh Apples"
    type: "Fruit"
    batchId: "BATCH-2026-001"
    harvestDate: "2026-08-05"
    originFarmId: "farm123"
    status: "AT_FARM"
  }) { id name status }
}`,
};

/** Authenticated compatibility playground for GraphQL reads and admin writes. */
export default function GraphQLPlayground() {
  const [activeTab, setActiveTab] = useState('query');
  const [query, setQuery] = useState(examples.query);
  const [mutation, setMutation] = useState(examples.mutation);
  const [result, setResult] = useState('');
  const [loading, setLoading] = useState(false);

  const executeGraphQL = async () => {
    setLoading(true);
    setResult('');
    try {
      const response = await fetch('http://localhost:8080/graphql', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${localStorage.getItem('token')}`,
        },
        body: JSON.stringify({ query: activeTab === 'query' ? query : mutation }),
      });
      setResult(JSON.stringify(await response.json(), null, 2));
    } catch (error) {
      setResult(`Error: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="p-6">
      <div className="mb-6">
        <h2 className="text-3xl font-bold mb-2">GraphQL API</h2>
        <p className="text-gray-600">Authenticated v1 compatibility queries and admin mutations.</p>
        <p className="mt-2 text-sm text-amber-700">
          Product subscriptions were removed because the old implementation exposed a global stream. Real-time notifications use authenticated user-scoped delivery.
        </p>
      </div>
      <div className="flex space-x-2 mb-4 border-b">
        {['query', 'mutation'].map((tab) => (
          <button key={tab} onClick={() => { setActiveTab(tab); setResult(''); }}
            className={`px-4 py-2 font-medium ${activeTab === tab ? 'text-blue-600 border-b-2 border-blue-600' : 'text-gray-500'}`}>
            {tab === 'query' ? '📊 Queries' : '✏️ Mutations'}
          </button>
        ))}
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div>
          <textarea value={activeTab === 'query' ? query : mutation}
            onChange={(event) => activeTab === 'query' ? setQuery(event.target.value) : setMutation(event.target.value)}
            className="w-full h-96 p-4 border rounded-lg font-mono text-sm" />
          <button onClick={executeGraphQL} disabled={loading}
            className="mt-4 w-full px-6 py-3 bg-blue-600 text-white rounded-lg disabled:bg-gray-400">
            {loading ? '⏳ Executing...' : '▶️ Execute'}
          </button>
        </div>
        <pre className="w-full h-96 p-4 border rounded-lg bg-gray-50 overflow-auto font-mono text-sm">
          {result || 'Execute a query or mutation to see results here...'}
        </pre>
      </div>
    </div>
  );
}
