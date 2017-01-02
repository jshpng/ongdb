/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
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
package org.neo4j.kernel;

import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import org.neo4j.graphdb.DependencyResolver;
import org.neo4j.kernel.impl.core.KernelPanicEventGenerator;
import org.neo4j.kernel.impl.transaction.log.PhysicalLogFiles;
import org.neo4j.kernel.impl.transaction.log.entry.LogEntryVersion;
import org.neo4j.kernel.impl.transaction.log.entry.LogHeader;
import org.neo4j.kernel.logging.DevNullLoggingService;
import org.neo4j.test.EphemeralFileSystemRule;
import org.neo4j.test.NeoStoreDataSourceRule;
import org.neo4j.test.PageCacheRule;
import org.neo4j.test.TargetDirectory;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.neo4j.helpers.collection.MapUtil.stringMap;

public class NeoStoreDataSourceTest
{
    @Rule
    public EphemeralFileSystemRule fs = new EphemeralFileSystemRule();

    @Rule
    public TargetDirectory.TestDirectory dir = TargetDirectory.testDirForTestWithEphemeralFS( fs.get(), getClass() );

    @Rule
    public NeoStoreDataSourceRule ds = new NeoStoreDataSourceRule();

    @Rule
    public PageCacheRule pageCacheRule = new PageCacheRule();

    @Test
    public void kernelHealthShouldBeHealedOnStart() throws Throwable
    {
        NeoStoreDataSource theDataSource = null;
        try
        {
            KernelHealth kernelHealth = new KernelHealth( mock( KernelPanicEventGenerator.class ),
                    DevNullLoggingService.DEV_NULL );

            theDataSource = ds.getDataSource( dir, fs.get(), pageCacheRule.getPageCache( fs.get() ),
                    stringMap(), kernelHealth );

            kernelHealth.panic( new Throwable() );

            theDataSource.start();

            kernelHealth.assertHealthy( Throwable.class );
        }
        finally
        {
            if ( theDataSource!= null )
            {
                theDataSource.stop();
                theDataSource.shutdown();
            }
        }
    }

    @Test
    public void shouldLogCorrectTransactionLogDiagnosticsForNoTransactionLogs() throws Exception
    {
        // GIVEN
        NeoStoreDataSource dataSource = neoStoreDataSourceWithLogFilesContainingLowestTxId( noLogs() );
        FakeLogger logger = new FakeLogger();

        // WHEN
        NeoStoreDataSource.Diagnostics.TRANSACTION_RANGE.dump( dataSource, logger );
        String messages = logger.getMessages();

        // THEN
        assertThat( messages, containsString( "No transactions" ) );
    }

    @Test
    public void shouldLogCorrectTransactionLogDiagnosticsForTransactionsInOldestLog() throws Exception
    {
        // GIVEN
        long logVersion = 2, prevLogLastTxId = 45;
        NeoStoreDataSource dataSource = neoStoreDataSourceWithLogFilesContainingLowestTxId(
                logWithTransactions( logVersion, prevLogLastTxId ) );
        FakeLogger logger = new FakeLogger();

        // WHEN
        NeoStoreDataSource.Diagnostics.TRANSACTION_RANGE.dump( dataSource, logger );
        String messages = logger.getMessages();

        // THEN
        assertThat( messages, containsString( "transaction " + (prevLogLastTxId + 1) ) );
        assertThat( messages, containsString( "version " + logVersion ) );
    }

    @Test
    public void shouldLogCorrectTransactionLogDiagnosticsForTransactionsInSecondOldestLog() throws Exception
    {
        // GIVEN
        long logVersion = 2, prevLogLastTxId = 45;
        NeoStoreDataSource dataSource = neoStoreDataSourceWithLogFilesContainingLowestTxId(
                logWithTransactionsInNextToOldestLog( logVersion, prevLogLastTxId ) );
        FakeLogger logger = new FakeLogger();

        // WHEN
        NeoStoreDataSource.Diagnostics.TRANSACTION_RANGE.dump( dataSource, logger );
        String messages = logger.getMessages();

        // THEN
        assertThat( messages, containsString( "transaction " + (prevLogLastTxId + 1) ) );
        assertThat( messages, containsString( "version " + (logVersion + 1) ) );
    }

    private NeoStoreDataSource neoStoreDataSourceWithLogFilesContainingLowestTxId( PhysicalLogFiles files )
    {
        DependencyResolver resolver = mock( DependencyResolver.class );
        when( resolver.resolveDependency( PhysicalLogFiles.class ) ).thenReturn( files );
        NeoStoreDataSource dataSource = mock( NeoStoreDataSource.class );
        when( dataSource.getDependencyResolver() ).thenReturn( resolver );
        return dataSource;
    }

    private PhysicalLogFiles noLogs()
    {
        PhysicalLogFiles files = mock( PhysicalLogFiles.class );
        when( files.getLowestLogVersion() ).thenReturn( -1L );
        return files;
    }

    private PhysicalLogFiles logWithTransactions( long logVersion, long headerTxId ) throws IOException
    {
        PhysicalLogFiles files = mock( PhysicalLogFiles.class );
        when( files.getLowestLogVersion() ).thenReturn( logVersion );
        when( files.hasAnyTransaction( logVersion ) ).thenReturn( true );
        when( files.versionExists( logVersion ) ).thenReturn( true );
        when( files.extractHeader( logVersion ) ).thenReturn( new LogHeader( LogEntryVersion.CURRENT.byteCode(),
                logVersion, headerTxId ) );
        return files;
    }

    private PhysicalLogFiles logWithTransactionsInNextToOldestLog( long logVersion, long prevLogLastTxId )
            throws IOException
    {
        PhysicalLogFiles files = logWithTransactions( logVersion + 1, prevLogLastTxId );
        when( files.getLowestLogVersion() ).thenReturn( logVersion );
        when( files.hasAnyTransaction( logVersion ) ).thenReturn( false );
        when( files.versionExists( logVersion ) ).thenReturn( true );
        return files;
    }
}
