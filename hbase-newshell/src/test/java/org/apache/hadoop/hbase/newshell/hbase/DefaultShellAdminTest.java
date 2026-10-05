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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@Tag(SmallTests.TAG)
public class DefaultShellAdminTest {

  private Admin admin;
  private DefaultShellAdmin shellAdmin;

  @BeforeEach
  public void setUp() {
    admin = mock(Admin.class);
    shellAdmin = new DefaultShellAdmin(admin);
  }

  @Test
  public void rejectsIllegalTableNameBeforeCallingAdmin() {
    // Colon form "ns:table" is legal; leading '.' is not.
    List<Map<String, Object>> families =
      Collections.singletonList(Collections.<String, Object> singletonMap("NAME", "f1"));
    IOException e = assertThrows(IOException.class,
      () -> shellAdmin.createTable(".bad", families, Collections.<String, Object> emptyMap()));
    assertTrue(e.getMessage().contains(".bad"));
  }

  @Test
  public void rejectsIllegalFamilyNameThroughAttributeTranslation() {
    // Family names may not contain ':'; table names of the form ns:table are fine.
    List<Map<String, Object>> families =
      Collections.singletonList(Collections.<String, Object> singletonMap("NAME", "f:bad"));
    IOException e = assertThrows(IOException.class,
      () -> shellAdmin.createTable("t1", families, Collections.<String, Object> emptyMap()));
    assertTrue(e.getCause() instanceof IllegalArgumentException
      || e.getMessage().toLowerCase().contains("family"));
  }

  @Test
  public void rejectsBadSplitsThroughAttributeTranslation() {
    List<Map<String, Object>> families =
      Collections.singletonList(Collections.<String, Object> singletonMap("NAME", "f1"));
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("SPLITS", "x");
    IOException e =
      assertThrows(IOException.class, () -> shellAdmin.createTable("t1", families, attrs));
    assertTrue(e.getMessage().contains("SPLITS"));
  }

  @Test
  public void createsValidTableViaAttributeTranslation() throws IOException {
    List<Map<String, Object>> families =
      Collections.singletonList(Collections.<String, Object> singletonMap("NAME", "f1"));
    shellAdmin.createTable("t1", families, Collections.<String, Object> emptyMap());
    verify(admin).createTable(any(TableDescriptor.class));
  }

  private TableDescriptor twoFamilyTable() {
    return TableDescriptorBuilder.newBuilder(TableName.valueOf("t"))
      .setColumnFamily(ColumnFamilyDescriptorBuilder.of("f1"))
      .setColumnFamily(ColumnFamilyDescriptorBuilder.of("f2")).build();
  }

  @Test
  public void alterMethodDeleteDropsFamily() throws IOException {
    when(admin.tableExists(TableName.valueOf("t"))).thenReturn(true);
    when(admin.getDescriptor(TableName.valueOf("t"))).thenReturn(twoFamilyTable());
    Map<String, Object> spec = new HashMap<>();
    spec.put("NAME", "f1");
    spec.put("METHOD", "delete");

    shellAdmin.alterTable("t", Collections.singletonList(spec));

    ArgumentCaptor<TableDescriptor> captor = ArgumentCaptor.forClass(TableDescriptor.class);
    verify(admin).modifyTable(captor.capture());
    assertEquals(1, captor.getValue().getColumnFamilyCount());
    assertTrue(captor.getValue().hasColumnFamily(Bytes.toBytes("f2")));
  }

  @Test
  public void alterRejectsUnknownMethodAndDeleteWithExtraAttributes() throws IOException {
    when(admin.tableExists(TableName.valueOf("t"))).thenReturn(true);
    when(admin.getDescriptor(TableName.valueOf("t"))).thenReturn(twoFamilyTable());
    Map<String, Object> bogus = new HashMap<>();
    bogus.put("NAME", "f1");
    bogus.put("METHOD", "add");
    assertThrows(IOException.class,
      () -> shellAdmin.alterTable("t", Collections.singletonList(bogus)));
    Map<String, Object> extra = new HashMap<>();
    extra.put("NAME", "f1");
    extra.put("METHOD", "delete");
    extra.put("TTL", 5L);
    assertThrows(IOException.class,
      () -> shellAdmin.alterTable("t", Collections.singletonList(extra)));
  }
}
