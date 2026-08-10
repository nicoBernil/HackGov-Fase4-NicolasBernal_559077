-- ============================================================================
--  HackGov - Dados iniciais (rode DEPOIS do schema.sql)
-- ----------------------------------------------------------------------------
--  Insere as categorias e os status basicos do sistema.
--  OBS: o usuario GESTOR de exemplo NAO e criado aqui. Ele e criado
--  automaticamente pela aplicacao Java na primeira vez que voce roda o
--  projeto (assim a senha ja vai criptografada corretamente).
-- ============================================================================

-- -------------------------- CATEGORIAS --------------------------------------
INSERT INTO CATEGORIA (ID_CATEGORIA, NOME) VALUES (SEQ_CATEGORIA.NEXTVAL, 'Buraco na Via');
INSERT INTO CATEGORIA (ID_CATEGORIA, NOME) VALUES (SEQ_CATEGORIA.NEXTVAL, 'Iluminacao Publica');
INSERT INTO CATEGORIA (ID_CATEGORIA, NOME) VALUES (SEQ_CATEGORIA.NEXTVAL, 'Coleta de Lixo');
INSERT INTO CATEGORIA (ID_CATEGORIA, NOME) VALUES (SEQ_CATEGORIA.NEXTVAL, 'Saneamento e Esgoto');
INSERT INTO CATEGORIA (ID_CATEGORIA, NOME) VALUES (SEQ_CATEGORIA.NEXTVAL, 'Arborizacao e Pracas');
INSERT INTO CATEGORIA (ID_CATEGORIA, NOME) VALUES (SEQ_CATEGORIA.NEXTVAL, 'Sinalizacao e Transito');
INSERT INTO CATEGORIA (ID_CATEGORIA, NOME) VALUES (SEQ_CATEGORIA.NEXTVAL, 'Outros');

-- ---------------------------- STATUS ----------------------------------------
INSERT INTO STATUS_SOLICITACAO (ID_STATUS, NOME, ORDEM) VALUES (SEQ_STATUS.NEXTVAL, 'Recebido',     1);
INSERT INTO STATUS_SOLICITACAO (ID_STATUS, NOME, ORDEM) VALUES (SEQ_STATUS.NEXTVAL, 'Em Analise',   2);
INSERT INTO STATUS_SOLICITACAO (ID_STATUS, NOME, ORDEM) VALUES (SEQ_STATUS.NEXTVAL, 'Em Andamento', 3);
INSERT INTO STATUS_SOLICITACAO (ID_STATUS, NOME, ORDEM) VALUES (SEQ_STATUS.NEXTVAL, 'Concluido',    4);

-- Salva tudo no banco
COMMIT;

-- Conferindo o que foi inserido:
SELECT * FROM CATEGORIA ORDER BY ID_CATEGORIA;
SELECT * FROM STATUS_SOLICITACAO ORDER BY ORDEM;
