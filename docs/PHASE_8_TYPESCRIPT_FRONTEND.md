# Phase 8 TypeScript Operational Frontend

Phase 8 starts the frontend migration from a JavaScript demo shell toward a typed operator product.

## TypeScript Foundation

- Added `tsconfig.json` with strict TypeScript checking for TypeScript source.
- Added `npm run typecheck`.
- Added Vite environment typings.
- Migrated the app entry point to `src/main.tsx`.
- Migrated API and React Query support modules to TypeScript.

## Centralized Frontend Contracts

- `src/types/api.ts` defines typed REST contracts for products, farms, organizations, facilities, batches, inspection jobs, lineage edges, and recall cases.
- `src/lib/auth.ts` centralizes local auth state parsing, role checks, and auth clearing.
- `src/lib/apiErrors.ts` normalizes Axios failures into stable UI-safe error codes.
- `src/lib/queryKeys.ts` centralizes TanStack Query keys.
- `src/lib/realtime.ts` centralizes API and WebSocket base URL derivation.

## Operator Console

Added `src/features/operations/OperatorConsole.tsx`, a typed operational screen that can:

- Load batches by organization.
- Load inspection review jobs by organization.
- Load recall cases by organization.
- Load lineage edges for a source batch.
- Create a simulation recall without notifying real users.

The console is reachable from the existing app tab rail as `Operations`.

## Verification

- `cd supplytracker-frontend && npm run typecheck`
- `cd supplytracker-frontend && npm run lint`
- `cd supplytracker-frontend && npm test`
- `cd supplytracker-frontend && npm run build`
