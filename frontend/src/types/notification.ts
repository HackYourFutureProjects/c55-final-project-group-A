export type NotificationType =
  | "EVENT_CANCELLED"
  | "EVENT_UPDATED"
  | "EVENT_REMINDER"
  | "COMMENT_REPLY"
  | "NEW_FEEDBACK";

export interface AppNotification {
  id: string;
  type: NotificationType;
  title: string;
  body: string;
  resourceId: string;
  linkPath?: string | null;
  read: boolean;
  readAt?: string | null;
  createdAt: string;
}

export interface NotificationPage {
  notifications: AppNotification[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export interface UnreadCount {
  count: number;
}
