import { BrowserRouter, Navigate, NavLink, Route, Routes } from 'react-router-dom'
import { GridTransacoesPage } from './features/grid-transacoes/GridTransacoesPage'
import { PainelOperadorPage } from './features/painel-operador/PainelOperadorPage'

const linkClasse = ({ isActive }: { isActive: boolean }) =>
  `rounded-md px-3 py-2 text-sm font-medium transition-colors ${
    isActive ? 'bg-brand-600/15 text-brand-400' : 'text-ink-muted hover:text-ink hover:bg-surface-muted'
  }`

function Layout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-canvas">
      <header className="border-b border-border">
        <div className="mx-auto flex max-w-6xl items-center gap-6 px-6 py-4">
          <span className="text-sm font-semibold tracking-wide text-ink">
            SRM <span className="text-brand-500">Credit Engine</span>
          </span>
          <nav className="flex gap-1">
            <NavLink to="/painel" className={linkClasse}>
              Painel do Operador
            </NavLink>
            <NavLink to="/transacoes" className={linkClasse}>
              Grid de Transações
            </NavLink>
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-6 py-8">{children}</main>
    </div>
  )
}

export function App() {
  return (
    <BrowserRouter>
      <Layout>
        <Routes>
          <Route path="/" element={<Navigate to="/painel" replace />} />
          <Route path="/painel" element={<PainelOperadorPage />} />
          <Route path="/transacoes" element={<GridTransacoesPage />} />
        </Routes>
      </Layout>
    </BrowserRouter>
  )
}
