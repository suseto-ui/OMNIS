import { MessageItem } from "../components/MessageBubble";
import { TopicRule } from "../UserDashboard";

export type { MessageItem, TopicRule };

export interface ChatThread {
  id: string;
  title: string;
  messages: MessageItem[];
  createdAt: string;
  updatedAt: string;
}

export type ToastType = "success" | "error" | "info";

export interface ToastMessage {
  id: string;
  message: string;
  type: ToastType;
}
