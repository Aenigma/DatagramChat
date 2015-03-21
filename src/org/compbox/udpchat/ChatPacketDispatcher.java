/*
 * Copyright (C) 2014 Kevin Raoofi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.compbox.udpchat;

import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.compbox.udpchat.ChatPacketFactory.PacketType;

/**
 * So, this class has a kind of an Observer pattern for event driven processing
 * of packets. {@link ChatClient} and {@link ChatServer} use this and register
 * {@link BiConsumer}s based on the packet type.
 *
 * @author Kevin Raoofi
 */
public class ChatPacketDispatcher {

    /**
     * Look up table for handlers
     */
    private final EnumMap<PacketType, Collection<BiConsumer<ChatPacket, SocketAddress>>> packetTypeMap;

    /**
     * The class logger for the default logging {@code Consumer}s.
     */
    private static final Logger LOG = Logger.getLogger(
            ChatPacketDispatcher.class.getName());

    /**
     *
     * @return a {@link ChatPacketDispatcher} event handlers which log data for
     *         messages
     */
    public static ChatPacketDispatcher constructWithLoggingConsumers() {
        ChatPacketDispatcher dispatcher = new ChatPacketDispatcher();
        dispatcher.register(PacketType.MESSAGE, (cp, sa) -> {
            LOG.log(Level.INFO, "Got a MESSAGE: {0}", cp);
        });
        dispatcher.register(PacketType.ACK, (cp, sa) -> {
            LOG.log(Level.INFO, "Got an ACK: {0}", cp);
        });
        return dispatcher;
    }

    /**
     * Constructs a {@link ChatPacketDispatcher} with no event handlers
     */
    public ChatPacketDispatcher() {
        packetTypeMap = new EnumMap(PacketType.class);
        /*
         * Sets up every possible
         */
        Arrays.stream(PacketType.values())
                .forEach((PacketType type) -> packetTypeMap.put(type,
                                new LinkedList<>()));
    }

    /**
     * Adds handlers associated with the {@link PacketType}
     *
     * @param type          the {@link PacketType} to remove a handler from
     * @param eventHandlers {@code Consumer}s which handles {@link ChatPacket}s
     */
    public void register(PacketType type,
            BiConsumer<ChatPacket, SocketAddress>... eventHandlers) {
        Collection<BiConsumer<ChatPacket, SocketAddress>> eventsSet = packetTypeMap
                .get(type);
        eventsSet.addAll(Arrays.asList(eventHandlers));
    }

    /**
     * Removes a handler associated for the {@link PacketType}
     *
     * @param type    the {@link PacketType} to remove a handler from
     * @param handler a {@code Consumer} which handles {@link ChatPacket}s
     * @return true if handler was found; otherwise, false
     */
    public boolean unregister(PacketType type,
            BiConsumer<ChatPacket, SocketAddress> handler) {
        return packetTypeMap.get(type).remove(handler);
    }

    /**
     * Gets the handlers for a given {@link PacketType}.
     *
     * This method returns a copy of the {@code Collection} used internally and
     * changes to it will not modify the internal state of this instance.
     *
     * @param type the {@link PacketType} to look up handlers for
     * @return copy of the {@code Collection} of handlers for a given
     *         {@link PacketType}
     */
    public Collection<BiConsumer<ChatPacket, SocketAddress>> getHandlers(
            PacketType type) {
        return new LinkedList<>(packetTypeMap.get(type));
    }

    /**
     *
     * @param cp ChatPacket with the data
     * @param sa The destination address
     */
    public void dispatch(ChatPacket cp, SocketAddress sa) {
        PacketType type = PacketType.getPacketType(cp.getType());
        packetTypeMap.get(type).stream()
                .forEach((evh) -> {
                    evh.accept(cp, sa);
                });
    }
}
