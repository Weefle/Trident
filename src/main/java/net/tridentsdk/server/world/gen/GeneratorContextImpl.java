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
package net.tridentsdk.server.world.gen;

import net.tridentsdk.server.world.opt.ChunkSection;
import net.tridentsdk.world.gen.GeneratorContext;
import net.tridentsdk.world.opt.BlockState;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Implementation of a generator context.
 */
@ThreadSafe
public class GeneratorContextImpl implements GeneratorContext {
    /**
     * The seed to be used for generation
     */
    private long seed;
    /**
     * List of chunk sections
     */
    @GuardedBy("list")
    private final ChunkSection[] sections = new ChunkSection[16];

    public GeneratorContextImpl(long seed) {
        this.seed = seed;
    }

    /**
     * Obtains the collection of chunk sections that were
     * generated by the context.
     *
     * @return the section list
     */
    public ChunkSection[] sections() {
        return this.sections;
    }

    @Override
    public long nextLong() {
        long x = this.seed;
        x ^= (x << 21);
        x ^= (x >>> 35);
        x ^= (x << 4);
        return x;
    }

    @Override
    public long nextLong(long max) {
        return this.nextLong() % max;
    }

    @Override
    public int nextInt() {
        return 0;
    }

    @Override
    public int nextInt(int max) {
        return 0;
    }

    @Override
    public long seed() {
        return 0;
    }

    // short is perfect for storing block data because
    // short = 2 bytes = 16 bits
    // 8 bit block id
    // 4 bit meta
    // 4 bit add (unused)
    // ------------------
    // 16 bits

    @Override
    public BlockState build(int id, byte meta) {
        return new BlockState(id, meta);
    }

    /**
     * http://minecraft.gamepedia.com/Chunk_format
     * int BlockPos = y*16*16 + z*16 + x;
     *
     * return (y * (2^8)) + (z * (2^4)) + x;
     * use OR instead because bitwise ops are faster and
     * provides the same results as addition
     *
     * max size of this array is blocks in section, 4096
     * 16*16*16
     */
    @Override
    public int idx(int x, int y, int z) {
        return y << 8 | z << 4 | x;
    }

    @Override
    public int section(int y) {
        return y / 16;
    }

    @Override
    public void set(int x, int y, int z, BlockState state) {
        int secY = this.section(y);
        ChunkSection section;
        synchronized (this.sections) {
            section = this.sections[secY];
            if (section == null) {
                section = new ChunkSection(secY);
                this.sections[secY] = section;
            }
        }

        int idx = this.idx(x, y & 15, z);
        section.set(idx, state);
    }
}