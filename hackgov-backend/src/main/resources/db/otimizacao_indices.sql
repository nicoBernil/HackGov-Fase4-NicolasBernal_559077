-- ============================================================================
--  HackGov - Otimizacao com indices (Fase 5 - Parte 4: eficiencia dos dados)
-- ----------------------------------------------------------------------------
--  POR QUE: o Oracle cria indice automaticamente para PRIMARY KEY e UNIQUE,
--  mas NAO para as chaves estrangeiras. Sem indice, cada consulta que filtra
--  por categoria, status, cidadao ou solicitacao precisa ler a tabela
--  INTEIRA (full table scan = O(n)). Com um indice B-tree, o Oracle desce
--  pela arvore ate as linhas certas em O(log n).
--
--  Rode este arquivo UMA VEZ (F5 / "Run Script"). E seguro: nao altera dados.
--  Se der "ORA-00955: nome ja esta sendo usado", o indice ja existe - ignore.
-- ============================================================================

-- "Minhas solicitacoes" (AreaCidadao): WHERE ID_CIDADAO = ? ORDER BY DATA_ABERTURA DESC
-- O indice ja guarda as linhas de cada cidadao em ordem de data: nem precisa ordenar.
CREATE INDEX IDX_SOLIC_CIDADAO_DATA ON SOLICITACAO (ID_CIDADAO, DATA_ABERTURA);

-- Estatisticas e consultas analiticas que agrupam/juntam por categoria e status.
CREATE INDEX IDX_SOLIC_CATEGORIA ON SOLICITACAO (ID_CATEGORIA);
CREATE INDEX IDX_SOLIC_STATUS    ON SOLICITACAO (ID_STATUS);

-- Ranking por bairro (C2, C6) e buscas por localidade: agrupam/filtram por cidade e bairro.
CREATE INDEX IDX_SOLIC_LOCAL ON SOLICITACAO (CIDADE, BAIRRO);

-- Linha do tempo / trilha de auditoria: WHERE ID_SOLICITACAO = ? ORDER BY DATA_ALTERACAO
-- Tambem acelera a consulta C5 (LEAD ... PARTITION BY ID_SOLICITACAO).
CREATE INDEX IDX_HIST_SOLIC_DATA ON HISTORICO_STATUS (ID_SOLICITACAO, DATA_ALTERACAO);

-- Atualiza as estatisticas que o otimizador do Oracle usa para escolher o plano.
BEGIN
  DBMS_STATS.GATHER_TABLE_STATS(USER, 'SOLICITACAO');
  DBMS_STATS.GATHER_TABLE_STATS(USER, 'HISTORICO_STATUS');
END;
/

-- Conferindo os indices criados:
SELECT INDEX_NAME, TABLE_NAME
FROM USER_INDEXES
WHERE TABLE_NAME IN ('SOLICITACAO', 'HISTORICO_STATUS')
ORDER BY TABLE_NAME, INDEX_NAME;

-- ----------------------------------------------------------------------------
--  EVIDENCIA PARA O DOCUMENTO (opcional): plano de execucao
--  No SQL Developer, selecione a consulta abaixo e aperte F10 ("Explain Plan").
--  Com o indice, deve aparecer "INDEX RANGE SCAN IDX_SOLIC_CIDADAO_DATA" em
--  vez de "TABLE ACCESS FULL SOLICITACAO". Com poucos dados de teste o Oracle
--  as vezes ainda prefere ler a tabela toda (e tao pequena que compensa) -
--  isso muda conforme o volume cresce.
-- ----------------------------------------------------------------------------
SELECT *
FROM SOLICITACAO
WHERE ID_CIDADAO = 1
ORDER BY DATA_ABERTURA DESC;
