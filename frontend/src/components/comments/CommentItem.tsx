// One comment in the list. Lets the author edit or delete their own comment.

"use client";

import { useState } from "react";
import ConfirmModal from "@/components/admin/ConfirmModal";
import { useAuth } from "@/context/AuthContext";
import { deleteComment, updateComment } from "@/lib/api";
import type { Comment } from "@/types/comment";

interface CommentItemProps {
  comment: Comment;
  onChanged: () => void;
}

function formatCommentDate(isoDate: string) {
  return new Date(isoDate).toLocaleDateString("en-GB", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

export default function CommentItem({ comment, onChanged }: CommentItemProps) {
  const { user } = useAuth();
  const [isEditing, setIsEditing] = useState(false);
  const [text, setText] = useState(comment.content);
  const [isConfirmOpen, setIsConfirmOpen] = useState(false);
  const [isBusy, setIsBusy] = useState(false);
  const [error, setError] = useState("");

  const isMine = comment.userId === user?.userId;

  function startEditing() {
    setText(comment.content);
    setError("");
    setIsEditing(true);
  }

  async function handleSave() {
    setError("");
    setIsBusy(true);
    try {
      await updateComment(comment.id, { content: text });
      setIsEditing(false);
      onChanged();
    } catch (saveError) {
      setError((saveError as Error).message);
    } finally {
      setIsBusy(false);
    }
  }

  async function handleDelete() {
    setError("");
    setIsBusy(true);
    try {
      await deleteComment(comment.id);
      setIsConfirmOpen(false);
      onChanged();
    } catch (deleteError) {
      setError((deleteError as Error).message);
    } finally {
      setIsBusy(false);
    }
  }

  return (
    <li>
      <p className="font-semibold text-sm">
        {comment.userName}{" "}
        <span className="font-normal text-neutral-400">
          · {formatCommentDate(comment.createdAt)}
        </span>
      </p>

      {isEditing ? (
        <div className="mt-2">
          <textarea
            value={text}
            onChange={(event) => setText(event.target.value)}
            maxLength={500}
            rows={3}
            className="w-full resize-y rounded-lg border border-neutral-200 px-3 py-2 outline-none focus:border-neutral-900"
          />
          <div className="mt-2 flex items-center justify-end gap-3">
            <span className="text-neutral-400 text-xs">{text.length}/500</span>
            <button
              type="button"
              onClick={() => setIsEditing(false)}
              className="rounded-full border border-neutral-200 px-5 py-2 font-semibold text-sm hover:bg-neutral-50"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={handleSave}
              disabled={isBusy || text.trim().length === 0}
              className="rounded-full bg-orange-600 px-5 py-2 font-semibold text-sm text-white hover:bg-orange-700 disabled:opacity-50"
            >
              {isBusy ? "Saving..." : "Save"}
            </button>
          </div>
        </div>
      ) : (
        <p className="mt-1 text-neutral-700">{comment.content}</p>
      )}

      {isMine && !isEditing && (
        <div className="mt-2 flex gap-4">
          <button
            type="button"
            onClick={startEditing}
            className="font-semibold text-neutral-500 text-sm hover:text-neutral-900"
          >
            Edit
          </button>
          <button
            type="button"
            onClick={() => setIsConfirmOpen(true)}
            className="font-semibold text-red-600 text-sm hover:text-red-700"
          >
            Delete
          </button>
        </div>
      )}

      {error && <p className="mt-2 text-red-700 text-sm">{error}</p>}

      {/* An admin reply is plain text on the comment itself */}
      {comment.adminReply && (
        <div className="mt-3 ml-4 border-neutral-200 border-l-2 pl-4">
          <p className="font-semibold text-neutral-500 text-sm">Organizer</p>
          <p className="mt-1 text-neutral-700">{comment.adminReply}</p>
        </div>
      )}

      {isConfirmOpen && (
        <ConfirmModal
          title="Delete comment"
          message="This comment will be removed for everyone. This cannot be undone."
          confirmLabel="Delete"
          isBusy={isBusy}
          onClose={() => setIsConfirmOpen(false)}
          onConfirm={handleDelete}
        />
      )}
    </li>
  );
}
