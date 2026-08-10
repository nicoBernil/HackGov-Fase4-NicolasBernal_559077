package br.com.fiap.hackgov.service;

import br.com.fiap.hackgov.dto.CadastroRequest;
import br.com.fiap.hackgov.dto.LoginRequest;
import br.com.fiap.hackgov.dto.UsuarioResponse;
import br.com.fiap.hackgov.exception.RegraNegocioException;
import br.com.fiap.hackgov.model.Perfil;
import br.com.fiap.hackgov.model.Usuario;
import br.com.fiap.hackgov.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Onde mora a "regra de negocio" de usuarios.
 *
 * O Controller (a porta da API) so recebe a requisicao e chama os metodos
 * daqui. Manter a logica separada deixa o codigo organizado e mais facil de
 * testar.
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    // O Spring entrega automaticamente o repository e o encoder aqui.
    // Isso se chama "injecao de dependencia".
    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Cadastra um novo CIDADAO.
     * Gestores nao se cadastram sozinhos por aqui (questao de seguranca):
     * eles ja vem prontos no sistema.
     */
    public UsuarioResponse cadastrar(CadastroRequest dados) {
        // 1) Nao deixa repetir e-mail nem CPF.
        if (usuarioRepository.existsByEmail(dados.email())) {
            throw new RegraNegocioException("Ja existe um usuario com este e-mail.");
        }
        if (usuarioRepository.existsByCpf(dados.cpf())) {
            throw new RegraNegocioException("Ja existe um usuario com este CPF.");
        }

        // 2) Transforma a senha em hash ANTES de salvar.
        String senhaComHash = passwordEncoder.encode(dados.senha());

        // 3) Cria e salva o usuario no banco.
        Usuario novo = new Usuario(
                dados.nome(),
                dados.cpf(),
                dados.email(),
                senhaComHash,
                Perfil.CIDADAO
        );
        Usuario salvo = usuarioRepository.save(novo);

        // 4) Devolve os dados (sem a senha).
        return UsuarioResponse.de(salvo);
    }

    /**
     * Faz o login: confere e-mail e senha.
     */
    public UsuarioResponse login(LoginRequest dados) {
        // Busca o usuario pelo e-mail.
        Usuario usuario = usuarioRepository.findByEmail(dados.email())
                .orElseThrow(() -> new RegraNegocioException("E-mail ou senha invalidos."));

        // Compara a senha digitada com o hash salvo.
        // Usamos a MESMA mensagem para e-mail errado e senha errada, de proposito:
        // assim um atacante nao descobre se o e-mail existe ou nao.
        boolean senhaConfere = passwordEncoder.matches(dados.senha(), usuario.getSenhaHash());
        if (!senhaConfere) {
            throw new RegraNegocioException("E-mail ou senha invalidos.");
        }

        return UsuarioResponse.de(usuario);
    }
}
