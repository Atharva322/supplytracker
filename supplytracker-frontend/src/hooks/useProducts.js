import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getProducts, createProduct, updateProduct, deleteProduct } from '../api';

// React Query hook for products
export function useProducts(page = 0, size = 10, sortBy = 'name', sortDir = 'asc') {
  return useQuery({
    queryKey: ['products', page, size, sortBy, sortDir],
    queryFn: () => getProducts(page, size, sortBy, sortDir),
    keepPreviousData: true,
  });
}

// Create product mutation
export function useCreateProduct() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: createProduct,
    onSuccess: () => {
      queryClient.invalidateQueries(['products']);
    },
  });
}

// Update product mutation
export function useUpdateProduct() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ id, product }) => updateProduct(id, product),
    onSuccess: () => {
      queryClient.invalidateQueries(['products']);
    },
  });
}

// Delete product mutation
export function useDeleteProduct() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: deleteProduct,
    onSuccess: () => {
      queryClient.invalidateQueries(['products']);
    },
  });
}
