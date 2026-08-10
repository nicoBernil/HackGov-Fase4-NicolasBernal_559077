package br.com.fiap.hackgov.repository;

import br.com.fiap.hackgov.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    // Lista as categorias em ordem alfabetica (para o menu suspenso do formulario).
    List<Categoria> findAllByOrderByNome();
}
