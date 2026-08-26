import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getProducts, createProduct, updateProduct, deleteProduct } from '../api';
import { queryKeys } from '../lib/queryKeys';
import type { ProductWriteRequest } from '../types/api';

// React Query hook for products
export function useProducts(page = 0, size = 10, sortBy = 'name', sortDir: 'asc' | 'desc' = 'asc') {
  return useQuery({
    queryKey: queryKeys.products(page, size, sortBy, sortDir),
    queryFn: () => getProducts(page, size, sortBy, sortDir),
    placeholderData: keepPreviousData,
  });
}

// Create product mutation
export function useCreateProduct() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: createProduct,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.productRoot });
    },
  });
}

// Update product mutation
export function useUpdateProduct() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ id, product }: { id: string; product: ProductWriteRequest }) => updateProduct(id, product),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.productRoot });
    },
  });
}

// Delete product mutation
export function useDeleteProduct() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: deleteProduct,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.productRoot });
    },
  });
}
