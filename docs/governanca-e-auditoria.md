# Parte 5 — Governança, controle de acesso e auditoria

## 1. Como o controle de acesso foi implementado

O HackGov não usa o Spring Security completo (só o `BCryptPasswordEncoder`, em
`SegurancaConfig`), então o controle de acesso foi implementado de forma
simples e propositalmente visível, para poder ser explicado na Banca:

- Depois do login, o front-end guarda o usuário logado em memória
  (`definirSessao()` em `hackgovApi.js`) e passa a enviar o cabeçalho
  `X-Usuario-Id` em toda chamada ao back-end.
- Um interceptor (`AutorizacaoInterceptor`, pacote `security`) intercepta
  toda requisição em `/api/**` (exceto `/login`, `/cadastro` e `/categorias`,
  que são públicas) e:
  1. Exige o cabeçalho `X-Usuario-Id` e confirma que o usuário existe no banco;
  2. Disponibiliza esse usuário para o controller (`usuarioAutenticado`);
  3. Se o método do controller tiver a anotação `@ExigePerfil(Perfil.GESTOR)`,
     bloqueia quem não for GESTOR.
- Endpoints que não são "gestor-only" (como ver as próprias solicitações ou o
  próprio histórico) fazem uma checagem extra dentro do controller: o dado só
  é liberado se quem pediu for o GESTOR **ou** o próprio cidadão dono daquele
  dado. Isso impede que o cidadão A veja as solicitações ou o histórico do
  cidadão B.
- Qualquer falha nessas checagens lança `AcessoNegadoException`, que vira uma
  resposta HTTP 403 (tratada no `GlobalExceptionHandler`).

## 2. Tabela de permissões (perfil × funcionalidade × dado pessoal)

| Funcionalidade | Endpoint | CIDADÃO | GESTOR | Acessa dado pessoal? |
|---|---|---|---|---|
| Cadastro e login | `POST /cadastro`, `POST /login` | Sim (o próprio) | Sim (o próprio) | Sim — nome, CPF, e-mail (senha nunca trafega em texto puro nem é devolvida) |
| Listar categorias | `GET /categorias` | Sim | Sim | Não |
| Sugestão da IA (categoria/prioridade) | `POST /solicitacoes/classificar-ia` | Sim | Sim | Não (só analisa o texto digitado, não persiste nada) |
| Registrar solicitação | `POST /solicitacoes` | Sim, só em nome próprio | Não é o público-alvo | Sim — endereço, descrição do problema |
| Ver minhas solicitações | `GET /solicitacoes/cidadao/{id}` | Sim, só as próprias | Sim, de qualquer cidadão | Sim |
| Ver TODAS as solicitações (painel) | `GET /solicitacoes` | Não | Sim | Sim |
| Alterar status de uma solicitação | `PATCH /solicitacoes/{id}/status` | Não | Sim | Sim (gera registro de auditoria) |
| Desfazer última alteração de status | `POST /solicitacoes/{id}/status/desfazer` | Não | Sim | Sim (gera novo registro de auditoria) |
| Ver histórico/linha do tempo | `GET /solicitacoes/{id}/historico` | Sim, só da própria solicitação | Sim, de qualquer solicitação | Sim |

## 3. Quais operações exigem trilha de auditoria

Toda operação que **muda o estado de uma solicitação** grava um registro em
`HISTORICO_STATUS` (via `HistoricoStatusService.registrar`):

- Abertura da solicitação pelo cidadão (status inicial "Recebido");
- Toda alteração de status feita pelo gestor;
- Todo "desfazer", que **não apaga** o registro anterior — apenas empilha um
  novo registro apontando de volta para o status anterior (ver a pilha em
  `HistoricoStatusService`).

Operações somente de leitura (listar, ver histórico, pedir sugestão da IA)
não geram registro de auditoria — só o que **altera dado** entra na trilha.
Isso segue o princípio de que auditoria serve para reconstruir "quem mudou o
quê, quando", não para logar toda leitura.

## 4. Log técnico × registro de auditoria

| | Log técnico | Registro de auditoria (`HISTORICO_STATUS`) |
|---|---|---|
| Para que serve | Depurar erros, medir desempenho, monitorar a aplicação | Provar, de forma confiável, quem fez qual mudança de status e quando |
| Onde fica | Console/arquivo de log do Spring Boot (efêmero, roda no servidor) | Tabela do banco de dados, persistente |
| Quem pode alterar depois de gravado | Não é preocupação — logs técnicos podem até ser descartados | Ninguém: é append-only, nenhum endpoint faz UPDATE/DELETE nela |
| Conteúdo típico | Stack trace, tempo de resposta, SQL executado | Solicitação, status novo, quem fez (`ID_GESTOR`), quando, observação |
| Nível de detalhe | Alto, pensado para quem desenvolve | Objetivo, pensado para responder "o que aconteceu com o pedido X" |

## 5. Medidas para reduzir a exposição de dados pela API

- **Nunca devolver a senha**: `SENHA_HASH` nunca aparece em nenhum DTO de
  resposta (`UsuarioResponse` só expõe id, nome e perfil).
- **Mensagem de erro genérica no login**: sucesso/falha de autenticação usa a
  mesma mensagem, para não revelar se o e-mail existe ou não (evita
  enumeração de contas).
- **DTOs em vez de expor a entidade JPA direto**: cada resposta usa um
  `record` específico (`SolicitacaoResponse`, `HistoricoStatusResponse`
  etc.), então só os campos realmente necessários trafegam pela API — nunca
  o objeto inteiro do banco.
- **Controle de acesso por dono do dado**: como descrito acima, um cidadão
  nunca recebe dados de outro cidadão, mesmo que troque o `{id}` na URL
  manualmente.
- **CORS restrito**: `CorsConfig` só libera as origens conhecidas do
  front-end (`localhost:5173`/`:3000`), não `*`.
- **Hash de senha com custo (BCrypt)**: mesmo se o banco vazar, as senhas não
  ficam em texto puro nem em hash "rápido" (MD5/SHA1).
