/*
 * Copyright (c) 2014, The TridentSDK Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     1. Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *     2. Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *     3. Neither the name of TridentSDK nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.tridentsdk.packets.play.out;

import io.netty.buffer.ByteBuf;
import net.tridentsdk.api.Location;
import net.tridentsdk.api.util.Vector;
import net.tridentsdk.data.RecordBuilder;
import net.tridentsdk.server.netty.packet.OutPacket;

public class PacketPlayOutExplosion extends OutPacket {

    private Location loc;
    private int recordCount;
    private RecordBuilder[] records;
    private Vector velocity;

    @Override
    public int getId() {
        return 0x27;
    }

    public Location getLoc() {
        return loc;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public RecordBuilder[] getRecords() {
        return records;
    }

    public Vector getVelocity() {
        return velocity;
    }

    public void cleanup() {
        RecordBuilder[] newRecords = new RecordBuilder[] {};

        for(RecordBuilder builder : records) {
            if(builder != null) {
                newRecords[newRecords.length] = builder;
            }
        }

        this.records = newRecords;
    }

    @Override
    public void encode(ByteBuf buf) {
        buf.writeFloat((float) loc.getX());
        buf.writeFloat((float) loc.getY());
        buf.writeFloat((float) loc.getZ());
        buf.writeFloat(0F); // unused by client

        buf.writeInt(recordCount);

        for(RecordBuilder builder : records) {
            builder.write(buf);
        }

        buf.writeFloat((float) velocity.getX());
        buf.writeFloat((float) velocity.getY());
        buf.writeFloat((float) velocity.getZ());
    }
}
