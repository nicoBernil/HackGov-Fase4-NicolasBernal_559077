-- ============================================================================
--  HackGov - Dados de TESTE para os relatorios estatisticos (Parte 4)
-- ----------------------------------------------------------------------------
--  Rode isso UMA VEZ, depois do schema.sql e do dados_iniciais.sql, e depois
--  de ja ter feito login pelo menos uma vez (para o usuario GESTOR existir).
--
--  Sem isso, os relatorios ficam sem graca porque so temos poucas
--  solicitacoes de teste manual, quase todas "MEDIA" e "Recebido". Este
--  script cria 20 solicitacoes variadas (categoria, prioridade, status e
--  datas diferentes) para os graficos e a media de dias de resolucao
--  fazerem sentido na demonstracao e no relatorio.
--
--  Os cidadaos usados sao sorteados entre os que ja existem no seu banco
--  (os que voce ja cadastrou pela tela). E 100% seguro rodar: nao mexe em
--  nenhuma solicitacao existente, so ACRESCENTA linhas novas.
-- ============================================================================

DECLARE
  v_id_solicitacao SOLICITACAO.ID_SOLICITACAO%TYPE;
  v_id_gestor      USUARIO.ID_USUARIO%TYPE;

  v_cat_buraco     CATEGORIA.ID_CATEGORIA%TYPE;
  v_cat_ilumina    CATEGORIA.ID_CATEGORIA%TYPE;
  v_cat_lixo       CATEGORIA.ID_CATEGORIA%TYPE;
  v_cat_saneamento CATEGORIA.ID_CATEGORIA%TYPE;
  v_cat_arboriz    CATEGORIA.ID_CATEGORIA%TYPE;
  v_cat_transito   CATEGORIA.ID_CATEGORIA%TYPE;
  v_cat_outros     CATEGORIA.ID_CATEGORIA%TYPE;

  v_st_recebido    STATUS_SOLICITACAO.ID_STATUS%TYPE;
  v_st_analise     STATUS_SOLICITACAO.ID_STATUS%TYPE;
  v_st_andamento   STATUS_SOLICITACAO.ID_STATUS%TYPE;
  v_st_concluido   STATUS_SOLICITACAO.ID_STATUS%TYPE;

  -- Cria uma solicitacao de teste + o historico de status correspondente.
  -- p_dias_aberta: ha quantos dias ela foi aberta (a partir de hoje).
  -- p_dias_para_concluir: quantos dias depois da abertura ela foi concluida
  --                        (NULL = ainda nao foi concluida).
  PROCEDURE nova_solicitacao(
    p_protocolo          VARCHAR2,
    p_titulo             VARCHAR2,
    p_descricao          VARCHAR2,
    p_bairro             VARCHAR2,
    p_prioridade         VARCHAR2,
    p_categoria          NUMBER,
    p_status             NUMBER,
    p_dias_aberta        NUMBER,
    p_dias_para_concluir NUMBER
  ) IS
    v_data_abertura  DATE := SYSDATE - p_dias_aberta;
    v_data_conclusao DATE;
    v_cidadao        USUARIO.ID_USUARIO%TYPE;
  BEGIN
    IF p_dias_para_concluir IS NOT NULL THEN
      v_data_conclusao := v_data_abertura + p_dias_para_concluir;
    END IF;

    -- Sorteia um cidadao entre os que ja existem no banco.
    SELECT ID_USUARIO INTO v_cidadao FROM (
      SELECT ID_USUARIO FROM USUARIO WHERE PERFIL = 'CIDADAO' ORDER BY DBMS_RANDOM.VALUE
    ) WHERE ROWNUM = 1;

    INSERT INTO SOLICITACAO (
      ID_SOLICITACAO, PROTOCOLO, TITULO, DESCRICAO, LOGRADOURO, BAIRRO, CIDADE,
      PRIORIDADE, ID_CATEGORIA, ID_STATUS, ID_CIDADAO, DATA_ABERTURA, DATA_CONCLUSAO
    ) VALUES (
      SEQ_SOLICITACAO.NEXTVAL, p_protocolo, p_titulo, p_descricao, 'Rua de teste', p_bairro, 'Sao Paulo',
      p_prioridade, p_categoria, p_status, v_cidadao, v_data_abertura, v_data_conclusao
    ) RETURNING ID_SOLICITACAO INTO v_id_solicitacao;

    -- Historico: sempre comeca com "Recebido" na data de abertura.
    INSERT INTO HISTORICO_STATUS (ID_HISTORICO, ID_SOLICITACAO, ID_STATUS, ID_GESTOR, OBSERVACAO, DATA_ALTERACAO)
    VALUES (SEQ_HISTORICO.NEXTVAL, v_id_solicitacao, v_st_recebido, v_id_gestor,
            'Solicitacao registrada (dado de teste).', v_data_abertura);

    -- Se o status final for diferente de "Recebido", registra a mudanca.
    IF p_status != v_st_recebido THEN
      INSERT INTO HISTORICO_STATUS (ID_HISTORICO, ID_SOLICITACAO, ID_STATUS, ID_GESTOR, OBSERVACAO, DATA_ALTERACAO)
      VALUES (SEQ_HISTORICO.NEXTVAL, v_id_solicitacao, p_status, v_id_gestor,
              'Status atualizado (dado de teste).', NVL(v_data_conclusao, v_data_abertura + 1));
    END IF;
  END;

