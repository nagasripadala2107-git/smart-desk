// ==========================================
// SmartDesk Domain Enums (Matching Java Enums)
// ==========================================

export type UserRole = 'CUSTOMER' | 'AGENT' | 'ADMIN';

export type TicketStatus =
  | 'OPEN'
  | 'NEW'
  | 'ASSIGNED'
  | 'IN_PROGRESS'
  | 'PENDING_CUSTOMER'
  | 'PENDING_INTERNAL'
  | 'RESOLVED'
  | 'CLOSED'
  | 'ESCALATED';

export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export type AgentAvailability = 'AVAILABLE' | 'BUSY' | 'AWAY' | 'OFFLINE';

export type EventType =
  | 'CREATED'
  | 'STATUS_CHANGED'
  | 'PRIORITY_CHANGED'
  | 'CATEGORY_CHANGED'
  | 'ASSIGNED'
  | 'REASSIGNED'
  | 'ESCALATED'
  | 'RESOLVED'
  | 'REOPENED'
  | 'CLOSED'
  | 'NOTE_ADDED';

export type EscalationStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'RESOLVED';

// ==========================================
// Common API Envelope Types
// ==========================================

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface ApiErrorResponse {
  status: number;
  error: string;
  message: string;
  path: string;
  timestamp: string;
  validationErrors?: Record<string, string>;
}

// ==========================================
// Auth DTOs
// ==========================================

export interface UserResponse {
  id: string;
  email: string;
  role: UserRole;
  isActive: boolean;
  firstName: string;
  lastName: string;
  customerCode?: string | null;
  employeeCode?: string | null;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  user: UserResponse;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  role: UserRole;
  firstName: string;
  lastName: string;
  phone?: string;
  companyName?: string;
  employeeCode?: string;
  teamName?: string;
}

// ==========================================
// Ticket DTOs
// ==========================================

export interface TicketSummaryResponse {
  id: string;
  ticketNumber: string;
  subject: string;
  customerName: string;
  categoryName?: string | null;
  priority: TicketPriority;
  status: TicketStatus;
  assignedAgentName?: string | null;
  assignedTeamName?: string | null;
  createdAt: string;
}

export interface TicketEventDto {
  id: string;
  actorName: string;
  eventType: EventType;
  oldValue?: string | null;
  newValue?: string | null;
  metadata?: string | null;
  createdAt: string;
}

export interface TicketDetailResponse {
  id: string;
  ticketNumber: string;
  customerId: string;
  customerName: string;
  customerCode?: string | null;
  companyName?: string | null;
  categoryId?: string | null;
  categoryName?: string | null;
  assignedAgentId?: string | null;
  assignedAgentName?: string | null;
  assignedTeamId?: string | null;
  assignedTeamName?: string | null;
  subject: string;
  description: string;
  priority: TicketPriority;
  status: TicketStatus;
  aiCategory?: string | null;
  aiConfidence?: number | null;
  aiModelVersion?: string | null;
  sentiment?: 'POSITIVE' | 'NEUTRAL' | 'NEGATIVE' | null;
  sentimentConfidence?: number | null;
  tone?: 'CALM' | 'FRUSTRATED' | 'URGENT' | 'ANGRY' | 'SATISFIED' | 'CONFUSED' | 'NEUTRAL' | string | null;
  sentimentModelVersion?: string | null;
  duplicateMatches?: DuplicateMatchDto[] | null;
  createdAt: string;
  updatedAt: string;
  resolvedAt?: string | null;
  closedAt?: string | null;
  messages: MessageResponse[];
  events: TicketEventDto[];
}

export interface DuplicateMatchDto {
  ticketId: string;
  ticketNumber: string;
  subject: string;
  similarityScore: number;
  modelVersion: string;
}


export interface TicketResponse {
  id: string;
  ticketNumber: string;
  customerId: string;
  categoryId?: string | null;
  assignedAgentId?: string | null;
  assignedTeamId?: string | null;
  subject: string;
  description: string;
  priority: TicketPriority;
  status: TicketStatus;
  aiCategory?: string | null;
  aiConfidence?: number | null;
  aiModelVersion?: string | null;
  createdAt: string;
  updatedAt: string;
  resolvedAt?: string | null;
  closedAt?: string | null;
}

export interface CreateTicketRequest {
  subject: string;
  description: string;
  priority: TicketPriority;
  categoryId?: string | null;
}

export interface UpdateTicketRequest {
  subject?: string;
  description?: string;
  priority?: TicketPriority;
  status?: TicketStatus;
  categoryId?: string | null;
  assignedAgentId?: string | null;
  assignedTeamId?: string | null;
  updateReason?: string;
}

