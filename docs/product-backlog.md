# Product Backlog — HackGov

> Consolidado para a Fase 5 do Enterprise Challenge. Reúne as user stories já entregues nas fases anteriores (marcadas como **Concluído**) e as novas user stories de front-end, segurança e manipulação de arquivos pedidas nesta atividade (marcadas como **Planejado** ou **Em andamento**). Critérios de aceitação estão detalhados para as histórias prioritárias da Parte 2.

## Legenda

- **Prioridade**: Alta / Média / Baixa
- **Status**: Concluído · Em andamento · Planejado

---

## 1. Histórias já entregues (Fases 2–4)

| ID | User Story | Prioridade | Status |
|---|---|---|---|
| US01 | Como cidadão, quero me cadastrar com nome, CPF, e-mail e senha, para acessar o sistema com uma conta própria. | Alta | Concluído |
| US02 | Como usuário (cidadão ou gestor), quero fazer login com e-mail e senha, para acessar a área correspondente ao meu perfil. | Alta | Concluído |
| US03 | Como cidadão, quero registrar uma nova solicitação com título, descrição, categoria e endereço, para relatar um problema urbano. | Alta | Concluído |
| US04 | Como cidadão, quero receber um número de protocolo ao registrar uma solicitação, para acompanhar meu pedido depois. | Média | Concluído |
| US05 | Como cidadão, quero ver a lista de todas as solicitações que já registrei, com status atual, para acompanhar o andamento. | Alta | Concluído |
| US06 | Como gestor, quero ver todas as solicitações registradas por todos os cidadãos, para priorizar o atendimento. | Alta | Concluído |
| US07 | Como usuário, quero que minha senha seja armazenada de forma criptografada (hash), para que ela não fique exposta no banco de dados. | Alta | Concluído |
| US08 | Como cidadão, quero ver mensagens de erro específicas por campo ao preencher o formulário, para corrigir rapidamente o que faltou. | Média | Concluído |
| US09 | Como cidadão, quero que a descrição do meu problema seja analisada automaticamente para sugerir categoria e prioridade, para não precisar classificar sozinho e para o gestor priorizar melhor os casos urgentes. | Alta | Concluído |

---

## 2. Novas user stories — Front-end

| ID | User Story | Prioridade | Status |
|---|---|---|---|
| US10 | Como gestor, quero ver as solicitações do painel já ordenadas por prioridade (ALTA primeiro), para atender primeiro os casos mais urgentes sem precisar reordenar manualmente. | Alta | Planejado |
| US11 | Como gestor, quero alterar o status de uma solicitação (Recebido → Em Análise → Em Andamento → Concluído) diretamente pelo painel, para manter o andamento atualizado sem depender de acesso ao banco de dados. | Alta | Planejado |
| US12 | Como cidadão, quero ver um indicador visual de prioridade (cor/selo) em cada solicitação da minha lista, para entender a urgência atribuída ao meu caso. | Média | Planejado |
| US13 | Como usuário, quero que a aplicação me avise de forma clara quando o backend estiver fora do ar, para não pensar que é um erro do meu formulário. | Baixa | Concluído |
| US14 | Como gestor, quero ver um painel com estatísticas (quantidade por categoria, por prioridade, tempo médio de atendimento), para embasar decisões de alocação de equipe. | Alta | Planejado |

---

## 3. Novas user stories — Segurança

| ID | User Story | Prioridade | Status |
|---|---|---|---|
| US15 | Como sistema, quero impedir que um cidadão autenticado acesse os dados de solicitações de outro cidadão, para proteger a privacidade dos dados pessoais. | Alta | Planejado |
| US16 | Como sistema, quero impedir que qualquer usuário não autenticado como gestor acesse o endpoint que lista todas as solicitações, para que dados de todos os cidadãos não fiquem expostos publicamente. | Alta | Planejado |
| US17 | Como sistema, quero registrar uma trilha de auditoria sempre que uma solicitação tiver o status alterado ou for excluída, para permitir rastrear quem fez o quê e quando. | Alta | Planejado |
| US18 | Como administrador do sistema, quero que a chave de acesso a serviços externos (ex.: IA) não fique escrita diretamente no código-fonte, para reduzir o risco de vazamento de credenciais. | Média | Concluído |
| US19 | Como usuário, quero que tentativas de login incorretas não revelem se o e-mail existe ou não na base, para dificultar ataques de enumeração de contas. | Média | Concluído |

