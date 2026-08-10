-- ============================================================================
--  HackGov - Script de criacao do banco de dados (Oracle)
-- ----------------------------------------------------------------------------
--  COMO USAR:
--  1) Abra o Oracle SQL Developer (ou sqlplus) conectado no seu usuario.
--  2) Rode este arquivo INTEIRO uma unica vez (botao "Run Script" / F5).
--  3) Depois rode o arquivo dados_iniciais.sql para inserir os dados basicos.
--
--  Se precisar APAGAR tudo e recriar do zero, descomente o bloco "LIMPEZA"
--  abaixo e rode antes. O Oracle nao tem "DROP IF EXISTS", entao se a tabela
--  nao existir ele vai reclamar - pode ignorar esse erro especifico.
-- ============================================================================

-- ----------------------- LIMPEZA (opcional) ---------------------------------
-- DROP TABLE HISTORICO_STATUS CASCADE CONSTRAINTS;
-- DROP TABLE SOLICITACAO     CASCADE CONSTRAINTS;
-- DROP TABLE USUARIO         CASCADE CONSTRAINTS;
-- DROP TABLE CATEGORIA       CASCADE CONSTRAINTS;
-- DROP TABLE STATUS_SOLICITACAO CASCADE CONSTRAINTS;
-- DROP SEQUENCE SEQ_USUARIO;
-- DROP SEQUENCE SEQ_CATEGORIA;
-- DROP SEQUENCE SEQ_STATUS;
-- DROP SEQUENCE SEQ_SOLICITACAO;
-- DROP SEQUENCE SEQ_HISTORICO;
-- ----------------------------------------------------------------------------


-- ===========================================================================
--  TABELA: USUARIO
--  Guarda cidadaos e gestores. A senha NUNCA fica em texto puro: salvamos
--  apenas o hash (SENHA_HASH). O campo PERFIL separa quem e CIDADAO de GESTOR.
-- ===========================================================================
CREATE TABLE USUARIO (
    ID_USUARIO    NUMBER(10)        NOT NULL,
    NOME          VARCHAR2(120)     NOT NULL,
    CPF           VARCHAR2(11)      NOT NULL,
    EMAIL         VARCHAR2(150)     NOT NULL,
    SENHA_HASH    VARCHAR2(100)     NOT NULL,
    PERFIL        VARCHAR2(10)      NOT NULL,
    DATA_CRIACAO  DATE DEFAULT SYSDATE NOT NULL,

    CONSTRAINT PK_USUARIO        PRIMARY KEY (ID_USUARIO),
    CONSTRAINT UK_USUARIO_CPF    UNIQUE (CPF),
    CONSTRAINT UK_USUARIO_EMAIL  UNIQUE (EMAIL),
    CONSTRAINT CK_USUARIO_PERFIL CHECK (PERFIL IN ('CIDADAO', 'GESTOR'))
);

CREATE SEQUENCE SEQ_USUARIO START WITH 1 INCREMENT BY 1 NOCACHE;


-- ===========================================================================
--  TABELA: CATEGORIA
--  Tipos de problema urbano (ex.: "Buraco na Via", "Iluminacao Publica").
--  E uma "tabela de apoio" (lookup) referenciada pela SOLICITACAO.
-- ===========================================================================
CREATE TABLE CATEGORIA (
    ID_CATEGORIA  NUMBER(5)     NOT NULL,
    NOME          VARCHAR2(80)  NOT NULL,

    CONSTRAINT PK_CATEGORIA      PRIMARY KEY (ID_CATEGORIA),
    CONSTRAINT UK_CATEGORIA_NOME UNIQUE (NOME)
);

CREATE SEQUENCE SEQ_CATEGORIA START WITH 1 INCREMENT BY 1 NOCACHE;


-- ===========================================================================
--  TABELA: STATUS_SOLICITACAO
--  Etapas do atendimento (Recebido, Em Analise, Em Andamento, Concluido).
--  O campo ORDEM ajuda a exibir/ordenar o fluxo na tela.
-- ===========================================================================
CREATE TABLE STATUS_SOLICITACAO (
    ID_STATUS  NUMBER(5)     NOT NULL,
    NOME       VARCHAR2(40)  NOT NULL,
    ORDEM      NUMBER(3)     NOT NULL,

    CONSTRAINT PK_STATUS      PRIMARY KEY (ID_STATUS),
    CONSTRAINT UK_STATUS_NOME UNIQUE (NOME)
);

