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
package org.neo4j.causalclustering.stresstests;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import org.neo4j.diagnostics.utils.DumpUtils;
import org.neo4j.logging.Log;
import org.neo4j.util.concurrent.Futures;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.neo4j.function.Suppliers.untilTimeExpired;
import static org.neo4j.helpers.Exceptions.findCauseOrSuppressed;

public class Control
{
    private final AtomicBoolean stopTheWorld = new AtomicBoolean();
    private final BooleanSupplier keepGoing;
    private final Log log;
    private final long totalDurationMinutes;
    private Throwable failure;

    public Control( Config config )
    {
        this.log = config.logProvider().getLog( getClass() );
        long workDurationMinutes = config.workDurationMinutes();
        this.totalDurationMinutes = workDurationMinutes + config.shutdownDurationMinutes();

        BooleanSupplier notExpired = untilTimeExpired( workDurationMinutes, MINUTES );
        this.keepGoing = () -> !stopTheWorld.get() && notExpired.getAsBoolean();
    }

    public boolean keepGoing()
    {
        return keepGoing.getAsBoolean();
    }

    public synchronized void onFailure( Throwable cause )
    {
        if ( !keepGoing() && findCauseOrSuppressed( cause, t -> t instanceof InterruptedException ).isPresent() )
        {
            log.info( "Ignoring interrupt at end of test", cause );
            return;
        }

        if ( failure == null )
        {
            failure = cause;
        }
        else
        {
            failure.addSuppressed( cause );
        }
        log.error( "Failure occurred", cause );
        log.error( "Thread dump always printed on failure" );
        threadDump();
        stopTheWorld.set( true );
    }

    public synchronized void assertNoFailure()
    {
        if ( failure != null )
        {
            throw new RuntimeException( "Test failed", failure );
        }
    }

    public void awaitEnd( Iterable<Future<?>> completions ) throws InterruptedException, TimeoutException, ExecutionException
    {
        Futures.combine( completions ).get( totalDurationMinutes, MINUTES );
    }

    private void threadDump()
    {
        log.info( DumpUtils.threadDump() );
    }
}
