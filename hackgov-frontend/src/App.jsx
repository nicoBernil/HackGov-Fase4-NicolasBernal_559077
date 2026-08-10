import { useState } from 'react'
import { cadastrar, login } from './api/hackgovApi'
import AreaCidadao from './pages/AreaCidadao'
import AreaGestor from './pages/AreaGestor'

function App() {
  const [aba, setAba] = useState('entrar')

  // Campos de cadastro
  const [nome, setNome] = useState('')
  const [cpf, setCpf] = useState('')
  const [emailCad, setEmailCad] = useState('')
  const [senhaCad, setSenhaCad] = useState('')

  // Campos de login
  const [emailLogin, setEmailLogin] = useState('')
  const [senhaLogin, setSenhaLogin] = useState('')

  const [carregando, setCarregando] = useState(false)
  const [mensagem, setMensagem] = useState(null)
  const [usuario, setUsuario] = useState(null)

  function limparMensagem() {
    setMensagem(null)
  }

  async function aoCadastrar() {
    limparMensagem()
    setCarregando(true)
    try {
      const novo = await cadastrar({ nome, cpf, email: emailCad, senha: senhaCad })
      setMensagem({ tipo: 'sucesso', texto: `Conta criada para ${novo.nome}! Agora e so entrar.` })
      setNome(''); setCpf(''); setEmailCad(''); setSenhaCad('')
      setEmailLogin(novo.email)
      setAba('entrar')
    } catch (erro) {
      setMensagem({ tipo: 'erro', texto: erro.message })
    } finally {
      setCarregando(false)
    }
  }

  async function aoEntrar() {
    limparMensagem()
    setCarregando(true)
    try {
      const logado = await login({ email: emailLogin, senha: senhaLogin })
      setUsuario(logado)
      setSenhaLogin('')
    } catch (erro) {
      setMensagem({ tipo: 'erro', texto: erro.message })
    } finally {
      setCarregando(false)
    }
  }

  function sair() {
    setUsuario(null)
    setEmailLogin(''); setSenhaLogin('')
    limparMensagem()
  }

  // ===================== Usuario logado: vai para a area certa =====================
  if (usuario) {
    if (usuario.perfil === 'GESTOR') {
      return <AreaGestor usuario={usuario} aoSair={sair} />
    }
    return <AreaCidadao usuario={usuario} aoSair={sair} />
  }

  // ===================== Tela de login / cadastro =====================
  return (
    <div className="pagina">
      <header className="topo">
        <span className="marca">HackGov</span>
        <span className="topo-sub">Canal do cidadao</span>
      </header>

      <main className="conteudo">
        <div className="cartao">
          <p className="eyebrow">Prefeitura digital</p>
          <h1>Solicitacoes urbanas</h1>
          <p className="subtitulo">
            Registre problemas da sua cidade e acompanhe o atendimento.
          </p>

          <div className="abas">
            <button
              className={aba === 'entrar' ? 'aba ativa' : 'aba'}
              onClick={() => { setAba('entrar'); limparMensagem() }}
            >
              Entrar
            </button>
            <button
              className={aba === 'criar' ? 'aba ativa' : 'aba'}
              onClick={() => { setAba('criar'); limparMensagem() }}
            >
              Criar conta
            </button>
          </div>

          {mensagem && (
            <div className={`alerta alerta-${mensagem.tipo}`}>{mensagem.texto}</div>
          )}

          {aba === 'entrar' && (
            <div className="formulario">
              <label>E-mail</label>
              <input type="email" placeholder="seu@email.com"
                value={emailLogin} onChange={(e) => setEmailLogin(e.target.value)} />

              <label>Senha</label>
              <input type="password" placeholder="Sua senha"
                value={senhaLogin} onChange={(e) => setSenhaLogin(e.target.value)} />

              <button className="botao" onClick={aoEntrar} disabled={carregando}>
                {carregando ? 'Entrando...' : 'Entrar'}
              </button>

              <p className="dica">
                Dica: use o gestor de exemplo &mdash; gestor@hackgov.gov.br / gestor123
              </p>
            </div>
          )}

          {aba === 'criar' && (
            <div className="formulario">
              <label>Nome completo</label>
              <input type="text" placeholder="Maria Silva"
                value={nome} onChange={(e) => setNome(e.target.value)} />

              <label>CPF (somente numeros)</label>
              <input type="text" placeholder="12345678901"
                value={cpf} onChange={(e) => setCpf(e.target.value)} />

              <label>E-mail</label>
              <input type="email" placeholder="seu@email.com"
                value={emailCad} onChange={(e) => setEmailCad(e.target.value)} />

              <label>Senha (minimo 6 caracteres)</label>
              <input type="password" placeholder="Crie uma senha"
                value={senhaCad} onChange={(e) => setSenhaCad(e.target.value)} />

              <button className="botao" onClick={aoCadastrar} disabled={carregando}>
                {carregando ? 'Criando conta...' : 'Criar conta'}
              </button>
            </div>
          )}
        </div>

        <p className="rodape">HackGov &middot; Projeto academico de GovTech</p>
      </main>
    </div>
  )
}

export default App
