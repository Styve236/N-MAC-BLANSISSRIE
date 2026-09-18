export interface NotificationDTO {
  id: number;
  message: string;
  commandeId?: number;
  numeroTicket?: string;
  type: string;
  dateCreation: string;
  lue: boolean;
}

export const LIBELLE_TYPE_NOTIFICATION: Record<string, string> = {
  NOUVELLE_COMMANDE: 'Nouvelle commande',
};