// ==========================================
// Ticket Messages
// ==========================================

export interface MessageResponse {
  id: string;
  ticketId: string;
  senderId: string;
  senderName: string;
  senderRole: UserRole;
  message: string;
  internal: boolean;
  createdAt: string;
}

export interface CreateMessageRequest {
  message: string;
  isInternal?: boolean;
}

// ==========================================
// Escalation DTOs
// ==========================================

export interface EscalationRequest {
  targetTeamId: string;
  targetAgentId?: string | null;
  level: number;
  reason: string;
}

export interface EscalationResponse {
  id: string;
  ticketId: string;
  ticketNumber: string;
  fromTeamId?: string | null;
  fromTeamName?: string | null;
  toTeamId: string;
  toTeamName: string;
  fromAgentId?: string | null;
  fromAgentName?: string | null;
  toAgentId?: string | null;
  toAgentName?: string | null;
  level: number;
  reason: string;
  status: EscalationStatus;
  createdAt: string;
  resolvedAt?: string | null;
}

// ==========================================
// Customer & Agent Profiles
// ==========================================

export interface CustomerProfileResponse {
  customerId: string;
  userId: string;
  customerCode: string;
  companyName: string;
  plan: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string | null;
  avatarUrl?: string | null;
  totalTickets: number;
  activeTickets: number;
}

export interface AgentResponse {
  id: string;
  userId: string;
  employeeCode: string;
  firstName: string;
  lastName: string;
  email: string;
  teamId?: string | null;
  teamName?: string | null;
  availabilityStatus: AgentAvailability;
  skills?: string | null;
  maxActiveTickets: number;
  currentActiveTickets: number;
}

export interface AgentQueueResponse {
  agentId: string;
  employeeCode: string;
  agentName: string;
  teamId?: string | null;
  teamName?: string | null;
  assignedCount: number;
  assignedTickets: TicketSummaryResponse[];
  teamQueueTickets: TicketSummaryResponse[];
}

// ==========================================
// Master Data: Teams & Categories
// ==========================================

export interface TeamResponse {
  id: string;
  name: string;
  description?: string | null;
  isActive: boolean;
  agentCount: number;
}

export interface CategoryResponse {
  id: string;
  name: string;
  description?: string | null;
  isActive: boolean;
}

// ==========================================
// Analytics DTOs
// ==========================================

export interface AnalyticsOverviewResponse {
  totalTickets: number;
  openTickets: number;
  inProgressTickets: number;
  pendingTickets: number;
  escalatedTickets: number;
  resolvedTickets: number;
  closedTickets: number;
  avgResolutionMinutes?: number | null;
}

export interface TicketVolumeResponse {
  date: string;
  count: number;
}

export interface CategoryStatsResponse {
  categoryId: string;
  categoryName: string;
  ticketCount: number;
  percentage: number;
}

export interface PriorityStatsResponse {
  priority: TicketPriority;
  ticketCount: number;
  percentage: number;
}

export interface StatusStatsResponse {
  status: TicketStatus;
  ticketCount: number;
  percentage: number;
}

export interface TeamWorkloadResponse {
  teamId: string;
  teamName: string;
  activeTickets: number;
  resolvedClosedTickets: number;
  escalatedTickets: number;
}

export interface AgentPerformanceResponse {
  agentId: string;
  agentName: string;
  employeeCode: string;
  teamName: string;
  assignedTickets: number;
  openTickets: number;
  resolvedTickets: number;
  avgResolutionMinutes?: number | null;
}

export interface ResolutionTimeStatsResponse {
  avgResolutionMinutes?: number | null;
  minResolutionMinutes?: number | null;
  maxResolutionMinutes?: number | null;
  medianResolutionMinutes?: number | null;
  resolvedCount: number;
}

export interface EscalationSummaryDto {
  id: string;
  ticketNumber: string;
  level: number;
  fromTeamName: string;
  toTeamName: string;
  reason: string;
  status: string;
  createdAt: string;
}

export interface EscalationStatsResponse {
  totalEscalations: number;
  byLevel: Record<number, number>;
  byTeam: Record<string, number>;
  recentEscalations: EscalationSummaryDto[];
}

export interface SlaPolicyMetricDto {
  id: string;
  name: string;
  priority: TicketPriority;
  firstResponseMinutes: number;
  resolutionMinutes: number;
  isActive: boolean;
}

export interface SlaAnalyticsResponse {
  configuredPoliciesCount: number;
  ticketsPastResponseTargetCount: number;
  ticketsPastResolutionTargetCount: number;
  policies: SlaPolicyMetricDto[];
}
