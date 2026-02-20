package net.corda.samples.example.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;
import net.corda.samples.example.contracts.TemperatureContract;
import net.corda.samples.example.schema.TemperatureSchemaV1;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@BelongsToContract(TemperatureContract.class)
public class TemperatureState implements LinearState, QueryableState {
    private final Instant timestamp;
    private final double temperature;
    // private final double
    private final boolean isCritical;
    private final Party owner;
    private final Party receiver;
    private final UniqueIdentifier linearId;

    public TemperatureState(Instant timestamp, double temperature, boolean isCritical,
                            Party owner, Party receiver, UniqueIdentifier linearId) {
        this.timestamp = timestamp;
        this.temperature = temperature;
        this.isCritical = isCritical;
        this.owner = owner;
        this.receiver = receiver;
        this.linearId = linearId;
    }

    // Getters
    public Instant getTimestamp() { return timestamp; }
    public double getTemperature() { return temperature; }
    public boolean isCritical() { return isCritical; }
    public Party getOwner() { return owner; }
    public Party getReceiver() { return receiver; }
    @Override public UniqueIdentifier getLinearId() { return linearId; }
    @Override public List<AbstractParty> getParticipants() {
        return Arrays.asList(owner, receiver);
    }

    @Override
    public PersistentState generateMappedObject(MappedSchema schema) {
        if (schema instanceof TemperatureSchemaV1) {
            return new TemperatureSchemaV1.PersistentTemperature(
                    this.timestamp.toString(),
                    this.temperature,
                    this.isCritical,
                    this.owner.getName().toString(),
                    this.receiver.getName().toString(),
                    this.linearId.getId());
        } else {
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }

    @Override
    public Iterable<MappedSchema> supportedSchemas() {
        return Arrays.asList(new TemperatureSchemaV1());
    }
}