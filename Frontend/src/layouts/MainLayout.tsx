import type { PropsWithChildren } from 'react'
import type { PageId } from '../types/agrotis'
import './MainLayout.css'

const navigation: Array<{
  id: PageId
  label: string
  shortLabel: string
  description: string
}> = [
  {
    id: 'producers',
    label: 'Produtores',
    shortLabel: 'PR',
    description: 'Cadastre, consulte e mantenha os produtores e seus vínculos.',
  },
  {
    id: 'properties',
    label: 'Propriedades',
    shortLabel: 'PO',
    description: 'Mantenha os cadastros e vínculos de propriedades rurais.',
  },
  {
    id: 'laboratories',
    label: 'Laboratórios',
    shortLabel: 'LA',
    description: 'Gerencie os laboratórios e seus dados cadastrais.',
  },
  {
    id: 'analytics',
    label: 'Dashboard analítico',
    shortLabel: 'DA',
    description: 'Acompanhe a consolidação operacional dos laboratórios.',
  },
  {
    id: 'integrations',
    label: 'Monitoramento de integração',
    shortLabel: 'MI',
    description: 'Acompanhe lotes sincronizados, falhas e DLQ.',
  },
]

type Props = PropsWithChildren<{
  activePage: PageId
  onNavigate: (page: PageId) => void
}>

export default function MainLayout({ activePage, onNavigate, children }: Props) {
  const currentPage = navigation.find((page) => page.id === activePage) ?? navigation[0]

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <span className="brand-mark">A</span>
          <div>
            <strong>Agrotis</strong>
            <small>Agroinformática</small>
          </div>
        </div>
        <p className="sidebar-caption">MENU PRINCIPAL</p>
        <nav className="side-nav" aria-label="Navegação principal">
          {navigation.map((page) => (
            <button
              key={page.id}
              type="button"
              className={`nav-item ${activePage === page.id ? 'active' : ''}`}
              aria-current={activePage === page.id ? 'page' : undefined}
              onClick={() => onNavigate(page.id)}
            >
              <span className="nav-icon" aria-hidden="true">{page.shortLabel}</span>
              <span>{page.label}</span>
            </button>
          ))}
        </nav>
        <div className="sidebar-footer">
          <span className="sidebar-status-dot" />
          <span>Ambiente operacional</span>
        </div>
      </aside>

      <div className="main-area">
        <header className="page-header">
          <div>
            <p className="eyebrow">Agrotis Agroinformática</p>
            <h1>{currentPage.label}</h1>
            <p className="page-description">{currentPage.description}</p>
          </div>
          <div className="status-badge">Sistema ativo</div>
        </header>
        <main className="page-content">{children}</main>
      </div>
    </div>
  )
}
