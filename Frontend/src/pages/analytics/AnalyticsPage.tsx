import { useEffect, useState } from 'react'
import { requestJson } from '../../services/api'
import type { ActionRunner, AnalyticsRow, Feedback } from '../../types/agrotis'

type Props = {
  busyAction: string | null
  runAction: ActionRunner
  onFeedback: (feedback: Feedback) => void
}

type Filters = {
  dataInicialInicio: string
  dataInicialFim: string
  dataFinalInicio: string
  dataFinalFim: string
  buscaTexto: string
  qtdMinimaProdutores: string
}

const emptyFilters: Filters = {
  dataInicialInicio: '',
  dataInicialFim: '',
  dataFinalInicio: '',
  dataFinalFim: '',
  buscaTexto: '',
  qtdMinimaProdutores: '1',
}

export default function AnalyticsPage({ busyAction, runAction, onFeedback }: Props) {
  const [filters, setFilters] = useState<Filters>(emptyFilters)
  const [analytics, setAnalytics] = useState<AnalyticsRow[]>([])

  const loadAnalytics = async (activeFilters: Filters) => {
    const params = new URLSearchParams()
    Object.entries(activeFilters).forEach(([key, value]) => {
      if (value) params.set(key, value)
    })
    const rows = await requestJson<AnalyticsRow[]>(`/api/analytics/laboratories?${params.toString()}`)
    setAnalytics(rows)
    return rows
  }

  useEffect(() => {
    let active = true
    void requestJson<AnalyticsRow[]>('/api/analytics/laboratories?qtdMinimaProdutores=1')
      .then((rows) => {
        if (active) setAnalytics(rows)
      })
      .catch((error: unknown) => {
        if (!active) return
        onFeedback({
          type: 'error',
          title: 'Falha ao carregar o dashboard',
          message: error instanceof Error ? error.message : 'Ocorreu um erro inesperado.',
        })
      })
    return () => {
      active = false
    }
  }, [onFeedback])

  const applyFilters = async () => {
    await runAction('Aplicar filtros', async () => {
      const rows = await loadAnalytics(filters)
      return `Filtros aplicados. ${rows.length} laboratório(s) encontrado(s).`
    })
  }

  const updateFilter = (key: keyof Filters, value: string) => {
    setFilters((current) => ({ ...current, [key]: value }))
  }

  return (
    <section className="panel">
      <div className="panel-header">
        <h2>Consolidação por laboratório</h2>
      </div>

      <div className="analytics-filters grid four-cols">
        <label>
          Data inicial início
          <input type="datetime-local" value={filters.dataInicialInicio} onChange={(event) => updateFilter('dataInicialInicio', event.target.value)} />
        </label>
        <label>
          Data inicial fim
          <input type="datetime-local" value={filters.dataInicialFim} onChange={(event) => updateFilter('dataInicialFim', event.target.value)} />
        </label>
        <label>
          Data final início
          <input type="datetime-local" value={filters.dataFinalInicio} onChange={(event) => updateFilter('dataFinalInicio', event.target.value)} />
        </label>
        <label>
          Data final fim
          <input type="datetime-local" value={filters.dataFinalFim} onChange={(event) => updateFilter('dataFinalFim', event.target.value)} />
        </label>
        <label className="wide">
          Busca textual
          <input value={filters.buscaTexto} placeholder="Filtrar por observações" onChange={(event) => updateFilter('buscaTexto', event.target.value)} />
        </label>
        <label>
          Quantidade mínima
          <input
            type="number"
            required
            min={0}
            value={filters.qtdMinimaProdutores}
            onChange={(event) => updateFilter('qtdMinimaProdutores', event.target.value)}
          />
        </label>
      </div>

      <div className="actions-row right-align">
        <button type="button" className="primary-button" onClick={applyFilters} disabled={busyAction !== null}>
          {busyAction || 'Aplicar filtros'}
        </button>
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Nome</th>
              <th>Produtores</th>
              <th>Valor financeiro consolidado</th>
            </tr>
          </thead>
          <tbody>
            {analytics.length === 0 ? (
              <tr><td colSpan={4}>Nenhum laboratório corresponde aos filtros.</td></tr>
            ) : analytics.map((item) => (
              <tr key={item.id}>
                <td>{item.codigo}</td>
                <td>{item.nome}</td>
                <td>{item.totalProdutores}</td>
                <td>R$ {Number(item.valorFinanceiroConsolidado).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
