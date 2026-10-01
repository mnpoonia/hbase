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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag(SmallTests.TAG)
public class TableAttributesTest {

  @TempDir
  Path tempDir;

  private static TableDescriptorBuilder newBuilder() {
    return TableDescriptorBuilder.newBuilder(TableName.valueOf("t1"));
  }

  @Test
  public void rejectsNonListSplitsWithIOException() {
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("SPLITS", "not-a-list");
    IOException e = assertThrows(IOException.class,
      () -> TableAttributes.apply(newBuilder(), attrs, HBaseConfiguration.create()));
    assertTrue(e.getMessage().contains("SPLITS"));
  }

  @Test
  public void rejectsUnknownAttributeKey() {
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("COMPRESION", "GZ");
    IOException e = assertThrows(IOException.class,
      () -> TableAttributes.apply(newBuilder(), attrs, HBaseConfiguration.create()));
    assertTrue(e.getMessage().contains("COMPRESION"));
  }

  @Test
  public void acceptsQuotedNumericStringForMaxFilesize() throws IOException {
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("MAX_FILESIZE", "536870912");
    TableDescriptorBuilder builder = newBuilder();
    TableAttributes.apply(builder, attrs, HBaseConfiguration.create());
    assertEquals(536870912L, builder.build().getMaxFileSize());
  }

  @Test
  public void appliesConfigurationMapAsTableValues() throws IOException {
    Map<String, Object> config = new HashMap<>();
    config.put("hbase.hstore.blockingStoreFiles", "10");
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("CONFIGURATION", config);
    TableDescriptorBuilder builder = newBuilder();
    TableAttributes.apply(builder, attrs, HBaseConfiguration.create());
    assertEquals("10", builder.build().getValue("hbase.hstore.blockingStoreFiles"));
  }

  @Test
  public void appliesMetadataMapAsTableValues() throws IOException {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("mykey", "myvalue");
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("METADATA", metadata);
    TableDescriptorBuilder builder = newBuilder();
    TableAttributes.apply(builder, attrs, HBaseConfiguration.create());
    assertEquals("myvalue", builder.build().getValue("mykey"));
  }

  @Test
  public void rejectsNonMapConfiguration() {
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("CONFIGURATION", "not-a-map");
    IOException e = assertThrows(IOException.class,
      () -> TableAttributes.apply(newBuilder(), attrs, HBaseConfiguration.create()));
    assertTrue(e.getMessage().contains("CONFIGURATION"));
  }

  @Test
  public void appliesValidSplits() throws IOException {
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("SPLITS", Collections.singletonList("1000"));
    byte[][] splits = TableAttributes.apply(newBuilder(), attrs, HBaseConfiguration.create());
    assertTrue(splits != null && splits.length == 1);
  }

  @Test
  public void appliesNumRegionsAndSplitAlgoUsingRegionSplitter() throws IOException {
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("NUMREGIONS", 3L);
    attrs.put("SPLITALGO", "HexStringSplit");
    byte[][] splits = TableAttributes.apply(newBuilder(), attrs, HBaseConfiguration.create());
    assertTrue(splits != null && splits.length == 2);
  }

  @Test
  public void splitsTakesPrecedenceOverNumRegionsAndSplitAlgo() throws IOException {
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("SPLITS", Collections.singletonList("1000"));
    attrs.put("NUMREGIONS", 3L);
    attrs.put("SPLITALGO", "HexStringSplit");
    byte[][] splits = TableAttributes.apply(newBuilder(), attrs, HBaseConfiguration.create());
    assertTrue(splits != null && splits.length == 1);
  }

  @Test
  public void rejectsNumRegionsWithoutSplitAlgo() {
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("NUMREGIONS", 3L);
    IOException e = assertThrows(IOException.class,
      () -> TableAttributes.apply(newBuilder(), attrs, HBaseConfiguration.create()));
    assertTrue(e.getMessage().contains("SPLITALGO"));
  }

  @Test
  public void readsSplitsFromSplitsFileAndRecordsPathAsTableValue() throws IOException {
    File splitsFile = tempDir.resolve("splits.txt").toFile();
    Files.write(splitsFile.toPath(), "1000\n2000\n".getBytes(StandardCharsets.UTF_8));
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("SPLITS_FILE", splitsFile.getAbsolutePath());
    TableDescriptorBuilder builder = newBuilder();
    byte[][] splits = TableAttributes.apply(builder, attrs, HBaseConfiguration.create());
    assertTrue(splits != null && splits.length == 2);
    assertEquals(splitsFile.getAbsolutePath(), builder.build().getValue("SPLITS_FILE"));
  }

  @Test
  public void rejectsMissingSplitsFile() {
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("SPLITS_FILE", "/no/such/splits-file-should-exist.txt");
    IOException e = assertThrows(IOException.class,
      () -> TableAttributes.apply(newBuilder(), attrs, HBaseConfiguration.create()));
    assertTrue(e.getMessage().contains("splits-file-should-exist.txt"));
  }

  @Test
  public void rejectsNumRegionsNotGreaterThanOne() {
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("NUMREGIONS", 1L);
    attrs.put("SPLITALGO", "HexStringSplit");
    IOException e = assertThrows(IOException.class,
      () -> TableAttributes.apply(newBuilder(), attrs, HBaseConfiguration.create()));
    assertTrue(e.getMessage().contains("NUMREGIONS"));
  }

  @Test
  public void appliesRecognizedSettersAndReturnsNoSplitsWhenNoneGiven() throws IOException {
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("READONLY", "true");
    attrs.put("REGION_REPLICATION", "2");
    TableDescriptorBuilder builder = newBuilder();
    byte[][] splits = TableAttributes.apply(builder, attrs, HBaseConfiguration.create());
    assertNull(splits);
    assertTrue(builder.build().isReadOnly());
    assertEquals(2, builder.build().getRegionReplication());
  }

  @Test
  public void rejectsInvalidValueForRecognizedAttribute() {
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("DURABILITY", "NOT_A_DURABILITY");
    IOException e = assertThrows(IOException.class,
      () -> TableAttributes.apply(newBuilder(), attrs, HBaseConfiguration.create()));
    assertTrue(e.getMessage().contains("DURABILITY"));
  }

  @Test
  public void toIntRejectsNullValue() {
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("PRIORITY", null);
    IOException e = assertThrows(IOException.class,
      () -> TableAttributes.apply(newBuilder(), attrs, HBaseConfiguration.create()));
    assertTrue(e.getMessage().contains("PRIORITY"));
  }
}
