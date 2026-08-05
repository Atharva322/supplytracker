import test from 'node:test';
import assert from 'node:assert/strict';

import { loginSchema, productSchema, registerSchema } from './productSchema.js';

const validProduct = {
  name: 'Fresh Apples',
  type: 'Fruit',
  batchId: 'BATCH-001',
  harvestDate: '2026-08-04',
  originFarmId: 'FARM-001',
  destination: 'Seattle Distribution Center',
  status: 'AT_FARM',
};

test('product schema accepts the current product form contract', () => {
  assert.equal(productSchema.safeParse(validProduct).success, true);
});

test('product schema rejects unsupported lifecycle status', () => {
  assert.equal(productSchema.safeParse({ ...validProduct, status: 'UNKNOWN_STATUS' }).success, false);
});

test('authentication schemas preserve current input requirements', () => {
  assert.equal(loginSchema.safeParse({ username: 'user123', password: 'secret1' }).success, true);
  assert.equal(
    registerSchema.safeParse({ username: 'user123', email: 'bad-email', password: 'Secret1' }).success,
    false,
  );
  assert.equal(
    registerSchema.safeParse({ username: 'user123', email: 'user@example.com', password: 'Short1' }).success,
    false,
  );
});
