
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
