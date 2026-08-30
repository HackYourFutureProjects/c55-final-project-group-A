// Comments on an event. Anyone can read them, logged-in users can post.

"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import CommentItem from "@/components/comments/CommentItem";
import { useAuth } from "@/context/AuthContext";
import { createComment, getEventComments } from "@/lib/api";
import type { CommentPage } from "@/types/comment";

interface CommentListProps {
  eventId: string;
}

export default function CommentList({ eventId }: CommentListProps) {
  const { user } = useAuth();
  // null means "not loaded yet"
  const [data, setData] = useState<CommentPage | null>(null);
  const [newComment, setNewComment] = useState("");
  const [error, setError] = useState("");
  const [isPosting, setIsPosting] = useState(false);

  const loadComments = useCallback(() => {
    getEventComments(eventId)
      .then(setData)
      .catch(() => setError("Could not load comments"));
  }, [eventId]);

  useEffect(loadComments, [loadComments]);

  async function handlePost() {
    setError("");
    setIsPosting(true);
    try {
      await createComment(eventId, { content: newComment });
      setNewComment("");
      // Reload instead of adding locally: the backend owns id, date and name
      loadComments();
    } catch (postError) {
      setError((postError as Error).message);
    } finally {
      setIsPosting(false);
    }
  }

  return (
    <section className="mt-8 rounded-2xl border border-neutral-200 bg-white p-6">
      <h2 className="font-semibold text-xl">
        Comments · {data?.totalComments ?? 0}
      </h2>

      {user ? (
        <div className="mt-4">
          <textarea
            value={newComment}
            onChange={(event) => setNewComment(event.target.value)}
            maxLength={500}
            rows={3}
            placeholder="Add a comment..."
            className="w-full resize-y rounded-lg border border-neutral-200 px-3 py-2 outline-none focus:border-neutral-900"
          />
          <div className="mt-2 flex items-center justify-end gap-3">
            <span className="text-neutral-400 text-xs">
              {newComment.length}/500
            </span>
            <button
              type="button"
              onClick={handlePost}
              disabled={isPosting || newComment.trim().length === 0}
              className="rounded-full bg-orange-600 px-5 py-2 font-semibold text-sm text-white hover:bg-orange-700 disabled:opacity-50"
            >
              {isPosting ? "Posting..." : "Post"}
            </button>
          </div>
        </div>
      ) : (
        <p className="mt-4 text-neutral-500 text-sm">
          <Link href="/login" className="font-semibold underline">
            Log in
          </Link>{" "}
          to leave a comment.
        </p>
      )}

      {error && (
        <p className="mt-4 rounded-lg bg-red-50 px-4 py-3 font-semibold text-red-700 text-sm">
          {error}
        </p>
      )}

      {data?.comments.length === 0 && (
        <p className="mt-4 text-neutral-500 text-sm">
          No comments yet. Be the first to ask something.
        </p>
      )}
      <ul className="mt-6 space-y-6">
        {data?.comments.map((comment) => (
          <CommentItem
            key={comment.id}
            comment={comment}
            onChanged={loadComments}
          />
        ))}
      </ul>
    </section>
  );
}
