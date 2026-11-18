import { useEffect, useState, type FormEvent } from 'react'
import ConfirmationModal from '../../components/ConfirmationModal'
import { requestJson } from '../../services/api'
import type {
  ActionRunner,
  CepAddress,
  Feedback,
  LaboratoryItem,
  Producer,
  ProducerFormValues,
  PropertyItem,
} from '../../types/agrotis'
import { formatCep, formatCpfCnpj, formatUf } from '../../utils/masks'

type Props = {
  busyAction: string | null
  runAction: ActionRunner
  onFeedback: (feedback: Feedback) => void
}

const emptyForm: ProducerFormValues = {
  nome: '',
  inscricaoFiscal: '',
  logradouro: '',
  cidade: '',
  estado: 'MG',
  cep: '',
  propriedadeIds: [],
  laboratorioId: '',
  dataInicial: '',
  dataFinal: '',
  observacoes: '',
}

export default function ProducersPage({ busyAction, runAction, onFeedback }: Props) {
  const [producers, setProducers] = useState<Producer[]>([])
  const [properties, setProperties] = useState<PropertyItem[]>([])
  const [laboratories, setLaboratories] = useState<LaboratoryItem[]>([])
  const [form, setForm] = useState<ProducerFormValues>(emptyForm)
  const [producerToDelete, setProducerToDelete] = useState<Producer | null>(null)
  const [cepLookupInProgress, setCepLookupInProgress] = useState(false)

  const loadProducers = async () => {
    setProducers(await requestJson<Producer[]>('/api/producers'))
  }

  useEffect(() => {
    let active = true
    void Promise.all([
      requestJson<Producer[]>('/api/producers'),
      requestJson<PropertyItem[]>('/api/properties'),
      requestJson<LaboratoryItem[]>('/api/laboratories'),
    ]).then(([producerData, propertyData, laboratoryData]) => {
      if (!active) return
      setProducers(producerData)
      setProperties(propertyData)
      setLaboratories(laboratoryData)
    }).catch((error: unknown) => {
      if (!active) return
      onFeedback({
        type: 'error',
        title: 'Falha ao carregar os cadastros',
        message: error instanceof Error ? error.message : 'Ocorreu um erro inesperado.',
      })
    })
    return () => {
      active = false
    }
  }, [onFeedback])

  const resetForm = () => {
    setForm(emptyForm)
    onFeedback({ type: 'success', title: 'Formulário limpo', message: 'Os campos do cadastro foram limpos.' })
  }

  const toggleProperty = (propertyId: number) => {
    setForm((current) => ({
      ...current,
      propriedadeIds: current.propriedadeIds.includes(propertyId)
        ? current.propriedadeIds.filter((id) => id !== propertyId)
        : [...current.propriedadeIds, propertyId],
    }))
  }

  const lookupCep = async () => {
    const cep = form.cep.replace(/\D/g, '')
    if (cep.length !== 8) return

    setCepLookupInProgress(true)
    try {
      const address = await requestJson<CepAddress>(`/api/cep/${cep}`)
      setForm((current) => current.cep.replace(/\D/g, '') === cep
        ? {
            ...current,
            logradouro: address.logradouro,
            cidade: address.cidade,
            estado: formatUf(address.estado),
          }
        : current)
    } catch (error) {
      onFeedback({
        type: 'error',
        title: 'Falha ao consultar o CEP',
        message: error instanceof Error ? error.message : 'Não foi possível consultar a API dos Correios.',
      })
    } finally {
      setCepLookupInProgress(false)
    }
  }

  const submitForm = async (event: FormEvent) => {
    event.preventDefault()
    const isEditing = form.id !== undefined

    await runAction(isEditing ? 'Atualizar produtor' : 'Salvar produtor', async () => {
      const payload = {
        nome: form.nome,
        inscricaoFiscal: form.inscricaoFiscal,
        endereco: {
          logradouro: form.logradouro,
          cidade: form.cidade,
          estado: form.estado,
          cep: form.cep,
        },
        propriedadeIds: form.propriedadeIds,
        laboratorioId: Number(form.laboratorioId),
        dataInicial: form.dataInicial ? new Date(form.dataInicial).toISOString() : null,
        dataFinal: form.dataFinal ? new Date(form.dataFinal).toISOString() : null,
        observacoes: form.observacoes,
      }

      await requestJson<Producer>(
        isEditing ? `/api/producers/${form.id}` : '/api/producers',
        {
          method: isEditing ? 'PUT' : 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload),
        },
      )
      setForm(emptyForm)
      await loadProducers()
      return isEditing ? 'Produtor atualizado com sucesso.' : 'Produtor cadastrado com sucesso.'
    })
  }

  const editProducer = (producer: Producer) => {
    setForm({
      id: producer.id,
      nome: producer.nome,
      inscricaoFiscal: formatCpfCnpj(producer.inscricaoFiscal),
      logradouro: producer.endereco.logradouro,
      cidade: producer.endereco.cidade,
      estado: producer.endereco.estado,
      cep: formatCep(producer.endereco.cep),
      propriedadeIds: producer.propriedades.map((property) => property.id),
      laboratorioId: String(producer.laboratorio.id),
      dataInicial: producer.dataInicial ? producer.dataInicial.slice(0, 16) : '',
      dataFinal: producer.dataFinal ? producer.dataFinal.slice(0, 16) : '',
      observacoes: producer.observacoes ?? '',
    })
    onFeedback({
      type: 'success',
      title: 'Edição iniciada',
      message: `Cadastro de ${producer.nome} carregado no formulário.`,
    })
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const deleteProducer = async (id: number) => {
    await runAction('Excluir produtor', async () => {
      await requestJson<void>(`/api/producers/${id}`, { method: 'DELETE' })
      await loadProducers()
      return 'Produtor excluído com sucesso.'
    })
  }

  const confirmDeleteProducer = () => {
    if (!producerToDelete) return
    const producerId = producerToDelete.id
    setProducerToDelete(null)
    void deleteProducer(producerId)
  }

  return (
    <>
      {cepLookupInProgress && (
        <div className="cep-loading-overlay" role="status" aria-live="polite">
          <div className="cep-loading-card">
            <span className="cep-spinner" aria-hidden="true" />
            <p>Buscando o endereço do CEP...</p>
          </div>
        </div>
      )}
      <div className="page-stack" inert={cepLookupInProgress}>
      {producerToDelete && (
        <ConfirmationModal
          itemName={producerToDelete.nome}
          itemType="produtor"
          onCancel={() => setProducerToDelete(null)}
          onConfirm={confirmDeleteProducer}
        />
      )}
      <section className="panel">
        <div className="panel-header">
          <h2>Dados do produtor</h2>
        </div>

        <form onSubmit={submitForm} className="producer-form">
          <div className="grid two-cols">
            <label>
              Nome
              <input required value={form.nome} onChange={(event) => setForm({ ...form, nome: event.target.value })} />
            </label>
            <label>
              CPF/CNPJ
              <input
                required
                inputMode="numeric"
                autoComplete="off"
                placeholder="000.000.000-00 ou 00.000.000/0000-00"
                value={form.inscricaoFiscal}
                onChange={(event) => setForm({ ...form, inscricaoFiscal: formatCpfCnpj(event.target.value) })}
              />
            </label>
          </div>

          <div className="grid two-cols">
            <label>
              CEP
              <input
                required
                inputMode="numeric"
                maxLength={9}
                placeholder="00000-000"
                value={form.cep}
                onChange={(event) => setForm({ ...form, cep: formatCep(event.target.value) })}
                onBlur={() => void lookupCep()}
                aria-busy={cepLookupInProgress}
              />
              {cepLookupInProgress && <small>Consultando os Correios...</small>}
            </label>
            <label>
              Logradouro
              <input required value={form.logradouro} onChange={(event) => setForm({ ...form, logradouro: event.target.value })} />
            </label>
          </div>

          <div className="grid two-cols">
            <label>
              Cidade
              <input required value={form.cidade} onChange={(event) => setForm({ ...form, cidade: event.target.value })} />
            </label>
            <label>
              UF
              <input
                required
                maxLength={2}
                placeholder="PR"
                value={form.estado}
                onChange={(event) => setForm({ ...form, estado: formatUf(event.target.value) })}
              />
            </label>
          </div>

          <div className="grid two-cols">
            <label>
              Laboratório
              <select required value={form.laboratorioId} onChange={(event) => setForm({ ...form, laboratorioId: event.target.value })}>
                <option value="">Selecione</option>
                {laboratories.map((laboratory) => (
                  <option key={laboratory.id} value={String(laboratory.id)}>
                    {laboratory.nome}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="grid two-cols">
            <label>
              Data inicial
              <input type="datetime-local" value={form.dataInicial} onChange={(event) => setForm({ ...form, dataInicial: event.target.value })} />
            </label>
            <label>
              Data final
              <input type="datetime-local" value={form.dataFinal} onChange={(event) => setForm({ ...form, dataFinal: event.target.value })} />
            </label>
          </div>

          <label>
            Observações
            <textarea value={form.observacoes} onChange={(event) => setForm({ ...form, observacoes: event.target.value })} rows={3} />
          </label>

          <div className="property-selector">
            <span>Propriedades vinculadas</span>
            <div className="chip-list">
              {properties.map((property) => {
                const unavailable = property.produtorId != null && property.produtorId !== form.id
                return (
                  <label key={property.id} className={`chip${unavailable ? ' unavailable' : ''}`}>
                    <input
                      type="checkbox"
                      checked={form.propriedadeIds.includes(property.id)}
                      onChange={() => toggleProperty(property.id)}
                      disabled={unavailable}
                    />
                    <span>{property.nome}</span>
                    {unavailable && (
                      <small className="chip-owner">
                        Vinculada a {property.produtorNome || 'outro produtor'}
                      </small>
                    )}
                  </label>
                )
              })}
            </div>
            {properties.length === 0 ? (
              <p className="property-help">Nenhuma propriedade cadastrada. Cadastre uma propriedade para vinculá-la ao produtor.</p>
            ) : properties.every((property) => property.produtorId != null && property.produtorId !== form.id) ? (
              <p className="property-help">
                Todas as propriedades já estão vinculadas a outros produtores. Cadastre uma nova propriedade ou edite o produtor atual.
              </p>
            ) : null}
          </div>

          <div className="actions-row">
            <button type="submit" className="primary-button" disabled={busyAction !== null}>
              {busyAction || (form.id ? 'Atualizar produtor' : 'Salvar produtor')}
            </button>
            <button type="button" className="secondary-button" onClick={resetForm} disabled={busyAction !== null}>
              Limpar
            </button>
          </div>
        </form>
      </section>

      <section className="panel">
        <div className="panel-header">
          <h2>Listagem operacional</h2>
        </div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Produtor</th>
                <th>Laboratório</th>
                <th>Propriedades</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {producers.length === 0 ? (
                <tr><td colSpan={4}>Nenhum produtor cadastrado.</td></tr>
              ) : producers.map((producer) => (
                <tr key={producer.id}>
                  <td>
                    <strong>{producer.nome}</strong>
                    <small>{producer.inscricaoFiscal}</small>
                  </td>
                  <td>{producer.laboratorio?.nome}</td>
                  <td>{producer.propriedades.map((property) => property.nome).join(', ')}</td>
                  <td className="action-cell">
                    <button type="button" className="link-button" onClick={() => editProducer(producer)} disabled={busyAction !== null}>
                      Editar
                    </button>
                    <button type="button" className="link-button danger" onClick={() => setProducerToDelete(producer)} disabled={busyAction !== null}>
                      Excluir
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
      </div>
    </>
  )
}
