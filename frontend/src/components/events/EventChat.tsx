// Ask-a-question chat for one event

"use client";

import { useState } from "react";
import { sendChatMessage } from "@/lib/api";
import type { ChatMessage } from "@/types/chat";

const MAX_MESSAGE_LENGTH = 2000;
const MAX_HISTORY = 20;

const STARTERS = [
  "What's this event about?",
  "What's the weather?",
  "How do I get there?",
];

interface EventChatProps {
  eventId: string;
}

export default function EventChat({ eventId }: EventChatProps) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [isSending, setIsSending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isFull = messages.length >= MAX_HISTORY;
  const canSend = input.trim() !== "" && !isSending && !isFull;

  async function send() {
    if (!canSend) return;

    const history: ChatMessage[] = [
      ...messages,
      { role: "user", message: input.trim() },
    ];
    setMessages(history);
    setInput("");
    setError(null);
    setIsSending(true);

    try {
      const reply = await sendChatMessage(eventId, history);
      setMessages([...history, { role: "assistant", message: reply }]);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong.");
    } finally {
      setIsSending(false);
    }
  }

  return (
    <div className="rounded-2xl bg-neutral-900 p-6">
      <div className="mb-5 flex items-center gap-3">
        <div className="h-10 w-10 shrink-0 rounded-xl bg-purple-300" />
        <div>
          <h2 className="font-bold text-lg text-white">
            Ask AI about this event
          </h2>
          <p className="text-neutral-400 text-sm">
            Answers based on the event details and the forecast
          </p>
        </div>
      </div>

      {messages.length === 0 ? (
        <>
          <div className="mb-4 rounded-2xl bg-white/10 px-4 py-3 text-neutral-100 text-sm">
            Hi! I've read this listing and the local forecast. Ask me anything.
          </div>
          <div className="mb-4 flex flex-wrap gap-2">
            {STARTERS.map((question) => (
              <button
                key={question}
                type="button"
                onClick={() => setInput(question)}
                className="rounded-full bg-white/10 px-4 py-2 text-sm text-white hover:bg-white/20"
              >
                {question}
              </button>
            ))}
          </div>
        </>
      ) : (
        <div className="mb-4 space-y-3">
          {messages.map((entry, index) => (
            <div
              key={`${index}-${entry.role}`}
              className={
                entry.role === "user"
                  ? "ml-auto max-w-[85%] rounded-2xl bg-purple-300 px-4 py-2 text-neutral-900 text-sm"
                  : "max-w-[85%] rounded-2xl bg-white/10 px-4 py-2 text-neutral-100 text-sm"
              }
            >
              {entry.message}
            </div>
          ))}
        </div>
      )}

      {isSending && (
        <p className="mb-3 text-neutral-400 text-sm">Thinking...</p>
      )}
      {error && <p className="mb-3 text-red-400 text-sm">{error}</p>}

      <div className="flex gap-2">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") send();
          }}
          maxLength={MAX_MESSAGE_LENGTH}
          placeholder={
            isFull ? "Chat limit reached" : "Ask anything about this event"
          }
          disabled={isFull}
          className="w-full rounded-lg border border-white/10 bg-white/5 px-3 py-2 text-white placeholder:text-neutral-500 outline-none focus:border-purple-300"
        />
        <button
          type="button"
          onClick={send}
          disabled={!canSend}
          className="shrink-0 rounded-full bg-orange-600 px-5 py-2 font-semibold text-sm text-white hover:bg-orange-700 disabled:opacity-50"
        >
          Send
        </button>
      </div>
    </div>
  );
}
