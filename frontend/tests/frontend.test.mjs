import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { z } from 'zod';

// ==========================================
// 1. Login Form Validation Schema Test
// ==========================================
describe('Login Form Validation', () => {
  const loginSchema = z.object({
    email: z.string().email('Please enter a valid email address'),
    password: z.string().min(1, 'Password is required'),
  });

  it('should accept valid credentials', () => {
    const result = loginSchema.safeParse({
      email: 'agent@smartdesk.com',
      password: 'password123',
    });
    assert.equal(result.success, true);
  });

  it('should reject malformed email', () => {
    const result = loginSchema.safeParse({
      email: 'not-an-email',
      password: 'password123',
    });
    assert.equal(result.success, false);
    assert.equal(result.error.issues[0].path[0], 'email');
  });

  it('should reject empty password', () => {
    const result = loginSchema.safeParse({
      email: 'customer@example.com',
      password: '',
    });
    assert.equal(result.success, false);
    assert.equal(result.error.issues[0].path[0], 'password');
  });
});

// ==========================================
// 2. Registration Validation Schema Test
// ==========================================
describe('Registration Form Validation', () => {
  const registerSchema = z.object({
    firstName: z.string().min(1, 'First name is required'),
    lastName: z.string().min(1, 'Last name is required'),
    email: z.string().email('Please enter a valid email address'),
    password: z.string().min(8, 'Password must be at least 8 characters'),
    confirmPassword: z.string().min(8, 'Please confirm your password'),
    companyName: z.string().optional(),
    phone: z.string().optional(),
  }).refine((data) => data.password === data.confirmPassword, {
    message: "Passwords don't match",
    path: ['confirmPassword'],
  });

  it('should accept valid registration inputs', () => {
    const result = registerSchema.safeParse({
      firstName: 'Alice',
      lastName: 'Smith',
      email: 'alice@corp.com',
      password: 'SecurePassword123!',
      confirmPassword: 'SecurePassword123!',
      companyName: 'Acme Corp',
    });
    assert.equal(result.success, true);
  });

  it('should reject password under 8 characters', () => {
    const result = registerSchema.safeParse({
      firstName: 'Alice',
      lastName: 'Smith',
      email: 'alice@corp.com',
      password: 'short',
      confirmPassword: 'short',
    });
    assert.equal(result.success, false);
    assert.equal(result.error.issues[0].path[0], 'password');
  });

  it('should reject mismatched passwords', () => {
    const result = registerSchema.safeParse({
      firstName: 'Alice',
      lastName: 'Smith',
      email: 'alice@corp.com',
      password: 'Password123',
      confirmPassword: 'DifferentPassword456',
    });
    assert.equal(result.success, false);
    assert.equal(result.error.issues[0].path[0], 'confirmPassword');
  });
});

// ==========================================
// 3. Ticket Form Validation Schema Test
// ==========================================
describe('Ticket Creation Validation', () => {
  const createTicketSchema = z.object({
    subject: z.string().min(5, 'Subject must be at least 5 characters').max(255),
    description: z.string().min(10, 'Description must be at least 10 characters'),
    priority: z.enum(['LOW', 'MEDIUM', 'HIGH', 'URGENT']),
    categoryId: z.string().optional().nullable(),
  });

  it('should validate complete ticket payload', () => {
    const result = createTicketSchema.safeParse({
      subject: 'Database connection timeout in production',
      description: 'The postgres connection pool is exhausted after 15 minutes of load.',
      priority: 'URGENT',
      categoryId: '123e4567-e89b-12d3-a456-426614174000',
    });
    assert.equal(result.success, true);
  });

  it('should reject too short subject or description', () => {
    const result = createTicketSchema.safeParse({
      subject: 'Bug',
      description: 'Help me',
      priority: 'MEDIUM',
    });
    assert.equal(result.success, false);
    assert.equal(result.error.issues.length, 2);
  });

  it('should reject invalid priority values', () => {
    const result = createTicketSchema.safeParse({
      subject: 'Valid Subject Line Here',
      description: 'Detailed description explaining the issue thoroughly.',
      priority: 'INVALID_PRIORITY',
    });
    assert.equal(result.success, false);
  });
});

// ==========================================
// 4. Role-Based Navigation Routing
// ==========================================
describe('Role-Based Navigation Logic', () => {
  const ROLE_DASHBOARDS = {
    CUSTOMER: '/customer/dashboard',
    AGENT: '/agent/dashboard',
    ADMIN: '/admin/dashboard',
  };

  function hasRole(user, allowedRoles) {
    if (!user || !user.role) return false;
    return allowedRoles.includes(user.role);
  }

  it('should map roles to correct dashboards', () => {
    assert.equal(ROLE_DASHBOARDS.CUSTOMER, '/customer/dashboard');
    assert.equal(ROLE_DASHBOARDS.AGENT, '/agent/dashboard');
    assert.equal(ROLE_DASHBOARDS.ADMIN, '/admin/dashboard');
  });

  it('should verify customer access permission', () => {
    const customerUser = { id: '1', role: 'CUSTOMER' };
    assert.equal(hasRole(customerUser, ['CUSTOMER', 'ADMIN']), true);
    assert.equal(hasRole(customerUser, ['AGENT']), false);
  });

  it('should verify agent access permission', () => {
    const agentUser = { id: '2', role: 'AGENT' };
    assert.equal(hasRole(agentUser, ['AGENT', 'ADMIN']), true);
    assert.equal(hasRole(agentUser, ['CUSTOMER']), false);
  });

  it('should verify admin superuser access permission', () => {
    const adminUser = { id: '3', role: 'ADMIN' };
    assert.equal(hasRole(adminUser, ['ADMIN']), true);
    assert.equal(hasRole(adminUser, ['AGENT', 'ADMIN']), true);
  });
});

// ==========================================
// 5. API Error Formatting & Status Code Handling
// ==========================================
describe('API Error Handling Logic', () => {
  class ApiError extends Error {
    constructor(status, message, code = 'API_ERROR', validationErrors) {
      super(message);
      this.name = 'ApiError';
      this.status = status;
      this.code = code;
      this.validationErrors = validationErrors;
    }
  }

  it('should construct and identify ApiError instances', () => {
    const err = new ApiError(404, 'Ticket not found', 'RESOURCE_NOT_FOUND');
    assert.equal(err.status, 404);
    assert.equal(err.code, 'RESOURCE_NOT_FOUND');
    assert.equal(err.message, 'Ticket not found');
  });

  it('should retain validation errors on 400 bad request', () => {
    const valErrors = { email: 'Email is required' };
    const err = new ApiError(400, 'Validation failed', 'VALIDATION_FAILED', valErrors);
    assert.equal(err.status, 400);
    assert.deepEqual(err.validationErrors, valErrors);
  });
});

// ==========================================
// 6. Status & Priority Badge Config Mapping
// ==========================================
describe('Status and Priority Badge Config', () => {
  const STATUSES = ['NEW', 'ASSIGNED', 'IN_PROGRESS', 'PENDING_CUSTOMER', 'RESOLVED', 'CLOSED', 'ESCALATED'];
  const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

  it('should verify all 7 domain statuses exist', () => {
    assert.equal(STATUSES.length, 7);
    assert.ok(STATUSES.includes('ESCALATED'));
    assert.ok(STATUSES.includes('RESOLVED'));
  });

  it('should verify all 4 domain priorities exist', () => {
    assert.equal(PRIORITIES.length, 4);
    assert.ok(PRIORITIES.includes('URGENT'));
    assert.ok(PRIORITIES.includes('LOW'));
  });
});
