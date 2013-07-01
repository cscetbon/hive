package org.apache.cassandra;

import org.apache.cassandra.contrib.utils.service.CassandraEmbeddedTestSetup;
import org.apache.cassandra.hadoop.fs.CassandraFileSystemTest;
import org.apache.cassandra.hadoop.fs.CassandraOutputStreamTest;
import org.apache.cassandra.hadoop.fs.INodeTest;
import org.apache.cassandra.hadoop.hive.metastore.CassandraHiveMetaStoreTest;
import org.apache.cassandra.hadoop.hive.metastore.MetaStorePersisterTest;
import org.apache.cassandra.hadoop.hive.metastore.SchemaManagerServiceTest;
import org.apache.hadoop.hive.cassandra.*;
import org.apache.thrift.TException;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.IOException;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        TestCassandraProxyClient.class,
        CassandraPushdownPredicateTest.class,
        CassandraClientHolderTest.class,
        INodeTest.class,
        CassandraFileSystemTest.class,
        CassandraOutputStreamTest.class,
        CassandraHiveMetaStoreTest.class,
        MetaStorePersisterTest.class,
        SchemaManagerServiceTest.class
})
public class SingleTestSuite {
  @BeforeClass
  public static void setUp() throws Exception {
    //BaseCassandraConnection.getInstance().maybeStartServer();
    new CassandraEmbeddedTestSetup();
  }
}
