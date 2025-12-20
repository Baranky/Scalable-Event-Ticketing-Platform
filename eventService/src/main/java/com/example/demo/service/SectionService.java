package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.dto.SectionReq;
import com.example.demo.dto.SectionRes;
import com.example.demo.entity.Section;

@Service
public interface SectionService {

    SectionRes create(SectionReq request);

    SectionRes getById(String id);

    Section getSectionById(String id);

    List<SectionRes> getAll();

    void delete(String id);
}
