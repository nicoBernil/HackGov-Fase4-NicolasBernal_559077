// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import {Test} from "forge-std/Test.sol";
import {HackGovRegistro} from "../src/HackGovRegistro.sol";

/// Testes do prototipo. Rodar com:  forge test -vv
contract HackGovRegistroTest is Test {
    HackGovRegistro registro;

    address prefeitura = address(this);
    address gestor = makeAddr("gestor");
    address invasor = makeAddr("invasor");

    bytes32 protocolo = keccak256("HG202600028");
    bytes32 conteudo = sha256("buraco na via|Buraco na Via|laranjeiras|2026-11-05");

    function setUp() public {
        registro = new HackGovRegistro();
        registro.autorizarGestor(gestor, true);
    }

    function _abrir(HackGovRegistro.Prioridade prioridade) internal {
        vm.prank(gestor);
        registro.registrarSolicitacao(protocolo, conteudo, prioridade);
    }

    function test_registraSolicitacaoComPrazoDaPrioridade() public {
        _abrir(HackGovRegistro.Prioridade.ALTA);
        HackGovRegistro.Solicitacao memory s = registro.consultar(protocolo);
        assertEq(s.statusAtual, registro.RECEBIDO());
        assertEq(s.prazoLimite, s.abertaEm + 3 days);
        assertTrue(registro.verificarConteudo(protocolo, conteudo));
        assertEq(registro.totalSolicitacoes(), 1);
    }

    function test_naoRegistraOMesmoProtocoloDuasVezes() public {
        _abrir(HackGovRegistro.Prioridade.MEDIA);
        vm.prank(gestor);
        vm.expectRevert(HackGovRegistro.SolicitacaoJaRegistrada.selector);
        registro.registrarSolicitacao(protocolo, conteudo, HackGovRegistro.Prioridade.MEDIA);
    }

    function test_somenteGestorAlteraStatus() public {
        _abrir(HackGovRegistro.Prioridade.MEDIA);
        vm.prank(invasor);
        vm.expectRevert(HackGovRegistro.SomenteGestor.selector);
        registro.alterarStatus(protocolo, 2, keccak256("linha falsa"));
    }

    function test_somentePrefeituraAutorizaGestor() public {
        vm.prank(invasor);
        vm.expectRevert(HackGovRegistro.SomentePrefeitura.selector);
        registro.autorizarGestor(invasor, true);
    }

    function test_historicoDetectaAdulteracaoNoBanco() public {
        _abrir(HackGovRegistro.Prioridade.MEDIA);
        bytes32 linhaOriginal = sha256("HG202600028|Recebido->Em Analise|gestor 1|2026-11-05T10:00");

        vm.prank(gestor);
        registro.alterarStatus(protocolo, 2, linhaOriginal);

        assertTrue(registro.verificarAlteracao(protocolo, 0, linhaOriginal));
        // Um DBA reescreve a linha no Oracle trocando o gestor: o hash nao bate mais.
        bytes32 linhaAdulterada = sha256("HG202600028|Recebido->Em Analise|gestor 2|2026-11-05T10:00");
        assertFalse(registro.verificarAlteracao(protocolo, 0, linhaAdulterada));
    }

    function test_desfazerEmpilhaNovaAlteracaoSemApagar() public {
        _abrir(HackGovRegistro.Prioridade.MEDIA);
        vm.startPrank(gestor);
        registro.alterarStatus(protocolo, 3, keccak256("h1"));
        registro.alterarStatus(protocolo, 1, keccak256("h2")); // "desfazer"
        vm.stopPrank();

        assertEq(registro.totalAlteracoes(protocolo), 2);
        HackGovRegistro.Alteracao memory a = registro.alteracao(protocolo, 1);
        assertEq(a.statusAnterior, 3);
        assertEq(a.statusNovo, 1);
        assertTrue(registro.verificarAlteracao(protocolo, 0, keccak256("h1")));
    }

    function test_rejeitaStatusInvalidoOuRepetido() public {
        _abrir(HackGovRegistro.Prioridade.BAIXA);
        vm.startPrank(gestor);
        vm.expectRevert(abi.encodeWithSelector(HackGovRegistro.StatusInvalido.selector, uint8(5)));
        registro.alterarStatus(protocolo, 5, bytes32(0));
        vm.expectRevert(HackGovRegistro.StatusIgualAoAtual.selector);
        registro.alterarStatus(protocolo, 1, bytes32(0));
        vm.stopPrank();
    }

    function test_atrasoFicaPublicoQuandoPassaDoPrazo() public {
        _abrir(HackGovRegistro.Prioridade.ALTA); // prazo: 3 dias
        assertFalse(registro.estaAtrasada(protocolo));

        vm.warp(block.timestamp + 7 days); // a ALTA levou 7 dias, como na regressao da Parte 3
        assertTrue(registro.estaAtrasada(protocolo));

        vm.expectEmit(true, false, false, true);
        emit HackGovRegistro.ConcluidaForaDoPrazo(protocolo, 4);
        vm.prank(gestor);
        registro.alterarStatus(protocolo, 4, keccak256("concluida"));
        assertTrue(registro.estaAtrasada(protocolo));
    }

    function test_concluidaDentroDoPrazoNaoFicaAtrasada() public {
        _abrir(HackGovRegistro.Prioridade.MEDIA); // prazo: 7 dias
        vm.warp(block.timestamp + 4 days);
        vm.prank(gestor);
        registro.alterarStatus(protocolo, 4, keccak256("concluida"));

        vm.warp(block.timestamp + 30 days); // muito tempo depois, continua no prazo
        assertFalse(registro.estaAtrasada(protocolo));
    }

    function test_prefeituraAjustaPrazo() public {
        registro.definirPrazo(HackGovRegistro.Prioridade.ALTA, 2);
        _abrir(HackGovRegistro.Prioridade.ALTA);
        HackGovRegistro.Solicitacao memory s = registro.consultar(protocolo);
        assertEq(s.prazoLimite, s.abertaEm + 2 days);
    }
}
