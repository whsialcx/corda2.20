package net.corda.samples.example.contracts;

import net.corda.samples.example.states.TemperatureState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;

import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class TemperatureContract implements Contract {
    public static final String ID = "net.corda.samples.example.contracts.TemperatureContract";

    @Override
    public void verify(LedgerTransaction tx) {
        final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);

        if (command.getValue() instanceof Commands.Create) {
            verifyCreate(tx, command);
        } else if (command.getValue() instanceof Commands.Transfer) {
            verifyTransfer(tx, command);
        } else {
            throw new IllegalArgumentException("Unrecognized command");
        }
    }

    private void verifyCreate(LedgerTransaction tx, CommandWithParties<Commands> command) {
        requireThat(require -> {
            require.using("No inputs should be consumed when creating a temperature record.",
                    tx.getInputs().isEmpty());
            require.using("Only one output state should be created.",
                    tx.getOutputs().size() == 1);

            final TemperatureState output = tx.outputsOfType(TemperatureState.class).get(0);
            require.using("The owner and receiver cannot be the same entity.",
                    !output.getOwner().equals(output.getReceiver()));
            require.using("All participants must be signers.",
                    command.getSigners().containsAll(output.getParticipants().stream()
                            .map(AbstractParty::getOwningKey)
                            .collect(Collectors.toList())));
            return null;
        });
    }

    private void verifyTransfer(LedgerTransaction tx, CommandWithParties<Commands> command) {
        requireThat(require -> {
            require.using("Transfer must consume one input.",
                    tx.getInputs().size() == 1);
            require.using("Transfer must create one output.",
                    tx.getOutputs().size() == 1);

            final TemperatureState input = tx.inputsOfType(TemperatureState.class).get(0);
            final TemperatureState output = tx.outputsOfType(TemperatureState.class).get(0);

            require.using("Only the receiver can be changed.",
                    input.getTimestamp().equals(output.getTimestamp()) &&
                            input.getTemperature() == output.getTemperature() &&
                            input.isCritical() == output.isCritical() &&
                            input.getOwner().equals(output.getOwner()));

            require.using("All participants must be signers.",
                    command.getSigners().containsAll(output.getParticipants().stream()
                            .map(AbstractParty::getOwningKey)
                            .collect(Collectors.toList())));
            return null;
        });
    }

    public interface Commands extends CommandData {
        class Create implements Commands {}
        class Transfer implements Commands {}
    }
}