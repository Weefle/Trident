/*
 * Trident - A Multithreaded Server Alternative
 * Copyright 2014 The TridentSDK Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.tridentsdk.server.packets.play.in;

import io.netty.buffer.ByteBuf;
import net.tridentsdk.Handler;
import net.tridentsdk.Position;
import net.tridentsdk.event.player.PlayerMoveEvent;
import net.tridentsdk.server.netty.ClientConnection;
import net.tridentsdk.server.netty.packet.InPacket;
import net.tridentsdk.server.netty.packet.Packet;
import net.tridentsdk.server.packets.play.out.PacketPlayOutEntityTeleport;
import net.tridentsdk.server.player.PlayerConnection;
import net.tridentsdk.server.player.TridentPlayer;

/**
 * This packet is sent when the player wishes updates the player's XYZ position on the server.
 */
public class PacketPlayInPlayerMove extends InPacket {
    /**
     * Updated location, Y is the feet location
     */
    protected Position location;
    /**
     * Wether the player is on the ground or not
     */
    protected boolean onGround;

    @Override
    public int id() {
        return 0x04;
    }

    @Override
    public Packet decode(ByteBuf buf) {
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();

        this.location = Position.create(null, x, y, z); // TODO: Get the player's world

        this.onGround = buf.readBoolean();

        return this;
    }

    public Position location() {
        return this.location;
    }

    public boolean onGround() {
        return this.onGround;
    }

    @Override
    public void handleReceived(ClientConnection connection) {
        TridentPlayer player = ((PlayerConnection) connection).player();
        this.location.setWorld(player.world());
        Position from = player.position();
        Position to = this.location;

        PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);

        Handler.forEvents().fire(event);

        if (event.isIgnored()) {
            PacketPlayOutEntityTeleport cancel = new PacketPlayOutEntityTeleport();

            cancel.set("entityId", player.entityId()).set("location", from).set("onGround", player.onGround());

            TridentPlayer.sendAll(cancel);
            return;
        }

        player.setLocation(to);
    }
}
