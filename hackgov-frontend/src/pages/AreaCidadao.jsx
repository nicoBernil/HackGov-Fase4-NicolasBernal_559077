import { useState, useEffect } from 'react'
import {
  listarCategorias,
  criarSolicitacao,
  listarSolicitacoesDoCidadao,
} from '../api/hackgovApi'

function AreaCidadao({ usuario, aoSair }) {
  // Listas vindas do backend
  const [categorias, setCategorias] = useState([])
  const [solicitacoes, setSolicitacoes] = useState([])

  // Campos do formulario
  const [titulo, setTitulo] = useState('')
  const [descricao, setDescricao] = useState('')
  const [idCategoria, setIdCategoria] = useState('')
  const [logradouro, setLogradouro] = useState('')
  const [bairro, setBairro] = useState('')
  const [cidade, setCidade] = useState('')

  // Estados de integracao
  const [enviando, setEnviando] = useState(false)
  const [mensagem, setMensagem] = useState(null)

  // Estado de validacao inline: um erro de texto por campo, indexado pelo nome do campo
  const [erros, setErros] = useState({})

  // Carrega categorias e a lista de solicitacoes quando a tela abre.
  useEffect(() => {
    listarCategorias()
      .then(setCategorias)
      .catch((e) => setMensagem({ tipo: 'erro', texto: e.message }))
    recarregarLista()
  }, [])

  function recarregarLista() {
    listarSolicitacoesDoCidadao(usuario.id)
      .then(setSolicitacoes)
      .catch((e) => setMensagem({ tipo: 'erro', texto: e.message }))
  }

  // ---- Validacao inline: roda antes de enviar, um erro por campo ----
  function validarCampos() {
    const novosErros = {}

    if (!titulo.trim()) {
      novosErros.titulo = 'Informe um titulo para a solicitacao.'
    }
    if (!descricao.trim() || descricao.trim().length < 10) {
      novosErros.descricao = 'Descreva o problema com pelo menos 10 caracteres.'
    }
    if (!idCategoria) {
      novosErros.idCategoria = 'Selecione uma categoria.'
    }
    if (!logradouro.trim()) {
      novosErros.logradouro = 'Informe o logradouro.'
    }
    if (!bairro.trim()) {
      novosErros.bairro = 'Informe o bairro.'
    }
    if (!cidade.trim()) {
      novosErros.cidade = 'Informe a cidade.'
    }

    setErros(novosErros)
    return Object.keys(novosErros).length === 0
  }

  // Helper: atualiza um campo e limpa o erro dele assim que o usuario comeca a corrigir
  function atualizarCampo(setter, nomeDoCampo) {
    return (e) => {
      setter(e.target.value)
      if (erros[nomeDoCampo]) {
        setErros((atual) => {
          const copia = { ...atual }
          delete copia[nomeDoCampo]
          return copia
        })
      }
    }
  }

  async function enviar() {
    setMensagem(null)

    // Validacao inline roda antes de qualquer chamada ao backend
    if (!validarCampos()) {
      setMensagem({ tipo: 'erro', texto: 'Corrija os campos destacados antes de enviar.' })
      return
    }

    setEnviando(true)
    try {
      const nova = await criarSolicitacao({
        titulo,
        descricao,
        idCategoria: Number(idCategoria),
        logradouro,
        bairro,
        cidade,
        idCidadao: usuario.id,
      })
      setMensagem({
        tipo: 'sucesso',
        texto: `Solicitacao registrada! Protocolo ${nova.protocolo}.`,
      })
      // limpa o formulario, os erros e atualiza a lista
      setTitulo(''); setDescricao(''); setIdCategoria('')
      setLogradouro(''); setBairro(''); setCidade('')
      setErros({})
      recarregarLista()
    } catch (erro) {
      setMensagem({ tipo: 'erro', texto: erro.message })
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="pagina">
      <header className="topo">
        <span className="marca">HackGov</span>
        <div className="topo-direita">
          <span className="topo-sub">Ola, {usuario.nome.split(' ')[0]}</span>
          <button className="botao-link" onClick={aoSair}>Sair</button>
        </div>
      </header>

      <main className="conteudo conteudo-largo">
        {mensagem && (
          <div className={`alerta alerta-${mensagem.tipo} alerta-topo`}>
            {mensagem.texto}
          </div>
        )}

        <div className="grade">
          {/* ---------------- Formulario ---------------- */}
          <section className="cartao">
            <p className="eyebrow">Nova demanda</p>
            <h2>Registrar solicitacao</h2>

            <div className="formulario">
              <label>Titulo</label>
              <input
                type="text"
                placeholder="Ex.: Buraco na rua"
                value={titulo}
                onChange={atualizarCampo(setTitulo, 'titulo')}
                className={erros.titulo ? 'campo-invalido' : ''}
              />
              {erros.titulo && <span className="erro-campo">{erros.titulo}</span>}

              <label>Descricao</label>
              <textarea
                rows="3"
                placeholder="Descreva o problema com detalhes"
                value={descricao}
                onChange={atualizarCampo(setDescricao, 'descricao')}
                className={erros.descricao ? 'campo-invalido' : ''}
              />
              {erros.descricao && <span className="erro-campo">{erros.descricao}</span>}

              <label>Categoria</label>
              <select
                value={idCategoria}
                onChange={atualizarCampo(setIdCategoria, 'idCategoria')}
                className={erros.idCategoria ? 'campo-invalido' : ''}
              >
                <option value="">Selecione...</option>
                {categorias.map((c) => (
                  <option key={c.id} value={c.id}>{c.nome}</option>
                ))}
              </select>
              {erros.idCategoria && <span className="erro-campo">{erros.idCategoria}</span>}

              <label>Logradouro</label>
              <input
                type="text"
                placeholder="Rua, avenida..."
                value={logradouro}
                onChange={atualizarCampo(setLogradouro, 'logradouro')}
                className={erros.logradouro ? 'campo-invalido' : ''}
              />
              {erros.logradouro && <span className="erro-campo">{erros.logradouro}</span>}

              <div className="linha-dupla">
                <div>
                  <label>Bairro</label>
                  <input
                    type="text"
                    value={bairro}
                    onChange={atualizarCampo(setBairro, 'bairro')}
                    className={erros.bairro ? 'campo-invalido' : ''}
                  />
                  {erros.bairro && <span className="erro-campo">{erros.bairro}</span>}
                </div>
                <div>
                  <label>Cidade</label>
                  <input
                    type="text"
                    value={cidade}
                    onChange={atualizarCampo(setCidade, 'cidade')}
                    className={erros.cidade ? 'campo-invalido' : ''}
                  />
                  {erros.cidade && <span className="erro-campo">{erros.cidade}</span>}
                </div>
              </div>

              <button className="botao" onClick={enviar} disabled={enviando}>
                {enviando ? 'Enviando...' : 'Registrar solicitacao'}
              </button>
            </div>
          </section>

          {/* ---------------- Lista ---------------- */}
          <section className="cartao">
            <p className="eyebrow">Acompanhamento</p>
            <h2>Minhas solicitacoes</h2>

            {solicitacoes.length === 0 ? (
              <p className="vazio">Voce ainda nao registrou nenhuma solicitacao.</p>
            ) : (
              <div className="lista">
                {solicitacoes.map((s) => (
                  <div key={s.id} className="item">
                    <div className="item-topo">
                      <strong>{s.titulo}</strong>
                      <span className="badge">{s.status}</span>
                    </div>
                    <div className="item-protocolo">{s.protocolo}</div>
                    <div className="item-meta">
                      {s.categoria} &middot; {s.bairro}, {s.cidade}
                    </div>
                    <div className="item-data">Aberta em {s.dataAbertura}</div>
                  </div>
                ))}
              </div>
            )}
          </section>
        </div>
      </main>
    </div>
  )
}

export default AreaCidadao
