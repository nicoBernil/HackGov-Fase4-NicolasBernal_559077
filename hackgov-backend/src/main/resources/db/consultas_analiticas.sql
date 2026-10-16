-- ============================================================================
--  HackGov - Consultas analiticas (Fase 5 - Parte 4: Analise com SQL)
-- ----------------------------------------------------------------------------
--  COMO USAR (Oracle SQL Developer):
--  1) Garanta que ja rodou schema.sql, dados_iniciais.sql e
--     dados_teste_estatisticas.sql (sem dados, os resultados ficam vazios).
--  2) Clique DENTRO de uma consulta e aperte Ctrl+Enter ("Run Statement").
--     O resultado aparece em grade, embaixo - e dela que voce tira o print.
--  3) Repita para cada consulta. Nenhuma delas altera dados: so SELECT.
--
--  RECURSOS USADOS (para o documento):
--    - Agrupamento e totais .......... GROUP BY, COUNT, SUM, AVG, HAVING
--    - Logica condicional ............ CASE WHEN dentro de SUM (conta por tipo)
--    - Funcoes analiticas (janela) ... RATIO_TO_REPORT, RANK, LEAD ... OVER()
--    - Subconsultas .................. no HAVING (C3), correlacionada no
--                                      WHERE e no SELECT (C4), no FROM (C2)
--    - Subtotais ..................... GROUP BY ROLLUP + GROUPING (C6)
--    - CTE ........................... WITH (C5)
--
--  Datas no Oracle: subtrair duas DATE da o numero de DIAS entre elas
--  (com fracao). Ex.: DATA_CONCLUSAO - DATA_ABERTURA = 3.5 -> 3 dias e meio.
-- ============================================================================


-- ----------------------------------------------------------------------------
--  C1) Visao geral por categoria
--  PERGUNTA: quais tipos de problema mais aparecem, quantos ainda estao em
--  aberto e quanto tempo, em media, cada tipo leva para ser resolvido?
--  DESTAQUE: RATIO_TO_REPORT(...) OVER () e uma funcao analitica - calcula o
--  percentual de cada categoria sobre o TOTAL sem precisar de outra consulta.
--  AVG ignora valores nulos, entao a media de resolucao so considera as
--  solicitacoes ja concluidas (DATA_CONCLUSAO preenchida).
-- ----------------------------------------------------------------------------
SELECT c.NOME                                                        AS CATEGORIA,
       COUNT(*)                                                      AS TOTAL,
       SUM(CASE WHEN s.DATA_CONCLUSAO IS NULL     THEN 1 ELSE 0 END) AS ABERTAS,
       SUM(CASE WHEN s.DATA_CONCLUSAO IS NOT NULL THEN 1 ELSE 0 END) AS CONCLUIDAS,
       ROUND(100 * RATIO_TO_REPORT(COUNT(*)) OVER (), 1)             AS PERC_DO_TOTAL,
       ROUND(AVG(s.DATA_CONCLUSAO - s.DATA_ABERTURA), 1)             AS MEDIA_DIAS_RESOLUCAO
FROM SOLICITACAO s
JOIN CATEGORIA   c ON c.ID_CATEGORIA = s.ID_CATEGORIA
GROUP BY c.NOME
ORDER BY TOTAL DESC, CATEGORIA;


-- ----------------------------------------------------------------------------
--  C2) Ranking de bairros por pendencias
--  PERGUNTA: onde a fila de atendimento esta mais acumulada? Onde se
--  concentram os casos de prioridade ALTA?
--  DESTAQUE: subconsulta no FROM (inline view) faz a contagem por bairro, e
--  RANK() OVER (...) numera os bairros do mais para o menos pendente
--  (empates recebem a mesma posicao).
-- ----------------------------------------------------------------------------
SELECT RANK() OVER (ORDER BY ABERTAS DESC) AS POSICAO,
       BAIRRO,
       TOTAL,
       ABERTAS,
       ALTA_PRIORIDADE,
       ROUND(100 * ABERTAS / TOTAL, 1)     AS PERC_PENDENTE
FROM (
        SELECT BAIRRO,
               COUNT(*)                                                   AS TOTAL,
               SUM(CASE WHEN DATA_CONCLUSAO IS NULL THEN 1 ELSE 0 END)    AS ABERTAS,
               SUM(CASE WHEN PRIORIDADE = 'ALTA'    THEN 1 ELSE 0 END)    AS ALTA_PRIORIDADE
        FROM SOLICITACAO
        GROUP BY BAIRRO
     )
ORDER BY POSICAO, BAIRRO;


-- ----------------------------------------------------------------------------
--  C3) Categorias mais lentas que a media geral
--  PERGUNTA: quais tipos de problema demoram MAIS que a media da prefeitura
--  para serem resolvidos? (candidatos a reforco de equipe)
--  DESTAQUE: subconsulta dentro do HAVING - compara a media de cada grupo
--  com a media geral, calculada pela subconsulta.
-- ----------------------------------------------------------------------------
SELECT c.NOME                                            AS CATEGORIA,
       COUNT(*)                                          AS CONCLUIDAS,
       ROUND(AVG(s.DATA_CONCLUSAO - s.DATA_ABERTURA), 1) AS MEDIA_DIAS_CATEGORIA,
       (SELECT ROUND(AVG(DATA_CONCLUSAO - DATA_ABERTURA), 1)
          FROM SOLICITACAO
         WHERE DATA_CONCLUSAO IS NOT NULL)               AS MEDIA_DIAS_GERAL
