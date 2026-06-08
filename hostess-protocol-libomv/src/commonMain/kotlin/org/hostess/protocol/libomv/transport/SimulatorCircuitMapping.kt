package org.hostess.protocol.libomv.transport

import org.hostess.protocol.libomv.LibomvSessionIdentity

internal fun LibomvSessionIdentity.toSimulatorCircuit(): SimulatorCircuit = SimulatorCircuit(
    agentId = agentId,
    sessionId = sessionId,
    seedCapability = seedCapability,
    simulatorIp = simulatorIp,
    simulatorPort = simulatorPort,
    regionHandle = regionHandle,
    circuitCode = circuitCode,
)
