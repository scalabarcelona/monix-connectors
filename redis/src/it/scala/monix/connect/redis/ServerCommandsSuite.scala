package monix.connect.redis

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.scalacheck.Gen
import org.scalatest.concurrent.Eventually
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import java.time.Instant
import java.util.Date
import scala.concurrent.duration._

class ServerCommandsSuite
  extends AnyFlatSpec with RedisIntegrationFixture with Matchers with BeforeAndAfterEach with BeforeAndAfterAll
    with Eventually {

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(4.seconds, 100.milliseconds)

  override def beforeEach(): Unit = {
    super.beforeEach()
    utfConnection.use(cmd => cmd.server.flushAll).runSyncUnsafe()
  }

  "client name" can "be set and fetch back" in {
    val name = Gen.identifier.sample.get

    utfConnection.use(cmd =>
      for {
        emptyClientName <- cmd.server.clientName
        _ <- cmd.server.setClientName(name)
        clientName <- cmd.server.clientName
      } yield {
        emptyClientName shouldBe None
        clientName shouldBe Some(name)
      }).runSyncUnsafe()
  }

  it should "clientList" in {
    val clientName = Gen.identifier.sample.get
    // clientList info example
    // "id=868 addr=172.18.0.1:57766 fd=8 name=ljmzqfvmuocjszlmmlyzkLpyimeselldcryBhn3pk age=0 idle=0 flags=N db=0 sub=0 psub=0 multi=-1 qbuf=26 qbuf-free=32742 obl=0 oll=0 omem=0 events=r cmd=client user=default"

    utfConnection.use(cmd =>
      for {
        _ <- cmd.server.setClientName(clientName)
        clientInfo <- cmd.server.clientList
      } yield {
        val fields = clientInfo.get.split(" ")
        fields.size shouldBe 19
        val name = fields.filter(_.contains("name")).headOption
        name.isDefined shouldBe true
        name.get.contains(clientName) shouldBe true
      }).runSyncUnsafe()
  }

  it should "commandCount" in {
    val commandCount = utfConnection.use(_.server.commandCount).runSyncUnsafe()
    commandCount shouldBe 204L
  }

  "config parameters" can "be added and read" in {

    utfConnection.use(cmd =>
      for {
        initialLoglevel <-  cmd.server.configSet("loglevel", "notice") >> cmd.server.configGet("loglevel")
        _ <- cmd.server.configSet("loglevel", "warning")
        updatedLoglevel <- cmd.server.configGet("loglevel")
        emptyParameter <- cmd.server.configGet("non-existing-param")
      } yield {
        initialLoglevel shouldBe Some("notice")
        updatedLoglevel shouldBe Some("warning")
        emptyParameter shouldBe None
      }).runSyncUnsafe()
  }

  "dbSize" can "return the number of keys in the selected database" in {
    val key1 = Gen.identifier.sample.get
    val key2 = Gen.identifier.sample.get
    val value = Gen.identifier.sample.get

    utfConnection.use(cmd =>
      for {
        _ <- cmd.string.set(key1, value) >> cmd.string.set(key2, value)
        dbSize <- cmd.server.dbSize
        _ <- cmd.server.flushDb
        dbSizeAfterFlush <- cmd.server.dbSize

      } yield {
        dbSize shouldBe 2
        dbSizeAfterFlush shouldBe 0
      }).runSyncUnsafe()
  }

  it should "configResetStat" in {}

  it should "flushAll" in {
    //given
    val key: K = genRedisKey.sample.get
    val value: String = genRedisValue.sample.get

    //when
    utfConnection.use(_.string.set(key, value)).runSyncUnsafe()
    val existsBeforeFlush: Boolean = utfConnection.use(_.key.exists(key)).runSyncUnsafe()

    //and
    utfConnection.use(_.server.flushAll).runSyncUnsafe()
    val existsAfterFlush: Boolean = utfConnection.use(_.key.exists(key)).runSyncUnsafe()

    //then
    existsBeforeFlush shouldEqual true
    existsAfterFlush shouldEqual false
  }

  it should "flushDb" in {
    //given
    val key: K = genRedisKey.sample.get
    val value: String = genRedisValue.sample.get

    //when
    utfConnection.use(_.string.set(key, value)).runSyncUnsafe()
    val existsBeforeFlushDb: Boolean = utfConnection.use(_.key.exists(key)).runSyncUnsafe()

    //and
    utfConnection.use(_.server.flushDb).runSyncUnsafe()
    val existsAfterFlushDb: Boolean = utfConnection.use(_.key.exists(key)).runSyncUnsafe()

    //then
    existsBeforeFlushDb shouldEqual true
    existsAfterFlushDb shouldEqual false
  }

  "info" should "return information and statistics about the server" in {
    val expectedFields = List(
      "# Server",
      "redis_version:6.0.0",
      "redis_git_sha1",
      "redis_git_dirty",
      "redis_build_id",
      "redis_mode:standalone",
      "os",
      "arch_bits",
      "multiplexing_api",
      "atomicvar_api",
      "gcc_version",
      "process_id",
      "run_id",
      "tcp_port",
      "uptime_in_seconds",
      "uptime_in_days",
      "hz",
      "configured_hz",
      "lru_clock",
      "executable",
      "config_file",
      "# Clients",
      "connected_clients",
      "client_recent_max_input_buffer",
      "client_recent_max_output_buffer",
      "blocked_clients",
      "tracking_clients",
      "clients_in_timeout_table",
      "# Memory",
      "used_memory",
      "used_memory_human",
      "used_memory_rss",
      "used_memory_rss_human",
      "used_memory_peak",
      "used_memory_peak_human",
      "used_memory_peak_perc",
      "used_memory_overhead",
      "used_memory_startup",
      "used_memory_dataset",
      "used_memory_dataset_perc",
      "allocator_allocated",
      "allocator_active",
      "allocator_resident",
      "total_system_memory",
      "total_system_memory_human",
      "used_memory_lua",
      "used_memory_lua_human",
      "used_memory_scripts",
      "used_memory_scripts_human",
      "number_of_cached_scripts",
      "maxmemory",
      "maxmemory_human",
      "maxmemory_policy",
      "allocator_frag_ratio",
      "allocator_frag_bytes",
      "allocator_rss_ratio",
      "allocator_rss_bytes",
      "rss_overhead_ratio",
      "rss_overhead_bytes",
      "mem_fragmentation_ratio",
      "mem_fragmentation_bytes",
      "mem_not_counted_for_evict",
      "mem_replication_backlog",
      "mem_clients_slaves",
      "mem_clients_normal",
      "mem_aof_buffer",
      "mem_allocator",
      "active_defrag_running",
      "lazyfree_pending_objects",
      "# Persistence",
      "loading",
      "rdb_changes_since_last_save",
      "rdb_bgsave_in_progress",
      "rdb_last_save_time",
      "rdb_last_bgsave_status",
      "rdb_last_bgsave_time_sec",
      "rdb_current_bgsave_time_sec",
      "rdb_last_cow_size",
      "aof_enabled",
      "aof_rewrite_in_progress",
      "aof_rewrite_scheduled",
      "aof_last_rewrite_time_sec",
      "aof_current_rewrite_time_sec",
      "aof_last_bgrewrite_status",
      "aof_last_write_status",
      "aof_last_cow_size",
      "module_fork_in_progress",
      "module_fork_last_cow_size",
      "# Stats",
      "total_connections_received",
      "total_commands_processed",
      "instantaneous_ops_per_sec",
      "total_net_input_bytes",
      "total_net_output_bytes",
      "instantaneous_input_kbps",
      "instantaneous_output_kbps",
      "rejected_connections",
      "sync_full",
      "sync_partial_ok",
      "sync_partial_err",
      "expired_keys",
      "expired_stale_perc",
      "expired_time_cap_reached_count",
      "expire_cycle_cpu_milliseconds",
      "evicted_keys",
      "keyspace_hits",
      "keyspace_misses",
      "pubsub_channels",
      "pubsub_patterns",
      "latest_fork_usec",
      "migrate_cached_sockets",
      "slave_expires_tracked_keys",
      "active_defrag_hits",
      "active_defrag_misses",
      "active_defrag_key_hits",
      "active_defrag_key_misses",
      "tracking_total_keys",
      "tracking_total_items",
      "unexpected_error_replies",
      "# Replication",
      "role:master",
      "connected_slaves",
      "master_replid",
      "master_replid2",
      "master_repl_offset",
      "master_repl_meaningful_offset",
      "second_repl_offset",
      "repl_backlog_active",
      "repl_backlog_size",
      "repl_backlog_first_byte_offset",
      "repl_backlog_histlen",
      "# CPU",
      "used_cpu_sys",
      "used_cpu_user",
      "used_cpu_sys_children",
      "used_cpu_user_children",
      "# Cluster",
      "cluster_enabled"
    )

    val info = utfConnection.use(_.server.info).runSyncUnsafe()
    println(info)
    expectedFields.filter(field => info.contains(field)) should contain theSameElementsAs expectedFields
  }

  it should "return specific server info section" in {
    //given
    val clientsSectionFields = List(
      "# Clients",
        "connected_clients",
        "client_recent_max_input_buffer",
        "client_recent_max_output_buffer",
        "blocked_clients",
        "tracking_clients")
    val otherNonClientFields = List(
      "# Stats", "# CPU", "# Persistence", "# Memory", "# Server", "# Modules", "# Cluster", "used_cpu_sys", "used_cpu_user", "# Keyspace")

    //when
    val sectionInfo = utfConnection.use(_.server.info("Clients")).runSyncUnsafe()

    //then
    clientsSectionFields.filter(fieldName => sectionInfo.contains(fieldName)) should contain theSameElementsAs clientsSectionFields
    otherNonClientFields.count(fieldName => sectionInfo.contains(fieldName)) shouldBe 0
  }

  "memoryUsage" should "number of bytes that a key and its value require to be stored in RAM" in {
    val stringKey: K = genRedisKey.sample.get
    val value: V = "myRedisValue"

    utfConnection.use(cmd =>
      for {
        _ <- cmd.string.set(stringKey, value)
        stringMemSize <- cmd.server.memoryUsage(stringKey)
      } yield (stringMemSize shouldBe 66)
    ).runSyncUnsafe()
  }

  "last save" should "return the date of the last time the database was saved" in {
    val stringKey: K = genRedisKey.sample.get
    val value: V = genRedisValue.sample.get

    utfConnection.use(cmd =>
      for {
        lastSave0 <- cmd.server.lastSave
        _ <- cmd.string.set(stringKey, value)
        lastSave1 <- Task.sleep(2.seconds) >> cmd.server.save >> cmd.server.lastSave
        lastSave2 <- Task.sleep(2.seconds) >> cmd.server.save >> cmd.server.lastSave
      } yield {
        lastSave1.get.before(Date.from(Instant.now())) shouldBe true
        lastSave1.get.after(lastSave0.get) shouldBe true
        lastSave2.get.after(lastSave1.get) shouldBe true
      }
    ).runSyncUnsafe()
  }


}
