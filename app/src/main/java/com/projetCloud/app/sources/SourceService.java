package com.projetCloud.app.sources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SourceService {

    @Autowired
    private SourceRepository sourceRepository;

    public List<Source> findAll() {
        return sourceRepository.findAll();
    }

    public Optional<Source> findById(Long id) {
        return sourceRepository.findById(id);
    }

    public Source save(Source source) {
        return sourceRepository.save(source);
    }

    public void deleteById(Long id) {
        sourceRepository.deleteById(id);
    }

    public Optional<Source> findByLibelle(String libelle) {
        return sourceRepository.findByLibelle(libelle);
    }
}