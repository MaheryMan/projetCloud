package com.projetCloud.app.typesSignalement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TypeSignalementService {

    @Autowired
    private TypeSignalementRepository typeSignalementRepository;

    public List<TypeSignalement> findAll() {
        return typeSignalementRepository.findAll();
    }

    public Optional<TypeSignalement> findById(Long id) {
        return typeSignalementRepository.findById(id);
    }

    public Optional<TypeSignalement> findByLibelle(String libelle) {
        return typeSignalementRepository.findByLibelle(libelle);
    }

    public TypeSignalement save(TypeSignalement typeSignalement) {
        return typeSignalementRepository.save(typeSignalement);
    }

    public void deleteById(Long id) {
        typeSignalementRepository.deleteById(id);
    }
}
