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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Collections;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.compbox.udpchat.ChatPacketFactory.PacketType.*;

/**
 * In charge of handling datagram connections. The implementation for
 * {@link ChatClient} and this should be the same and the class should handle
 * its own dispatching of threads. Additionally, NIO features were not even made
 * use of. The implementation here is a significant design flaw and should
 * totally be rewritten from scratch if revisited.
 *
 * The terrible design here negatively impacted the designs of the other files.
 *
 * @author Kevin Raoofi
 */
public class ChatServer implements Runnable {

    public final SocketAddress add;

    private static final Logger LOG = Logger.getLogger(ChatServer.class
            .getName());

    private final DatagramChannel srvChannel;
    private final ChatPacketFactory factory;
    private final ByteBuffer buf;
    private final NavigableSet<ChatPacket> receivedMsgs;

    private final SortedSet<ChatPacket> allMsgs;
    private final ChatPacketDispatcher dispatcher;

    public ChatServer(SortedSet<ChatPacket> allMsgs) throws IOException {
        this(allMsgs, new InetSocketAddress(65434));

    }

    public ChatServer(SortedSet<ChatPacket> allMsgs, SocketAddress add)
            throws IOException {
        this.srvChannel = DatagramChannel.open();
        this.factory = new ChatPacketFactory();
        this.add = add;
        this.srvChannel.bind(add);
        this.buf = ByteBuffer.allocate(2048);
        this.receivedMsgs = new TreeSet<>(ChatPacket
                .getSequenceComparator());

        this.allMsgs = allMsgs;
//        this.allMsgs = Collections.synchronizedSortedSet(new TreeSet<>(
//                (ChatPacket o1, ChatPacket o2) -> {
//                    return o1.timestamp.compareTo(o2.timestamp);
//                }));

        this.dispatcher = ChatPacketDispatcher.constructWithLoggingConsumers();

        dispatcher.register(MESSAGE, (pck, sa) -> receivedMsgs.add(pck));
        dispatcher.register(MESSAGE, (pck, sa) -> allMsgs.add(pck));

        dispatcher.register(MESSAGE, (pck, sa) -> {
            try {
                buf.putShort(pck.getSequence());
                buf.flip();
                ChatPacket cp = factory.createPacket(
                        ChatPacketFactory.PacketType.ACK, buf);

                LOG.log(Level.INFO, "Sending ACK: {0}", cp);
                buf.clear();
                cp.toByteBuffer(buf);
                buf.flip();
                this.srvChannel.send(buf, sa);
                LOG.log(Level.INFO, "Finished serving {0}",
                        sa);
                buf.clear();
            } catch (IOException ex) {
                LOG.log(Level.SEVERE,
                        "Got an error processing event: {0}", ex);
            }
        });
    }

    public void register(ChatPacketFactory.PacketType type,
            BiConsumer<ChatPacket, SocketAddress>... eventHandlers) {
        this.dispatcher.register(type, eventHandlers);
    }

    /*
     * Should be run as daemon thread
     */
    @Override
    public void run() {
        while (true) {
            try {
                //DatagramPacket pck = new DatagramPacket(buf, buf.length);
                SocketAddress clientAddr = this.srvChannel.receive(buf);
                buf.flip();
                LOG.log(Level.INFO, "Got a connection from {0}", clientAddr);
                ChatPacket packet = ChatPacketFactory.parsePacket(buf);
                buf.clear();
                dispatcher.dispatch(packet, clientAddr);

            } catch (IOException ex) {
                LOG.log(Level.SEVERE, "Server error! Crashing....", ex);
                return;
            }
        }
    }

    public static void main(String... args) throws IOException,
            InterruptedException {
        ExecutorService exec = Executors.newCachedThreadPool();
        SortedSet<ChatPacket> allMsgs;
        allMsgs = Collections.synchronizedSortedSet(new TreeSet<>(
                (ChatPacket o1, ChatPacket o2) -> {
                    return o1.timestamp.compareTo(o2.timestamp);
                }));

        exec.submit(new ChatServer(allMsgs, new InetSocketAddress(1234)));
        final ChatPacketFactory cpf = new ChatPacketFactory();
        Runnable r;
        r = () -> {
            try {
                DatagramChannel clientSocket = DatagramChannel.open();
                InetSocketAddress add = new InetSocketAddress("127.0.0.1", 1234);
                ByteBuffer buf = ByteBuffer.allocate(2048);
                buf.put("Hello, how are you?".getBytes());
                buf.flip();

                ChatPacket cp = cpf.createPacket(
                        ChatPacketFactory.PacketType.MESSAGE, buf);
                LOG.log(Level.INFO, "Finished crafting packet, data is: {0}", cp
                        .toString());
                buf.clear();
                cp.toByteBuffer(buf);
                buf.flip();
                clientSocket.send(buf, add);
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
        };
        exec.shutdown();

        for (int i = 0; i < 10; i++) {
            r.run();
        }
    }
}