FROM SOLICITACAO s
JOIN CATEGORIA   c ON c.ID_CATEGORIA = s.ID_CATEGORIA
WHERE s.DATA_CONCLUSAO IS NOT NULL
GROUP BY c.NOME
HAVING AVG(s.DATA_CONCLUSAO - s.DATA_ABERTURA) > (
          SELECT AVG(DATA_CONCLUSAO - DATA_ABERTURA)
            FROM SOLICITACAO
           WHERE DATA_CONCLUSAO IS NOT NULL
       )
ORDER BY MEDIA_DIAS_CATEGORIA DESC;


-- ----------------------------------------------------------------------------
--  C4) Solicitacoes abertas "atrasadas"
--  PERGUNTA: quais pedidos em aberto ja passaram do tempo que a sua propria
--  categoria costuma levar para ser resolvida? (alerta para o gestor)
--  DESTAQUE: SUBCONSULTA CORRELACIONADA - ela e recalculada para cada linha,
--  usando a categoria daquela linha (x.ID_CATEGORIA = s.ID_CATEGORIA).
--  OBS: o resultado depende da data em que voce roda (usa SYSDATE).
-- ----------------------------------------------------------------------------
SELECT s.PROTOCOLO,
       s.TITULO,
       c.NOME                                   AS CATEGORIA,
       s.PRIORIDADE,
       st.NOME                                  AS STATUS,
       s.BAIRRO,
       ROUND(SYSDATE - s.DATA_ABERTURA, 1)      AS DIAS_EM_ABERTO,
       (SELECT ROUND(AVG(x.DATA_CONCLUSAO - x.DATA_ABERTURA), 1)
          FROM SOLICITACAO x
         WHERE x.ID_CATEGORIA = s.ID_CATEGORIA
           AND x.DATA_CONCLUSAO IS NOT NULL)    AS MEDIA_DIAS_CATEGORIA
FROM SOLICITACAO        s
JOIN CATEGORIA          c  ON c.ID_CATEGORIA = s.ID_CATEGORIA
JOIN STATUS_SOLICITACAO st ON st.ID_STATUS   = s.ID_STATUS
WHERE s.DATA_CONCLUSAO IS NULL
  AND SYSDATE - s.DATA_ABERTURA > (
        SELECT AVG(x.DATA_CONCLUSAO - x.DATA_ABERTURA)
          FROM SOLICITACAO x
         WHERE x.ID_CATEGORIA = s.ID_CATEGORIA
           AND x.DATA_CONCLUSAO IS NOT NULL
      )
ORDER BY CASE s.PRIORIDADE WHEN 'ALTA' THEN 1 WHEN 'MEDIA' THEN 2 ELSE 3 END,
         DIAS_EM_ABERTO DESC;


-- ----------------------------------------------------------------------------
--  C5) Gargalos do fluxo: quanto tempo as solicitacoes ficam em cada etapa
--  PERGUNTA: em qual status os pedidos ficam parados por mais tempo?
--  DESTAQUE: usa a trilha de auditoria (HISTORICO_STATUS) + a funcao
--  analitica LEAD, que "olha a proxima linha" do historico da mesma
--  solicitacao: o momento em que ela SAIU de uma etapa e o momento em que
--  ENTROU na seguinte. Se ainda nao saiu, conta ate agora (SYSDATE).
--  WITH (CTE) da um nome para a consulta intermediaria e deixa tudo legivel.
-- ----------------------------------------------------------------------------
WITH ETAPAS AS (
    SELECT h.ID_SOLICITACAO,
           st.NOME          AS STATUS,
           st.ORDEM,
           h.DATA_ALTERACAO AS ENTROU_EM,
           LEAD(h.DATA_ALTERACAO) OVER (
               PARTITION BY h.ID_SOLICITACAO
               ORDER BY h.DATA_ALTERACAO, h.ID_HISTORICO
           )                AS SAIU_EM
    FROM HISTORICO_STATUS   h
    JOIN STATUS_SOLICITACAO st ON st.ID_STATUS = h.ID_STATUS
)
SELECT STATUS,
       COUNT(*)                                         AS PASSAGENS,
       SUM(CASE WHEN SAIU_EM IS NULL THEN 1 ELSE 0 END) AS PARADAS_AGORA,
       ROUND(AVG(NVL(SAIU_EM, SYSDATE) - ENTROU_EM), 1) AS MEDIA_DIAS_NA_ETAPA,
       ROUND(MAX(NVL(SAIU_EM, SYSDATE) - ENTROU_EM), 1) AS MAIOR_ESPERA_DIAS
FROM ETAPAS
WHERE STATUS <> 'Concluido'          -- "Concluido" e o fim do fluxo, nao uma espera
GROUP BY STATUS, ORDEM
ORDER BY ORDEM;


-- ----------------------------------------------------------------------------
--  C6) Matriz bairro x prioridade, com total geral
--  PERGUNTA: como a urgencia se distribui entre os bairros?
--  DESTAQUE: GROUP BY ROLLUP gera, alem de uma linha por bairro, uma linha
--  extra de TOTAL GERAL. A funcao GROUPING(BAIRRO) devolve 1 justamente na
--  linha de total, o que permite rotula-la e manda-la para o fim.
-- ----------------------------------------------------------------------------
SELECT DECODE(GROUPING(BAIRRO), 1, '>> TOTAL GERAL', BAIRRO) AS BAIRRO,
       SUM(CASE WHEN PRIORIDADE = 'ALTA'  THEN 1 ELSE 0 END)  AS ALTA,
       SUM(CASE WHEN PRIORIDADE = 'MEDIA' THEN 1 ELSE 0 END)  AS MEDIA,
       SUM(CASE WHEN PRIORIDADE = 'BAIXA' THEN 1 ELSE 0 END)  AS BAIXA,
       COUNT(*)                                               AS TOTAL
FROM SOLICITACAO
GROUP BY ROLLUP (BAIRRO)
ORDER BY GROUPING(BAIRRO), BAIRRO;
