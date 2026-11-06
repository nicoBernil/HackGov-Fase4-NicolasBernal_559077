// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * @title  HackGovRegistro
 * @notice Prototipo de smart contract do HackGov (Visao de Futuro - Parte 4).
 *
 * PROBLEMA: o historico de status fica no Oracle da prefeitura. Quem administra
 * o banco (um DBA, ou um invasor com acesso a ele) consegue apagar ou reescrever
 * linhas da tabela HISTORICO_STATUS sem deixar rastro.
 *
 * SOLUCAO: a cada abertura e a cada mudanca de status, o back-end do HackGov
 * grava aqui o HASH (impressao digital) do registro. A blockchain e append-only:
 * ninguem, nem a prefeitura, consegue alterar o que ja foi gravado. Qualquer
 * cidadao ou auditor pode recalcular o hash de uma linha do banco e conferir
 * com a funcao verificarAlteracao().
 *
 * O contrato tambem torna PUBLICO o prazo (SLA) de cada prioridade: o atraso
 * de uma solicitacao deixa de ser uma informacao que so a prefeitura tem.
 *
 * LGPD: nenhum dado pessoal vai para a blockchain. So hashes - do protocolo
 * e do conteudo (titulo, categoria, bairro, data). Nome, CPF e e-mail ficam
 * apenas no Oracle.
 */
