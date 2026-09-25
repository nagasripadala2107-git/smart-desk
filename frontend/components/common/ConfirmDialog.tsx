'use client';

import React from 'react';
import { Modal } from '@/components/ui/Modal';
import { Button } from '@/components/ui/Button';
import { AlertCircle } from 'lucide-react';

interface ConfirmDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  isDangerous?: boolean;
  isLoading?: boolean;
}

export function ConfirmDialog({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  isDangerous = false,
  isLoading = false,
}: ConfirmDialogProps) {
  return (
    <Modal isOpen={isOpen} onClose={onClose} title={title}>
      <div className="flex items-start gap-3 mt-2">
        {isDangerous && (
          <div className="p-2 rounded-full bg-rose-100 dark:bg-rose-950/60 text-rose-600 dark:text-rose-400 shrink-0">
            <AlertCircle className="w-5 h-5" />
          </div>
        )}
        <p className="text-sm text-slate-600 dark:text-slate-300 leading-relaxed">{message}</p>
      </div>
      <div className="mt-6 flex justify-end gap-3">
        <Button variant="outline" size="sm" onClick={onClose} disabled={isLoading}>
          {cancelLabel}
        </Button>
        <Button
          variant={isDangerous ? 'danger' : 'primary'}
          size="sm"
          onClick={onConfirm}
          isLoading={isLoading}
        >
          {confirmLabel}
        </Button>
      </div>
    </Modal>
  );
}
