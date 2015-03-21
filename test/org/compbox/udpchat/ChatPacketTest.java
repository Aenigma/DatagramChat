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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.*;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Comparator;
import java.util.TreeSet;

/**
 * Tests all logic in {@link ChatPacket}
 *
 * @author Kevin Raoofi
 */
public class ChatPacketTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test of getSequenceComparator method, of class ChatPacket.
     */
    @Test
    public void testGetSequenceComparator() {
        System.out.println("getSequenceComparator");
        Comparator<ChatPacket> cmp = ChatPacket.getSequenceComparator();
        int expResult = 0;
        int result = cmp.compare(
                new ChatPacket((byte) 0, (byte) 0, (short) 0, new byte[]{},
                        Instant.MIN),
                new ChatPacket((byte) 0, (byte) 0, (short) 0, new byte[]{},
                        Instant.MIN));
        assertThat(result, is(expResult));

        TreeSet<ChatPacket> ts = new TreeSet<>(cmp);
        ts.add(new ChatPacket((byte) 0, (byte) 0, (short) 5, new byte[]{0},
                Instant.MIN));
        ts.add(new ChatPacket((byte) 0, (byte) 0, (short) 2, new byte[]{1},
                Instant.MIN));
        ts.add(new ChatPacket((byte) 0, (byte) 0, (short) 19, new byte[]{2},
                Instant.MIN));
        ts.add(new ChatPacket((byte) 0, (byte) 0, (short) 0, new byte[]{3},
                Instant.MIN));

        assertThat(ts.pollFirst().getData(), is(new byte[]{3}));
        assertThat(ts.pollFirst().getData(), is(new byte[]{1}));
        assertThat(ts.pollFirst().getData(), is(new byte[]{0}));
        assertThat(ts.pollFirst().getData(), is(new byte[]{2}));
    }

    /**
     * Test of toByteBuffer method, of class ChatPacket.
     */
    @Test
    public void testToByteBuffer() {
        System.out.println("toByteBuffer");
        ByteBuffer buf = ByteBuffer.allocate(2048);
        buf.clear();
        ChatPacket instance = new ChatPacket((byte) 0, (byte) 0, (short) 0,
                new byte[]{1, 2, 10, 30, 32}, Instant.MIN);
        instance.toByteBuffer(buf);
        buf.flip();
        ChatPacket result = ChatPacketFactory.parsePacket(buf, Instant.MIN);
        assertThat(result, is(instance));
    }
}
