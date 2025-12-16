# GraphQL API Documentation

This project includes a fully functional GraphQL API alongside the existing REST API. GraphQL provides a flexible and efficient way to query and mutate data.

## 🚀 Quick Start

### Access GraphQL Playground

Once the backend is running, visit:
```
http://localhost:8080/graphiql
```

This provides an interactive GraphQL IDE with:
- Auto-completion
- Schema documentation
- Query history
- Real-time subscriptions

### GraphQL Endpoint

```
POST http://localhost:8080/graphql
```

## 📊 Schema Overview

### Queries (Read Operations)

#### Get All Products
```graphql
query GetAllProducts {
  products {
    id
    name
    type
    status
    currentLocation
    trackingHistory {
      stage
      location
      timestamp
    }
  }
}
```

#### Get Single Product
```graphql
query GetProduct($id: ID!) {
  product(id: $id) {
    id
    name
    type
    batchId
    harvestDate
    originFarmId
    originFarmName
    currentLocation
    destination
    status
    trackingHistory {
      stage
      location
      timestamp
      description
      handledBy
    }
  }
}
```

#### Search Products
```graphql
query SearchProducts($keyword: String!) {
  searchProducts(keyword: $keyword) {
    id
    name
    type
    status
    currentLocation
  }
}
```

#### Filter by Status
```graphql
query ProductsByStatus($status: String!) {
  productsByStatus(status: $status) {
    id
    name
    currentLocation
    destination
    status
  }
}
```

#### Filter by Farm
```graphql
query ProductsByFarm($farmId: String!) {
  productsByFarm(farmId: $farmId) {
    id
    name
    type
    originFarmName
    status
  }
}
```

### Mutations (Write Operations)

#### Create Product
```graphql
mutation CreateProduct($input: ProductInput!) {
  createProduct(input: $input) {
    id
    name
    type
    batchId
    status
  }
}

# Variables:
{
  "input": {
    "name": "Fresh Apples",
    "type": "Fruit",
    "batchId": "BATCH-2024-001",
    "harvestDate": "2024-12-15",
    "originFarmId": "farm123",
    "originFarmName": "Green Valley Farm",
    "currentLocation": "Farm Storage",
    "status": "AT_FARM"
  }
}
```

#### Update Product
```graphql
mutation UpdateProduct($id: ID!, $input: ProductInput!) {
  updateProduct(id: $id, input: $input) {
    id
    name
    status
    currentLocation
  }
}

# Variables:
{
  "id": "67890",
  "input": {
    "name": "Premium Apples",
    "status": "IN_TRANSIT",
    "currentLocation": "Highway 101"
  }
}
```

#### Update Product Status
```graphql
mutation UpdateStatus($id: ID!, $status: String!, $location: String) {
  updateProductStatus(id: $id, status: $status, location: $location) {
    id
    status
    currentLocation
  }
}

# Variables:
{
  "id": "67890",
  "status": "DELIVERED",
  "location": "Mumbai Retail Store"
}
```

#### Add Tracking Stage
```graphql
mutation AddTracking($productId: ID!, $stage: TrackingStageInput!) {
  addTrackingStage(productId: $productId, stage: $stage) {
    id
    trackingHistory {
      stage
      location
      timestamp
      description
      handledBy
    }
  }
}

# Variables:
{
  "productId": "67890",
  "stage": {
    "stage": "Loaded onto Truck",
    "location": "Distribution Center",
    "timestamp": "2024-12-15T10:30:00",
    "description": "Loaded 50 boxes",
    "handledBy": "John Doe"
  }
}
```

#### Delete Product (Admin Only)
```graphql
mutation DeleteProduct($id: ID!) {
  deleteProduct(id: $id)
}

# Variables:
{
  "id": "67890"
}
```

### Subscriptions (Real-time Updates)

#### Subscribe to Product Updates
```graphql
subscription OnProductUpdate {
  productUpdated {
    id
    name
    status
    currentLocation
    trackingHistory {
      stage
      location
      timestamp
    }
  }
}
```

#### Subscribe to New Products
```graphql
subscription OnProductCreated {
  productCreated {
    id
    name
    type
    status
    originFarmName
  }
}
```

#### Subscribe to Status Changes
```graphql
subscription OnStatusChange($productId: ID) {
  productStatusChanged(productId: $productId) {
    productId
    oldStatus
    newStatus
    location
    timestamp
  }
}

# Variables (optional - filter by product ID):
{
  "productId": "67890"
}
```

