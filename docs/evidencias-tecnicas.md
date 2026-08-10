# Evidências Técnicas — Frontend React (HackGov)

> Este documento traz trechos representativos do código-fonte implementado no frontend, complementando a descrição arquitetural apresentada no relatório da Fase 4. O objetivo é evidenciar, com código real, os três pontos destacados no feedback do mentor: (1) o cliente HTTP centralizado / interceptor de API, (2) o formulário com validação inline, e (3) o tratamento reativo de estados (carregando, sucesso, falha).

---

## 1. Cliente HTTP centralizado (Interceptor de API)

Todas as chamadas ao backend passam por um único ponto de saída, em `src/api/hackgovApi.js`. Isso garante desacoplamento entre os componentes visuais e a camada de comunicação, além de centralizar o tratamento de erros de rede e de respostas HTTP não bem-sucedidas.

```javascript
const URL_BASE = "http://localhost:8080/api";

// ---- funcao interna: trata a resposta (sucesso ou erro) de qualquer chamada ----
async function tratarResposta(resposta) {
  const corpo = await resposta.json().catch(() => ({}));
  if (!resposta.ok) {
    const mensagem =
      corpo.erro ||
      Object.values(corpo)[0] ||
      "Ocorreu um erro inesperado.";
    throw new Error(mensagem);
  }
  return corpo;
}

// ---- funcao interna: POST com JSON ----
async function enviarPost(caminho, dados) {
  let resposta;
  try {
    resposta = await fetch(URL_BASE + caminho, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(dados),
    });
  } catch {
    throw new Error("Nao foi possivel falar com o servidor. O backend esta rodando?");
  }
  return tratarResposta(resposta);
}

// ---- funcao interna: GET ----
async function buscarGet(caminho) {
  let resposta;
  try {
    resposta = await fetch(URL_BASE + caminho);
  } catch {
    throw new Error("Nao foi possivel falar com o servidor. O backend esta rodando?");
  }
  return tratarResposta(resposta);
}
```

Sobre esse conjunto de funções internas, dois pontos de design valem destaque:

- **Um único formato de erro em toda a aplicação.** Tanto `enviarPost` quanto `buscarGet` delegam o tratamento da resposta para `tratarResposta`, que decide entre extrair a mensagem de erro vinda do backend ou usar um fallback genérico. Nenhum componente da interface precisa saber como o backend formata seus erros.
- **Falha de rede tratada separadamente de erro de negócio.** Se o `fetch` falhar (backend fora do ar, sem conexão), a função lança uma mensagem específica ("Não foi possível falar com o servidor..."), diferente de um erro de validação retornado pelo backend (ex.: e-mail já cadastrado). Isso permite que a interface reaja de forma apropriada a cada cenário.

Por cima dessas duas funções internas, o arquivo expõe uma função por operação de negócio — é essa a API que os componentes React realmente importam e usam:

```javascript
// --- Autenticacao ---
export function cadastrar(dados) {
  return enviarPost("/cadastro", dados);
}

export function login(dados) {
  return enviarPost("/login", dados);
}

// Lista as categorias (para o menu suspenso do formulario).
export function listarCategorias() {
  return buscarGet("/categorias");
}

// Cria uma nova solicitacao.
export function criarSolicitacao(dados) {
  return enviarPost("/solicitacoes", dados);
}

// Lista as solicitacoes de um cidadao especifico.
export function listarSolicitacoesDoCidadao(idCidadao) {
  return buscarGet("/solicitacoes/cidadao/" + idCidadao);
}

// Lista TODAS as solicitacoes (painel do gestor).
export function listarTodasSolicitacoes() {
  return buscarGet("/solicitacoes");
}
```

**Por que isso é relevante:** qualquer componente que precise falar com o backend chama uma dessas funções exportadas (ex.: `criarSolicitacao`, `listarCategorias`) — nunca chama `fetch` diretamente. Isso significa que, se a URL base mudar, se um novo header de autenticação precisar ser adicionado, ou se o formato de erro do backend mudar, a alteração acontece em um único lugar (`hackgovApi.js`), sem tocar em nenhum componente visual.

---

## 2. Formulário com validação inline

O componente `AreaCidadao.jsx` (tela onde o cidadão registra uma nova ocorrência) implementa validação por campo, executada antes de qualquer chamada ao backend. Cada campo inválido recebe uma mensagem de erro específica, exibida logo abaixo dele, e o erro é removido automaticamente assim que o usuário começa a corrigir o campo.

```javascript
// Estado de validacao inline: um erro de texto por campo, indexado pelo nome do campo
const [erros, setErros] = useState({})

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
  // ... demais campos (logradouro, bairro, cidade)

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
```

No JSX, o campo exibe a mensagem de erro condicionalmente:

```jsx
<label>Titulo</label>
<input
  type="text"
  placeholder="Ex.: Buraco na rua"
  value={titulo}
  onChange={atualizarCampo(setTitulo, 'titulo')}
  className={erros.titulo ? 'campo-invalido' : ''}
/>
{erros.titulo && <span className="erro-campo">{erros.titulo}</span>}
```

**Por que isso é relevante:** o usuário recebe feedback imediato e específico por campo, sem precisar esperar a resposta do backend para descobrir que esqueceu de preencher algo — reduzindo requisições desnecessárias e melhorando a experiência de uso.

---

## 3. Tratamento reativo de estados (carregando / sucesso / falha)

O envio do formulário é controlado por dois estados React: `enviando` (booleano, controla o estado de carregamento) e `mensagem` (objeto com `tipo` e `texto`, controla o feedback de sucesso ou falha).

```javascript
const [enviando, setEnviando] = useState(false)
const [mensagem, setMensagem] = useState(null)

async function enviar() {
  setMensagem(null)

  if (!validarCampos()) {
    setMensagem({ tipo: 'erro', texto: 'Corrija os campos destacados antes de enviar.' })
    return
  }

  setEnviando(true)
  try {
    const nova = await criarSolicitacao({ /* ...dados do formulario... */ })
    setMensagem({
      tipo: 'sucesso',
      texto: `Solicitacao registrada! Protocolo ${nova.protocolo}.`,
    })
    recarregarLista()
  } catch (erro) {
    setMensagem({ tipo: 'erro', texto: erro.message })
  } finally {
    setEnviando(false)
  }
}
```

O botão de envio reflete o estado `enviando` diretamente na interface, ficando desabilitado durante a requisição:

```jsx
<button className="botao" onClick={enviar} disabled={enviando}>
  {enviando ? 'Enviando...' : 'Registrar solicitacao'}
</button>
```

E a mensagem de sucesso/falha é renderizada condicionalmente no topo da página:

```jsx
{mensagem && (
  <div className={`alerta alerta-${mensagem.tipo} alerta-topo`}>
    {mensagem.texto}
  </div>
)}
```

**Por que isso é relevante:** o usuário sempre sabe em que estado a aplicação está — enviando, com erro, ou com sucesso — sem telas em branco ou comportamento ambíguo durante a espera pela resposta do backend.

---

## Onde encontrar no repositório

| Trecho | Arquivo |
|---|---|
| Cliente HTTP / interceptor | `src/api/hackgovApi.js` |
| Formulário com validação inline e estados reativos | `src/pages/AreaCidadao.jsx` |
