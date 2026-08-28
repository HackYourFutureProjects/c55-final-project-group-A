// Comments on an event

"use client";

import { useCallback, useEffect, useState } from "react";
import { getEventComments } from "@/lib/api";
import type { Comment } from "@/types/comment";

interface CommentListProps {
  eventId: string;
}

function formatCommentDate(isoDate: string) {
  return new Date(isoDate).toLocaleDateString("en-GB", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

export default function CommentList({ eventId }: CommentListProps) {
  const [comments, setComments] = useState<Comment[]>([]);
  const [total, setTotal] = useState(0);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  // Wrapped in useCallback so posting and deleting can reuse it later
  const loadComments = useCallback(() => {
    getEventComments(eventId)
      .then((data) => {
        setComments(data.comments);
        setTotal(data.totalComments);
      })
      .catch(() => setError("Could not load comments"))
      .finally(() => setIsLoading(false));
  }, [eventId]);

  useEffect(loadComments, [loadComments]);

  return (
    <section className="mt-8 rounded-2xl border border-neutral-200 bg-white p-6">
      <h2 className="font-semibold text-xl">Comments · {total}</h2>

      {error && (
        <p className="mt-4 rounded-lg bg-red-50 px-4 py-3 font-semibold text-red-700 text-sm">
          {error}
        </p>
      )}

      {isLoading && <p className="mt-4 text-neutral-500 text-sm">Loading...</p>}

      {!isLoading && !error && comments.length === 0 && (
        <p className="mt-4 text-neutral-500 text-sm">
          No comments yet. Be the first to ask something.
        </p>
      )}

      <ul className="mt-6 space-y-6">
        {comments.map((comment) => (
          <li key={comment.id}>
            <p className="font-semibold text-sm">
              {comment.userName}{" "}
              <span className="font-normal text-neutral-400">
                · {formatCommentDate(comment.createdAt)}
              </span>
            </p>
            <p className="mt-1 text-neutral-700">{comment.content}</p>

            {/* An admin reply is plain text on the comment itself, not a separate object */}
            {comment.adminReply && (
              <div className="mt-3 ml-4 border-neutral-200 border-l-2 pl-4">
                <p className="font-semibold text-neutral-500 text-sm">
                  Organizer
                </p>
                <p className="mt-1 text-neutral-700">{comment.adminReply}</p>
              </div>
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}
