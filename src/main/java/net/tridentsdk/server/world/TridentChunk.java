/*
 * Trident - A Multithreaded Server Alternative
 * Copyright 2016 The TridentSDK Team
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
package net.tridentsdk.server.world;

import io.netty.buffer.ByteBuf;
import net.tridentsdk.base.Block;
import net.tridentsdk.server.world.gen.GeneratorContextImpl;
import net.tridentsdk.server.world.opt.ChunkSection;
import net.tridentsdk.world.Chunk;
import net.tridentsdk.world.World;
import net.tridentsdk.world.gen.GenContainer;
import net.tridentsdk.world.gen.GeneratorProvider;
import net.tridentsdk.world.gen.TerrainGenerator;
import net.tridentsdk.world.opt.GenOpts;

import javax.annotation.Nonnull;
import java.util.concurrent.CountDownLatch;

import static net.tridentsdk.server.net.NetData.wvint;

/**
 * Represents a chunk column.
 */
public class TridentChunk implements Chunk {
    /**
     * The ready state for this chunk, whether it has fully
     * generated yet.
     */
    private final CountDownLatch ready = new CountDownLatch(1);
    /**
     * The world in which this chunk is located
     */
    private final TridentWorld world;
    /**
     * The x coordinate
     */
    private final int x;
    /**
     * The z coordinate
     */
    private final int z;
    /**
     * The sections that the chunk has generated
     */
    private volatile ChunkSection[] sections;

    /**
     * Creates a new chunk at the specified coordinates.
     *
     * @param x the x coordinate
     * @param z the z coordinate
     */
    public TridentChunk(TridentWorld world, int x, int z) {
        this.world = world;
        this.x = x;
        this.z = z;
    }

    /**
     * Generates the chunk.
     */
    public void generate() {
        GenOpts opts = this.world.genOpts();
        GeneratorProvider provider = opts.provider();
        GenContainer container = provider.container();

        TerrainGenerator terrain = provider.terrain(this.world);
        GeneratorContextImpl context = new GeneratorContextImpl(opts.seed());
        terrain.generate(this.x, this.z, context);
        this.sections = context.sections();

        this.ready.countDown();
    }

    /**
     * Awaits for the chunk ready state to finish,
     * indicating that the chunk has finished generation.
     *
     * @return the chunk, when ready
     */
    public TridentChunk waitReady() {
        try {
            this.ready.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return this;
    }

    /**
     * Calculates the bitmask for this chunk.
     *
     * @return the chunk bitmask
     */
    public int mask() {
        int mask = 0;
        for (int i = 0; i < this.sections.length; i++) {
            if (this.sections[i] == null) break;
            mask |= 1 << i;
        }

        return mask;
    }

    public void write(ByteBuf buf, boolean continuous) {
        int mask = 0;
        int highestIdx = 0;
        for (int i = 0; i < this.sections.length; i++) {
            if (this.sections[i] == null) break;
            mask |= 1 << i;
            highestIdx++;
        }
        wvint(buf, mask);

        wvint(buf, highestIdx);
        for (ChunkSection section : this.sections) {
            if (section == null) break;
            section.write(buf);
        }

        if (continuous) {
            for (int i = 0; i < 256; i++) {
                buf.writeByte(0);
            }
        }

        wvint(buf, 0);
    }

    @Override
    public int x() {
        return this.x;
    }

    @Override
    public int z() {
        return this.z;
    }

    @Nonnull
    @Override
    public Block blockAt(int x, int y, int z) {
        return null;
    }

    @Override
    public World world() {
        return this.world;
    }
}