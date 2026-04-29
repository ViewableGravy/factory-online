package com.factoryonline.transport.commands;

import com.esotericsoftware.kryo.Kryo;
import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.foundation.timing.TickControl;
import com.factoryonline.foundation.timing.TickMode;
import com.factoryonline.simulation.SimulationAugmentation;
import com.factoryonline.simulation.SimulationDirection;
import com.factoryonline.simulation.SimulationSnapshot;

public final class SharedCommands {
    public static void registerDTOs(Kryo kryo) {
        Kryo validatedKryo = requireKryo(kryo);
        validatedKryo.register(ProtocolCommand.class);
        validatedKryo.register(ClientTransportCommand.class);
        validatedKryo.register(JoinSimulationCommand.class);
        validatedKryo.register(SimulationInputCommand.class);
        validatedKryo.register(InitialSimulationStateCommand.class);
        validatedKryo.register(TickSyncCommand.class);
        validatedKryo.register(AckCommand.class);
        validatedKryo.register(RejectionCommand.class);
        validatedKryo.register(SimulationUpdateCommand.class);

        validatedKryo.register(ClientId.class);
        validatedKryo.register(SimulationId.class);
        validatedKryo.register(SimulationAugmentation.class);
        validatedKryo.register(SimulationSnapshot.class);
        validatedKryo.register(SimulationDirection.class);
        validatedKryo.register(TickControl.class);
        validatedKryo.register(TickMode.class);
    }

    private static Kryo requireKryo(Kryo kryo) {
        if (kryo == null) {
            throw new IllegalArgumentException("kryo must not be null");
        }

        return kryo;
    }
}
