import { useEffect } from 'react'
import type { Feedback } from '../types/agrotis'
import './FeedbackModal.css'

type Props = {
  feedback: Feedback
  onClose: () => void
}

export default function FeedbackModal({ feedback, onClose }: Props) {
  useEffect(() => {
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [onClose])

  return (
    <div className="modal-backdrop">
      <section
        className={`feedback-modal ${feedback.type}`}
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="feedback-title"
        aria-describedby="feedback-message"
      >
        <div className="feedback-icon" aria-hidden="true">
          {feedback.type === 'success' ? '✓' : '!'}
        </div>
        <h2 id="feedback-title">{feedback.title}</h2>
        <p id="feedback-message">{feedback.message}</p>
        <button type="button" className="primary-button" onClick={onClose} autoFocus>
          Fechar
        </button>
      </section>
    </div>
  )
}
