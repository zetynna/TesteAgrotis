import { useEffect } from 'react'
import './FeedbackModal.css'

type Props = {
  itemName: string
  itemType: string
  onCancel: () => void
  onConfirm: () => void
}

export default function ConfirmationModal({ itemName, itemType, onCancel, onConfirm }: Props) {
  useEffect(() => {
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onCancel()
    }
    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [onCancel])

  return (
    <div className="modal-backdrop">
      <section
        className="feedback-modal confirmation-modal"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="confirmation-title"
        aria-describedby="confirmation-message"
      >
        <h2 id="confirmation-title">Confirmar exclusão</h2>
        <p id="confirmation-message">
          Tem certeza de que deseja excluir {itemType.toLocaleLowerCase('pt-BR')} <strong>{itemName}</strong>? Esta ação não pode ser desfeita.
        </p>
        <div className="confirmation-actions">
          <button type="button" className="secondary-button" onClick={onCancel} autoFocus>
            Cancelar
          </button>
          <button type="button" className="danger-button" onClick={onConfirm}>
            Excluir {itemType.toLocaleLowerCase('pt-BR')}
          </button>
        </div>
      </section>
    </div>
  )
}
