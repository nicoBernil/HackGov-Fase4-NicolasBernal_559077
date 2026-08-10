import { useState, useEffect } from 'react'
import { listarTodasSolicitacoes } from '../api/hackgovApi'

function AreaGestor({ usuario, aoSair }) {
  const [solicitacoes, setSolicitacoes] = useState([])
  const [mensagem, setMensagem] = useState(null)
  const [carregando, setCarregando] = useState(true)

  useEffect(() => {
    listarTodasSolicitacoes()
      .then((lista) => {
        setSolicitacoes(lista)
        setCarregando(false)
      })
      .catch((e) => {
        setMensagem({ tipo: 'erro', texto: e.message })
        setCarregando(false)
      })
  }, [])

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
        </div>

        <section className="cartao">
          <p className="eyebrow">Demandas recebidas</p>
          <h2>Todas as solicitacoes</h2>

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
                    <th>Status</th>
                    <th>Aberta em</th>
                  </tr>
                </thead>
                <tbody>
                  {solicitacoes.map((s) => (
                    <tr key={s.id}>
                      <td>{s.protocolo}</td>
                      <td>{s.titulo}</td>
                      <td>{s.cidadao}</td>
                      <td>{s.categoria}</td>
                      <td><span className="badge">{s.status}</span></td>
                      <td>{s.dataAbertura}</td>
                    </tr>
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
