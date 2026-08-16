package com.formula1.service;

import com.formula1.model.Circuit;
import com.formula1.model.Driver;
import com.formula1.model.Team;

import java.util.List;

public interface OpenF1Service {

    List<Driver> syncDrivers();

    List<Team> syncTeams();

    List<Circuit> syncCircuits();
}
