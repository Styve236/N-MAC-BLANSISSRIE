export interface SmsNotificationDTO {
  id?: number;
  type?: string;
  telephone?: string;
  message?: string;
  statut?: string;
  dateEnvoi?: string;
}