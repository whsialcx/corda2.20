package net.corda.samples.example.schema;

import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Arrays;
import java.util.UUID;
import org.hibernate.annotations.Type;
import javax.annotation.Nullable;

public class TemperatureSchemaV1 extends MappedSchema {
    public TemperatureSchemaV1() {
        super(TemperatureSchema.class, 1, Arrays.asList(PersistentTemperature.class));
    }

    @Nullable
    @Override
    public String getMigrationResource() {
        return "temperature.changelog-master";
    }

    @Entity
    @Table(name = "temperature_states")
    public static class PersistentTemperature extends PersistentState {
        @Column(name = "timestamp") private final String timestamp;
        @Column(name = "temperature") private final double temperature;
        @Column(name = "is_critical") private final boolean isCritical;
        @Column(name = "owner") private final String owner;
        @Column(name = "receiver") private final String receiver;
        @Column(name = "linear_id") @Type(type = "uuid-char") private final UUID linearId;

        public PersistentTemperature(String timestamp, double temperature, boolean isCritical,
                                     String owner, String receiver, UUID linearId) {
            this.timestamp = timestamp;
            this.temperature = temperature;
            this.isCritical = isCritical;
            this.owner = owner;
            this.receiver = receiver;
            this.linearId = linearId;
        }

        public PersistentTemperature() {
            this.timestamp = null;
            this.temperature = 0;
            this.isCritical = false;
            this.owner = null;
            this.receiver = null;
            this.linearId = null;
        }

        // Getters
        public String getTimestamp() { return timestamp; }
        public double getTemperature() { return temperature; }
        public boolean isCritical() { return isCritical; }
        public String getOwner() { return owner; }
        public String getReceiver() { return receiver; }
        public UUID getLinearId() { return linearId; }
    }
}