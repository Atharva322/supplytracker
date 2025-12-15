import { useState } from 'react';
import { apiClient } from '../api';

function ObjectDetection() {
  const [selectedFile, setSelectedFile] = useState(null);
  const [preview, setPreview] = useState(null);
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState(null);
  const [error, setError] = useState(null);
  const [activeMode, setActiveMode] = useState('detect'); // 'detect' or 'quality'

  const handleFileSelect = (e) => {
    const file = e.target.files[0];
    if (file) {
      setSelectedFile(file);
      setError(null);
      setResults(null);

      // Create preview
      const reader = new FileReader();
      reader.onloadend = () => {
        setPreview(reader.result);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleDetection = async () => {
    if (!selectedFile) {
      setError('Please select an image first');
      return;
    }

    setLoading(true);
    setError(null);
    setResults(null);

    try {
      const formData = new FormData();
      formData.append('file', selectedFile);

      const endpoint = activeMode === 'quality' ? '/detection/quality-check' : '/detection/detect';
      const response = await apiClient.post(endpoint, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      setResults(response.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Detection failed. Please try again.');
      console.error('Detection error:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleReset = () => {
    setSelectedFile(null);
    setPreview(null);
    setResults(null);
    setError(null);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-emerald-50 via-green-50 to-lime-50 p-6">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="bg-white rounded-2xl shadow-xl p-6 mb-6">
          <h1 className="text-3xl font-bold text-emerald-800 mb-2 flex items-center gap-3">
            <span className="text-4xl">🔍</span>
            AI Object Detection
          </h1>
          <p className="text-gray-600">
            Upload images to detect and analyze agricultural products using YOLOv3
          </p>
        </div>

        {/* Mode Selection */}
        <div className="bg-white rounded-2xl shadow-xl p-6 mb-6">
          <div className="flex gap-4">
            <button
              onClick={() => setActiveMode('detect')}
              className={`flex-1 py-3 px-6 rounded-xl font-semibold transition-all transform hover:scale-105 ${
                activeMode === 'detect'
                  ? 'bg-gradient-to-r from-emerald-600 to-green-600 text-white shadow-lg'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              🎯 Object Detection
            </button>
            <button
              onClick={() => setActiveMode('quality')}
              className={`flex-1 py-3 px-6 rounded-xl font-semibold transition-all transform hover:scale-105 ${
                activeMode === 'quality'
                  ? 'bg-gradient-to-r from-emerald-600 to-green-600 text-white shadow-lg'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              ✅ Quality Check
            </button>
          </div>
        </div>

        <div className="grid md:grid-cols-2 gap-6">
          {/* Upload Section */}
          <div className="bg-white rounded-2xl shadow-xl p-6">
            <h2 className="text-xl font-bold text-gray-800 mb-4">Upload Image</h2>

            {!preview ? (
              <label className="flex flex-col items-center justify-center w-full h-64 border-4 border-dashed border-emerald-300 rounded-xl cursor-pointer bg-emerald-50 hover:bg-emerald-100 transition-all">
                <div className="flex flex-col items-center justify-center pt-5 pb-6">
                  <span className="text-6xl mb-4">📸</span>
                  <p className="mb-2 text-lg font-semibold text-gray-700">
                    Click to upload image
                  </p>
                  <p className="text-sm text-gray-500">
                    PNG, JPG, JPEG (MAX. 10MB)
                  </p>
                </div>
                <input
                  type="file"
                  className="hidden"
                  accept="image/*"
                  onChange={handleFileSelect}
                />
              </label>
            ) : (
              <div className="space-y-4">
                <div className="relative">
                  <img
                    src={preview}
                    alt="Preview"
                    className="w-full h-64 object-contain rounded-xl border-2 border-emerald-200"
                  />
                  <button
                    onClick={handleReset}
                    className="absolute top-2 right-2 bg-red-500 text-white p-2 rounded-lg hover:bg-red-600 transition-all"
                  >
                    ❌
                  </button>
                </div>

                <button
                  onClick={handleDetection}
                  disabled={loading}
                  className="w-full bg-gradient-to-r from-emerald-600 to-green-600 text-white py-3 px-6 rounded-xl font-semibold hover:from-emerald-700 hover:to-green-700 disabled:opacity-50 disabled:cursor-not-allowed transform hover:scale-105 transition-all shadow-lg"
                >
                  {loading ? (
                    <span className="flex items-center justify-center gap-2">
                      <span className="animate-spin">⏳</span>
                      Processing...
                    </span>
                  ) : (
                    <span>
                      {activeMode === 'quality' ? '✅ Check Quality' : '🔍 Detect Objects'}
                    </span>
                  )}
                </button>
              </div>
            )}

            {error && (
              <div className="mt-4 p-4 bg-red-50 border-2 border-red-200 rounded-xl">
                <p className="text-red-700 font-semibold">❌ {error}</p>
              </div>
            )}
          </div>

          {/* Results Section */}
          <div className="bg-white rounded-2xl shadow-xl p-6">
            <h2 className="text-xl font-bold text-gray-800 mb-4">Detection Results</h2>

            {!results ? (
              <div className="flex flex-col items-center justify-center h-64 text-gray-400">
                <span className="text-6xl mb-4">📊</span>
                <p className="text-lg">Results will appear here</p>
              </div>
            ) : activeMode === 'quality' ? (
              // Quality Check Results
              <div className="space-y-4">
                <div className="bg-gradient-to-r from-emerald-50 to-green-50 p-6 rounded-xl border-2 border-emerald-200">
                  <div className="flex items-center justify-between mb-4">
                    <h3 className="text-2xl font-bold text-emerald-800">
                      Quality Score
                    </h3>
                    <span className="text-4xl font-bold text-emerald-600">
                      {results.quality_score}%
                    </span>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="text-3xl">
                      {results.grade === 'A' ? '🌟' : results.grade === 'B' ? '⭐' : '⚠️'}
                    </span>
                    <div>
                      <p className="text-lg font-semibold text-gray-800">
                        Grade: {results.grade}
                      </p>
                      <p className="text-sm text-gray-600">
                        Status: <span className={results.status === 'approved' ? 'text-green-600 font-bold' : 'text-red-600 font-bold'}>
                          {results.status.toUpperCase()}
                        </span>
                      </p>
                    </div>
                  </div>
                </div>

                {results.issues && results.issues.length > 0 && (
                  <div className="bg-yellow-50 p-4 rounded-xl border-2 border-yellow-200">
                    <h4 className="font-bold text-yellow-800 mb-2">⚠️ Issues Detected:</h4>
                    <ul className="space-y-2">
                      {results.issues.map((issue, idx) => (
                        <li key={idx} className="text-yellow-700">
                          • {issue.type} - Severity: {issue.severity} ({(issue.confidence * 100).toFixed(1)}%)
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            ) : (
              // Object Detection Results
              <div className="space-y-4">
                {results.image_with_boxes && (
                  <img
                    src={`data:image/jpeg;base64,${results.image_with_boxes}`}
                    alt="Detection result"
                    className="w-full rounded-xl border-2 border-emerald-200"
                  />
                )}

                <div className="bg-emerald-50 p-4 rounded-xl border-2 border-emerald-200">
                  <p className="text-lg font-bold text-emerald-800 mb-3">
                    🎯 Detected {results.count} object{results.count !== 1 ? 's' : ''}
                  </p>

                  <div className="space-y-2 max-h-64 overflow-y-auto">
                    {results.detections && results.detections.map((det, idx) => (
                      <div
                        key={idx}
                        className="bg-white p-3 rounded-lg border border-emerald-200 flex items-center justify-between"
                      >
                        <div>
                          <p className="font-semibold text-gray-800">{det.class}</p>
                          <p className="text-sm text-gray-600">
                            Position: ({det.center.x}, {det.center.y})
                          </p>
                        </div>
                        <div className="text-right">
                          <p className="text-lg font-bold text-emerald-600">
                            {(det.confidence * 100).toFixed(1)}%
                          </p>
                          <p className="text-xs text-gray-500">confidence</p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default ObjectDetection;
