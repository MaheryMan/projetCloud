package com.projetCloud.app.status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StatusService {

    @Autowired
    private StatusRepository statusRepository;

    public List<Status> findAll() {
        return statusRepository.findAll();
    }

    public Optional<Status> findById(Long id) {
        return statusRepository.findById(id);
    }

    public Status save(Status status) {
        return statusRepository.save(status);
    }

    public void deleteById(Long id) {
        statusRepository.deleteById(id);
    }

    public Optional<Status> findByStatusId(String statusId) {
        return statusRepository.findByStatusId(statusId);
    }

    public Optional<Status> findByLibelle(String libelle) {
        return statusRepository.findByLibelle(libelle);
    }
}