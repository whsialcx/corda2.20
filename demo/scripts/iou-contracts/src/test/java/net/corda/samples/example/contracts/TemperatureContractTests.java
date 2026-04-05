package net.corda.samples.example.contracts;

import net.corda.samples.example.states.TemperatureState;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.time.Instant;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class TemperatureContractTests {
    static private final MockServices ledgerServices = new MockServices();
    static private final TestIdentity megaCorp = new TestIdentity(new CordaX500Name("MegaCorp", "London", "GB"));
    static private final TestIdentity miniCorp = new TestIdentity(new CordaX500Name("MiniCorp", "London", "GB"));
    static private final Instant now = Instant.now();

    @Test
    public void transactionMustIncludeCreateCommand() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(TemperatureContract.ID, new TemperatureState(now, 25.5, false, miniCorp.getParty(), megaCorp.getParty(), new UniqueIdentifier()));
                tx.fails();
                tx.command(ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()), new TemperatureContract.Commands.Create());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void transactionMustHaveNoInputs() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(TemperatureContract.ID, new TemperatureState(now, 25.5, false, miniCorp.getParty(), megaCorp.getParty(), new UniqueIdentifier()));
                tx.output(TemperatureContract.ID, new TemperatureState(now, 25.5, false, miniCorp.getParty(), megaCorp.getParty(), new UniqueIdentifier()));
                tx.command(ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()), new TemperatureContract.Commands.Create());
                tx.failsWith("No inputs should be consumed when creating a temperature record.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void transactionMustHaveOneOutput() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(TemperatureContract.ID, new TemperatureState(now, 25.5, false, miniCorp.getParty(), megaCorp.getParty(), new UniqueIdentifier()));
                tx.output(TemperatureContract.ID, new TemperatureState(now, 25.5, false, miniCorp.getParty(), megaCorp.getParty(), new UniqueIdentifier()));
                tx.command(ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()), new TemperatureContract.Commands.Create());
                tx.failsWith("Only one output state should be created.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void ownerMustSignTransaction() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(TemperatureContract.ID, new TemperatureState(now, 25.5, false, miniCorp.getParty(), megaCorp.getParty(), new UniqueIdentifier()));
                tx.command(megaCorp.getPublicKey(), new TemperatureContract.Commands.Create());
                tx.failsWith("All participants must be signers.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void receiverMustSignTransaction() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(TemperatureContract.ID, new TemperatureState(now, 25.5, false, miniCorp.getParty(), megaCorp.getParty(), new UniqueIdentifier()));
                tx.command(miniCorp.getPublicKey(), new TemperatureContract.Commands.Create());
                tx.failsWith("All participants must be signers.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void ownerIsNotReceiver() {
        final TestIdentity megaCorpDupe = new TestIdentity(megaCorp.getName(), megaCorp.getKeyPair());
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(TemperatureContract.ID, new TemperatureState(now, 25.5, false, megaCorp.getParty(), megaCorpDupe.getParty(), new UniqueIdentifier()));
                tx.command(ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()), new TemperatureContract.Commands.Create());
                tx.failsWith("The owner and receiver cannot be the same entity.");
                return null;
            });
            return null;
        }));
    }
}