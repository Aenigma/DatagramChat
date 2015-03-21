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

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayDeque;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.compbox.udpchat.ChatPacketFactory.PacketType.MESSAGE;

/**
 * Client code for each server. Client sockets are re-instantiated for each
 * packet sent. Mostly similar to {@link ChatServer} in implementation
 *
 * @author Kevin Raoofi
 */
public class ChatClient implements Runnable {

    private static final Logger LOG = Logger.getLogger(ChatClient.class
            .getName());
    private final NavigableSet<ChatPacket> sentMsgs;
    private final SortedSet<ChatPacket> allMsgs;
    private final ChatPacketFactory cpf;
    private final ByteBuffer buf;
    private final SocketAddress add;
    private SocketAddress listeningAdd;
    private final Queue<String> msgQueue;
    private final ChatPacketDispatcher sentEvents;
    private DatagramChannel clientSocket;

    public ChatClient(SortedSet<ChatPacket> allMsgs, SocketAddress add) {
        cpf = new ChatPacketFactory();
        buf = ByteBuffer.allocate(2048);
        this.add = add;
        msgQueue = new ArrayDeque<>();
        this.sentEvents = new ChatPacketDispatcher();
        this.allMsgs = allMsgs;
        this.sentMsgs = new TreeSet<>(ChatPacket.getSequenceComparator());

        sentEvents.register(MESSAGE, (pck, sa) -> sentMsgs.add(pck));
        sentEvents.register(MESSAGE, (pck, sa) -> allMsgs.add(pck));
    }

    public void resetChannel(SocketAddress listeningAdd) {
        try {
            this.listeningAdd = listeningAdd;
            if (clientSocket != null) {
                clientSocket.close();
            }
            clientSocket = DatagramChannel.open();
            clientSocket.bind(this.listeningAdd);
        } catch (IOException ex) {
            Logger.getLogger(ChatClient.class.getName()).log(Level.SEVERE, null,
                    ex);
        }
    }

    @Override
    public void run() {
        try {
            clientSocket = DatagramChannel.open();

            buf.put(msgQueue.poll().getBytes());
            buf.flip();

            ChatPacket cp = cpf.createPacket(
                    ChatPacketFactory.PacketType.MESSAGE, buf);
            LOG.log(Level.INFO, "Finished crafting packet, data is: {0}", cp
                    .toString());
            buf.clear();
            cp.toByteBuffer(buf);
            buf.flip();
            LOG.log(Level.INFO, "Sending packet...: {0}", cp.toString());
            sentEvents.dispatch(cp, add);
            clientSocket.send(buf, add);
            LOG.log(Level.INFO, "Sent packet: {0}", cp.toString());

            buf.clear();
            clientSocket.receive(buf);
            buf.flip();
            cp = ChatPacketFactory.parsePacket(buf);
            buf.clear();

            LOG.log(Level.INFO,
                    "Got Packet: {0}", cp);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Client error!", ex);
        }
    }

    public NavigableSet<ChatPacket> getSentMsgs() {
        return sentMsgs;
    }

    public SortedSet<ChatPacket> getAllMsgs() {
        return allMsgs;
    }

    public void sendMsg(String msg) {
        msgQueue.add(msg);
        this.run();
    }
}
