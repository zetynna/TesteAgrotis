import { useEffect, useState, type FormEvent } from 'react'
import ConfirmationModal from '../../components/ConfirmationModal'
import { requestJson } from '../../services/api'
import type { ActionRunner, Address, CepAddress, Feedback, LaboratoryItem, PropertyItem } from '../../types/agrotis'
import { formatCep, formatCpfCnpj, formatUf } from '../../utils/masks'

export type CatalogType = 'properties' | 'laboratories'
type CatalogItem = PropertyItem | LaboratoryItem
type Props = {
  type: CatalogType
  busyAction: string | null
  runAction: ActionRunner
  onFeedback: (feedback: Feedback) => void
}
type Form = {
  id?: number
  nome: string
  inscricaoFiscal: string
  logradouro: string
  cidade: string
  estado: string
  cep: string
}

const emptyForm: Form = {
  nome: '',
  inscricaoFiscal: '',
  logradouro: '',
  cidade: '',
  estado: 'MG',
  cep: '',
}

export default function CatalogPage({ type, busyAction, runAction, onFeedback }: Props) {
  const [items, setItems] = useState<CatalogItem[]>([])
  const [form, setForm] = useState<Form>(emptyForm)
  const [itemToDelete, setItemToDelete] = useState<CatalogItem | null>(null)
  const [lookingUpCep, setLookingUpCep] = useState(false)
  const entityName = type === 'properties' ? 'propriedade' : 'laboratório'
  const entityTitle = type === 'properties' ? 'propriedade' : 'laboratório'
  const entityArticle = type === 'properties' ? 'da' : 'do'
  const emptyListMessage = type === 'properties'
    ? 'Nenhuma propriedade cadastrada.'
    : 'Nenhum laboratório cadastrado.'
  const endpoint = `/api/${type}`

  const loadItems = async () => setItems(await requestJson<CatalogItem[]>(endpoint))

  useEffect(() => {
    let active = true
    void requestJson<CatalogItem[]>(endpoint).then((result) => {
      if (active) setItems(result)
    }).catch((error: unknown) => {
      if (active) onFeedback({
        type: 'error',
        title: `Falha ao carregar ${entityName}s`,
        message: error instanceof Error ? error.message : 'Ocorreu um erro inesperado.',
      })
    })
    return () => { active = false }
  }, [endpoint, entityName, onFeedback])

  const lookupCep = async () => {
    const cep = form.cep.replace(/\D/g, '')
    if (cep.length !== 8) return
    setLookingUpCep(true)
    try {
      const address = await requestJson<CepAddress>(`/api/cep/${cep}`)
      setForm((current) => current.cep.replace(/\D/g, '') === cep
        ? { ...current, logradouro: address.logradouro, cidade: address.cidade, estado: formatUf(address.estado) }
        : current)
    } catch (error) {
      onFeedback({
        type: 'error',
        title: 'Falha ao consultar o CEP',
        message: error instanceof Error ? error.message : 'Não foi possível consultar a API de CEP.',
      })
    } finally {
      setLookingUpCep(false)
    }
  }

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    const editing = form.id !== undefined
    await runAction(editing ? `Atualizar ${entityName}` : `Cadastrar ${entityName}`, async () => {
      const payload = {
        nome: form.nome,
        inscricaoFiscal: form.inscricaoFiscal,
        endereco: {
          logradouro: form.logradouro,
          cidade: form.cidade,
          estado: form.estado,
          cep: form.cep,
        } satisfies Address,
      }
      await requestJson<CatalogItem>(editing ? `${endpoint}/${form.id}` : endpoint, {
        method: editing ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
      setForm(emptyForm)
      await loadItems()
      const verb = type === 'properties'
        ? (editing ? 'atualizada' : 'cadastrada')
        : (editing ? 'atualizado' : 'cadastrado')
      return `${entityName[0].toLocaleUpperCase('pt-BR')}${entityName.slice(1)} ${verb} com sucesso.`
    })
  }

  const deleteItem = async () => {
    if (!itemToDelete) return
    const id = itemToDelete.id
    setItemToDelete(null)
    await runAction(`Excluir ${entityName}`, async () => {
      await requestJson<void>(`${endpoint}/${id}`, { method: 'DELETE' })
      await loadItems()
      return `${entityName[0].toLocaleUpperCase('pt-BR')}${entityName.slice(1)} ${type === 'properties' ? 'excluída' : 'excluído'} com sucesso.`
    })
  }

  const editItem = (item: CatalogItem) => setForm({
    id: item.id,
    nome: item.nome,
    inscricaoFiscal: formatCpfCnpj(item.inscricaoFiscal),
    logradouro: item.endereco.logradouro,
    cidade: item.endereco.cidade,
    estado: formatUf(item.endereco.estado),
    cep: formatCep(item.endereco.cep),
  })

  return (
    <div className="page-stack">
      {itemToDelete && (
        <ConfirmationModal
          itemName={itemToDelete.nome}
          itemType={entityName}
          onCancel={() => setItemToDelete(null)}
          onConfirm={() => void deleteItem()}
        />
      )}
      <section className="panel">
        <div className="panel-header"><h2>Dados {entityArticle} {entityTitle}</h2></div>
        <form className="producer-form" onSubmit={submit}>
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
                aria-busy={lookingUpCep}
              />
              {lookingUpCep && <small>Consultando o CEP...</small>}
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
              <input required maxLength={2} value={form.estado} onChange={(event) => setForm({ ...form, estado: formatUf(event.target.value) })} />
            </label>
          </div>
          <div className="actions-row">
            <button type="submit" className="primary-button" disabled={busyAction !== null}>
              {busyAction || (form.id ? `Atualizar ${entityName}` : `Cadastrar ${entityName}`)}
            </button>
            <button type="button" className="secondary-button" disabled={busyAction !== null} onClick={() => setForm(emptyForm)}>
              Limpar
            </button>
          </div>
        </form>
      </section>
      <section className="panel">
        <div className="panel-header"><h2>Listagem de {entityName}s</h2></div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>Nome</th><th>Inscrição fiscal</th><th>Endereço</th>{type === 'properties' && <th>Produtor responsável</th>}<th>Ações</th></tr>
            </thead>
            <tbody>
              {items.length === 0 ? (
                <tr><td colSpan={type === 'properties' ? 5 : 4}>{emptyListMessage}</td></tr>
              ) : items.map((item) => (
                <tr key={item.id}>
                  <td>{item.nome}</td>
                  <td>{item.inscricaoFiscal}</td>
                  <td>{item.endereco.logradouro}, {item.endereco.cidade} - {item.endereco.estado}</td>
                  {type === 'properties' && <td>{(item as PropertyItem).produtorNome ?? '—'}</td>}
                  <td className="action-cell">
                    <button type="button" className="link-button" disabled={busyAction !== null} onClick={() => editItem(item)}>Editar</button>
                    <button type="button" className="link-button danger" disabled={busyAction !== null} onClick={() => setItemToDelete(item)}>Excluir</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  )
}
