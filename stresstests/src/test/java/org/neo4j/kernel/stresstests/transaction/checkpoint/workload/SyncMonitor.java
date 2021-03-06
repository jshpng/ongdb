/*
 * Copyright (c) 2018-2020 "Graph Foundation"
 * Graph Foundation, Inc. [https://graphfoundation.org]
 *
 * Copyright (c) 2002-2018 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of ONgDB Enterprise Edition. The included source
 * code can be redistributed and/or modified under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE Version 3
 * (http://www.fsf.org/licensing/licenses/agpl-3.0.html) as found
 * in the associated LICENSE.txt file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 */
package org.neo4j.kernel.stresstests.transaction.checkpoint.workload;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

class SyncMonitor implements Worker.Monitor
{
    private final AtomicBoolean stopSignal = new AtomicBoolean();
    private final AtomicLong transactionCounter = new AtomicLong();
    private final CountDownLatch stopLatch;

    SyncMonitor( int threads )
    {
        this.stopLatch = new CountDownLatch( threads );
    }

    @Override
    public void transactionCompleted()
    {
        transactionCounter.incrementAndGet();
    }

    @Override
    public boolean stop()
    {
        return stopSignal.get();
    }

    @Override
    public void done()
    {
        stopLatch.countDown();
    }

    public long transactions()
    {
        return transactionCounter.get();
    }

    public void stopAndWaitWorkers() throws InterruptedException
    {
        stopSignal.set( true );
        stopLatch.await();
    }
}
