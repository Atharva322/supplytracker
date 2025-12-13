# 🌾 Agri Supply Tracker

A full-stack agricultural supply chain management system built with React, Spring Boot, and MongoDB. Track products from farm to distribution with role-based access control, authentication, and comprehensive data management features.

## 📋 Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Running the Application](#running-the-application)
- [Usage Guide](#usage-guide)
- [API Documentation](#api-documentation)
- [Security & Authentication](#security--authentication)
- [Project Structure](#project-structure)
- [Contributing](#contributing)

## ✨ Features

### 🔐 Authentication & Authorization
- **JWT-based Authentication** - Secure login/register system
- **Google OAuth2 Sign-In** - One-click authentication with Google
- **Role-Based Access Control** (RBAC)
  - **Admin Role**: Full CRUD operations, CSV import, user management
  - **User Role**: Read-only access to product data
- **Session Management** - Persistent login with localStorage

### 📊 Product Management
- **CRUD Operations** - Create, Read, Update, Delete products
- **Advanced Search** - Search products by name
- **Smart Filtering** 
  - Filter by product type
  - Date range filtering (harvest date)
  - Combine multiple filters
- **Pagination** - Efficient data loading (10 items per page)

### 📁 Data Import/Export
- **CSV Export** - Export products with applied filters
- **CSV Import** (Admin only) - Bulk product upload
- **Sample CSV Templates** - Quick start with example data

### 🎨 User Interface
- **Modern UI** - Built with Tailwind CSS
- **Responsive Design** - Works on desktop and mobile
- **Role Badges** - Visual indicators for user roles (👑 Admin, 👤 User)
- **Conditional UI** - Features shown based on user permissions
- **Loading States** - Smooth user experience with loading indicators

## 🛠️ Tech Stack

### Frontend
- **React 19.2.0** - UI library
- **Vite 7.2.5** - Build tool and dev server
- **Tailwind CSS 3.4.14** - Utility-first CSS framework
- **Axios 1.13.2** - HTTP client

### Backend
- **Spring Boot 3.1.1** - Application framework
- **Spring Security** - Authentication and authorization
- **Spring Data MongoDB** - Database integration
- **Spring OAuth2 Client** - Google OAuth integration
- **JWT (jjwt 0.11.5)** - Token-based authentication
- **Maven** - Build and dependency management

### Database
- **MongoDB** - NoSQL database for flexible data storage

## 📦 Prerequisites

Before running this project, make sure you have:

- **Java 17** or higher
- **Maven 3.6+**
- **Node.js 18+** and npm
- **MongoDB 4.4+** (running on `localhost:27017`)
- **Google Cloud Console Account** (for OAuth2 - optional)

## 🚀 Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/Atharva322/supplytracker.git
cd supplytracker
```

### 2. Backend Setup

#### Configure MongoDB
Make sure MongoDB is running on `localhost:27017`. The application will automatically create the `agriproj` database.

#### Set Environment Variables

**Windows (PowerShell):**
```powershell
$env:GOOGLE_CLIENT_ID="your-google-client-id"
$env:GOOGLE_CLIENT_SECRET="your-google-client-secret"
$env:JWT_SECRET="mySecretKeyForJWTTokenGenerationAndValidation12345"
```

**Linux/Mac:**
```bash
export GOOGLE_CLIENT_ID="your-google-client-id"
export GOOGLE_CLIENT_SECRET="your-google-client-secret"
export JWT_SECRET="mySecretKeyForJWTTokenGenerationAndValidation12345"
```

> **Note:** See [GOOGLE_OAUTH_SETUP.md](GOOGLE_OAUTH_SETUP.md) for detailed OAuth2 setup instructions.

#### Build the Backend

```bash
cd supplytracker1
mvn clean package -DskipTests
```

### 3. Frontend Setup

```bash
cd supplytracker-frontend
npm install
```

## 🎯 Running the Application

### Start Backend Server

```bash
cd supplytracker1
java -jar target/supplytracker-1.0-SNAPSHOT.jar
```

The backend will start on **http://localhost:8080**

### Start Frontend Development Server

```bash
cd supplytracker-frontend
npm run dev
```

The frontend will start on **http://localhost:5173**

## 📖 Usage Guide

### First Time Setup

1. **Register an Account**
   - Open http://localhost:5173
   - Click "Register" 
   - Enter username, email, and password
   - Default role: `ROLE_USER` (read-only access)

2. **Create Admin Account** (Optional)
   - See [CREATE_ADMIN_USER.md](CREATE_ADMIN_USER.md) for instructions
   - Default admin credentials: `adminuser` / `admin123`

### Using the Application

#### As a User (ROLE_USER)
- ✅ View all products
- ✅ Search products by name
- ✅ Filter by type and date range
- ✅ Export data to CSV
- ✅ Navigate through pages
- ❌ Cannot create, edit, or delete products
- ❌ Cannot import CSV

#### As an Admin (ROLE_ADMIN)
- ✅ All user permissions
- ✅ Create new products
- ✅ Edit existing products
- ✅ Delete products
- ✅ Import products from CSV

### Google Sign-In

1. Click "Sign in with Google" button
2. Authenticate with your Google account
3. Automatically logged in with `ROLE_USER`

### CSV Import/Export

#### Export Products
1. Apply desired filters (optional)
2. Click "Export CSV" button
3. File downloads with current filtered data

#### Import Products (Admin Only)
1. Prepare CSV file with columns: `name,type,batchId,harvestDate,originFarmId`
2. Click "Import CSV" button
3. Select your CSV file
4. Products are created in bulk

**Sample CSV Format:**
```csv
name,type,batchId,harvestDate,originFarmId
Tomatoes,VEGETABLE,BATCH-001,2024-01-15,FARM-123
Potatoes,VEGETABLE,BATCH-002,2024-01-20,FARM-456
```

## 🔌 API Documentation

### Authentication Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/register` | Register new user | No |
| POST | `/api/auth/login` | Login with credentials | No |
| GET | `/oauth2/authorization/google` | Initiate Google OAuth | No |

### Product Endpoints

| Method | Endpoint | Description | Auth Required | Role |
|--------|----------|-------------|---------------|------|
| GET | `/api/products` | Get paginated products | Yes | Any |
| GET | `/api/products/search` | Search products by name | Yes | Any |
| POST | `/api/products` | Create new product | Yes | Admin |
| PUT | `/api/products/{id}` | Update product | Yes | Admin |
| DELETE | `/api/products/{id}` | Delete product | Yes | Admin |
| POST | `/api/products/import-csv` | Import from CSV | Yes | Admin |
| GET | `/api/products/export-csv` | Export to CSV | Yes | Any |

### Query Parameters

**Pagination:**
- `page` - Page number (0-indexed)
- `size` - Items per page (default: 10)

**Search:**
- `name` - Search term for product name

**Filters:**
- `type` - Product type (VEGETABLE, FRUIT, GRAIN, etc.)
- `startDate` - Filter from date (format: YYYY-MM-DD)
- `endDate` - Filter to date (format: YYYY-MM-DD)

**Example Request:**
```bash
GET /api/products?page=0&size=10&name=tomato&type=VEGETABLE&startDate=2024-01-01&endDate=2024-12-31
```

## 🔒 Security & Authentication

### JWT Authentication
- Tokens expire after 24 hours
- Token stored in localStorage
- Sent as `Authorization: Bearer <token>` header

### Password Security
- Passwords hashed with BCrypt
- Minimum password requirements enforced

### OAuth2 Security
- Google OAuth2 integration
- Secure state parameter validation
- Environment variable configuration for secrets

### CORS Configuration
- Allowed origins: `http://localhost:5173`, `http://localhost:3000`
- Credentials supported
- All HTTP methods allowed

## 📁 Project Structure

```
supplytracker/
├── supplytracker-frontend/          # React frontend
│   ├── src/
│   │   ├── App.jsx                  # Main application component
│   │   ├── api.js                   # API client with Axios
│   │   ├── main.jsx                 # Entry point
│   │   └── index.css                # Global styles
│   ├── public/
│   │   └── oauth-callback.html      # OAuth callback page
│   └── package.json
│
├── supplytracker1/                  # Spring Boot backend
│   ├── src/main/java/com/agri/supplytracker/
│   │   ├── config/
│   │   │   └── SecurityConfig.java           # Security configuration
│   │   ├── controller/
│   │   │   ├── ProductController.java        # Product REST API
│   │   │   └── AuthController.java           # Authentication API
│   │   ├── dto/                              # Data Transfer Objects
│   │   │   ├── AuthResponse.java
│   │   │   ├── LoginRequest.java
│   │   │   └── RegisterRequest.java
│   │   ├── exception/                        # Exception handlers
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   └── ProductNotFoundException.java
│   │   ├── model/
│   │   │   ├── Product.java                  # Product entity
│   │   │   └── User.java                     # User entity
│   │   ├── repository/
│   │   │   ├── ProductRepository.java        # Product data access
│   │   │   └── UserRepository.java           # User data access
│   │   ├── security/
│   │   │   ├── JwtUtil.java                  # JWT utilities
│   │   │   ├── JwtAuthenticationFilter.java  # JWT filter
│   │   │   ├── CustomUserDetailsService.java # User details service
│   │   │   └── OAuth2LoginSuccessHandler.java # OAuth handler
│   │   └── SupplytrackerApplication.java     # Main application
│   ├── src/main/resources/
│   │   └── application.properties            # Configuration
│   └── pom.xml                               # Maven dependencies
│
├── CREATE_ADMIN_USER.md             # Admin creation guide
├── GOOGLE_OAUTH_SETUP.md            # OAuth2 setup guide
├── sample-products.csv              # Sample data for import
├── env-config.md                    # Environment variables (not in git)
└── README.md                        # This file
```

## 🎨 UI Screenshots

### Login Page
- Username/password authentication
- Google Sign-In button
- Register link

### Product Dashboard
- Product list with pagination
- Search and filter controls
- Create/Edit/Delete buttons (admin only)
- CSV Import/Export buttons
- Role badge indicator

### Product Form
- Add/Edit product details
- Form validation
- Cancel and save options

## 🐛 Troubleshooting

### Backend Won't Start
- Check if MongoDB is running: `mongosh` or `mongo`
- Verify environment variables are set
- Check if port 8080 is available

### Frontend Build Issues
```bash
cd supplytracker-frontend
rm -rf node_modules package-lock.json
npm install
```

### OAuth2 Errors
- Verify redirect URIs in Google Cloud Console
- Check environment variables are correctly set
- See [GOOGLE_OAUTH_SETUP.md](GOOGLE_OAUTH_SETUP.md)

### CORS Errors
- Ensure frontend is running on `http://localhost:5173`
- Check SecurityConfig CORS configuration

## 🔄 Environment Variables Reference

Create a `.env` file or set these variables:

```properties
# Required for OAuth2 (optional feature)
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# JWT Configuration
JWT_SECRET=mySecretKeyForJWTTokenGenerationAndValidation12345

# MongoDB (optional - defaults shown)
MONGODB_URI=mongodb://localhost:27017/agriproj
```

## 📝 Sample Data

Use the provided [sample-products.csv](sample-products.csv) file to quickly populate your database:

```csv
name,type,batchId,harvestDate,originFarmId
Tomatoes,VEGETABLE,BATCH-001,2024-01-15,FARM-123
Potatoes,VEGETABLE,BATCH-002,2024-01-20,FARM-456
Carrots,VEGETABLE,BATCH-003,2024-01-25,FARM-789
Apples,FRUIT,BATCH-004,2024-02-01,FARM-123
Oranges,FRUIT,BATCH-005,2024-02-10,FARM-456
```

## 🚦 Development Workflow

1. Start MongoDB
2. Set environment variables
3. Start backend server
4. Start frontend dev server
5. Open browser to http://localhost:5173
6. Register/login and start managing products!

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is open source and available under the MIT License.

## 👨‍💻 Author

**Atharva**
- GitHub: [@Atharva322](https://github.com/Atharva322)

## 🙏 Acknowledgments

- Spring Boot team for the amazing framework
- React team for the powerful UI library
- MongoDB for flexible data storage
- Tailwind CSS for beautiful styling

---

**Built with ❤️ for the agricultural supply chain industry**
