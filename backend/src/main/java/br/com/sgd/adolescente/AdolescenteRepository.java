package br.com.sgd.adolescente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AdolescenteRepository
    extends JpaRepository<Adolescente, Long>, JpaSpecificationExecutor<Adolescente> {}