contract HackGovRegistro {
    // ------------------------------------------------------------------
    // Tipos
    // ------------------------------------------------------------------

    enum Prioridade { BAIXA, MEDIA, ALTA }

    // Mesmas etapas da tabela STATUS_SOLICITACAO (coluna ORDEM).
    uint8 public constant RECEBIDO = 1;
    uint8 public constant EM_ANALISE = 2;
    uint8 public constant EM_ANDAMENTO = 3;
    uint8 public constant CONCLUIDO = 4;

    struct Solicitacao {
        bytes32 hashConteudo;   // sha256 do conteudo da solicitacao no momento da abertura
        Prioridade prioridade;
        uint8 statusAtual;
        uint64 abertaEm;        // timestamp do bloco
        uint64 prazoLimite;     // abertaEm + SLA da prioridade
        uint64 concluidaEm;     // 0 enquanto nao concluida
        bool existe;
    }

    struct Alteracao {
        uint8 statusAnterior;
        uint8 statusNovo;
        bytes32 hashHistorico;  // sha256 da linha gravada em HISTORICO_STATUS
        address gestor;         // conta que assinou a transacao
        uint64 em;
    }

    // ------------------------------------------------------------------
    // Estado
    // ------------------------------------------------------------------

    address public immutable prefeitura;
    mapping(address => bool) public gestores;
    mapping(Prioridade => uint32) public prazoEmDias;

    mapping(bytes32 => Solicitacao) private solicitacoes;      // chave: keccak256(protocolo)
    mapping(bytes32 => Alteracao[]) private historico;
    uint256 public totalSolicitacoes;

    // ------------------------------------------------------------------
    // Eventos (ficam no log publico da blockchain)
    // ------------------------------------------------------------------

    event GestorAutorizado(address indexed gestor, bool autorizado);
    event PrazoDefinido(Prioridade prioridade, uint32 dias);
    event SolicitacaoRegistrada(bytes32 indexed protocolo, Prioridade prioridade, uint64 prazoLimite);
    event StatusAlterado(bytes32 indexed protocolo, uint8 de, uint8 para, bytes32 hashHistorico, address indexed gestor);
    event ConcluidaForaDoPrazo(bytes32 indexed protocolo, uint64 diasDeAtraso);

    // ------------------------------------------------------------------
    // Erros
    // ------------------------------------------------------------------

    error SomentePrefeitura();
    error SomenteGestor();
    error SolicitacaoJaRegistrada();
    error SolicitacaoInexistente();
    error StatusInvalido(uint8 status);
    error StatusIgualAoAtual();
    error PrazoInvalido();

    modifier somentePrefeitura() {
        if (msg.sender != prefeitura) revert SomentePrefeitura();
        _;
    }

    modifier somenteGestor() {
        if (!gestores[msg.sender]) revert SomenteGestor();
        _;
    }

    constructor() {
        prefeitura = msg.sender;
        gestores[msg.sender] = true;
        // SLA inicial (pode ser mudado pela prefeitura). Na Parte 3, a regressao
        // mostrou ALTA levando ~7 dias: o compromisso publico passa a ser 3.
        prazoEmDias[Prioridade.ALTA] = 3;
        prazoEmDias[Prioridade.MEDIA] = 7;
        prazoEmDias[Prioridade.BAIXA] = 15;
    }

    // ------------------------------------------------------------------
    // Administracao (so a prefeitura)
    // ------------------------------------------------------------------

    function autorizarGestor(address gestor, bool autorizado) external somentePrefeitura {
        gestores[gestor] = autorizado;
        emit GestorAutorizado(gestor, autorizado);
    }

    function definirPrazo(Prioridade prioridade, uint32 dias) external somentePrefeitura {
        if (dias == 0) revert PrazoInvalido();
        prazoEmDias[prioridade] = dias;
        emit PrazoDefinido(prioridade, dias);
    }

    // ------------------------------------------------------------------
    // Escrita (back-end do HackGov, com a conta de um gestor autorizado)
    // ------------------------------------------------------------------

    /// @notice Ancora a abertura de uma solicitacao. Chamado quando o cidadao registra.
    function registrarSolicitacao(bytes32 protocolo, bytes32 hashConteudo, Prioridade prioridade)
        external
        somenteGestor
    {
        if (solicitacoes[protocolo].existe) revert SolicitacaoJaRegistrada();

        uint64 agora = uint64(block.timestamp);
        uint64 prazo = agora + uint64(prazoEmDias[prioridade]) * 1 days;

        solicitacoes[protocolo] = Solicitacao({
            hashConteudo: hashConteudo,
            prioridade: prioridade,
            statusAtual: RECEBIDO,
            abertaEm: agora,
            prazoLimite: prazo,
            concluidaEm: 0,
            existe: true
        });
        totalSolicitacoes++;
        emit SolicitacaoRegistrada(protocolo, prioridade, prazo);
    }

    /**
     * @notice Ancora uma mudanca de status. Vale tambem para o "desfazer" do
     *         HackGov: voltar para a etapa anterior e uma NOVA alteracao, nunca
     *         a remocao da antiga - igual a pilha append-only do back-end.
     */
    function alterarStatus(bytes32 protocolo, uint8 novoStatus, bytes32 hashHistorico) external somenteGestor {
        Solicitacao storage s = solicitacoes[protocolo];
        if (!s.existe) revert SolicitacaoInexistente();
        if (novoStatus < RECEBIDO || novoStatus > CONCLUIDO) revert StatusInvalido(novoStatus);
        if (novoStatus == s.statusAtual) revert StatusIgualAoAtual();

        uint8 anterior = s.statusAtual;
        uint64 agora = uint64(block.timestamp);
        s.statusAtual = novoStatus;

        if (novoStatus == CONCLUIDO) {
            s.concluidaEm = agora;
            if (agora > s.prazoLimite) {
                emit ConcluidaForaDoPrazo(protocolo, (agora - s.prazoLimite) / 1 days);
            }
        } else if (anterior == CONCLUIDO) {
            s.concluidaEm = 0; // conclusao desfeita: a solicitacao voltou a ficar aberta
        }

        historico[protocolo].push(Alteracao(anterior, novoStatus, hashHistorico, msg.sender, agora));
        emit StatusAlterado(protocolo, anterior, novoStatus, hashHistorico, msg.sender);
    }

    // ------------------------------------------------------------------
    // Leitura publica (qualquer pessoa, sem custo de gas)
    // ------------------------------------------------------------------

    function consultar(bytes32 protocolo) external view returns (Solicitacao memory) {
        if (!solicitacoes[protocolo].existe) revert SolicitacaoInexistente();
        return solicitacoes[protocolo];
    }

    function totalAlteracoes(bytes32 protocolo) external view returns (uint256) {
        return historico[protocolo].length;
    }

    function alteracao(bytes32 protocolo, uint256 indice) external view returns (Alteracao memory) {
        return historico[protocolo][indice];
    }

    /// @notice Confere se o conteudo guardado no Oracle ainda e o mesmo da abertura.
    function verificarConteudo(bytes32 protocolo, bytes32 hashConteudo) external view returns (bool) {
        return solicitacoes[protocolo].existe && solicitacoes[protocolo].hashConteudo == hashConteudo;
    }

    /// @notice Confere se a linha "indice" do HISTORICO_STATUS do Oracle nao foi adulterada.
    function verificarAlteracao(bytes32 protocolo, uint256 indice, bytes32 hashHistorico)
        external
        view
        returns (bool)
    {
        return indice < historico[protocolo].length && historico[protocolo][indice].hashHistorico == hashHistorico;
    }

    /// @notice true se passou do prazo sem concluir, ou se foi concluida depois do prazo.
    function estaAtrasada(bytes32 protocolo) public view returns (bool) {
        Solicitacao storage s = solicitacoes[protocolo];
        if (!s.existe) revert SolicitacaoInexistente();
        uint64 referencia = s.concluidaEm != 0 ? s.concluidaEm : uint64(block.timestamp);
        return referencia > s.prazoLimite;
    }
}
