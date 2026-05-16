export interface AuditEvent {

  id?: string;

  eventId?: string;

  eventType: string;

  actor: string;

  action?: string;

  target?: string;

  sourceSystem?: string;

  eventTime?: string;

  timestamp?: string;

  status: string;

  metadata?: Record<string, any>;

  payload?: Record<string, any>;

}
