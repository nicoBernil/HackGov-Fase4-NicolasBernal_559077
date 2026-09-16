import { useState, useEffect, Fragment } from 'react'
import {
  listarTodasSolicitacoes,
  listarStatusDisponiveis,
  alterarStatusSolicitacao,
  desfazerUltimaAlteracao,
  listarHistoricoSolicitacao,
  buscarEstatisticas,
} from '../api/hackgovApi'

// Mapeia cada nivel de prioridade para uma classe CSS (cor do badge)
const CLASSE_PRIORIDADE = {
  ALTA: 'prioridade-alta',
  MEDIA: 'prioridade-media',
  BAIXA: 'prioridade-baixa',
}

// Grafico de barras simples, so com CSS (sem biblioteca externa) - recebe
// uma lista [{rotulo, total}] vinda do endpoint /api/estatisticas.
function GraficoBarras({ itens }) {
  if (!itens || itens.length === 0) {
    return <p className="vazio">Sem dados ainda.</p>
  }
  const maior = Math.max(...itens.map((i) => i.total), 1)
  return (
    <div className="grafico-barras">
      {itens.map((item) => (
        <div className="barra-linha" key={item.rotulo}>
          <span className="barra-rotulo">{item.rotulo}</span>
          <div className="barra-fundo">
            <div className="barra-preenchida" style={{ width: `${(item.total / maior) * 100}%` }} />
          </div>
          <span className="barra-valor">{item.total}</span>
        </div>
      ))}
    </div>
  )
}

