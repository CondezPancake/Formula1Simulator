package com.formula1.service;

import com.formula1.model.Team;

import java.util.List;
import java.util.Optional;

public interface TeamService {

    Team register(Team team);

    Optional<Team> findById(String id);

    List<Team> findAll();

    Team update(Team team);

    void delete(String id);
}
