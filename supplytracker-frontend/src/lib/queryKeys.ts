export const queryKeys = {
  products: (page?: number, size?: number, sortBy?: string, sortDir?: string) => ['products', page, size, sortBy, sortDir] as const,
  productRoot: ['products'] as const,
  farms: ['farms'] as const,
  organizations: ['organizations'] as const,
  batches: (organizationId: string) => ['batches', organizationId] as const,
  inspectionJobs: (organizationId: string) => ['inspectionJobs', organizationId] as const,
  lineage: (batchId: string) => ['lineage', batchId] as const,
  recalls: (organizationId: string) => ['recalls', organizationId] as const,
};
