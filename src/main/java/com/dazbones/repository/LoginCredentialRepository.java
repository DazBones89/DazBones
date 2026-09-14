package com.dazbones.repository;
import com.dazbones.model.LoginCredential;
import org.springframework.data.jpa.repository.JpaRepository;
public interface LoginCredentialRepository extends JpaRepository<LoginCredential,String> {}
