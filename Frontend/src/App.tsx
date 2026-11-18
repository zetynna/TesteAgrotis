import { useCallback, useState } from 'react'
import AnalyticsPage from './pages/analytics/AnalyticsPage'
import IntegrationsPage from './pages/integrations/IntegrationsPage'
import ProducersPage from './pages/producers/ProducersPage'
import CatalogPage from './pages/catalog/CatalogPage'
import FeedbackModal from './components/FeedbackModal'
import MainLayout from './layouts/MainLayout'
import type { ActionRunner, Feedback, PageId } from './types/agrotis'
import './styles/global.css'

export default function App() {
  const [activePage, setActivePage] = useState<PageId>('producers')
  const [feedback, setFeedback] = useState<Feedback | null>(null)
  const [busyAction, setBusyAction] = useState<string | null>(null)

  const showFeedback = useCallback((nextFeedback: Feedback) => {
    setFeedback(nextFeedback)
  }, [])

  const closeFeedback = useCallback(() => {
    setFeedback(null)
  }, [])

  const runAction: ActionRunner = useCallback(async (title, action) => {
    if (busyAction !== null) return
    setBusyAction(title)
    try {
      const message = await action()
      setFeedback({
        type: 'success',
        title: `${title} concluído`,
        message: message || 'Operação realizada com sucesso.',
      })
    } catch (error) {
      setFeedback({
        type: 'error',
        title: `Falha ao ${title.toLocaleLowerCase('pt-BR')}`,
        message: error instanceof Error ? error.message : 'Ocorreu um erro inesperado.',
      })
    } finally {
      setBusyAction(null)
    }
  }, [busyAction])

  return (
    <>
      <MainLayout activePage={activePage} onNavigate={setActivePage}>
        {activePage === 'producers' && (
          <ProducersPage
            busyAction={busyAction}
            runAction={runAction}
            onFeedback={showFeedback}
          />
        )}
        {(activePage === 'properties' || activePage === 'laboratories') && (
          <CatalogPage
            type={activePage}
            busyAction={busyAction}
            runAction={runAction}
            onFeedback={showFeedback}
          />
        )}
        {activePage === 'analytics' && (
          <AnalyticsPage
            busyAction={busyAction}
            runAction={runAction}
            onFeedback={showFeedback}
          />
        )}
        {activePage === 'integrations' && (
          <IntegrationsPage
            busyAction={busyAction}
            runAction={runAction}
            onFeedback={showFeedback}
          />
        )}
      </MainLayout>
      {feedback && <FeedbackModal feedback={feedback} onClose={closeFeedback} />}
    </>
  )
}