CREATE SEQUENCE SEQ_STATUS START WITH 1 INCREMENT BY 1 NOCACHE;


-- ===========================================================================
--  TABELA: SOLICITACAO
--  A demanda urbana registrada pelo cidadao. Tem chaves estrangeiras (FK)
--  para CATEGORIA, STATUS e para o USUARIO que abriu (cidadao).
-- ===========================================================================
CREATE TABLE SOLICITACAO (
    ID_SOLICITACAO  NUMBER(10)      NOT NULL,
    PROTOCOLO       VARCHAR2(20)    NOT NULL,
    TITULO          VARCHAR2(120)   NOT NULL,
    DESCRICAO       VARCHAR2(1000)  NOT NULL,
    LOGRADOURO      VARCHAR2(150)   NOT NULL,
    BAIRRO          VARCHAR2(100)   NOT NULL,
    CIDADE          VARCHAR2(100)   NOT NULL,
    PRIORIDADE      VARCHAR2(10)    NOT NULL,
    ID_CATEGORIA    NUMBER(5)       NOT NULL,
    ID_STATUS       NUMBER(5)       NOT NULL,
    ID_CIDADAO      NUMBER(10)      NOT NULL,
    DATA_ABERTURA   DATE DEFAULT SYSDATE NOT NULL,
    DATA_CONCLUSAO  DATE,

    CONSTRAINT PK_SOLICITACAO         PRIMARY KEY (ID_SOLICITACAO),
    CONSTRAINT UK_SOLICITACAO_PROTOCOLO UNIQUE (PROTOCOLO),
    CONSTRAINT CK_SOLICITACAO_PRIOR   CHECK (PRIORIDADE IN ('BAIXA', 'MEDIA', 'ALTA')),
    CONSTRAINT FK_SOLIC_CATEGORIA     FOREIGN KEY (ID_CATEGORIA) REFERENCES CATEGORIA (ID_CATEGORIA),
    CONSTRAINT FK_SOLIC_STATUS        FOREIGN KEY (ID_STATUS)    REFERENCES STATUS_SOLICITACAO (ID_STATUS),
    CONSTRAINT FK_SOLIC_CIDADAO       FOREIGN KEY (ID_CIDADAO)   REFERENCES USUARIO (ID_USUARIO)
);

CREATE SEQUENCE SEQ_SOLICITACAO START WITH 1 INCREMENT BY 1 NOCACHE;


-- ===========================================================================
--  TABELA: HISTORICO_STATUS
--  Cada vez que o gestor muda o status de uma solicitacao, gravamos uma linha
--  aqui (rastreabilidade / auditoria). Guarda quem mudou, para qual status,
--  quando e a observacao.
-- ===========================================================================
CREATE TABLE HISTORICO_STATUS (
    ID_HISTORICO    NUMBER(10)      NOT NULL,
    ID_SOLICITACAO  NUMBER(10)      NOT NULL,
    ID_STATUS       NUMBER(5)       NOT NULL,
    ID_GESTOR       NUMBER(10)      NOT NULL,
    OBSERVACAO      VARCHAR2(500),
    DATA_ALTERACAO  DATE DEFAULT SYSDATE NOT NULL,

    CONSTRAINT PK_HISTORICO       PRIMARY KEY (ID_HISTORICO),
    CONSTRAINT FK_HIST_SOLICITACAO FOREIGN KEY (ID_SOLICITACAO) REFERENCES SOLICITACAO (ID_SOLICITACAO),
    CONSTRAINT FK_HIST_STATUS     FOREIGN KEY (ID_STATUS)     REFERENCES STATUS_SOLICITACAO (ID_STATUS),
    CONSTRAINT FK_HIST_GESTOR     FOREIGN KEY (ID_GESTOR)     REFERENCES USUARIO (ID_USUARIO)
);

CREATE SEQUENCE SEQ_HISTORICO START WITH 1 INCREMENT BY 1 NOCACHE;


-- Confirma a criacao das estruturas
COMMIT;
