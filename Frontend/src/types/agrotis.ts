export type Address = {
  logradouro: string
  cidade: string
  estado: string
  cep: string
}

export type CepAddress = {
  cep: string
  logradouro: string
  cidade: string
  estado: string
}

export type PropertyItem = {
  id: number
  nome: string
  inscricaoFiscal: string
  endereco: Address
  produtorId?: number | null
  produtorNome?: string | null
}

export type LaboratoryItem = {
  id: number
  nome: string
  inscricaoFiscal: string
  endereco: Address
}

export type Producer = {
  id: number
  nome: string
  inscricaoFiscal: string
  endereco: Address
  propriedades: PropertyItem[]
  laboratorio: LaboratoryItem
  dataInicial: string | null
  dataFinal: string | null
  observacoes: string | null
}

export type AnalyticsRow = {
  id: number
  codigo: string
  nome: string
  totalProdutores: number
  valorFinanceiroConsolidado: number
}

export type SyncBatch = {
  id: number
  schemaVersion: number
  entityType: 'PRODUCER' | 'PROPERTY' | 'LABORATORY'
  source: string
  externalReference: string
  status: string
  errorMessage?: string | null
  attemptCount: number
  createdAt: string
  updatedAt: string
}

export type ProducerFormValues = {
  id?: number
  nome: string
  inscricaoFiscal: string
  logradouro: string
  cidade: string
  estado: string
  cep: string
  propriedadeIds: number[]
  laboratorioId: string
  dataInicial: string
  dataFinal: string
  observacoes: string
}

export type Feedback = {
  type: 'success' | 'error'
  title: string
  message: string
}

export type PageId = 'producers' | 'properties' | 'laboratories' | 'analytics' | 'integrations'

export type ActionRunner = (
  title: string,
  action: () => Promise<string | void>,
) => Promise<void>