---

## 4. Novas user stories — Manipulação de arquivos

| ID | User Story | Prioridade | Status |
|---|---|---|---|
| US20 | Como cidadão, quero anexar uma foto do problema ao registrar uma solicitação, para dar mais evidência visual ao gestor responsável pela análise. | Alta | Planejado |
| US21 | Como gestor, quero baixar/visualizar o anexo enviado pelo cidadão ao abrir os detalhes de uma solicitação, para avaliar o caso com mais contexto. | Alta | Planejado |
| US22 | Como gestor, quero exportar a lista de solicitações filtradas (ex.: por status ou período) em um arquivo (CSV), para levar os dados para análise externa ou para relatórios da prefeitura. | Média | Planejado |
| US23 | Como sistema, quero validar tipo e tamanho do arquivo anexado antes de salvar, para impedir upload de arquivos maliciosos ou excessivamente grandes. | Alta | Planejado |

---

## 5. Critérios de aceitação — Histórias prioritárias

### US10 — Painel do gestor ordenado por prioridade

- Dado que existem solicitações com prioridades ALTA, MEDIA e BAIXA, quando o gestor abre o painel, então a lista deve exibir primeiro todas as de prioridade ALTA, depois MEDIA, depois BAIXA.
- Dentro de uma mesma prioridade, as solicitações mais antigas aparecem primeiro (ordem de chegada).
- A ordenação deve ser feita no backend (não depender de lógica no front-end), para garantir consistência entre diferentes clientes.

### US15 / US16 — Controle de acesso por perfil

- Dado um cidadão autenticado, quando ele tenta acessar as solicitações de outro cidadão (por ID), então o sistema retorna erro 403 (Forbidden).
- Dado um usuário sem perfil de gestor, quando ele tenta acessar o endpoint `GET /api/solicitacoes` (lista completa), então o sistema retorna erro 403.
- Dado um gestor autenticado, quando ele acessa qualquer endpoint de solicitações, então o acesso é permitido normalmente.
- Todo acesso negado deve gerar um registro de log técnico com data/hora, endpoint chamado e motivo da negação.

### US17 — Trilha de auditoria

- Toda mudança de status de uma solicitação gera um registro de auditoria contendo: quem alterou, quando, status anterior, status novo.
- Toda exclusão de registro gera um registro de auditoria contendo: quem excluiu, quando, e um resumo do registro excluído.
- Os registros de auditoria não podem ser alterados nem excluídos por nenhum endpoint da API (somente leitura após criados).
- Deve existir um endpoint restrito a gestores para consultar a trilha de auditoria de uma solicitação específica.

### US20 / US23 — Anexo de foto na solicitação

- Dado um cidadão preenchendo o formulário de nova solicitação, quando ele seleciona um arquivo de imagem (JPEG ou PNG, até 5 MB), então o arquivo é enviado junto com a solicitação e associado a ela.
- Dado um arquivo que não é imagem (ex.: `.exe`, `.zip`) ou que excede 5 MB, quando o cidadão tenta enviar, então o sistema recusa o upload com uma mensagem clara, antes de salvar qualquer coisa no servidor.
- O anexo enviado deve poder ser visualizado pelo gestor ao abrir os detalhes da solicitação correspondente.

### US14 — Painel de estatísticas

- O painel exibe, no mínimo: quantidade de solicitações por categoria, quantidade por prioridade, e tempo médio entre abertura e conclusão (em dias).
- Os números refletem os dados reais do banco no momento da consulta (não são fixos/mockados).
- Os dados são apresentados com pelo menos um gráfico (barras ou pizza) além da tabela numérica.
