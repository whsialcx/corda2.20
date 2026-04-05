package net.corda.samples.example;

import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.samples.example.flows.TemperatureFlows;
import net.corda.samples.example.states.TemperatureState;
import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.Assert.*;

public class TemperatureFlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                        TestCordapp.findCordapp("net.corda.samples.example.contracts"),
                        TestCordapp.findCordapp("net.corda.samples.example.flows")))
                .withNotarySpecs(ImmutableList.of(new MockNetworkNotarySpec(CordaX500Name.parse("O=Notary,L=London,C=GB")))));
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void flowRecordsTransactionInBothPartiesVaults() throws Exception {
        Party partyA = a.getInfo().getLegalIdentities().get(0);
        Party partyB = b.getInfo().getLegalIdentities().get(0);

        TemperatureFlows.CreateTemperatureFlow flow = new TemperatureFlows.CreateTemperatureFlow(
                Instant.now(), 25.5, false, partyB);
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();

        // Check both nodes recorded the transaction
        assertNotNull(a.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        assertNotNull(b.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));

        // Check both nodes have the state in their vaults
        List<StateAndRef<TemperatureState>> aStates = a.getServices().getVaultService()
                .queryBy(TemperatureState.class).getStates();
        List<StateAndRef<TemperatureState>> bStates = b.getServices().getVaultService()
                .queryBy(TemperatureState.class).getStates();

        assertEquals(1, aStates.size());
        assertEquals(1, bStates.size());

        TemperatureState aState = aStates.get(0).getState().getData();
        TemperatureState bState = bStates.get(0).getState().getData();

        assertEquals(25.5, aState.getTemperature(), 0.001);
        assertEquals(25.5, bState.getTemperature(), 0.001);
        assertEquals(partyA, aState.getOwner());
        assertEquals(partyB, aState.getReceiver());
    }

    @Test
    public void transferFlowUpdatesReceiver() throws Exception {
        Party partyA = a.getInfo().getLegalIdentities().get(0);
        Party partyB = b.getInfo().getLegalIdentities().get(0);

        // First create a temperature record
        TemperatureFlows.CreateTemperatureFlow createFlow = new TemperatureFlows.CreateTemperatureFlow(
                Instant.now(), 25.5, false, partyB);
        CordaFuture<SignedTransaction> createFuture = a.startFlow(createFlow);
        network.runNetwork();
        SignedTransaction createTx = createFuture.get();

        // Get the linearId of the created state
        TemperatureState createdState = (TemperatureState) createTx.getTx().getOutputs().get(0).getData();
        UniqueIdentifier linearId = createdState.getLinearId();

        // Now transfer to a new receiver (node C)
        StartedMockNode c = network.createPartyNode(null);
        network.runNetwork();
        Party partyC = c.getInfo().getLegalIdentities().get(0);

        TemperatureFlows.TransferTemperatureFlow transferFlow = new TemperatureFlows.TransferTemperatureFlow(
                linearId, partyC);
        CordaFuture<SignedTransaction> transferFuture = a.startFlow(transferFlow);
        network.runNetwork();
        SignedTransaction transferTx = transferFuture.get();

        // Check all nodes have the updated state
        List<StateAndRef<TemperatureState>> aStates = a.getServices().getVaultService()
                .queryBy(TemperatureState.class).getStates();
        List<StateAndRef<TemperatureState>> bStates = b.getServices().getVaultService()
                .queryBy(TemperatureState.class).getStates();
        List<StateAndRef<TemperatureState>> cStates = c.getServices().getVaultService()
                .queryBy(TemperatureState.class).getStates();

        assertEquals(1, aStates.size());
        assertEquals(0, bStates.size()); // Original receiver should no longer have the state
        assertEquals(1, cStates.size()); // New receiver should have the state

        TemperatureState transferredState = aStates.get(0).getState().getData();
        assertEquals(partyC, transferredState.getReceiver());
        assertEquals(25.5, transferredState.getTemperature(), 0.001);
    }
}