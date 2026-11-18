import { useCallback, useEffect, useState } from 'react'
import { requestJson } from '../../services/api'
import type { ActionRunner, Feedback, SyncBatch } from '../../types/agrotis'

type Props = {
  busyAction: string | null
  runAction: ActionRunner
  onFeedback: (feedback: Feedback) => void
}

function examplePayload(entityType: SyncBatch['entityType']) {
  const address = (logradouro: string, cep: string) => ({
    logradouro,
    cidade: 'Curitiba',
    estado: 'PR',
    cep,
  })
  if (entityType === 'PROPERTY') {
    return {
      nome: 'Fazenda Integração',
      inscricaoFiscal: '333.444.555-08',
      endereco: address('Estrada Rural, 1', '80000-001'),
    }
  }
  if (entityType === 'LABORATORY') {
    return {
      nome: 'Laboratório Integração',
      inscricaoFiscal: '33.444.555/0001-81',
      endereco: address('Av. Técnica, 5', '80000-002'),
    }
  }
  return {
    nome: 'Produtor Externo',
    inscricaoFiscal: '555.666.777-20',
    endereco: address('Rua Externa, 10', '80000-000'),
    dataInicial: '2026-06-11T12:00:00Z',
    dataFinal: '2026-06-15T18:00:00Z',
    propriedades: [{
      nome: 'Fazenda Integração',
      inscricaoFiscal: '333.444.555-08',
      endereco: address('Estrada Rural, 1', '80000-001'),
    }],
    laboratorio: {
      nome: 'Laboratório Integração',
      inscricaoFiscal: '33.444.555/0001-81',
      endereco: address('Av. Técnica, 5', '80000-002'),
    },
    observacoes: 'Carga de demonstração',
  }
}

export default function IntegrationsPage({ busyAction, runAction, onFeedback }: Props) {
  const [batches, setBatches] = useState<SyncBatch[]>([])
  const [source, setSource] = useState('ERP')
  const [externalReference, setExternalReference] = useState('producer-001')
  const [entityType, setEntityType] = useState<SyncBatch['entityType']>('PRODUCER')
  const [payload, setPayload] = useState(JSON.stringify(examplePayload('PRODUCER'), null, 2))

  const loadBatches = useCallback(async () => {
    setBatches(await requestJson<SyncBatch[]>('/api/integration/batches'))
  }, [])

  useEffect(() => {
    let active = true
    void requestJson<SyncBatch[]>('/api/integration/batches').then((data) => {
      if (active) setBatches(data)
    }).catch((error: unknown) => {
      if (!active) return
      onFeedback({
        type: 'error',
        title: 'Falha ao carregar as integrações',
        message: error instanceof Error ? error.message : 'Ocorreu um erro inesperado.',
      })
    })
    return () => {
      active = false
    }
  }, [loadBatches, onFeedback])

  useEffect(() => {
    const timer = window.setInterval(() => {
      void loadBatches().catch(() => undefined)
    }, 2500)
    return () => window.clearInterval(timer)
  }, [loadBatches])

  const startSync = async () => {
    await runAction('Executar sincronização', async () => {
      const batch = await requestJson<SyncBatch>('/api/integration/v1/sync', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ schemaVersion: 1, source, externalReference, entityType, payload: JSON.parse(payload) }),
      })
      await loadBatches()
      return `Lote ${batch.externalReference} enviado para processamento. Status inicial: ${batch.status}.`
    })
  }

  const reprocess = async (batchId: number) => {
    await runAction('Reprocessar lote', async () => {
      const batch = await requestJson<SyncBatch>(`/api/integration/batches/${batchId}/reprocess`, { method: 'POST' })
      await loadBatches()
      return `Lote ${batch.externalReference} enviado novamente para processamento.`
    })
  }

  return (
    <section className="panel">
      <div className="panel-header">
        <h2>Monitoramento da integração</h2>
      </div>

      <div className="integration-box">
        <div className="grid three-cols">
          <label>
            Sistema origem
            <input value={source} onChange={(event) => setSource(event.target.value)} />
          </label>
          <label>
            Entidade
            <select
              value={entityType}
              onChange={(event) => {
                const nextType = event.target.value as SyncBatch['entityType']
                setEntityType(nextType)
                setExternalReference(`${nextType.toLocaleLowerCase('pt-BR')}-001`)
                setPayload(JSON.stringify(examplePayload(nextType), null, 2))
              }}
            >
              <option value="PRODUCER">Produtor</option>
              <option value="PROPERTY">Propriedade</option>
              <option value="LABORATORY">Laboratório</option>
            </select>
          </label>
          <label>
            Referência externa
            <input value={externalReference} onChange={(event) => setExternalReference(event.target.value)} />
          </label>
          <label>
            Payload
            <textarea rows={10} value={payload} onChange={(event) => setPayload(event.target.value)} />
            <small>A alteração do tipo carrega um exemplo de payload v1; revise o conteúdo antes de enviar.</small>
          </label>
        </div>
        <div className="actions-row right-align">
          <button type="button" className="primary-button" onClick={startSync} disabled={busyAction !== null}>
            {busyAction || 'Executar sincronização'}
          </button>
        </div>
      </div>

      <div className="table-wrap small-table">
        <table>
          <thead>
            <tr>
              <th>Batch</th>
              <th>Entidade</th>
              <th>Status</th>
              <th>Fonte</th>
              <th>Tentativas</th>
              <th>Falha</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {batches.length === 0 ? (
              <tr><td colSpan={6}>Nenhum lote de integração disponível.</td></tr>
            ) : batches.map((batch) => (
              <tr key={batch.id}>
                <td>{batch.externalReference}</td>
                <td>{batch.entityType}</td>
                <td><span className={`status-pill ${batch.status.toLowerCase()}`}>{batch.status}</span></td>
                <td>{batch.source}</td>
                <td>{batch.attemptCount}</td>
                <td>{batch.errorMessage || '—'}</td>
                <td>
                  {(batch.status === 'DLQ' || batch.status === 'FAILED') && (
                    <button type="button" className="link-button" disabled={busyAction !== null} onClick={() => void reprocess(batch.id)}>
                      Reprocessar
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
