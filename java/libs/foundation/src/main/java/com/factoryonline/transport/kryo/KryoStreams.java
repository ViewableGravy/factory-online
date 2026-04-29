package com.factoryonline.transport.kryo;

import org.objenesis.strategy.StdInstantiatorStrategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.factoryonline.transport.commands.SharedCommands;

public final class KryoStreams {
    public static Kryo createKryo() {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(true);
        kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
        SharedCommands.registerDTOs(kryo);
        return kryo;
    }
}
