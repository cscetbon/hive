package org.apache.cassandra.hadoop.hive.metastore;

import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;

import org.apache.cassandra.thrift.KsDef;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.cassandra.CassandraClientHolder;
import org.apache.hadoop.hive.cassandra.CassandraProxyClient;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.junit.Before;
import org.junit.Test;


public class SchemaManagerServiceTest extends MetaStoreTestBase
{
    private CassandraClientHolder cassandraClientHolder;
    private Configuration configuration;
    private SchemaManagerService schemaManagerService;
    private CassandraHiveMetaStore cassandraHiveMetaStore;
    
    @Before
    public void setupLocal() throws Exception 
    {
                
        configuration = buildConfiguration();
        if ( cassandraClientHolder == null )
            cassandraClientHolder = new CassandraProxyClient(configuration.get(SchemaManagerService.CONF_PARAM_HOST, "localhost"),
                    configuration.getInt(SchemaManagerService.CONF_PARAM_PORT, 9170),
                    configuration.getBoolean(SchemaManagerService.CONF_PARAM_FRAMED, true),
                    CassandraProxyClient.ConnectionStrategy.valueOf(configuration.get(SchemaManagerService.CONF_PARAM_CONNECTION_STRATEGY, "RANDOM"))
                            .getValue()).getConnection();
        if ( cassandraHiveMetaStore == null)
        {
            cassandraHiveMetaStore = new CassandraHiveMetaStore();
            cassandraHiveMetaStore.setConf(configuration);        
        }
        schemaManagerService = new SchemaManagerService(cassandraHiveMetaStore, configuration);             
                
    }
    
    @Test
    public void testMetaStoreSchema() throws Exception 
    {
        boolean created = schemaManagerService.createMetaStoreIfNeeded();
        assertFalse(created);
    }
    

    @Test
    public void testDiscoverUnmappedKeyspaces() throws Exception 
    {        
        cassandraClientHolder.getClient().system_add_keyspace(setupOtherKeyspace(configuration,"OtherKeyspace", false));
        // init the meta store for usage

        List<KsDef> keyspaces = schemaManagerService.findUnmappedKeyspaces();
        boolean foundCreated = false;
        // don't impose a keyspace maintenance burden. Looking for specifics is good enough
        for (KsDef ksDef : keyspaces)
        {
            if ( StringUtils.equals(ksDef.name, "OtherKeyspace") )
            {
                foundCreated = true;
                break;
            }
        }
        assertTrue(foundCreated);
    }
    

    @Test
    public void testCreateKeyspaceSchema() throws Exception
    {
        KsDef ksDef = setupOtherKeyspace(configuration,"CreatedKeyspace", false);
        cassandraClientHolder.getClient().system_add_keyspace(ksDef);
        schemaManagerService.createKeyspaceSchema(ksDef);
        List<KsDef> keyspaces = schemaManagerService.findUnmappedKeyspaces();
        
        // don't impose a keyspace maintenance burden. Looking for specifics is good enough
        for (KsDef ks : keyspaces)
        {
            if ( StringUtils.equals(ks.name, "CreatedKeyspace") )
            {
                fail("created was not synched");         
            }
        }        
    }
    
    @Test
    public void testSkipCreateOnConfig() throws Exception
    {
        KsDef ksDef = setupOtherKeyspace(configuration,"SkipCreatedKeyspace", false);
        cassandraClientHolder.getClient().system_add_keyspace(ksDef);
        
        schemaManagerService.createKeyspaceSchemasIfNeeded();
        List<KsDef> keyspaces = schemaManagerService.findUnmappedKeyspaces();
        boolean skipped = false;
        for (KsDef ks : keyspaces)
        {
            if ( StringUtils.equals(ks.name, "SkipCreatedKeyspace") )
            {
                skipped = true;
            }
        }    
        assertTrue(skipped);
    }
    
    @Test
    public void testCreateOnConfig() throws Exception
    {
        KsDef ksDef = setupOtherKeyspace(configuration,"ConfigCreatedKeyspace", false);
        cassandraClientHolder.getClient().system_add_keyspace(ksDef);

        configuration.setBoolean("cassandra.autoCreateHiveSchema", true);        
        
        schemaManagerService.createKeyspaceSchemasIfNeeded();
        List<KsDef> keyspaces = schemaManagerService.findUnmappedKeyspaces();
        for (KsDef ks : keyspaces)
        {
            if ( StringUtils.equals(ks.name, "ConfigCreatedKeyspace") )
            {
                fail("keyspace not created by configuration");
            }
        }            
    }
    
    @Test
    public void testCreateOnConfigWithMetaData() throws Exception
    {
        KsDef ksDef = setupOtherKeyspace(configuration, "ConfigCreatedKeyspaceMetaData", true);
        cassandraClientHolder.getClient().system_add_keyspace(ksDef);

        configuration.setBoolean("cassandra.autoCreateHiveSchema", true);        
        
        schemaManagerService.createKeyspaceSchemasIfNeeded();
        List<KsDef> keyspaces = schemaManagerService.findUnmappedKeyspaces();
        for (KsDef ks : keyspaces)
        {
            if ( StringUtils.equals(ks.name, "ConfigCreatedKeyspaceMetaData") )
            {
                fail("keyspace not created by configuration");
            }
        }            
        Table table = cassandraHiveMetaStore.getTable("ConfigCreatedKeyspaceMetaData", "OtherCf1");
        assertNotNull(table);
        StorageDescriptor sd = table.getSd();
        assertEquals(5,sd.getColsSize());
        for (Iterator<FieldSchema> iterator = sd.getColsIterator(); iterator.hasNext();)
        {
            FieldSchema fs = iterator.next();
            if ( StringUtils.equals(fs.getName(), "col_name_utf8"))
                assertEquals("string", fs.getType());
            if ( StringUtils.equals(fs.getName(), "col_name_bytes"))
                assertEquals("string", fs.getType());
            if ( StringUtils.equals(fs.getName(), "col_name_timeuuid"))
                assertEquals("string", fs.getType());
            if ( StringUtils.equals(fs.getName(), "col_name_long"))
                assertEquals("int", fs.getType());
            if ( StringUtils.equals(fs.getName(), "col_name_int"))
                assertEquals("bigint", fs.getType());                            
        }
    }
    

}
