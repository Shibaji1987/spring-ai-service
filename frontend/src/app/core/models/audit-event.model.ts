export interface AuditEvent {

  eventId: string;

  eventType: string;

  actor: string;

  sourceSystem: string;

  timestamp: string;

  status: string;

  payload: Record<string, any>;

}
