
const URL_BASE = "http://localhost:8080/api";

// ---- Sessao atual (para o controle de acesso da Parte 5) ----
// Guarda so o essencial do usuario logado, em memoria (nao em localStorage).
// A cada chamada ao backend, mandamos o ID dele no cabecalho X-Usuario-Id,
// para o back-end saber QUEM esta pedindo e aplicar as regras de acesso
// (ex.: so o GESTOR pode ver todas as solicitacoes; um cidadao so ve as dele).
let usuarioAtual = null;

// Chame isso logo depois de um login/cadastro com sucesso (ex.: no App.jsx).
export function definirSessao(usuario) {
  usuarioAtual = usuario;
}

// Chame isso ao clicar em "Sair".
export function limparSessao() {
  usuarioAtual = null;
}

function cabecalhosAutenticacao() {
  if (!usuarioAtual) return {};
  return { "X-Usuario-Id": String(usuarioAtual.id) };
}

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
      headers: { "Content-Type": "application/json", ...cabecalhosAutenticacao() },
      body: JSON.stringify(dados),
    });
  } catch {
    throw new Error("Nao foi possivel falar com o servidor. O backend esta rodando?");
  }
  return tratarResposta(resposta);
}

// ---- funcao interna: POST sem corpo (ex.: desfazer) ----
async function enviarPostSemCorpo(caminho) {
  let resposta;
  try {
    resposta = await fetch(URL_BASE + caminho, {
      method: "POST",
      headers: { ...cabecalhosAutenticacao() },
    });
  } catch {
    throw new Error("Nao foi possivel falar com o servidor. O backend esta rodando?");
  }
  return tratarResposta(resposta);
}

// ---- funcao interna: PATCH com JSON ----
async function enviarPatch(caminho, dados) {
  let resposta;
  try {
    resposta = await fetch(URL_BASE + caminho, {
      method: "PATCH",
      headers: { "Content-Type": "application/json", ...cabecalhosAutenticacao() },
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
    resposta = await fetch(URL_BASE + caminho, {
      headers: { ...cabecalhosAutenticacao() },
    });
  } catch {
    throw new Error("Nao foi possivel falar com o servidor. O backend esta rodando?");
  }
  return tratarResposta(resposta);
}


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

// NOVO: pede para a IA sugerir categoria + prioridade a partir da descricao.
export function classificarComIa(descricao) {
  return enviarPost("/solicitacoes/classificar-ia", { descricao });
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

// NOVO: lista os status possiveis (para o <select> de mudar status).
export function listarStatusDisponiveis() {
  return buscarGet("/status");
}

// NOVO: relatorio estatistico (contagem por categoria/prioridade/status e
// tempo medio de resolucao) para o painel do gestor - Parte 4.
export function buscarEstatisticas() {
  return buscarGet("/estatisticas");
}

// NOVO: gestor altera o status de uma solicitacao (fica registrado no historico).
export function alterarStatusSolicitacao(idSolicitacao, dados) {
  return enviarPatch("/solicitacoes/" + idSolicitacao + "/status", dados);
}

// NOVO: desfaz a ultima alteracao de status (volta para o status anterior).
export function desfazerUltimaAlteracao(idSolicitacao, idGestor) {
  return enviarPostSemCorpo("/solicitacoes/" + idSolicitacao + "/status/desfazer?idGestor=" + idGestor);
}

// NOVO: historico completo (linha do tempo) de uma solicitacao.
export function listarHistoricoSolicitacao(idSolicitacao) {
  return buscarGet("/solicitacoes/" + idSolicitacao + "/historico");
}
