export interface ChatMessage {
  role: "user" | "assistant";
  message: string;
}

export interface ChatReply {
  reply: string;
}
