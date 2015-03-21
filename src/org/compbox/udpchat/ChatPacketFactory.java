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

import java.nio.ByteBuffer;
import java.time.Instant;

/**
 * Since creating {@link ChatPacket}s are kind of a pain, this alleviates some
 * of the responsibility. It allows for import and export of a
 * {@link ChatPacket} to a {@link ByteBuffer} to be sent via a
 * {@code DatagramChannel}. It also handles sequence numbers by incrementing
 * appropriately.
 *
 * @author Kevin Raoofi
 */
public class ChatPacketFactory {

    private static final ChatPacketFactoryInterface defaultFact = ChatPacket::new;
    private final ChatPacketFactoryInterface fact;

    /**
     * The version of the protocol
     */
    public final byte version;
    /**
     * The packet sequence number. NOT based on bytes but based on number of
     * packets sent
     */
    private short sequence;

    public ChatPacketFactory() {
        version = 0;
        fact = ChatPacket::new;
        sequence = 0;
    }

    protected ChatPacketFactory(byte version, ChatPacketFactoryInterface fact) {
        this.version = version;
        this.sequence = 0;
        this.fact = fact;
    }

    public ChatPacket createPacket(PacketType type) {
        return fact.createInstance(type.ID, version, sequence++, new byte[2048],
                Instant.now());
    }

    public ChatPacket createPacket(PacketType type, ByteBuffer content) {
        byte[] data = new byte[content.remaining()];
        content.get(data);
        return fact.createInstance(type.ID, version, sequence++, data, Instant
                .now());
    }

    public static ChatPacket parsePacket(ByteBuffer buf) {
        return parsePacket(buf, Instant.now());
    }

    public static ChatPacket parsePacket(ByteBuffer buf, Instant timestamp) {
        byte bufType = buf.get();
        byte bufVersion = buf.get();
        short bufSequence = buf.getShort();
        byte[] bufData = new byte[buf.remaining()];
        buf.get(bufData);

        return defaultFact.createInstance(bufType, bufVersion, bufSequence,
                bufData, timestamp);
    }

    @FunctionalInterface
    protected static interface ChatPacketFactoryInterface {

        public ChatPacket createInstance(byte type, byte version, short sequence,
                byte[] data, Instant timestamp);
    }

    /**
     *
     */
    public static enum PacketType {

        MESSAGE((byte) 0x00),
        ACK((byte) 0x01),
        UNKNOWN((byte) 0x00);

        public final byte ID;

        PacketType(byte ID) {
            this.ID = ID;
        }

        public static PacketType getPacketType(byte ID) {
            switch (ID) {
                case 0:
                    return MESSAGE;
                case 0x01:
                    return ACK;
                default:
                    return UNKNOWN;
            }
        }
    }
}
