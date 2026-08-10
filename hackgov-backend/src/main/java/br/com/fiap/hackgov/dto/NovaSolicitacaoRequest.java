package br.com.fiap.hackgov.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados que o cidadao envia ao registrar uma nova solicitacao. (US03)
 *
 * As anotacoes garantem que campos obrigatorios venham preenchidos. Se algo
 * faltar, a API recusa e o GlobalExceptionHandler devolve a mensagem.
 */
public record NovaSolicitacaoRequest(

        @NotBlank(message = "O titulo e obrigatorio")
        String titulo,

        @NotBlank(message = "A descricao e obrigatoria")
        @Size(min = 10, message = "Descreva o problema com pelo menos 10 caracteres")
        String descricao,

        @NotNull(message = "Selecione uma categoria")
        Long idCategoria,

        @NotBlank(message = "O logradouro e obrigatorio")
        String logradouro,

        @NotBlank(message = "O bairro e obrigatorio")
        String bairro,

        @NotBlank(message = "A cidade e obrigatoria")
        String cidade,

        // Qual cidadao esta abrindo (o front envia o id do usuario logado).
        @NotNull(message = "Usuario nao identificado")
        Long idCidadao
) {
}
