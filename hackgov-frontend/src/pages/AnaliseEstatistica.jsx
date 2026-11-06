import { useState, useEffect } from 'react'
import { buscarAnaliseEstatistica } from '../api/hackgovApi'

// Relatorio estatistico avancado do painel do gestor (Visao de Futuro - Parte 3).
// Mostra, para cada analise, o coeficiente de correlacao (r), a reta de
// regressao e uma leitura em portugues; depois o prazo previsto por
// prioridade e as solicitacoes que estao paradas no fluxo.
//
// Recebe "atualizarCom": quando esse valor muda (ex.: as estatisticas basicas
// foram recarregadas depois de mudar um status), a analise e recalculada.

function formatar(numero, casas = 2) {
  return numero == null ? '-' : numero.toFixed(casas).replace('.', ',')
}

function Equacao({ c }) {
  if (c.inclinacao == null) return null
  const sinal = c.inclinacao >= 0 ? '+' : '−'
  return (
    <p className="analise-equacao">
      {c.variavelY} = {formatar(c.intercepto)} {sinal} {formatar(Math.abs(c.inclinacao), 3)} × {c.variavelX}
    </p>
  )
}

function CartaoCorrelacao({ c }) {
  const forte = c.r != null && Math.abs(c.r) >= 0.7
  return (
    <div className="analise-cartao">
      <h3 className="relatorio-titulo">{c.titulo}</h3>
      <div className="analise-numeros">
        <span className={`analise-r ${forte ? 'analise-r-forte' : ''}`}>r = {formatar(c.r)}</span>
        <span className="analise-meta">R² = {formatar(c.r2)}</span>
        <span className="analise-meta">n = {c.amostra}</span>
      </div>
      <p className="analise-forca">Correlação {c.forca}</p>
      <Equacao c={c} />
      <p className="analise-leitura">{c.leitura}</p>
    </div>
  )
}

function AnaliseEstatistica({ atualizarCom }) {
  const [analise, setAnalise] = useState(null)
  const [erro, setErro] = useState(null)

  useEffect(() => {
    buscarAnaliseEstatistica()
      .then((dados) => { setAnalise(dados); setErro(null) })
      .catch((e) => setErro(e.message))
  }, [atualizarCom])

  return (
    <section className="cartao">
      <p className="eyebrow">Análise estatística</p>
      <h2>Correlação e regressão para decisão</h2>

      {erro && <p className="vazio">Não foi possível carregar a análise: {erro}</p>}
      {!erro && !analise && <p className="vazio">Calculando...</p>}

      {analise && (
        <>
          <div className="grade-relatorios">
            {analise.correlacoes.map((c) => <CartaoCorrelacao key={c.codigo} c={c} />)}
          </div>

          <div className="grade-relatorios analise-decisao">
            <div>
              <h3 className="relatorio-titulo">Prazo previsto por prioridade</h3>
              {analise.previsaoPorPrioridade.length === 0 ? (
                <p className="vazio">Ainda não há solicitações concluídas suficientes.</p>
              ) : (
                <table className="tabela">
                  <thead><tr><th>Prioridade</th><th>Dias previstos</th></tr></thead>
                  <tbody>
                    {analise.previsaoPorPrioridade.map((p) => (
                      <tr key={p.prioridade}><td>{p.prioridade}</td><td>{formatar(p.diasPrevistos, 1)}</td></tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
            <div className="analise-paradas">
              <h3 className="relatorio-titulo">Solicitações paradas no fluxo</h3>
              {analise.solicitacoesParadas.length === 0 ? (
                <p className="vazio">Nenhuma solicitação atrasada em relação ao ritmo esperado.</p>
              ) : (
                <ul>
                  {analise.solicitacoesParadas.map((s) => (
                    <li key={s.id ?? s.protocolo}>
                      <strong>{s.protocolo}</strong> — {s.titulo}
                      <span className="analise-meta">
                        {formatar(s.diasEmAberto, 1)} dias em aberto · está em {s.statusAtual}, o esperado era {s.statusEsperado}
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        </>
      )}
    </section>
  )
}

export default AnaliseEstatistica