## 🔐 Authentication

GraphQL endpoints use the same JWT authentication as REST APIs. Include the token in the Authorization header:

```javascript
fetch('http://localhost:8080/graphql', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${your_jwt_token}`
  },
  body: JSON.stringify({
    query: `
      query {
        products {
          id
          name
        }
      }
    `
  })
})
```

## 📱 Using GraphQL in React

### Example: Fetch Products
```javascript
const fetchProducts = async () => {
  const response = await fetch('http://localhost:8080/graphql', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    },
    body: JSON.stringify({
      query: `
        query {
          products {
            id
            name
            type
            status
            currentLocation
          }
        }
      `
    })
  });

  const { data } = await response.json();
  return data.products;
};
```

### Example: Create Product with Variables
```javascript
const createProduct = async (productData) => {
  const response = await fetch('http://localhost:8080/graphql', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    },
    body: JSON.stringify({
      query: `
        mutation CreateProduct($input: ProductInput!) {
          createProduct(input: $input) {
            id
            name
            status
          }
        }
      `,
      variables: {
        input: productData
      }
    })
  });

  const { data } = await response.json();
  return data.createProduct;
};
```

### Example: WebSocket Subscription
```javascript
import { Client } from 'graphql-ws';

const client = new Client({
  url: 'ws://localhost:8080/graphql',
  connectionParams: {
    Authorization: `Bearer ${localStorage.getItem('token')}`
  }
});

client.subscribe({
  query: `
    subscription {
      productUpdated {
        id
        name
        status
      }
    }
  `
}, {
  next: (data) => {
    console.log('Product updated:', data);
  },
  error: (error) => {
    console.error('Subscription error:', error);
  },
  complete: () => {
    console.log('Subscription complete');
  }
});
```

## 🎯 Advantages of GraphQL

1. **Precise Data Fetching**: Request only the fields you need
2. **Single Request**: Get multiple resources in one query
3. **Strong Typing**: Schema-based with built-in validation
4. **Real-time Updates**: WebSocket subscriptions for live data
5. **Self-Documenting**: Schema serves as API documentation
6. **Versioning**: No need for API versioning like REST

## 🛠️ GraphQL vs REST

| Feature | GraphQL | REST |
|---------|---------|------|
| Data Fetching | Single endpoint, flexible queries | Multiple endpoints, fixed responses |
| Over-fetching | ❌ No - request only what you need | ✅ Yes - get entire resource |
| Under-fetching | ❌ No - get related data in one query | ✅ Yes - need multiple requests |
| Real-time | ✅ Built-in subscriptions | ❌ Requires separate WebSocket setup |
| Documentation | ✅ Auto-generated from schema | ❌ Manual API docs |
| Caching | More complex | Simpler with HTTP caching |

## 📚 Resources

- **GraphiQL Playground**: `http://localhost:8080/graphiql`
- **GraphQL Endpoint**: `http://localhost:8080/graphql`
- **GraphQL UI Component**: Available in React app under "GraphQL" tab
- **Official Docs**: https://graphql.org/

## 🔧 Development Tips

1. **Use GraphiQL for development** - It provides better DX with auto-completion
2. **Use fragments** for reusable field selections
3. **Name your queries** for better debugging
4. **Use variables** instead of string interpolation
5. **Leverage subscriptions** for real-time features
6. **Monitor performance** - GraphQL can be expensive if not optimized

## 🐛 Troubleshooting

### CORS Issues
Make sure frontend origin is in the CORS configuration:
```properties
# application.properties
spring.graphql.cors.allowed-origins=http://localhost:5173,http://localhost:5174
```

### Authentication Errors
Ensure JWT token is included in the Authorization header:
```javascript
'Authorization': `Bearer ${localStorage.getItem('token')}`
```

### Subscription Connection Issues
Check WebSocket configuration and ensure the GraphQL endpoint supports WebSocket:
```
ws://localhost:8080/graphql
```

## 📝 Example Use Cases

1. **Mobile App**: Fetch only required fields to reduce bandwidth
2. **Dashboard**: Get aggregated data from multiple sources in one query
3. **Real-time Tracking**: Use subscriptions for live product updates
4. **Admin Panel**: Complex queries with nested relationships
5. **Analytics**: Flexible querying for custom reports

---

**Happy coding with GraphQL!** 🚀
