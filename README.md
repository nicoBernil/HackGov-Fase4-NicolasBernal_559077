# HackGov — Canais Digitais

> Plataforma de canais digitais para registro, triagem inteligente e acompanhamento de ocorrências junto a órgãos públicos, com rastreabilidade criptográfica das transições de status.

**Autor:** Nicolas Bernal — RM 559077
**Cidade:** Rio de Janeiro
**Projeto:** Enterprise Challenge — HackGov — FIAP, Fase 4

---

## 📋 Sobre o projeto

O HackGov é uma solução GovTech que permite ao cidadão reportar ocorrências (ex.: buraco na via, iluminação pública, coleta de lixo, saneamento, arborização, sinalização e trânsito, entre outras) por meio de canais digitais, com triagem automatizada por prioridade e categoria, acompanhamento transparente do status e um perfil de gestor responsável pela análise.

### Principais funcionalidades

- **Cadastro e login** de cidadãos e gestores, com senha protegida por hash (BCrypt).
- **Registro de ocorrências** pelo cidadão, com formulário validado campo a campo (validação inline) e feedback reativo (carregando / sucesso / falha).
- **Triagem inteligente**: classificação de categoria e cálculo de prioridade por heurística de gatilhos textuais (ex.: *"risco"*, *"acidente"*, *"perigo"*).
- **Acompanhamento** das solicitações pelo próprio cidadão, com protocolo, status e histórico.
- **Painel do gestor** para análise e mudança de status das solicitações.
- **Segurança**: hashing de senha com BCrypt, controle de acesso por perfil (RBAC — `CIDADAO` / `GESTOR`), integridade referencial no banco de dados.
- **Rastreabilidade com blockchain** *(conceitual/planejada)*: ancoragem criptográfica do evento de transição de status, inspirada nos modelos KSI Blockchain (Estônia) e EBSI, sem exposição de dados pessoais no ledger — em conformidade com a LGPD.

---

## 🛠️ Tecnologias utilizadas

### Backend (`hackgov-backend`)

| Tecnologia | Versão |
|---|---|
| Java (JDK) | 24 |
| Spring Boot | 3.5.15 |
| Spring Data JPA / Hibernate | 6.6.53.Final |
| Spring Security Crypto (BCrypt) | 6.5.11 |
| Spring Boot Validation | 3.5.15 |
| Banco de dados | Oracle Database (XE, local) |
| Driver JDBC | Oracle `ojdbc11` 23.7.0.25.01 |
| Pool de conexões | HikariCP |
| Build | Maven |
| Servidor embutido | Tomcat (porta 8080) |

### Frontend (`hackgov-frontend`)

| Tecnologia | Versão |
|---|---|
| React | — |
| Build tool | Vite |
| Node.js | 24.x |
| npm | 11.x |
| Comunicação com API | `fetch` nativo, cliente centralizado em `src/api/hackgovApi.js` |

---

## 🏗️ Arquitetura

```
┌───────────────────┐   fetch (JSON/REST)   ┌────────────────────┐        ┌───────────────────┐
│  Frontend (React)  │ ─────────────────────▶│  Backend (Spring    │───────▶│  Oracle Database   │
│  hackgov-frontend  │◀───────────────────── │  Boot) hackgov-     │◀───────│  (XEPDB1)          │
│  Vite · porta 5173 │                        │  backend · porta    │        │  porta 1521        │
└───────────────────┘                        │  8080                │        └───────────────────┘
                                              └────────────────────┘
```

- **Frontend**: todas as chamadas HTTP passam por um único cliente (`src/api/hackgovApi.js`), que centraliza a base da URL, o parsing de respostas de erro e as funções de negócio (`login`, `cadastrar`, `criarSolicitacao`, `listarCategorias`, etc.). Nenhum componente chama `fetch` diretamente.
- **Backend**: organizado em pacotes por responsabilidade — `config`, `controller`, `dto`, `exception`, `model`, `repository`, `service` — dentro de `br.com.fiap.hackgov`.
- **Banco de dados**: schema criado manualmente via scripts SQL (não pelo Hibernate — `ddl-auto=none`, decisão proposital do time para praticar modelagem de banco).

---

## 📂 Estrutura do repositório

```
hackgov/
├── hackgov-backend/
│   ├── src/main/java/br/com/fiap/hackgov/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── exception/
│   │   ├── model/
│   │   ├── repository/
│   │   ├── service/
│   │   └── HackgovApplication.java
│   ├── src/main/resources/
│   │   ├── db/
│   │   │   ├── schema.sql        # cria tabelas e sequences
│   │   │   └── dados_iniciais.sql # popula categorias e status
│   │   └── application.properties
│   ├── requests.http             # coleção de requisições para testar a API
│   └── pom.xml
│
├── hackgov-frontend/
│   ├── src/
│   │   ├── api/
│   │   │   └── hackgovApi.js     # cliente HTTP único (ponto de saída da API)
│   │   ├── pages/
│   │   │   └── AreaCidadao.jsx   # tela do cidadão: registrar e acompanhar solicitações
│   │   └── ...
│   └── package.json
│
└── README.md
```

---

## 🚀 Como executar o projeto

### Pré-requisitos

