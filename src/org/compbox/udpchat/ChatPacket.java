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

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

/**
 * This class deals entirely in abstracting the contents of the data part of a
 * {@code DatagramPacket}. It comes with additional metadata such as the packet
 * type, a version, sequence, and its very own data array. However, none of them
 * are really used except for the type, and, even then, it's rather useless
 * since only one kind of packet matters.
 * 
 *
 * @author Kevin Raoofi
 * @see ChatPacketFactory
 */
public class ChatPacket implements Serializable {

    /**
     * The type of the packet
     */
    protected final byte type;
    /**
     * The version of the chat protocol
     */
    protected final byte version;
    /**
     * The sequence number of the packet
     */
    protected final short sequence;
    /**
     * The actual data
     */
    protected final byte[] data;

    /**
     * Timestamps of the packet. Time is based on the perspective of the host.
     * There is no actual information related to time included when
     * serialized/deserialized.
     */
    public transient final Instant timestamp;

    /**
     * A {@code Comparator} which orders {@link ChatPacket} instances based on
     * its sequence number.
     *
     * @return {@code Comparator} based on sequence number
     */
    public static Comparator<ChatPacket> getSequenceComparator() {
        return (ChatPacket o1, ChatPacket o2) -> {
            return o1.getSequence() - o2.getSequence();
        };
    }

    protected ChatPacket(byte type, byte version, short sequence, byte[] data,
            Instant timestamp) {
        this.type = type;
        this.version = version;
        this.sequence = sequence;
        this.data = data;
        this.timestamp = timestamp;
    }

    public byte getVersion() {
        return this.version;
    }

    public byte getType() {
        return this.type;
    }

    /**
     * Get the sequence number of the packet. This value is used to
     *
     * @return the sequence number
     */
    public short getSequence() {
        return this.sequence;
    }

    /**
     * Returns the backing array for the packet.
     *
     * @return the backing array
     */
    public byte[] getData() {
        return this.data;
    }

    public void toByteBuffer(ByteBuffer buf) {
        buf.put(this.type);
        buf.put(this.version);
        buf.putShort(sequence);
        buf.put(this.data);
    }

    @Override
    public String toString() {
        return "ChatPacket{" + "type=" + type + ", version=" + version
                + ", sequence=" + sequence + ", data=" + Arrays.toString(data)
                + '}';
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + this.type;
        hash = 53 * hash + this.version;
        hash = 53 * hash + this.sequence;
        hash = 53 * hash + Arrays.hashCode(this.data);
        hash = 53 * hash + Objects.hashCode(this.timestamp);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ChatPacket other = (ChatPacket) obj;
        if (this.type != other.type) {
            return false;
        }
        if (this.version != other.version) {
            return false;
        }
        if (this.sequence != other.sequence) {
            return false;
        }
        if (!Arrays.equals(this.data, other.data)) {
            return false;
        }
        if (!Objects.equals(this.timestamp, other.timestamp)) {
            return false;
        }
        return true;
    }

}
