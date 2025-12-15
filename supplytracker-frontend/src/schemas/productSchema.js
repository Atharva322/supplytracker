import { z } from 'zod';

// Product form validation schema
export const productSchema = z.object({
  name: z.string()
    .min(2, 'Product name must be at least 2 characters')
    .max(100, 'Product name must be less than 100 characters'),
  
  type: z.string()
    .min(2, 'Product type must be at least 2 characters')
    .max(50, 'Product type must be less than 50 characters'),
  
  batchId: z.string()
    .min(3, 'Batch ID must be at least 3 characters')
    .max(50, 'Batch ID must be less than 50 characters'),
  
  harvestDate: z.string()
    .min(1, 'Harvest date is required'),
  
  originFarmId: z.string()
    .min(1, 'Origin farm ID is required'),
  
  destination: z.string()
    .min(2, 'Destination must be at least 2 characters')
    .max(100, 'Destination must be less than 100 characters'),
  
  status: z.enum(['AT_FARM', 'IN_TRANSIT', 'AT_WAREHOUSE', 'DELIVERED'], {
    errorMap: () => ({ message: 'Please select a valid status' })
  })
});

// Login form validation schema
export const loginSchema = z.object({
  username: z.string()
    .min(3, 'Username must be at least 3 characters')
    .max(50, 'Username must be less than 50 characters'),
  
  password: z.string()
    .min(6, 'Password must be at least 6 characters')
    .max(100, 'Password must be less than 100 characters')
});

// Register form validation schema
export const registerSchema = z.object({
  username: z.string()
    .min(3, 'Username must be at least 3 characters')
    .max(50, 'Username must be less than 50 characters'),
  
  email: z.string()
    .email('Please enter a valid email address'),
  
  password: z.string()
    .min(6, 'Password must be at least 6 characters')
    .max(100, 'Password must be less than 100 characters')
    .regex(/[A-Z]/, 'Password must contain at least one uppercase letter')
    .regex(/[a-z]/, 'Password must contain at least one lowercase letter')
    .regex(/[0-9]/, 'Password must contain at least one number')
});

// Farm form validation schema
export const farmSchema = z.object({
  name: z.string()
    .min(2, 'Farm name must be at least 2 characters')
    .max(100, 'Farm name must be less than 100 characters'),
  
  location: z.string()
    .min(2, 'Location must be at least 2 characters')
    .max(200, 'Location must be less than 200 characters'),
  
  owner: z.string()
    .min(2, 'Owner name must be at least 2 characters')
    .max(100, 'Owner name must be less than 100 characters'),
  
  contactInfo: z.string()
    .min(5, 'Contact info must be at least 5 characters')
    .max(100, 'Contact info must be less than 100 characters'),
  
  description: z.string()
    .max(500, 'Description must be less than 500 characters')
    .optional()
});
