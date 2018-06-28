/*
 * Copyright © 2013, General Electric Corporation
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.smoketurner.snowizard.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

import com.smoketurner.snowizard.exceptions.InvalidSystemClock;
import com.smoketurner.snowizard.exceptions.InvalidUserAgentError;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class IdWorkerTest {
  private static final long WORKER_MASK = 0x000000000001F000L;
  private static final long DATACENTER_MASK = 0x00000000003E0000L;
  private static final long TIMESTAMP_MASK = 0xFFFFFFFFFFC00000L;

  class EasyTimeWorker extends IdWorker {
    public List<Long> queue = new ArrayList<>();

    public EasyTimeWorker(int workerId, int datacenterId) {
      super(workerId, datacenterId);
    }

    public void addTimestamp(long timestamp) {
      queue.add(timestamp);
    }

    public long timeMaker() {
      return queue.remove(0);
    }

    @Override
    protected long timeGen() {
      return timeMaker();
    }
  }

  class WakingIdWorker extends EasyTimeWorker {
    public int slept = 0;

    public WakingIdWorker(int workerId, int datacenterId) {
      super(workerId, datacenterId);
    }

    @Override
    protected long tilNextMillis(long lastTimestamp) {
      slept += 1;
      return super.tilNextMillis(lastTimestamp);
    }
  }

  class StaticTimeWorker extends IdWorker {
    public long time = 1L;

    public StaticTimeWorker(int workerId, int datacenterId) {
      super(workerId, datacenterId);
    }

    @Override
    protected long timeGen() {
      return time + TWEPOCH;
    }
  }

  @Test
  public void testInvalidWorkerId() {
    try {
      IdWorker.builder(-1, 1).build();
      failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
    }

    try {
      IdWorker.builder(32, 1).build();
      failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
    }
  }

  @Test
  public void testInvalidDatacenterId() {
    try {
      IdWorker.builder(1, -1).build();
      failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
    }

    try {
      IdWorker.builder(1, 32).build();
      failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
    }
  }

  @Test
  public void testGenerateId() throws Exception {
    final IdWorker worker = IdWorker.builder(1, 1).build();
    final long id = worker.nextId();
    assertThat(id).isGreaterThan(0L);
  }

  @Test
  public void testAccurateTimestamp() throws Exception {
    final IdWorker worker = IdWorker.builder(1, 1).build();
    final long time = System.currentTimeMillis();
    assertThat(worker.getTimestamp() - time).isLessThan(50L);
  }

  @Test
  public void testWorkerId() throws Exception {
    final IdWorker worker = IdWorker.builder(1, 1).build();
    assertThat(worker.getWorkerId()).isEqualTo(1);
  }

  @Test
  public void testDatacenterId() throws Exception {
    final IdWorker worker = IdWorker.builder(1, 1).build();
    assertThat(worker.getDatacenterId()).isEqualTo(1);
  }

  @Test
  public void testMaskWorkerId() throws Exception {
    final int workerId = 0x1F;
    final int datacenterId = 0;
    final IdWorker worker = IdWorker.builder(workerId, datacenterId).build();
    for (int i = 0; i < 1000; i++) {
      Long id = worker.nextId();
      assertThat((id & WORKER_MASK) >> 12).isEqualTo(Long.valueOf(workerId));
    }
  }

  @Test
  public void testMaskDatacenterId() throws Exception {
    final int workerId = 0;
    final int datacenterId = 0x1F;
    final IdWorker worker = IdWorker.builder(workerId, datacenterId).build();
    final Long id = worker.nextId();
    assertThat((id & DATACENTER_MASK) >> 17).isEqualTo(Long.valueOf(datacenterId));
  }

  @Test
  public void testMaskTimestamp() throws Exception {
    final EasyTimeWorker worker = new EasyTimeWorker(31, 31);
    for (int i = 0; i < 100; i++) {
      long timestamp = System.currentTimeMillis();
      worker.addTimestamp(timestamp);
      long id = worker.nextId();
      assertThat((id & TIMESTAMP_MASK) >> 22).isEqualTo(timestamp - IdWorker.TWEPOCH);
    }
  }

  @Test
  public void testRollOverSequenceId() throws Exception {
    final int workerId = 4;
    final int datacenterId = 4;
    final long startSequence = 0xFFFFFF - 20L;
    final long endSequence = 0xFFFFFF + 20L;
    final IdWorker worker =
        IdWorker.builder(workerId, datacenterId).withStartSequence(startSequence).build();

    for (long i = startSequence; i < endSequence; i++) {
      long id = worker.nextId();
      assertThat((id & WORKER_MASK) >> 12).isEqualTo(Long.valueOf(workerId));
    }
  }

  @Test
  public void testIncreasingIds() throws Exception {
    final IdWorker worker = IdWorker.builder(1, 1).build();
    long lastId = 0L;
    for (int i = 0; i < 100; i++) {
      long id = worker.nextId();
      assertThat(id).isGreaterThan(lastId);
      lastId = id;
    }
  }

  @Test
  public void testMillionIds() throws Exception {
    final IdWorker worker = IdWorker.builder(31, 31).build();
    final long startTime = System.currentTimeMillis();
    for (int i = 0; i < 1000000; i++) {
      worker.nextId();
    }
    final long endTime = System.currentTimeMillis();
    System.out.println(
        String.format(
            "generated 1000000 ids in %d ms, or %,.0f ids/second",
            (endTime - startTime), 1000000000.0 / (endTime - startTime)));
  }

  @Test
  public void testSleep() throws Exception {
    final WakingIdWorker worker = new WakingIdWorker(1, 1);
    worker.addTimestamp(2L);
    worker.addTimestamp(2L);
    worker.addTimestamp(3L);

    worker.setSequence(4095L);
    worker.nextId();
    worker.setSequence(4095L);
    worker.nextId();

    assertThat(worker.slept).isEqualTo(1);
  }

  @Test
  public void testGenerateUniqueIds() throws Exception {
    final IdWorker worker = IdWorker.builder(31, 31).build();
    final Set<Long> ids = new HashSet<>();
    final int count = 2000000;
    for (int i = 0; i < count; i++) {
      long id = worker.nextId();
      if (ids.contains(id)) {
        System.out.println(Long.toBinaryString(id));
      } else {
        ids.add(id);
      }
    }
    assertThat(ids.size()).isEqualTo(count);
  }

  @Test
  public void testGenerateIdsOver50Billion() throws Exception {
    final IdWorker worker = IdWorker.builder(0, 0).build();
    assertThat(worker.nextId()).isGreaterThan(50000000000L);
  }

  @Test
  public void testUniqueIdsBackwardsTime() throws Exception {
    final long sequenceMask = -1L ^ (-1L << 12);
    final StaticTimeWorker worker = new StaticTimeWorker(0, 0);

    // first we generate 2 ids with the same time, so that we get the
    // sequqence to 1
    assertThat(worker.getSequence()).isEqualTo(0L);
    assertThat(worker.time).isEqualTo(1L);

    final long id1 = worker.nextId();
    assertThat(id1 >> 22).isEqualTo(1L);
    assertThat(id1 & sequenceMask).isEqualTo(0L);

    assertThat(worker.getSequence()).isEqualTo(0L);
    assertThat(worker.time).isEqualTo(1L);

    final long id2 = worker.nextId();
    assertThat(id2 >> 22).isEqualTo(1L);
    assertThat(id2 & sequenceMask).isEqualTo(1L);

    // then we set the time backwards
    worker.time = 0L;
    assertThat(worker.getSequence()).isEqualTo(1L);

    try {
      worker.nextId();
      failBecauseExceptionWasNotThrown(InvalidSystemClock.class);
    } catch (InvalidSystemClock ex) {
      assertThat(worker.getSequence()).isEqualTo(1L);
    }

    worker.time = 1L;
    final long id3 = worker.nextId();
    assertThat(id3 >> 22).isEqualTo(1L);
    assertThat(id3 & sequenceMask).isEqualTo(2L);
  }

  @Test
  public void testValidUserAgent() throws Exception {
    final IdWorker worker = IdWorker.builder(1, 1).build();
    assertThat(worker.isValidUserAgent("infra-dm")).isTrue();
  }

  @Test
  public void testInvalidUserAgent() throws Exception {
    final IdWorker worker = IdWorker.builder(1, 1).build();
    assertThat(worker.isValidUserAgent("1")).isFalse();
    assertThat(worker.isValidUserAgent("1asdf")).isFalse();
  }

  @Test
  public void testGetIdInvalidUserAgent() throws Exception {
    final IdWorker worker = IdWorker.builder(1, 1).build();
    try {
      worker.getId("1");
      failBecauseExceptionWasNotThrown(InvalidUserAgentError.class);
    } catch (InvalidUserAgentError e) {
    }
  }

  @Test
  public void testGetId() throws Exception {
    final IdWorker worker = IdWorker.builder(1, 1).build();
    final long id = worker.getId("infra-dm");
    assertThat(id).isGreaterThan(0L);
  }
}
