export type ProductStatus = 'AT_FARM' | 'IN_TRANSIT' | 'AT_WAREHOUSE' | 'DELIVERED';

export interface TrackingStage {
  stage?: string;
  location?: string;
  timestamp?: string;
  notes?: string;
  [key: string]: unknown;
}

export interface Product {
  id: string;
  name: string;
  type: string;
  batchId: string;
  harvestDate: string;
  originFarmId: string;
  destination?: string;
  status: ProductStatus;
  qualityStatus?: string;
  trackingHistory?: TrackingStage[];
  [key: string]: unknown;
}

export type ProductWriteRequest = Omit<Product, 'id'> & { id?: string };

export interface ProductPage {
  content: Product[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  [key: string]: unknown;
}

export interface Farm {
  id: string;
  name: string;
  location: string;
  owner: string;
  contactInfo: string;
  description?: string;
}

export type FarmWriteRequest = Omit<Farm, 'id'> & { id?: string };

export interface DashboardStats {
  totalProducts?: number;
  inTransit?: number;
  delivered?: number;
  atFarm?: number;
  atWarehouse?: number;
  [key: string]: unknown;
}

export interface AuthResponse {
  token: string;
  username?: string;
  roles?: string[] | string;
  [key: string]: unknown;
}

export interface Organization {
  id: string;
  name: string;
  slug: string;
  createdAt?: string;
}

export interface Facility {
  id: string;
  organizationId: string;
  code: string;
  name: string;
  type: string;
  address?: string;
}

export interface ProductBatch {
  id?: string;
  batchId: string;
  organizationId: string;
  productName: string;
  productType: string;
  quantity: number | string;
  unit: string;
  harvestDate?: string;
  currentFacilityId?: string;
  custodianOrganizationId?: string;
  pendingCustodianOrganizationId?: string;
  status?: string;
  qualityStatus?: string;
  version?: number;
}

export interface InspectionJob {
  id: string;
  organizationId: string;
  batchId?: string;
  status: string;
  objectKey: string;
  inputChecksum: string;
  modelVersion?: string;
  datasetVersion?: string;
  automatedDecision?: string;
  finalDecision?: string;
  qualityScore?: number;
  qualityBand?: string;
  labels?: string[];
}

export interface UploadSlot {
  objectKey: string;
  uploadUrl: string;
  expiresAt: string;
  maxSizeBytes: number;
}

export interface LineageEdge {
  id?: string;
  parentBatchId: string;
  childBatchId: string;
  operation: 'SPLIT' | 'MERGE' | 'DERIVE' | 'CONSUME';
  organizationId: string;
  quantity: number | string;
  unit: string;
}

export interface RecallCase {
  id: string;
  sourceBatchId: string;
  organizationId: string;
  reason: string;
  simulation: boolean;
  status: 'OPEN' | 'RESOLVED';
  scope: {
    affectedBatchIds: string[];
    affectedShipmentIds: string[];
    affectedFacilityIds: string[];
    affectedOrganizationIds: string[];
    inventoryHolderOrganizationIds: string[];
    recipientOrganizationIds: string[];
    explanations: Record<string, string>;
  };
}
