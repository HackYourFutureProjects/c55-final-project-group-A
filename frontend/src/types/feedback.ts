export type FeedbackTopic = "app" | "event";

// what the public form sends
export interface PostFeedbackRequest {
  topic: FeedbackTopic;
  rating: number;
  eventTitle?: string;
  message?: string;
  senderName?: string;
  senderEmail?: string;
}

// what the admin list returns
export interface Feedback {
  id: string;
  topic: FeedbackTopic;
  eventTitle: string | null;
  rating: number;
  message: string | null;
  senderName: string | null;
  senderEmail: string | null;
  isReviewed: boolean;
  createdAt: string;
}

export interface FeedbackPage {
  feedbacks: Feedback[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}