function AreaGestor({ usuario, aoSair }) {
  const [solicitacoes, setSolicitacoes] = useState([])
  const [statusDisponiveis, setStatusDisponiveis] = useState([])
  const [mensagem, setMensagem] = useState(null)
  const [carregando, setCarregando] = useState(true)

  // Qual status foi escolhido no <select> de cada linha, antes de aplicar.
  const [statusEscolhido, setStatusEscolhido] = useState({})
  // ID da solicitacao com uma acao (mudar status / desfazer) em andamento.
  const [processando, setProcessando] = useState(null)

  // Qual solicitacao esta com o historico (linha do tempo) aberto, e o
  // conteudo desse historico ja carregado.
  const [historicoAbertoId, setHistoricoAbertoId] = useState(null)
  const [historico, setHistorico] = useState([])
  const [carregandoHistorico, setCarregandoHistorico] = useState(false)

  // Relatorio estatistico (Parte 4).
  const [estatisticas, setEstatisticas] = useState(null)

  useEffect(() => {
    carregarTudo()
  }, [])

  function carregarTudo() {
    setCarregando(true)
    Promise.all([listarTodasSolicitacoes(), listarStatusDisponiveis(), buscarEstatisticas()])
      .then(([listaSolicitacoes, listaStatus, dadosEstatisticas]) => {
        setSolicitacoes(listaSolicitacoes)
        setStatusDisponiveis(listaStatus)
        setEstatisticas(dadosEstatisticas)
        setCarregando(false)
      })
      .catch((e) => {
        setMensagem({ tipo: 'erro', texto: e.message })
        setCarregando(false)
      })
  }

  // Recarrega so as estatisticas (depois de mudar/desfazer status, os
  // numeros de status mudam na hora).
  function recarregarEstatisticas() {
    return buscarEstatisticas()
      .then(setEstatisticas)
      .catch(() => {}) // nao interrompe o fluxo se o relatorio falhar
  }

  // Recarrega so a lista de solicitacoes (depois de mudar/desfazer status),
  // sem precisar buscar os status de novo.
  function recarregarSolicitacoes() {
    return listarTodasSolicitacoes()
      .then(setSolicitacoes)
      .catch((e) => setMensagem({ tipo: 'erro', texto: e.message }))
  }

  async function aplicarNovoStatus(idSolicitacao) {
    const idNovoStatus = statusEscolhido[idSolicitacao]
    if (!idNovoStatus) {
      setMensagem({ tipo: 'erro', texto: 'Escolha um status antes de aplicar.' })
      return
    }
    setMensagem(null)
    setProcessando(idSolicitacao)
    try {
      await alterarStatusSolicitacao(idSolicitacao, {
        idNovoStatus: Number(idNovoStatus),
        idGestor: usuario.id,
        observacao: null,
      })
      setMensagem({ tipo: 'sucesso', texto: 'Status atualizado.' })
      await recarregarSolicitacoes()
      recarregarEstatisticas()
      // Se o historico dessa solicitacao estiver aberto, atualiza ele tambem.
      if (historicoAbertoId === idSolicitacao) {
        await abrirHistorico(idSolicitacao)
      }
    } catch (e) {
      setMensagem({ tipo: 'erro', texto: e.message })
    } finally {
      setProcessando(null)
    }
  }

  async function desfazer(idSolicitacao) {
    setMensagem(null)
    setProcessando(idSolicitacao)
    try {
      await desfazerUltimaAlteracao(idSolicitacao, usuario.id)
      setMensagem({ tipo: 'sucesso', texto: 'Ultima alteracao desfeita.' })
      await recarregarSolicitacoes()
      recarregarEstatisticas()
      if (historicoAbertoId === idSolicitacao) {
        await abrirHistorico(idSolicitacao)
      }
    } catch (e) {
      setMensagem({ tipo: 'erro', texto: e.message })
    } finally {
      setProcessando(null)
    }
  }

  async function abrirHistorico(idSolicitacao) {
    // Clicar de novo na mesma linha fecha o historico.
    if (historicoAbertoId === idSolicitacao) {
      setHistoricoAbertoId(null)
      return
    }
    setHistoricoAbertoId(idSolicitacao)
    setCarregandoHistorico(true)
    try {
      const linhas = await listarHistoricoSolicitacao(idSolicitacao)
      setHistorico(linhas)
    } catch (e) {
      setMensagem({ tipo: 'erro', texto: e.message })
    } finally {
      setCarregandoHistorico(false)
    }
  }

  // Pequeno resumo para o topo do painel
  const total = solicitacoes.length
  const recebidas = solicitacoes.filter((s) => s.status === 'Recebido').length
  const concluidas = solicitacoes.filter((s) => s.status === 'Concluido').length

  return (
    <div className="pagina">
      <header className="topo">
        <span className="marca">HackGov</span>
        <div className="topo-direita">
          <span className="topo-sub">Painel do gestor</span>
          <button className="botao-link" onClick={aoSair}>Sair</button>
        </div>
      </header>

      <main className="conteudo conteudo-largo">
        {mensagem && (
          <div className={`alerta alerta-${mensagem.tipo} alerta-topo`}>
            {mensagem.texto}
          </div>
        )}

        {/* Cartoes de resumo */}
        <div className="resumo">
          <div className="cartao-num">
            <span className="num">{total}</span>
            <span className="num-rotulo">Total</span>
          </div>
          <div className="cartao-num">
            <span className="num">{recebidas}</span>
            <span className="num-rotulo">Recebidas</span>
          </div>
          <div className="cartao-num">
            <span className="num">{concluidas}</span>
            <span className="num-rotulo">Concluidas</span>
          </div>
          <div className="cartao-num">
            <span className="num">
              {estatisticas?.tempoMedioResolucaoDias != null
                ? estatisticas.tempoMedioResolucaoDias.toFixed(1)
                : '-'}
            </span>
            <span className="num-rotulo">Dias ate resolver (media)</span>
          </div>
        </div>

        {/* Relatorios estatisticos (Parte 4) */}
        <section className="cartao">
          <p className="eyebrow">Relatorios</p>
          <h2>Solicitacoes por categoria, prioridade e status</h2>
          {!estatisticas ? (
            <p className="vazio">Carregando relatorios...</p>
          ) : (
            <div className="grade-relatorios">
              <div>
                <h3 className="relatorio-titulo">Por categoria</h3>
                <GraficoBarras itens={estatisticas.porCategoria} />
              </div>
              <div>
                <h3 className="relatorio-titulo">Por prioridade</h3>
                <GraficoBarras itens={estatisticas.porPrioridade} />
              </div>
              <div>
                <h3 className="relatorio-titulo">Por status</h3>
                <GraficoBarras itens={estatisticas.porStatus} />
              </div>
            </div>
          )}
        </section>

        <section className="cartao">
          <p className="eyebrow">Demandas recebidas</p>
          <h2>Todas as solicitacoes</h2>
          <p className="dica" style={{ margin: '0 0 14px', textAlign: 'left' }}>
            Ordenadas por prioridade (fila de prioridade no back-end) - as mais
            urgentes aparecem primeiro.
          </p>

          {carregando ? (
            <p className="vazio">Carregando...</p>
          ) : solicitacoes.length === 0 ? (
            <p className="vazio">Nenhuma solicitacao cadastrada ainda.</p>
          ) : (
            <div className="tabela-rolavel">
              <table className="tabela">
                <thead>
                  <tr>
                    <th>Protocolo</th>
                    <th>Titulo</th>
                    <th>Cidadao</th>
                    <th>Categoria</th>
                    <th>Prioridade</th>
                    <th>Status</th>
                    <th>Aberta em</th>
                    <th>Acoes</th>
                  </tr>
                </thead>
                <tbody>
                  {solicitacoes.map((s) => (
                    <Fragment key={s.id}>
                      <tr>
                        <td>{s.protocolo}</td>
                        <td>{s.titulo}</td>
                        <td>{s.cidadao}</td>
                        <td>{s.categoria}</td>
                        <td>
                          <span className={`badge-prioridade ${CLASSE_PRIORIDADE[s.prioridade] || ''}`}>
                            {s.prioridade}
                          </span>
                        </td>
                        <td><span className="badge">{s.status}</span></td>
                        <td>{s.dataAbertura}</td>
                        <td>
                          <div className="acoes-linha">
                            <select
                              className="select-status"
                              value={statusEscolhido[s.id] || ''}
                              onChange={(e) =>
                                setStatusEscolhido((atual) => ({ ...atual, [s.id]: e.target.value }))
                              }
                              disabled={processando === s.id}
                            >
                              <option value="">Mudar para...</option>
                              {statusDisponiveis.map((st) => (
                                <option key={st.id} value={st.id}>{st.nome}</option>
                              ))}
                            </select>
                            <button
                              className="botao-pequeno"
                              onClick={() => aplicarNovoStatus(s.id)}
                              disabled={processando === s.id}
                            >
                              Aplicar
                            </button>
                            <button
                              className="botao-pequeno botao-pequeno-secundario"
                              onClick={() => desfazer(s.id)}
                              disabled={processando === s.id}
                              title="Volta para o status imediatamente anterior"
                            >
                              Desfazer
                            </button>
                            <button
                              className="botao-pequeno botao-pequeno-secundario"
                              onClick={() => abrirHistorico(s.id)}
                            >
                              {historicoAbertoId === s.id ? 'Ocultar historico' : 'Ver historico'}
                            </button>
                          </div>
                        </td>
                      </tr>

                      {historicoAbertoId === s.id && (
                        <tr>
                          <td colSpan={8} className="historico-linha">
                            {carregandoHistorico ? (
                              <span className="vazio">Carregando historico...</span>
                            ) : historico.length === 0 ? (
                              <span className="vazio">Sem historico registrado.</span>
                            ) : (
                              <ul className="historico-lista">
                                {historico.map((h) => (
                                  <li key={h.id} className="historico-item">
                                    <strong>{h.status}</strong>
                                    <span> - {h.responsavel} - {h.dataAlteracao}</span>
                                    {h.observacao && <div className="historico-obs">{h.observacao}</div>}
                                  </li>
                                ))}
                              </ul>
                            )}
                          </td>
                        </tr>
                      )}
                    </Fragment>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </main>
    </div>
  )
}

export default AreaGestor
