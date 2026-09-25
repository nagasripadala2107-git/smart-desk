import { z } from 'zod';

export const loginSchema = z.object({
  email: z.string().email('Please enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
});

export type LoginFormValues = z.infer<typeof loginSchema>;

export const registerSchema = z.object({
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

export type RegisterFormValues = z.infer<typeof registerSchema>;

export const createTicketSchema = z.object({
  subject: z.string().min(5, 'Subject must be at least 5 characters').max(255, 'Subject cannot exceed 255 characters'),
  description: z.string().min(10, 'Description must be at least 10 characters'),
  priority: z.enum(['LOW', 'MEDIUM', 'HIGH', 'URGENT']),
  categoryId: z.string().optional().nullable(),
});

export type CreateTicketFormValues = z.infer<typeof createTicketSchema>;

export const updateTicketSchema = z.object({
  subject: z.string().optional(),
  description: z.string().optional(),
  priority: z.enum(['LOW', 'MEDIUM', 'HIGH', 'URGENT']).optional(),
  status: z.enum(['NEW', 'ASSIGNED', 'IN_PROGRESS', 'PENDING_CUSTOMER', 'RESOLVED', 'CLOSED', 'ESCALATED']).optional(),
  categoryId: z.string().optional().nullable(),
  assignedAgentId: z.string().optional().nullable(),
  assignedTeamId: z.string().optional().nullable(),
  updateReason: z.string().optional(),
});

export type UpdateTicketFormValues = z.infer<typeof updateTicketSchema>;

export const createMessageSchema = z.object({
  message: z.string().min(1, 'Message cannot be empty'),
  isInternal: z.boolean().default(false),
});

export type CreateMessageFormValues = z.infer<typeof createMessageSchema>;

export const escalationSchema = z.object({
  targetTeamId: z.string().min(1, 'Target team is required'),
  targetAgentId: z.string().optional().nullable(),
  level: z.coerce.number().int().min(1, 'Level must be at least 1').max(5, 'Level cannot exceed 5'),
  reason: z.string().min(5, 'Reason must be at least 5 characters'),
});

export type EscalationFormValues = z.infer<typeof escalationSchema>;
