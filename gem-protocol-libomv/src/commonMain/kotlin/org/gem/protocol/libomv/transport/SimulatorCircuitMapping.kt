package org.gem.protocol.libomv.transport

import org.gem.protocol.libomv.LibomvSessionIdentity

internal fun LibomvSessionIdentity.toSimulatorCircuit(): SimulatorCircuit = SimulatorCircuit(
    agentId = agentId,
    sessionId = sessionId,
    seedCapability = seedCapability,
    simulatorIp = simulatorIp,
    simulatorPort = simulatorPort,
    regionHandle = regionHandle,
    circuitCode = circuitCode,
)