BEGIN
  SELECT ID_USUARIO INTO v_id_gestor FROM USUARIO WHERE PERFIL = 'GESTOR' AND ROWNUM = 1;

  SELECT ID_CATEGORIA INTO v_cat_buraco     FROM CATEGORIA WHERE NOME = 'Buraco na Via';
  SELECT ID_CATEGORIA INTO v_cat_ilumina    FROM CATEGORIA WHERE NOME = 'Iluminacao Publica';
  SELECT ID_CATEGORIA INTO v_cat_lixo       FROM CATEGORIA WHERE NOME = 'Coleta de Lixo';
  SELECT ID_CATEGORIA INTO v_cat_saneamento FROM CATEGORIA WHERE NOME = 'Saneamento e Esgoto';
  SELECT ID_CATEGORIA INTO v_cat_arboriz    FROM CATEGORIA WHERE NOME = 'Arborizacao e Pracas';
  SELECT ID_CATEGORIA INTO v_cat_transito   FROM CATEGORIA WHERE NOME = 'Sinalizacao e Transito';
  SELECT ID_CATEGORIA INTO v_cat_outros     FROM CATEGORIA WHERE NOME = 'Outros';

  SELECT ID_STATUS INTO v_st_recebido    FROM STATUS_SOLICITACAO WHERE NOME = 'Recebido';
  SELECT ID_STATUS INTO v_st_analise     FROM STATUS_SOLICITACAO WHERE NOME = 'Em Analise';
  SELECT ID_STATUS INTO v_st_andamento   FROM STATUS_SOLICITACAO WHERE NOME = 'Em Andamento';
  SELECT ID_STATUS INTO v_st_concluido   FROM STATUS_SOLICITACAO WHERE NOME = 'Concluido';

  nova_solicitacao('HGSEED0001', 'Buraco profundo com risco de acidente',
    'Buraco profundo na avenida, ha risco de acidente para motos e carros', 'Centro', 'ALTA',
    v_cat_buraco, v_st_concluido, 12, 10);

  nova_solicitacao('HGSEED0002', 'Buraco pequeno na calcada da praca',
    'Buraco pequeno e estetico na calcada, sem risco imediato', 'Jardim das Flores', 'BAIXA',
    v_cat_buraco, v_st_concluido, 20, 2);

  nova_solicitacao('HGSEED0003', 'Buraco na rua perto da escola',
    'Buraco na via em frente a escola municipal', 'Vila Nova', 'MEDIA',
    v_cat_buraco, v_st_andamento, 5, NULL);

  nova_solicitacao('HGSEED0004', 'Poste apagado ha dias',
    'Poste de luz apagado ha dias, rua fica muito escura a noite', 'Centro', 'MEDIA',
    v_cat_ilumina, v_st_concluido, 9, 3);

  nova_solicitacao('HGSEED0005', 'Fiacao exposta em poste',
    'Fiacao exposta no poste, risco de acidente para pedestres e criancas', 'Vila Nova', 'ALTA',
    v_cat_ilumina, v_st_concluido, 7, 6);

  nova_solicitacao('HGSEED0006', 'Lampada piscando',
    'Lampada do poste ficou piscando, so um detalhe estetico', 'Jardim das Flores', 'BAIXA',
    v_cat_ilumina, v_st_recebido, 1, NULL);

  nova_solicitacao('HGSEED0007', 'Lixo acumulado ha semanas',
    'Lixo acumulado ha semanas na esquina, atraindo insetos, risco a saude', 'Centro', 'ALTA',
    v_cat_lixo, v_st_concluido, 15, 11);

  nova_solicitacao('HGSEED0008', 'Caminhao de lixo nao passou',
    'O caminhao de coleta de lixo nao passou nessa semana', 'Vila Nova', 'MEDIA',
    v_cat_lixo, v_st_analise, 3, NULL);

  nova_solicitacao('HGSEED0009', 'Entulho de obra na calcada',
    'Entulho de obra deixado na calcada ha dias', 'Jardim das Flores', 'MEDIA',
    v_cat_lixo, v_st_concluido, 10, 3);

  nova_solicitacao('HGSEED0010', 'Vazamento de esgoto a ceu aberto',
    'Vazamento de esgoto a ceu aberto, risco grave a saude dos moradores', 'Centro', 'ALTA',
    v_cat_saneamento, v_st_andamento, 4, NULL);

  nova_solicitacao('HGSEED0011', 'Vazamento de agua na rua',
    'Vazamento de agua na tubulacao da rua', 'Vila Nova', 'MEDIA',
    v_cat_saneamento, v_st_concluido, 8, 3);

  nova_solicitacao('HGSEED0012', 'Cheiro forte de esgoto',
    'Cheiro forte de esgoto no bairro todo', 'Jardim das Flores', 'MEDIA',
    v_cat_saneamento, v_st_recebido, 2, NULL);

  nova_solicitacao('HGSEED0013', 'Galho podre prestes a cair',
    'Arvore com galho podre prestes a cair sobre a fiacao eletrica, risco grave', 'Centro', 'ALTA',
    v_cat_arboriz, v_st_concluido, 6, 5);

  nova_solicitacao('HGSEED0014', 'Poda apenas estetica',
    'Arvore precisa de poda, mas so por motivo estetico', 'Vila Nova', 'BAIXA',
    v_cat_arboriz, v_st_recebido, 1, NULL);

  nova_solicitacao('HGSEED0015', 'Praca com mato alto',
    'Praca do bairro com mato alto e sem manutencao ha meses', 'Jardim das Flores', 'MEDIA',
    v_cat_arboriz, v_st_analise, 4, NULL);

  nova_solicitacao('HGSEED0016', 'Semaforo quebrado',
    'Semaforo quebrado em cruzamento movimentado, risco grave de acidente', 'Centro', 'ALTA',
    v_cat_transito, v_st_concluido, 5, 4);

  nova_solicitacao('HGSEED0017', 'Placa de sinalizacao caida',
    'Placa de sinalizacao de transito caida na calcada', 'Vila Nova', 'MEDIA',
    v_cat_transito, v_st_andamento, 3, NULL);

  nova_solicitacao('HGSEED0018', 'Faixa de pedestre apagada',
    'Faixa de pedestre apagada, so questao estetica por enquanto', 'Jardim das Flores', 'BAIXA',
    v_cat_transito, v_st_recebido, 2, NULL);

  nova_solicitacao('HGSEED0019', 'Sujeira em terreno baldio',
    'Terreno baldio com sujeira acumulada, situacao geral do bairro', 'Centro', 'MEDIA',
    v_cat_outros, v_st_concluido, 11, 5);

  nova_solicitacao('HGSEED0020', 'Reclamacao diversa de manutencao',
    'Manutencao urbana geral pendente, sem grande urgencia', 'Vila Nova', 'BAIXA',
    v_cat_outros, v_st_recebido, 1, NULL);

  COMMIT;
END;
/
