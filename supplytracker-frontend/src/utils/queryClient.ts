import { QueryClient } from '@tanstack/react-query';

// Configure React Query client
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      gcTime: 10 * 60 * 1000, // 10 minutes
      retry: 1,
      refetchOnWindowFocus: false,
    },
    mutations: {
      onError: (error: Error) => {
        console.error('Mutation error:', error);
      },
    },
  },
});