- **JDK 24**
- **Maven** (ou o `mvnw` do projeto, se presente)
- **Node.js 18+** e npm
- **Oracle Database XE** rodando localmente (porta padrão `1521`, pluggable database `XEPDB1`) — via instalador ou Docker
- Um cliente SQL (SQL Developer, DBeaver, etc.) para rodar os scripts de banco
- Git

### 1. Clonar o repositório

```bash
git clone https://github.com/<seu-usuario>/<seu-repositorio>.git
cd <seu-repositorio>
```

### 2. Criar o schema e popular os dados iniciais no Oracle

Usando seu cliente SQL preferido, conectado ao seu Oracle local:

1. Execute o script completo de `hackgov-backend/src/main/resources/db/schema.sql` — cria as tabelas `USUARIO`, `CATEGORIA`, `STATUS_SOLICITACAO`, `SOLICITACAO`, `HISTORICO_STATUS` e suas sequences.
2. Execute o script completo de `hackgov-backend/src/main/resources/db/dados_iniciais.sql` — insere as 7 categorias e os 4 status padrão (Recebido, Em Análise, Em Andamento, Concluído).
3. Confirme com `SELECT COUNT(*) FROM CATEGORIA;` (deve retornar 7) e `SELECT COUNT(*) FROM STATUS_SOLICITACAO;` (deve retornar 4).

> ⚠️ Rode cada script apenas uma vez. Executar de novo gera erro (`ORA-00955` para tabelas já criadas, erro de `UNIQUE constraint` para dados já inseridos) — isso é esperado e pode ser ignorado se as tabelas/dados já existirem.

### 3. Configurar a conexão com o banco

Em `hackgov-backend/src/main/resources/application.properties`, ajuste para os dados do seu Oracle local:

```properties
spring.datasource.url=jdbc:oracle:thin:@//localhost:1521/XEPDB1
spring.datasource.username=<seu_usuario>
spring.datasource.password=<sua_senha>
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
```

> ⚠️ **Nunca commite credenciais reais no Git.** Em um ambiente real, mova esses valores para variáveis de ambiente.

### 4. Rodar o backend

```bash
cd hackgov-backend
./mvnw spring-boot:run
```

Ou, pela IDE (IntelliJ): abra `HackgovApplication.java` e clique em **Run**.

O backend sobe em `http://localhost:8080`, com a API disponível em `http://localhost:8080/api`.

### 5. Rodar o frontend

```bash
cd hackgov-frontend
npm install
npm run dev
```

O Vite vai indicar o endereço local no terminal (geralmente `http://localhost:5173`).

> A URL da API usada pelo frontend está definida em `src/api/hackgovApi.js` (`URL_BASE`). Confirme que aponta para `http://localhost:8080/api` antes de rodar.

---

## 🧠 Triagem inteligente

A triagem de ocorrências combina:

1. **Classificação de categoria** — associa a ocorrência a uma das categorias cadastradas.
2. **Cálculo de prioridade** — heurística baseada na presença de gatilhos textuais associados a urgência (ex.: *"risco"*, *"acidente"*, *"perigo"*) no relato do cidadão.

Mais detalhes técnicos e trechos de código estão em [`docs/evidencias-tecnicas.md`](./docs/evidencias-tecnicas.md).

---

## 🔐 Segurança

- Senhas armazenadas com **BCrypt** (`spring-security-crypto`).
- **RBAC**: a tabela `USUARIO` restringe o campo `PERFIL` a `CIDADAO` ou `GESTOR` via `CHECK constraint`, e o backend controla o acesso conforme o perfil autenticado.
- Chaves de integridade referencial entre `SOLICITACAO`, `CATEGORIA`, `STATUS_SOLICITACAO`, `USUARIO` e `HISTORICO_STATUS`.

---

## ⛓️ Rastreabilidade com Blockchain

As transições de status das ocorrências são o evento crítico pensado para ancoragem criptográfica, seguindo uma lógica inspirada nos modelos:

- **KSI Blockchain** (Estônia)
- **EBSI** (European Blockchain Services Infrastructure)

Apenas a "impressão digital técnica" (hash) da transição seria registrada — **nenhum dado pessoal (PII), texto livre ou anexo de mídia bruta** no ledger, em conformidade com a **LGPD**.

---

## 🧩 Modelo de dados

| Tabela | Descrição |
|---|---|
| `USUARIO` | Cidadãos e gestores (nome, CPF, e-mail, senha com hash, perfil) |
| `CATEGORIA` | Tipos de ocorrência (Buraco na Via, Iluminação Pública, Coleta de Lixo, Saneamento e Esgoto, Arborização e Praças, Sinalização e Trânsito, Outros) |
| `STATUS_SOLICITACAO` | Etapas do fluxo (Recebido, Em Análise, Em Andamento, Concluído) |
| `SOLICITACAO` | A ocorrência em si (protocolo, título, descrição, endereço, prioridade, categoria, status, cidadão) |
| `HISTORICO_STATUS` | Log de cada mudança de status, com o gestor responsável e observação |

---

## 🎥 Vídeo Pitch

Assista à demonstração funcional do projeto: **[link do vídeo pitch]**

---

## 👤 Equipe

| Nome | RM | Cidade |
|---|---|---|
| Nicolas Bernal | 559077 | Rio de Janeiro |

---

## 📄 Licença

É um projeto acadêmico sem licença de uso comercial
