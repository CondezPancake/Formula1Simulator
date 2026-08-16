package com.formula1.service;

import com.formula1.model.Team;
import com.formula1.repository.TeamRepository;
import com.formula1.repository.TeamRepositoryImpl;
import com.formula1.util.ValidationUtils;
import com.formula1.exception.InvalidSimulationException;

import java.util.List;
import java.util.Optional;

public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;

    public TeamServiceImpl() {
        this(new TeamRepositoryImpl());
    }

    public TeamServiceImpl(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    @Override
    public Team register(Team team) {
        if (team == null || !ValidationUtils.isNotBlank(team.getNombre())) {
            throw new InvalidSimulationException("El nombre del equipo no puede estar vacío");
        }
        return teamRepository.save(team);
    }

    @Override
    public Optional<Team> findById(String id) {
        return teamRepository.findById(id);
    }

    @Override
    public List<Team> findAll() {
        try {
            return teamRepository.findAll();
        } catch (RuntimeException e) {
            System.err.println("TeamService.findAll: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public Team update(Team team) {
        return teamRepository.update(team);
    }

    @Override
    public void delete(String id) {
        teamRepository.deleteById(id);
    }
}
