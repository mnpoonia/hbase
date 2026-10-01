/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hbase.newshell.hbase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.hadoop.hbase.KeepDeletedCells;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptor;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class ColumnFamilyAttributesTest {

  @Test
  public void acceptsQuotedNumericStringForVersions() throws IOException {
    Map<String, Object> familySpec = new HashMap<>();
    familySpec.put("NAME", "f1");
    familySpec.put("VERSIONS", "5");
    ColumnFamilyDescriptor descriptor = ColumnFamilyAttributes.build(familySpec);
    assertEquals(5, descriptor.getMaxVersions());
  }

  @Test
  public void rejectsUnknownCompressionWithIOException() {
    Map<String, Object> familySpec = new HashMap<>();
    familySpec.put("NAME", "f1");
    familySpec.put("COMPRESSION", "BOGUS");
    IOException e = assertThrows(IOException.class, () -> ColumnFamilyAttributes.build(familySpec));
    assertTrue(e.getMessage().contains("COMPRESSION"));
    assertTrue(e.getMessage().contains("BOGUS"));
  }

  @Test
  public void rejectsIllegalFamilyNameWithIOException() {
    Map<String, Object> familySpec = new HashMap<>();
    familySpec.put("NAME", "f:bad");
    IOException e = assertThrows(IOException.class, () -> ColumnFamilyAttributes.build(familySpec));
    assertTrue(e.getMessage().toLowerCase().contains("family") || e.getMessage().contains("f:bad")
      || e.getCause() instanceof IllegalArgumentException);
  }

  @Test
  public void appliesConfigurationMapAsFamilyConfiguration() throws IOException {
    Map<String, Object> config = new HashMap<>();
    config.put("hbase.hstore.blockingStoreFiles", "10");
    Map<String, Object> familySpec = new HashMap<>();
    familySpec.put("NAME", "f1");
    familySpec.put("CONFIGURATION", config);
    ColumnFamilyDescriptor descriptor = ColumnFamilyAttributes.build(familySpec);
    assertEquals("10", descriptor.getConfigurationValue("hbase.hstore.blockingStoreFiles"));
  }

  @Test
  public void appliesMetadataMapAsFamilyValues() throws IOException {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("mykey", "myvalue");
    Map<String, Object> familySpec = new HashMap<>();
    familySpec.put("NAME", "f1");
    familySpec.put("METADATA", metadata);
    ColumnFamilyDescriptor descriptor = ColumnFamilyAttributes.build(familySpec);
    assertEquals("myvalue", descriptor.getValue("mykey"));
  }

  @Test
  public void rejectsNonMapMetadata() {
    Map<String, Object> familySpec = new HashMap<>();
    familySpec.put("NAME", "f1");
    familySpec.put("METADATA", "not-a-map");
    IOException e = assertThrows(IOException.class, () -> ColumnFamilyAttributes.build(familySpec));
    assertTrue(e.getMessage().contains("METADATA"));
  }

  @Test
  public void appliesReplicationScope() throws IOException {
    Map<String, Object> familySpec = new HashMap<>();
    familySpec.put("NAME", "f1");
    familySpec.put("REPLICATION_SCOPE", "1");
    ColumnFamilyDescriptor descriptor = ColumnFamilyAttributes.build(familySpec);
    assertEquals(1, descriptor.getScope());
  }

  @Test
  public void appliesKeepDeletedCells() throws IOException {
    Map<String, Object> familySpec = new HashMap<>();
    familySpec.put("NAME", "f1");
    familySpec.put("KEEP_DELETED_CELLS", "TRUE");
    ColumnFamilyDescriptor descriptor = ColumnFamilyAttributes.build(familySpec);
    assertEquals(KeepDeletedCells.TRUE, descriptor.getKeepDeletedCells());
  }

  @Test
  public void appliesNewVersionBehavior() throws IOException {
    Map<String, Object> familySpec = new HashMap<>();
    familySpec.put("NAME", "f1");
    familySpec.put("NEW_VERSION_BEHAVIOR", "true");
    ColumnFamilyDescriptor descriptor = ColumnFamilyAttributes.build(familySpec);
    assertTrue(descriptor.isNewVersionBehavior());
  }

  @Test
  public void appliesMobEnabledAndThreshold() throws IOException {
    Map<String, Object> familySpec = new HashMap<>();
    familySpec.put("NAME", "f1");
    familySpec.put("IS_MOB", "true");
    familySpec.put("MOB_THRESHOLD", "102400");
    ColumnFamilyDescriptor descriptor = ColumnFamilyAttributes.build(familySpec);
    assertTrue(descriptor.isMobEnabled());
    assertEquals(102400L, descriptor.getMobThreshold());
  }

  @Test
  public void appliesDfsReplication() throws IOException {
    Map<String, Object> familySpec = new HashMap<>();
    familySpec.put("NAME", "f1");
    familySpec.put("DFS_REPLICATION", "3");
    ColumnFamilyDescriptor descriptor = ColumnFamilyAttributes.build(familySpec);
    assertEquals((short) 3, descriptor.getDFSReplication());
  }

  @Test
  public void appliesBlocksize() throws IOException {
    Map<String, Object> familySpec = new HashMap<>();
    familySpec.put("NAME", "f1");
    familySpec.put("BLOCKSIZE", "65536");
    ColumnFamilyDescriptor descriptor = ColumnFamilyAttributes.build(familySpec);
    assertEquals(65536, descriptor.getBlocksize());
  }

  @Test
  public void appliesCacheDataOnWrite() throws IOException {
    Map<String, Object> familySpec = new HashMap<>();
    familySpec.put("NAME", "f1");
    familySpec.put("CACHE_DATA_ON_WRITE", "true");
    ColumnFamilyDescriptor descriptor = ColumnFamilyAttributes.build(familySpec);
    assertTrue(descriptor.isCacheDataOnWrite());
  }

  @Test
  public void rejectsUnknownAttributeKey() {
    Map<String, Object> familySpec = new HashMap<>();
    familySpec.put("NAME", "f1");
    familySpec.put("COMPRESION", "GZ");
    IOException e = assertThrows(IOException.class, () -> ColumnFamilyAttributes.build(familySpec));
    assertTrue(e.getMessage().contains("COMPRESION"));
  }

  // Folded in from the PR's AttributeCoercionTest: numeric (not just numeric-string) values must
  // also be accepted, and a null value must surface as a shell-friendly IOException rather than a
  // raw NullPointerException.
  @Test
  public void acceptsNumericVersionsValue() throws IOException {
    Map<String, Object> familySpec = new HashMap<>();
    familySpec.put("NAME", "f1");
    familySpec.put("VERSIONS", 5);
    ColumnFamilyDescriptor descriptor = ColumnFamilyAttributes.build(familySpec);
    assertEquals(5, descriptor.getMaxVersions());
  }

  @Test
  public void acceptsQuotedNumericStringForMobThreshold() throws IOException {
    Map<String, Object> familySpec = new HashMap<>();
    familySpec.put("NAME", "f1");
    familySpec.put("MOB_THRESHOLD", "102400");
    ColumnFamilyDescriptor descriptor = ColumnFamilyAttributes.build(familySpec);
    assertEquals(102400L, descriptor.getMobThreshold());
  }

  @Test
  public void rejectsNullValueWithIOExceptionNotNullPointerException() {
    Map<String, Object> familySpec = new HashMap<>();
    familySpec.put("NAME", "f1");
    familySpec.put("VERSIONS", null);
    IOException e = assertThrows(IOException.class, () -> ColumnFamilyAttributes.build(familySpec));
    assertTrue(e.getMessage().contains("VERSIONS"));
  }
}
