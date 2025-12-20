package com.example.demo.service.Impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.dto.PerformerReq;
import com.example.demo.dto.PerformerRes;
import com.example.demo.entity.Performer;
import com.example.demo.repository.PerformerRepository;
import com.example.demo.service.PerformerService;

@Service
public class PerformerServiceImpl implements PerformerService {

    private final PerformerRepository performerRepository;

    public PerformerServiceImpl(PerformerRepository performerRepository) {
        this.performerRepository = performerRepository;
    }

    @Override
    public PerformerRes create(PerformerReq request) {
        Performer performer = mapToEntity(request);
        Performer saved = performerRepository.save(performer);
        return mapToResponse(saved);
    }

    @Override
    public PerformerRes getById(String id) {
        Performer performer = performerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Performer not found: " + id));
        return mapToResponse(performer);
    }

    @Override
    public List<PerformerRes> getAll() {
        return performerRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void delete(String id) {
        performerRepository.deleteById(id);
    }

    private Performer mapToEntity(PerformerReq request) {
        Performer performer = new Performer();
        performer.setName(request.name());
        performer.setType(request.type());
        return performer;
    }

    private PerformerRes mapToResponse(Performer performer) {
        return new PerformerRes(
                performer.getId(),
                performer.getName(),
                performer.getType()
        );
    }
}
