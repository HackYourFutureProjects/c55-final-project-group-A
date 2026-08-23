export interface User {
  userId: string;
  role: "user" | "admin";
  name: string;
  email: string;
  createdAt: string;
  location?: string;
}
