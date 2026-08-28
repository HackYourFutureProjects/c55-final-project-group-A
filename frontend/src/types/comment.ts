// A comment on an event
export interface Comment {
  id: string;
  eventId: string;
  userId: string;
  userName: string;
  content: string;
  createdAt: string;
  updatedAt: string;
  adminReply: string | null;
  adminReplyCreatedAt: string | null;
  adminReplyUpdatedAt: string | null;
}

// The list endpoint wraps the array. Note this shape is different from
// the event endpoints
export interface CommentPage {
  comments: Comment[];
  totalComments: number;
  hasMore: boolean;
}

// Used for creating a comment, editing it, and for admin replies —
// all four take the same single field, max 500 characters.
export interface CommentRequest {
  content: string;
}