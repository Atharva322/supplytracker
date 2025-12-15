# Role-Based Access Control Implementation

## Overview
Successfully implemented comprehensive role-based access control system with stage-specific user profiles for the agricultural supply chain tracking application.

## New User Roles

### 1. **FARMER** 
- Can manage "Farm" stage
- Typically associated with a specific farm
- Tracks products at origin/harvest

### 2. **PROCESSOR**
- Can manage "Processing" and "Quality Check" stages
- Handles product transformation and quality assurance
- Located at processing facilities

### 3. **WAREHOUSE_MANAGER**
- Can manage "Warehouse" stage
- Handles inventory and storage
- Located at warehouse facilities

### 4. **DISTRIBUTOR**
- Can manage "Distribution" stage
- Handles logistics and transportation
- Manages product movement between locations

### 5. **RETAILER**
- Can manage "Retail" stage
- Final stage before consumer
- Located at retail outlets

### 6. **ADMIN** (existing)
- Full access to all stages
- Can manage users, farms, and all products
- System-wide administrative privileges

## Backend Changes

### 1. **User Model** (`User.java`)
Extended with new fields:
```java
private String stageProfile;        // e.g., "FARMER", "PROCESSOR"
private String associatedFarmId;     // For farmers - their farm ID
private String location;             // Physical location of user
```

### 2. **Product Model** (`Product.java`)
Extended with supply chain tracking fields:
```java
private String originFarmName;       // Name of origin farm
private String currentLocation;      // Current physical location
private String destination;          // Final destination
private String status;               // Current status/stage
private List<TrackingStage> trackingHistory;  // Complete journey history
```

### 3. **ProductController** (`ProductController.java`)
**Authorization Logic for Tracking Stages:**
```java
@PostMapping("/{id}/tracking")
public ResponseEntity<?> addTrackingStage(..., Authentication authentication) {
    // Extract user's role
    String userRole = authentication.getAuthorities()...;
    
    // Check permissions based on role and stage
    if (FARMER) -> Can add "Farm" stage only
    if (PROCESSOR) -> Can add "Processing" or "Quality Check" stages
    if (WAREHOUSE_MANAGER) -> Can add "Warehouse" stage only
    if (DISTRIBUTOR) -> Can add "Distribution" stage only
    if (RETAILER) -> Can add "Retail" stage only
    if (ADMIN) -> Can add any stage
    
    // Returns 403 if unauthorized
}
```

### 4. **AuthResponse & AuthController**
Updated to return user's stage profile information:
```java
AuthResponse {
    token, username, roles,
    stageProfile,        // NEW
    location,            // NEW
    associatedFarmId     // NEW
}
```

## Frontend Changes

### 1. **User Profile Display**
Header now shows:
- Username
- Role (Admin/User)
- **Stage Profile** (e.g., "Farmer", "Processor")
- **Location** (e.g., "Mumbai Warehouse")

### 2. **Tracking Stage Form**
- Dropdown now shows **only authorized stages** based on user's role
- Example: FARMER users only see "Farm" option
- Example: PROCESSOR users see "Processing" and "Quality Check"
- ADMIN sees all stages

### 3. **Product Form Enhancements**
- **Origin Farm**: Changed from text input to dropdown populated from farms list
- **Destination**: New field to track final destination
- Better validation and user experience

### 4. **Stage-Based Button Visibility**
- "Add New Tracking Stage" button visible to all stage-specific roles (not just ADMIN)
- Each role can only add their authorized stage types

## State Management

### Frontend State Variables
```javascript
const [userStageProfile, setUserStageProfile] = useState("");
const [userLocation, setUserLocation] = useState("");
const [userFarmId, setUserFarmId] = useState("");
```

### localStorage Persistence
```javascript
localStorage.setItem("stageProfile", response.stageProfile);
localStorage.setItem("location", response.location);
localStorage.setItem("associatedFarmId", response.associatedFarmId);
```

## Authorization Matrix

| Role | Farm | Processing | Quality Check | Warehouse | Distribution | Retail |
|------|------|-----------|---------------|-----------|--------------|--------|
| ADMIN | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| FARMER | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| PROCESSOR | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ |
| WAREHOUSE_MANAGER | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ |
| DISTRIBUTOR | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ |
| RETAILER | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |

## API Endpoints

### Updated Endpoints

**POST `/api/products/{id}/tracking`**
- Authorization: `ADMIN`, `FARMER`, `PROCESSOR`, `WAREHOUSE_MANAGER`, `DISTRIBUTOR`, `RETAILER`
- Validates role matches stage being added
- Returns 403 if unauthorized

**POST `/api/auth/login` & `/api/auth/register`**
- Now returns stage profile information
- Includes: `stageProfile`, `location`, `associatedFarmId`

## Testing Workflow

### 1. Create Test Users
```bash
# Use CREATE_ADMIN_USER.md and test-roles.ps1 to create users with different roles
```

### 2. Test Scenario
1. **Login as FARMER**
   - Add product with origin farm
   - Add "Farm" tracking stage ✅
   - Try to add "Processing" stage ❌ (should fail)

2. **Login as PROCESSOR**
   - View products
   - Add "Processing" or "Quality Check" stage ✅
   - Try to add "Distribution" stage ❌ (should fail)

3. **Login as DISTRIBUTOR**
   - View products in warehouse
   - Add "Distribution" stage ✅
   - Track delivery

4. **Login as RETAILER**
   - Receive products
   - Add "Retail" stage ✅
   - Final stage

5. **Login as ADMIN**
   - Full access to all operations ✅
   - Can add any stage
   - Manage farms, users, products

## Security Features

1. **Backend Validation**: Role-based authorization at API level
2. **Frontend Filtering**: UI shows only authorized options
3. **JWT Token**: Contains role information
4. **Error Handling**: Clear 403 Forbidden responses
5. **Audit Trail**: Complete tracking history with handler information

## Future Enhancements

1. **Notifications**: Alert next stage handler when product ready
2. **QR Codes**: Scan products to add tracking stages
3. **Mobile App**: Mobile interface for field workers
4. **Analytics**: Stage-specific performance metrics
5. **Geolocation**: Auto-fill location based on GPS
6. **Approval Workflow**: Require approval before stage transitions

## Files Modified

### Backend (Java/Spring Boot)
- `User.java` - Added stage profile fields
- `Product.java` - Added tracking fields
- `TrackingStage.java` - Already existed
- `ProductController.java` - Added role-based authorization
- `AuthResponse.java` - Extended DTO
- `AuthController.java` - Return profile data
- `Farm.java`, `FarmRepository.java`, `FarmController.java` - Already existed

### Frontend (React)
- `App.jsx` - Updated state management, tracking form, product form, user profile display
- `api.js` - No changes needed (already had tracking endpoints)

## Build & Run

```bash
# Backend
cd supplytracker1
mvn clean package -DskipTests
java -jar target/supplytracker-1.0-SNAPSHOT.jar

# Frontend
cd supplytracker-frontend
npm run dev
```

## Access Application
- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- MongoDB: localhost:27017

---

**Status**: ✅ Implementation Complete
**Backend**: ✅ Build Successful
**Frontend**: ✅ Running
**Both Servers**: ✅ Active

Next: Test with different user roles to verify permissions work correctly!
